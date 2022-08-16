package com.artipie.hex;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

final public class ResourceUtil {
    private ResourceUtil() {
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
