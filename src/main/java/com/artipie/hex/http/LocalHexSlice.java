/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithHeaders;
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
     * New local {@code GET} slice.
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
        if (line.contains("/tarballs/my_artifact-0.4.0.tar")) {
            //todo
        }
        if (line.contains("/packages/my_artifact")) {
            return
                new RsWithBody(
                    new RsWithHeaders(
                        new RsWithStatus(RsStatus.OK),
                        new Headers.From("Content-Type", "application/json")
                    ),
                    ByteBuffer.wrap(""" 
                        {
                            "configs": {
                                "erlang.mk": "dep_my_artifact = hex 0.4.0",
                                "mix.exs": "{:my_artifact, \\"~> 0.4.0\\"}",
                                "rebar.config": "{my_artifact, \\"0.4.0\\"}"
                            },
                            "name": "my_artifact",
                            "releases": [
                                {
                                    "has_docs": false,
                                    "inserted_at": "2021-03-19T15:48:17.792564Z",
                                    "url": "https://hex.pm/api/packages/my_artifact/releases/0.4.0",
                                    "version": "0.4.0"
                                }
                            ],
                            "repository": "my_repo"
                        }
                        """.getBytes())
                );
        } else {
            return new RsWithStatus(RsStatus.BAD_REQUEST);
        }
    }
}
