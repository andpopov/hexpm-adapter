package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class MetadataConfigTest {
    @Test
    public void readApp() throws IOException {
        MetadataConfig mconf = MetadataConfig.create(ResourceUtil.asPath("tarball/metadata.config"));

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
        MetadataConfig mconf = MetadataConfig.create(ResourceUtil.asPath("tarball/metadata.config"));

        String version = mconf.getVersion();
        MatcherAssert.assertThat(
            version,
            new StringContains(
                "2.0.0"
            )
        );
    }
}
