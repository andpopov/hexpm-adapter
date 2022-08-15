package com.artipie.hex.tarball;

import com.metadave.etp.ETP;
import com.metadave.etp.rep.ETPBinary;
import com.metadave.etp.rep.ETPTerm;
import com.metadave.etp.rep.ETPTuple;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parses metadata.config file of erlang/elixir tarball's content.
 * Allows to extract 'app' and 'version' values of parsed tuples.
 * Expected following format of erlang tuple-expressions:
 * <pre>
 *  {<<"app">>,<<"decimal">>}.
 *  {<<"version">>,<<"2.0.0">>}.
 *  </pre>
 */
final public class MetadataConfig {
    private final List<Tuple> tuples;

    private MetadataConfig(List<Tuple> terms) {
        this.tuples = terms;
    }

    /**
     * Get all tuples of metadata.config
     * @return all tuples
     */
    public List<Tuple> getTuples() {
        return Collections.unmodifiableList(tuples);
    }

    /**
     * Get second value of tuple by first value as 'app'
     * @return second value of tuple
     */
    public String getApp() {
        return getAsString("app");
    }

    /**
     * Get second value of tuple by first value as 'version'
     * @return second value of tuple
     */
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

    /**
     * Parses metadata.config specified by path
     */
    public static MetadataConfig create(Path path) throws IOException {
        Objects.requireNonNull(path);
        return create(Files.newInputStream(path));
    }

    /**
     * Parses metadata.config specified by byte array
     */
    public static MetadataConfig create(byte[] bytes) {
        Objects.requireNonNull(bytes);
        return create(new ByteArrayInputStream(bytes));
    }

    /**
     * Parses metadata.config specified by InputStream
     */
    public static MetadataConfig create(InputStream is) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
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
        } catch (IOException | ETP.ParseException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * ETPTuple wrapper
     */
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

    /**
     * ETPBinary wrapper
     */
    private static class Binary {
        final ETPBinary value;

        public Binary(ETPBinary value) {
            this.value = value;
        }

        public BinaryValue getBinaryValue(int pos) {
            return new BinaryValue((ETPBinary.ETPBinaryValue<?>) value.getValue(pos));
        }
    }

    /**
     * TPBinary.ETPBinaryValue wrapper
     */
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