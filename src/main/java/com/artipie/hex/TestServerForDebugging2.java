/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.hex.http.HexSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class TestServerForDebugging2 {
    private static final Vertx VERTX = Vertx.vertx();
    private static VertxSliceServer server;
    private static Storage storage;
    private static int port;

    public static void main(String[] args) {
        storage = new InMemoryStorage();
        server = new VertxSliceServer(
            TestServerForDebugging2.VERTX,
            new LoggingSlice(new HexSlice(storage, Permissions.FREE, Authentication.ANONYMOUS)),
            8080
        );
        port = server.start();
        System.out.println("port = " + port);
        addFilesTo("binary", storage, new Key.From("binary"));
//        addFilesTo("responses", storage, new Key.From("responses"));
    }

    public static void addFilesTo(String name, final Storage storage, final Key base) {
        final Storage resources = new FileStorage(asPath(name));
        resources.list(Key.ROOT).thenCompose(
            keys -> CompletableFuture.allOf(
                keys.stream().map(Key::string).map(
                    item -> resources.value(new Key.From(item)).thenCompose(
                        content -> storage.save(new Key.From(base, item), content)
                    )
                ).toArray(CompletableFuture[]::new)
            )
        ).join();
    }

    /**
     * Obtains resources from context loader.
     *
     * @return File path
     */
    public static Path asPath(String name) {
        try {
            return Paths.get(
                Objects.requireNonNull(
                    Thread.currentThread().getContextClassLoader().getResource(name)
                ).toURI()
            );
        } catch (final URISyntaxException ex) {
            throw new IllegalStateException("Failed to obtain test recourse", ex);
        }
    }

}
