/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.tarball;

import com.artipie.hex.ResourceUtil;
import org.apache.commons.io.IOUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;

public class TarReaderTest {
    @Test
    public void readHexPackageName() throws IOException {
        final byte[] tarContent = IOUtils.toByteArray(Files.newInputStream(ResourceUtil.asPath("tarballs/decimal-2.0.0.tar")));
        MatcherAssert.assertThat(
            new TarReader(tarContent)
                .readEntryContent("metadata.config")
                .isPresent(),
            new IsEqual<>(true)
        );
    }

}
