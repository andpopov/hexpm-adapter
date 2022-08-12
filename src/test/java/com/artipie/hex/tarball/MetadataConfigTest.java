package com.artipie.hex.tarball;

import com.metadave.etp.ETP;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class MetadataConfigTest {
    @Test
    public void readApp() throws IOException, ETP.ParseException {
        MetadataConfig mconf = MetadataConfig.create(asPath("tarball/metadata.config"));

        String app = mconf.getApp();
        MatcherAssert.assertThat(
            app,
            new StringContains(
                "decimal"
            )
        );
    }

    @Test
    public void readVersion() throws IOException, ETP.ParseException {
        MetadataConfig mconf = MetadataConfig.create(asPath("tarball/metadata.config"));

        String version = mconf.getVersion();
        MatcherAssert.assertThat(
            version,
            new StringContains(
                "2.0.0"
            )
        );
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
