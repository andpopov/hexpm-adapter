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
import com.jcabi.log.Logger;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UploadSlice}.
 * @since 0.1
 */
class UploadSliceTest {
    /**
     * Repository storage.
     */
    private Storage storage;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
    }

    @Test
    void replaceTrue() throws IOException, ExecutionException, InterruptedException {
        this.runScenario(true);
        MatcherAssert.assertThat(
            "true",
            this.storage.exists(new Key.From("packages", "decimal")).get()
        );
        MatcherAssert.assertThat(
            "true",
            this.storage.exists(new Key.From("tarballs", "decimal-2.0.0.tar")).get()
        );
    }

    @Test
    void replaceFalse() throws IOException, ExecutionException, InterruptedException {
        this.runScenario(false);
        MatcherAssert.assertThat(
            "true",
            this.storage.exists(new Key.From("packages", "decimal")).get()
        );
        MatcherAssert.assertThat(
            "true",
            this.storage.exists(new Key.From("tarballs", "decimal-2.0.0.tar")).get()
        );
    }

    private void runScenario(final boolean replace)
        throws IOException, ExecutionException, InterruptedException {
        final String line = String.format("POST /publish?replace=%s HTTP_1_1", replace);
        final Map<String, String> headers = new HashMap<>();
        headers.put("user-agent", "Hex/1.0.1 (Elixir/1.13.4) (OTP/24.1.7)");
        headers.put("accept", "application/vnd.hex+erlang");
        final AsyncResponse response = (AsyncResponse) new UploadSlice(this.storage)
            .response(
                line,
                headers.entrySet(),
                new Content.From(
                    Files.readAllBytes(new ResourceUtil("tarballs/decimal-2.0.0.tar").asPath())
                )
            );
        response.send(
            (status, reqHeaders, body) -> {
                Logger.debug(this, "Response:");
                Logger.debug(this, "status: %s", status);
                Logger.debug(this, "headers:");
                reqHeaders.forEach(h -> Logger.debug(this, "%s", h));
                return CompletableFuture.completedFuture(null);
            }
        ).toCompletableFuture().get();
    }
}
