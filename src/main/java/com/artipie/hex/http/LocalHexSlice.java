/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

public class LocalHexSlice implements Slice {

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * New local Hex slice.
     *
     * @param storage Repository storage
     */
    public LocalHexSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line, final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        System.out.println("line = " + line);
        System.out.println("headers = " + headers);

        if (line.contains("/tarballs/decimal-2.0.0.tar")) {
            System.out.println("inside tarballs");
            try {
                return
                    new AsyncResponse(
                        this.storage.value(new Key.From("binary", "decimal-2.0.0.tar"))
                            .thenApply(
                                value -> new RsFull(
                                    RsStatus.OK,
                                    new Headers.From(
                                        new Header("Content-Type", "application/octet-stream")
                                        ),
                                    value
                                )
                            )
                    );
            } catch (Exception e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                throw new RuntimeException("ERROR in /tarballs/ = ", e);
            }
        } else if (line.contains("/packages/decimal")) {
            System.out.println("inside packages");
            try {
                return
                    new AsyncResponse(
                        this.storage.value(new Key.From("binary", "decimal"))
                            .thenApply(
                                value -> new RsFull(
                                    RsStatus.OK,
                                    new Headers.From(
                                        new Header("Content-Type", "application/octet-stream")
                                    ),
                                    value
                                )
                            )
                    );
            } catch (Exception e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                throw new RuntimeException(String.format("ERROR in /packages/ = %s", e.getMessage()), e);
            }
        } else {
            return new RsWithStatus(RsStatus.BAD_REQUEST);
        }
    }
}
