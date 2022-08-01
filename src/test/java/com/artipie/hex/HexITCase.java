/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex;

import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.hex.http.HexSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.text.StringContainsInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@EnabledOnOs({OS.LINUX, OS.MAC})
public class HexITCase {

    /**
     * Vertx instance.
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * Test user.
     */
    private static final Pair<String, String> USER = new ImmutablePair<>("Aladdin", "openSesame");

    /**
     * Logger for output from testcontainer.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HexITCase.class);

    /**
     * Temporary directory for all tests.
     * @checkstyle VisibilityModifierCheck (3 lines)
     */
    @TempDir
    Path tmp;

    /**
     * Vertx slice server instance.
     */
    private VertxSliceServer server;

    /**
     * Container.
     */
    private GenericContainer<?> cntn;

    /**
     * Storage.
     */
    private Storage storage;

    /**
     * Vertx slice server port.
     */
    private int port;

    @ParameterizedTest
    @ValueSource(booleans = {true, /*false*/})//todo
    void downloadsDependency(final boolean anonymous) throws Exception {
        this.init(anonymous);
//        this.addHelloworldToArtipie();
        System.out.println("install hex: " + this.exec("mix", "local.hex", "--force"));

        String url = String.format("http://host.testcontainers.internal:%d", this.port);
        System.out.println("make repo..." + this.exec("mix", "hex.repo", "add", "my_repo", url));
        System.out.println("list of repos:\n" + this.exec("mix", "hex.repo", "list"));

        MatcherAssert.assertThat(
//            this.exec("mix", "deps.get"),
            this.exec("mix", "hex.package", "fetch my_artifact", "0.4.0", "--repo=my_repo"),
            new StringContainsInOrder(List.of(
                "Resolving Hex dependencies...",
                "Dependency resolution completed:",
                "Unchanged:", //todo
                "my_artifact 0.4.0",
                "All dependencies are up to date"
                )
            )
        );
    }

    @AfterEach
    void stopContainer() {
        this.server.close();
        this.cntn.stop();
    }

    @AfterAll
    static void close() {
        HexITCase.VERTX.close();
    }

    @SuppressWarnings("resource")
    void init(final boolean anonymous) throws IOException {
        final Pair<Permissions, Authentication> auth = this.auth(anonymous);
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            HexITCase.VERTX,
            new LoggingSlice(new HexSlice(this.storage, auth.getKey(), auth.getValue()))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("elixir:1.13.4")
            .withClasspathResourceMapping("/kv",
                "/var/kv",
                BindMode.READ_WRITE
            )
            .withWorkingDirectory("/var/kv")
            .withCommand("tail", "-f", "/dev/null")
            .withFileSystemBind(this.tmp.toString(), "/home"); //todo нужна ли???
        this.cntn.start();
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);//todo
        cntn.followOutput(logConsumer);
    }

    private String exec(final String... actions) throws Exception {
        return this.cntn.execInContainer(actions).getStdout().replaceAll("\n", "");
    }

    private Pair<Permissions, Authentication> auth(final boolean anonymous) {
        final Pair<Permissions, Authentication> res;
        if (anonymous) {
            res = new ImmutablePair<>(Permissions.FREE, Authentication.ANONYMOUS);
        } else {
            res = new ImmutablePair<>(
                (user, action) -> HexITCase.USER.getKey().equals(user.name())
                    && ("download".equals(action) || "upload".equals(action)),
                new Authentication.Single(
                    HexITCase.USER.getKey(), HexITCase.USER.getValue()
                )
            );
        }
        return res;
    }

    private Optional<Pair<String, String>> getUser(final boolean anonymous) {
        Optional<Pair<String, String>> res = Optional.empty();
        if (!anonymous) {
            res = Optional.of(HexITCase.USER);
        }
        return res;
    }

}
