/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.hex.http.HexSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;

public class TestServerForDebugging {
    private static final Vertx VERTX = Vertx.vertx();
    private static VertxSliceServer server;
    private static Storage storage;
    private static int port;

    public static void main(String[] args) {
        storage = new InMemoryStorage();
        server = new VertxSliceServer(
            TestServerForDebugging.VERTX,
            new LoggingSlice(new HexSlice(storage, Permissions.FREE, Authentication.ANONYMOUS)),
            8080
        );
        port = server.start();
        System.out.println("port = " + port);
    }
}