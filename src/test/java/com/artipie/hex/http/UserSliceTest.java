/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.http.Slice;
import com.artipie.http.hm.RsHasStatus;
import com.artipie.http.hm.SliceHasResponse;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link UserSlice}.
 * @since 0.2
 */
class UserSliceTest {

    /**
     * User slice.
     */
    private Slice userslice;

    @BeforeEach
    void init() {
        this.userslice = new UserSlice();
    }

    @Test
    void responseNoContent() {
        MatcherAssert.assertThat(
            this.userslice,
            new SliceHasResponse(
                new RsHasStatus(RsStatus.NO_CONTENT),
                new RequestLine(RqMethod.GET, "/users/artipie")
            )
        );
    }
}
