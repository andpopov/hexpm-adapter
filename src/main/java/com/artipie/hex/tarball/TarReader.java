package com.artipie.hex.tarball;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.*;
import java.util.Optional;

/**
 * Allows to read content of named-entries from tar-source
 */
public class TarReader {
    final private byte[] bytes;
    public TarReader(final byte[] bytes) {
        this.bytes = bytes;
    }

    /**
     * Reads content of entry stored in tar-archive
     */
    public Optional<byte[]> readEntryContent(String entryName) {
        byte[] content = null;
        try {
            try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes)) {
                TarArchiveInputStream tar = new TarArchiveInputStream(bis);
                TarArchiveEntry entry;
                while ((entry = (TarArchiveEntry) tar.getNextEntry()) != null) {
                    if(entryName.equals(entry.getName())) {
                        ByteArrayOutputStream entryContent = new ByteArrayOutputStream();
                        int len;
                        byte[] buf = new byte[1024];
                        while((len = tar.read(buf)) != -1) {
                            entryContent.write(buf, 0, len);
                        }
                        content = entryContent.toByteArray();
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(String.format("Cannot read content of '%s' from tar-archive", entryName), e);
        }
        return Optional.ofNullable(content);
    }
}