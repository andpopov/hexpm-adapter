/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.hex.ResourceUtil;
import com.artipie.http.async.AsyncResponse;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.jcabi.log.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class UploadSliceTest {
    private Storage storage;

    @BeforeEach
    private void init() {
        storage = new InMemoryStorage();
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

        AsyncResponse response = (AsyncResponse) new UploadSlice(storage)
            .response(line,
                reqHeaders.entrySet(),
                new Content.From(Files.readAllBytes(ResourceUtil.asPath("tarballs/decimal-2.0.0.tar"))));
        response.send((status, headers, body) -> {
            Logger.debug(this, "Response:");
            Logger.debug(this, "status: %s", status);
            Logger.debug(this, "headers:");
            headers.forEach(h -> Logger.debug(this, "%s", h));
            return CompletableFuture.completedFuture(null);
        }).toCompletableFuture().get();
    }
}