/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Storage;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rs.StandardRs;
import static com.artipie.http.rt.ByMethodsRule.Standard.ALL_READ;
import static com.artipie.http.rt.ByMethodsRule.Standard.ALL_WRITE;
import com.artipie.http.rt.RtRule;
import com.artipie.http.rt.RtRulePath;
import com.artipie.http.rt.SliceRoute;
import com.artipie.http.slice.SliceSimple;

/**
 * Artipie {@link Slice} for Hex repository HTTP API.
 * @since 0.1
 */
public class HexSlice extends Slice.Wrap {

    /**
     * Ctor.
     * @param storage The storage.
     */
    public HexSlice(final Storage storage) {
        this(storage,
            Permissions.FREE,
            (login, pwd) -> java.util.Optional.of(new com.artipie.http.auth.Authentication.User("anonymous"))
        );
    }

    public HexSlice(
        final Storage storage,
        final Permissions perms,
        final Authentication users
    ) {
        super(
            new SliceRoute(
                new RtRulePath(
                    new RtRule.Any(
                        ALL_READ,
                        ALL_WRITE
//                            new ByMethodsRule(RqMethod.GET),
//                            new ByMethodsRule(RqMethod.PUT),
//                            new ByMethodsRule(RqMethod.DELETE),
//                            new ByMethodsRule(RqMethod.HEAD),
//                            new ByMethodsRule(RqMethod.OPTIONS),
//                            new ByMethodsRule(RqMethod.PATCH),
//                            new ByMethodsRule(RqMethod.POST)
                    ),
                    new BasicAuthSlice(
                        new LocalHexSlice(storage),
                        users,
                        new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND)
                )
            )
        );

    }
}
