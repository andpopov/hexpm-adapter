/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MetadataConfigTest {
    @Test
    public void readApp() throws IOException {
        MetadataConfig mconf = MetadataConfig.create(ResourceUtil.asPath("tarballs/metadata.config"));

        String app = mconf.getApp();
        MatcherAssert.assertThat(
            app,
            new StringContains(
                "decimal"
            )
        );
    }

    @Test
    public void readVersion() throws IOException {
        MetadataConfig mconf = MetadataConfig.create(ResourceUtil.asPath("tarballs/metadata.config"));

        String version = mconf.getVersion();
        MatcherAssert.assertThat(
            version,
            new StringContains(
                "2.0.0"
            )
        );
    }
}
