package com.artipie.hex.tarball;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.util.Optional;

/**
 * Extracts content of metadata.config file from tar-source
 */
public class HexPackageNameExtractor {
    final private static String METADATA_CONFIG = "metadata.config";

    final private InputStream tar;
    private HexPackageNameExtractor(final InputStream tar) {
        this.tar = tar;
    }

    /**
     * Extracts of metadata.config content from tar provided by byte-array source
     */
    public static Optional<String> extract(final byte[] bytes) {
        return extract(new ByteArrayInputStream(bytes));
    }

    /**
     * Extracts of metadata.config content from tar provided by {@link InputStream} source
     */
    public static Optional<String> extract(final InputStream tar) {
        return new HexPackageNameExtractor(tar)
            .readConfig()
            .map(bytes -> {
                MetadataConfig config = MetadataConfig.create(bytes);
                return config.getApp() +
                    '-' +
                    config.getVersion() +
                    ".tar.gz";
            });
    }

    /**
     * Reads content of metadata.config file stored in tar-archive
     */
    private Optional<byte[]> readConfig() {
        byte[] bytes = null;
        try {
            try (BufferedInputStream bis = new BufferedInputStream(tar)) {
                TarArchiveInputStream tar = new TarArchiveInputStream(bis);
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                    if(METADATA_CONFIG.equals(entry.getName())) {
                        ByteArrayOutputStream entryContent = new ByteArrayOutputStream();
                        int len;
                        byte[] buf = new byte[1024];
                        while((len = tar.read(buf)) != -1) {
                            entryContent.write(buf, 0, len);
                        }
                        bytes = entryContent.toByteArray();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Optional.ofNullable(bytes);
    }
}