package com.artipie.hex.tarball;

import com.metadave.etp.ETP;
import com.metadave.etp.rep.ETPBinary;
import com.metadave.etp.rep.ETPTerm;
import com.metadave.etp.rep.ETPTuple;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

final public class MetadataConfig {
    private final List<Tuple> tuples;

    private MetadataConfig(List<Tuple> terms) {
        this.tuples = terms;
    }

    public List<Tuple> getTuples() {
        return Collections.unmodifiableList(tuples);
    }

    public String getApp() {
        return getAsString("app");
    }

    public String getVersion() {
        return getAsString("version");
    }

    private String getAsString(String name) {
        String app = null;
        for(Tuple tuple: tuples) {
            if(tuple.isBinary(0) && tuple.asBinary(0).getBinaryValue(0).isBinString() && name.equals(tuple.asBinary(0).getBinaryValue(0).asBinString().getValue())) {
                if(tuple.isBinary(1)) {
                    app = tuple.asBinary(1).getBinaryValue(0).asBinString().getValue();
                }
                break;
            }
        }
        return app;
    }

    public static MetadataConfig create(Path path) throws IOException, ETP.ParseException {
        Objects.requireNonNull(path);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
            List<Tuple> tuples = new ArrayList<>();
            StringBuilder expr = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("%")) {
                    if (line.endsWith(".")) {
                        expr.append(line);
                        ETPTerm<?> term = ETP.parse(expr.toString());
                        if(term instanceof ETPTuple) {
                            tuples.add(new Tuple((ETPTuple) term));
                        }
                        expr.setLength(0);
                    } else {
                        expr.append(line);
                    }
                }
            }
            return new MetadataConfig(tuples);
        }
    }

    private static class Tuple {
        final ETPTuple value;

        public Tuple(ETPTuple tuple) {
            this.value = tuple;
        }

        public boolean isBinary(int pos) {
            ETPTerm<?> term = value.getValue(pos);
            return term instanceof ETPBinary;
        }

        public Binary asBinary(int pos) {
            return  new Binary((ETPBinary)(value.getValue(pos)));
        }
    }

    private static class Binary {
        final ETPBinary value;

        public Binary(ETPBinary value) {
            this.value = value;
        }

        public BinaryValue getBinaryValue(int pos) {
            return new BinaryValue((ETPBinary.ETPBinaryValue<?>) value.getValue(pos));
        }
    }

    private static class BinaryValue {
        private final ETPBinary.ETPBinaryValue<?> value;
        public BinaryValue(ETPBinary.ETPBinaryValue<?> value) {
            this.value = value;
        }

        public boolean isBinInt() {
            return value instanceof ETPBinary.BinInt;
        }

        public ETPBinary.BinInt asBinInt() {
            return (ETPBinary.BinInt) value;
        }

        public boolean isBinString() {
            return value instanceof ETPBinary.BinString;
        }

        public ETPBinary.BinString asBinString() {
            return (ETPBinary.BinString) value;
        }
    }
}