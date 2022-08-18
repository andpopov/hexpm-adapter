/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Storage;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import java.nio.ByteBuffer;
import java.util.Map;
import org.reactivestreams.Publisher;

/**
 * This slice returns content as bytes by Key from request path.
 */
public class UserSlice implements Slice {
    /**
     * Ctor.
     * @param storage Repository storage.
     */
    public UserSlice(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Path to users.
     */
    static final String USERS = "/users/";


    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        return new RsWithStatus(RsStatus.NO_CONTENT);
    }
}
