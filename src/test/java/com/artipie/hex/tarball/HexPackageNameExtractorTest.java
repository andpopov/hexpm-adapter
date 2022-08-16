package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public class HexPackageNameExtractorTest {
    @Test
    public void readHexPackageName() throws IOException {
        Optional<String> packageName = HexPackageNameExtractor.extract(Files.newInputStream(ResourceUtil.asPath("tarball/decimal-2.0.0.tar")));

        MatcherAssert.assertThat(
            packageName.orElse(""),
            new StringContains(
                "decimal-2.0.0.tar"
            )
        );
    }

}