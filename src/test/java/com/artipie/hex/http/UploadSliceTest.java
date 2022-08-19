/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.fs.FileStorage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.http.async.AsyncResponse;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class UploadSliceTest {
    private Storage storage;

    @BeforeEach
    private void init() {
        storage = new InMemoryStorage();
        addFilesTo("binary", storage, new Key.From("binary"));
    }

    @Test
    public void replaceTrue() throws IOException, ExecutionException, InterruptedException {
        runScenario(true);
        MatcherAssert.assertThat("true", storage.exists(new Key.From("packages", "decimal")).get());
        MatcherAssert.assertThat("true", storage.exists(new Key.From("tarballs", "decimal-2.0.0.tar")).get());
    }

    @Test
    public void replaceFalse() throws IOException, ExecutionException, InterruptedException {
        runScenario(false);
        MatcherAssert.assertThat("true", storage.exists(new Key.From("packages", "decimal")).get());
        MatcherAssert.assertThat("true", storage.exists(new Key.From("tarballs", "decimal-2.0.0.tar")).get());
    }

    public void runScenario(boolean replace) throws IOException, ExecutionException, InterruptedException {
        String line = String.format("POST /publish?replace=%s HTTP_1_1", replace);
        Map<String, String> reqHeaders = new HashMap<>();
        reqHeaders.put("user-agent", "Hex/1.0.1 (Elixir/1.13.4) (OTP/24.1.7)");
        reqHeaders.put("accept", "application/vnd.hex+erlang");
        reqHeaders.put("content-type", "application/octet-stream");

        AsyncResponse response = (AsyncResponse) new UploadSlice(storage).response(line, reqHeaders.entrySet(), new Content.From(Files.readAllBytes(asPath("binary/decimal-2.0.0.tar"))));
        response.send((status, headers, body) -> {
            System.out.println("\nResponse:");
            System.out.println("status:" + status);
            System.out.println("headers:");
            headers.forEach(System.out::println);
            return CompletableFuture.completedFuture(null);
        }).toCompletableFuture().get();
    }

    public static void addFilesTo(String name, final Storage storage, final Key base) {
        final Storage resources = new FileStorage(asPath(name));
        resources.list(Key.ROOT).thenCompose(keys -> CompletableFuture.allOf(keys.stream().map(Key::string).map(item -> resources.value(new Key.From(item)).thenCompose(content -> storage.save(new Key.From(base, item), content))).toArray(CompletableFuture[]::new))).join();
    }

    /**
     * Obtains resources from context loader.
     *
     * @return File path
     */
    public static Path asPath(String name) {
        try {
            return Paths.get(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(name)).toURI());
        } catch (final URISyntaxException ex) {
            throw new IllegalStateException("Failed to obtain test recourse", ex);
        }
    }
}