/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.ContentIs;
import com.artipie.hex.ResourceUtil;
import com.artipie.http.Headers;
import com.artipie.http.Slice;
import com.artipie.http.headers.ContentLength;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import java.io.IOException;
import java.nio.file.Files;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test for {@link UploadSlice}.
 * @since 0.1
 */
class UploadSliceTest {
    /**
     * Test storage.
     */
    private Storage storage;

    /**
     * UploadSlice.
     */
    private Slice slice;

    @BeforeEach
    void init() {
        this.storage = new InMemoryStorage();
        this.slice = new UploadSlice(this.storage);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void publishAndReplace(final boolean replace) throws IOException {
        final byte[] tar = Files.readAllBytes(
            new ResourceUtil("tarballs/decimal-2.0.0.tar").asPath()
        );
        MatcherAssert.assertThat(
            "Wrong response status, CREATED is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, String.format("/publish?replace=%s", replace)),
                new Headers.From(new ContentLength(tar.length)),
                new Content.From(tar)
            )
        );
        MatcherAssert.assertThat(
            "Package was not saved in storage",
            this.storage.value(new Key.From("packages/decimal")).join(),
            new ContentIs(Files.readAllBytes(new ResourceUtil("packages/decimal").asPath()))
        );
        MatcherAssert.assertThat(
            "Tarball was not saved in storage",
            this.storage.value(new Key.From("tarballs", "decimal-2.0.0.tar")).join(),
            new ContentIs(
                Files.readAllBytes(
                    new ResourceUtil("tarballs/decimal-2.0.0.tar").asPath()
                )
            )
        );
    }

    @Test
    void publishExistedPackageReplaceFalse() throws IOException {
        final byte[] tar = Files.readAllBytes(
            new ResourceUtil("tarballs/decimal-2.0.0.tar").asPath()
        );
        MatcherAssert.assertThat(
            "Wrong response status for the first upload, CREATED is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.CREATED),
                new RequestLine(RqMethod.POST, "/publish?replace=false"),
                new Headers.From(new ContentLength(tar.length)),
                new Content.From(tar)
            )
        );
        MatcherAssert.assertThat(
            "Wrong response status for a package that already exists, INTERNAL_ERROR is expected",
            this.slice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.INTERNAL_ERROR),
                new RequestLine(RqMethod.POST, "/publish?replace=false"),
                new Headers.From(new ContentLength(tar.length)),
                new Content.From(tar)
            )
        );
    }

    @Test
    void returnsBadRequestOnIncorrectRequest() {
        MatcherAssert.assertThat(
            "Wrong response status, BAD_REQUEST is expected",
            new UploadSlice(this.storage),
            new SliceHasResponse(
                new RsHasStatus(RsStatus.BAD_REQUEST),
                new RequestLine(RqMethod.POST, "/publish")
            )
        );
    }
}
