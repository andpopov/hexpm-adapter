/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.memory.InMemoryStorage;
import com.artipie.asto.test.TestResource;
import com.artipie.hex.http.HexSlice;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.Permissions;
import com.artipie.http.slice.LoggingSlice;
import com.artipie.vertx.VertxSliceServer;
import io.vertx.reactivex.core.Vertx;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

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
    void downloadOneDependency(final boolean anonymous) throws IOException, InterruptedException {
        this.init(anonymous);
        this.addArtifactToArtipie();

        MatcherAssert.assertThat(
            this.exec("mix", "hex.package", "fetch", "decimal", "2.0.0", "--repo=my_repo"),
            new StringContains(
                "decimal v2.0.0 downloaded to /var/kv/decimal-2.0.0.tar"
            )
        );
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(booleans = {true, /*false*/})//todo
    void downloadAllDependencies(final boolean anonymous) throws IOException, InterruptedException {
        this.init(anonymous);
        this.addArtifactToArtipie();

        MatcherAssert.assertThat(
            "Get dependency for the first time",
            this.exec("mix", "deps.get"),
            new StringContains("New:\n  decimal 2.0.0")
            //Resolving Hex dependencies...
            //Dependency resolution completed:
            //New:
            //  decimal 2.0.0
            //  earmark_parser 1.4.26
            //  ex_doc 0.28.4
            //  makeup 1.1.0
            //  makeup_elixir 0.16.0
            //  makeup_erlang 0.1.1
            //  nimble_parsec 1.2.3
            //* Getting decimal (Hex package)
            //* Getting ex_doc (Hex package)
            //* Getting earmark_parser (Hex package)
            //* Getting makeup_elixir (Hex package)
            //* Getting makeup_erlang (Hex package)
            //* Getting makeup (Hex package)
            //* Getting nimble_parsec (Hex package)
        );

        MatcherAssert.assertThat(
            "Get dependency for the second time",
            this.exec("mix", "hex.deps", "get"),
            new StringContains("Unchanged:\n  decimal 2.0.0")
            //Resolving Hex dependencies...
            //Dependency resolution completed:
            //Unchanged:
            //  decimal 2.0.0
            //  earmark_parser 1.4.26
            //  ex_doc 0.28.4
            //  makeup 1.1.0
            //  makeup_elixir 0.16.0
            //  makeup_erlang 0.1.1
            //  nimble_parsec 1.2.3
            //All dependencies are up to date
        );
    }

    @Disabled
    @ParameterizedTest
    @ValueSource(booleans = {true, /*false*/})//todo
    void uploadsDependency(final boolean anonymous) throws IOException, InterruptedException {
        this.init(anonymous);
        System.out.println("user auth = " + this.exec("mix", "hex.user", "auth"));//todo know how send data to container`s stdin

        MatcherAssert.assertThat(
            this.exec("mix", "hex.publish"),
            new StringContains(
                "Published kv v0.1.0"
            )
        );
    }

    private void addHexAndRepoToContainer() throws IOException, InterruptedException {//todo
        System.out.println("install hex: " +
            this.exec("mix", "local.hex", "--force"));

        System.out.println("make repo..." +
            this.exec("mix", "hex.repo", "add", "my_repo", String.format("http://host.testcontainers.internal:%d", this.port)));

        System.out.println("check repos" +
            this.exec("mix", "hex.repo", "list"));

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
    void init(final boolean anonymous) throws IOException, InterruptedException {
        final Pair<Permissions, Authentication> auth = this.auth(anonymous);
        this.storage = new InMemoryStorage();
        this.server = new VertxSliceServer(
            HexITCase.VERTX,
            new LoggingSlice(new HexSlice(this.storage, auth.getKey(), auth.getValue()))
        );
        this.port = this.server.start();
        Testcontainers.exposeHostPorts(this.port);
        this.cntn = new GenericContainer<>("elixir:1.13.4")
            .withClasspathResourceMapping(
                "/kv",
                "/var/kv",
                BindMode.READ_WRITE
            )
            .withWorkingDirectory("/var/kv")
            .withEnv("HEX_UNSAFE_REGISTRY", "1")
            .withEnv("HEX_NO_VERIFY_REPO_ORIGIN", "1")
            .withEnv("HEX_API_URL", String.format("http://host.testcontainers.internal:%d", this.port))//todo for pushing
            .withCommand("tail", "-f", "/dev/null")
            .withFileSystemBind(this.tmp.toString(), "/home"); //todo нужна ли???
        this.cntn.start();
        this.addHexAndRepoToContainer();
    }

    private String exec(final String... actions) throws IOException, InterruptedException {
        return this.cntn.execInContainer(actions).toString()
            //.replaceAll("\n", "")//todo
        ;
    }

    private void addArtifactToArtipie() {
        new TestResource("packages")
            .addFilesTo(this.storage, new Key.From("packages"));
        new TestResource("tarballs")
            .addFilesTo(this.storage, new Key.From("tarballs"));
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
