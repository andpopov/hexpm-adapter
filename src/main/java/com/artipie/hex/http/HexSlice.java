/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/http/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Storage;
import static com.artipie.hex.http.DownloadSlice.PACKAGES_PTRN;
import static com.artipie.hex.http.DownloadSlice.TARBALLS_PTRN;
import static com.artipie.hex.http.UploadSlice.PUBLISH;
import static com.artipie.hex.http.UserSlice.USERS;
import com.artipie.http.Slice;
import com.artipie.http.auth.Action;
import com.artipie.http.auth.Authentication;
import com.artipie.http.auth.BasicAuthSlice;
import com.artipie.http.auth.Permission;
import com.artipie.http.auth.Permissions;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.StandardRs;
import com.artipie.http.rt.ByMethodsRule;
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
//                new RtRulePath(//todo for testing
//                    new RtRule.Any(
//                        ALL_READ,
//                        ALL_WRITE
////                            new ByMethodsRule(RqMethod.GET),
////                            new ByMethodsRule(RqMethod.PUT),
////                            new ByMethodsRule(RqMethod.DELETE),
////                            new ByMethodsRule(RqMethod.HEAD),
////                            new ByMethodsRule(RqMethod.OPTIONS),
////                            new ByMethodsRule(RqMethod.PATCH),
////                            new ByMethodsRule(RqMethod.POST)
//                    ),
//                    new BasicAuthSlice(
//                        new LocalHexSlice(storage),
//                        users,
//                        new Permission.ByName(perms, Action.Standard.READ)
//                    )
//                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.Any(
                            new RtRule.ByPath(PACKAGES_PTRN),
                            new RtRule.ByPath(TARBALLS_PTRN)
                        )
                    ),
                        new BasicAuthSlice(
                            new DownloadSlice(storage),
                            users,
                            new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.GET),
                        new RtRule.ByPath(USERS)
                    ),
                        new BasicAuthSlice(
                            new UserSlice(storage),
                            users,
                            new Permission.ByName(perms, Action.Standard.READ)
                    )
                ),
                new RtRulePath(
                    new RtRule.All(
                        new ByMethodsRule(RqMethod.POST),
                        new RtRule.ByPath(PUBLISH)
                    ),
                        new BasicAuthSlice(
                            new UploadSlice(storage),
                            users,
                            new Permission.ByName(perms, Action.Standard.WRITE)
                    )
                ),
                new RtRulePath(
                    RtRule.FALLBACK, new SliceSimple(StandardRs.NOT_FOUND)
                )
            )
        );

    }
}
