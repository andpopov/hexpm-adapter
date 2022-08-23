/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.ArtipieException;
import com.metadave.etp.ETP;
import com.metadave.etp.rep.ETPBinary;
import com.metadave.etp.rep.ETPTerm;
import com.metadave.etp.rep.ETPTuple;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses metadata.config file of erlang/elixir tarball's content.
 * Allows to extract 'app' and 'version' values of parsed tuples.
 * Expected following format of erlang tuple-expressions:
 * <pre>
 *  {<<"app">>,<<"decimal">>}.
 *  {<<"version">>,<<"2.0.0">>}.
 *  </pre>
 *
 * @since 0.1
 */
@SuppressWarnings("PMD.ProhibitPublicStaticMethods")
public final class MetadataConfig {
    /**
     * List of Erlang tuples.
     */
    private final List<Tuple> tuples;

    /**
     * Ctor.
     *
     * @param terms Erlang terms
     */
    private MetadataConfig(final List<Tuple> terms) {
        this.tuples = terms;
    }

    /**
     * Get second value of tuple by first value as 'app'.
     *
     * @return Second value of tuple
     */
    public String getApp() {
        return this.getAsString("app");
    }

    /**
     * Get second value of tuple by first value as 'version'.
     *
     * @return Second value of tuple
     */
    public String getVersion() {
        return this.getAsString("version");
    }

    /**
     * Parses metadata.config specified by byte array.
     *
     * @param bytes MetadataConfig in bytes
     * @return Instance {@link MetadataConfig}
     */
    public static MetadataConfig create(final byte[] bytes) {
        Objects.requireNonNull(bytes);
        return create(new ByteArrayInputStream(bytes));
    }

    /**
     * Parses metadata.config specified by path.
     *
     * @param path Path to file
     * @return Instance {@link MetadataConfig}
     * @throws IOException if file not exist
     */
    static MetadataConfig create(final Path path) throws IOException {
        Objects.requireNonNull(path);
        return create(Files.newInputStream(path));
    }

    /**
     * Parses metadata.config specified by InputStream.
     *
     * @param input InputStream with metadata.config
     * @return Instance {@link MetadataConfig}
     * @checkstyle NestedIfDepthCheck (50 lines)
     */
    static MetadataConfig create(final InputStream input) {
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(input))) {
                final List<Tuple> tuples = new ArrayList<>(10);
                final StringBuilder expr = new StringBuilder();
                String line = reader.readLine();
                while (line != null) {
                    line = line.trim();
                    if (!line.isEmpty() && line.charAt(0) != '%') {
                        if (line.endsWith(".")) {
                            expr.append(line);
                            final ETPTerm<?> term = ETP.parse(expr.toString());
                            if (term instanceof ETPTuple) {
                                tuples.add(new Tuple((ETPTuple) term));
                            }
                            expr.setLength(0);
                        } else {
                            expr.append(line);
                        }
                    }
                    line = reader.readLine();
                }
                return new MetadataConfig(tuples);
            }
        } catch (final IOException | ETP.ParseException exception) {
            throw new ArtipieException("Cannot parse content of metadata.config", exception);
        }
    }

    /**
     * Get tuple as string.
     *
     * @param name Tuple name
     * @return Tuple as String
     */
    private String getAsString(final String name) {
        String app = null;
        for (final Tuple tuple : this.tuples) {
            if (tuple.isBinary(0)
                && tuple.asBinary(0).getBinaryValue(0).isBinString()
                && name.equals(tuple.asBinary(0).getBinaryValue(0).asBinString().getValue())
            ) {
                if (tuple.isBinary(1)) {
                    app = tuple.asBinary(1).getBinaryValue(0).asBinString().getValue();
                }
                break;
            }
        }
        return app;
    }

    /**
     * ETPTuple wrapper.
     * @since 0.1
     */
    private static class Tuple {
        /**
         * Value of tuple.
         */
        private final ETPTuple value;

        /**
         * Ctor.
         * @param tuple Erlang tuple
         */
        Tuple(final ETPTuple tuple) {
            this.value = tuple;
        }

        /**
         * Check on binary.
         * @param pos Position
         * @return True if it is Binary
         */
        public boolean isBinary(final int pos) {
            return this.value.getValue(pos) instanceof ETPBinary;
        }

        /**
         * Get Binary by position.
         * @param pos Position
         * @return Instance of {@link Binary}
         */
        public Binary asBinary(final int pos) {
            return new Binary((ETPBinary) (this.value.getValue(pos)));
        }
    }

    /**
     * ETPBinary wrapper.
     * @since 0.1
     */
    private static class Binary {
        /**
         * Value of Binary.
         */
        private final ETPBinary value;

        /**
         * Ctor.
         * @param value Erlang Binary
         */
        Binary(final ETPBinary value) {
            this.value = value;
        }

        /**
         * Get BinaryValue by position.
         * @param pos Position
         * @return Instance of {@link BinaryValue}
         */
        public BinaryValue getBinaryValue(final int pos) {
            return new BinaryValue((ETPBinary.ETPBinaryValue<?>) this.value.getValue(pos));
        }
    }

    /**
     * TPBinary.ETPBinaryValue wrapper.
     * @since 0.1
     */
    private static class BinaryValue {
        /**
         * Value of BinaryValue.
         */
        private final ETPBinary.ETPBinaryValue<?> value;

        /**
         * Ctor.
         * @param value Erlang BinaryValue wrapped by Erlang Binary
         */
        BinaryValue(final ETPBinary.ETPBinaryValue<?> value) {
            this.value = value;
        }

        /**
         * Check on Erlang BinString.
         * @return True if it is BinString
         */
        public boolean isBinString() {
            return this.value instanceof ETPBinary.BinString;
        }

        /**
         * Cast to BinString.
         * @return Instance of {@link BinaryValue}
         */
        public ETPBinary.BinString asBinString() {
            return (ETPBinary.BinString) this.value;
        }
    }
}
