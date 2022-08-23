/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import java.io.IOException;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link MetadataConfigTest}.
 * @since 0.1
 */
public class MetadataConfigTest {
    @Test
    void readApp() throws IOException {
        final MetadataConfig mconf =
            MetadataConfig.create(new ResourceUtil("metadata/metadata.config").asPath());
        MatcherAssert.assertThat(
            mconf.getApp(),
            new StringContains("decimal")
        );
    }

    @Test
    void readVersion() throws IOException {
        final MetadataConfig mconf =
            MetadataConfig.create(new ResourceUtil("metadata/metadata.config").asPath());
        MatcherAssert.assertThat(
            mconf.getVersion(),
            new StringContains("2.0.0")
        );
    }
}
