/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.ArtipieException;
import com.artipie.asto.Concatenation;
import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.OneTimePublisher;
import com.artipie.asto.Remaining;
import com.artipie.asto.Storage;
import com.artipie.hex.proto.generated.PackageOuterClass;
import com.artipie.hex.proto.generated.SignedOuterClass;
import com.artipie.hex.tarball.MetadataConfig;
import com.artipie.hex.tarball.TarReader;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rq.RequestLineFrom;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.reactivestreams.Publisher;

/**
 * This slice creates package meta-info from request body(tar-archive) and saves this tar-archive.
 * @since 0.1
 * @checkstyle ClassFanOutComplexityCheck (500 lines)
 * @checkstyle NestedIfDepthCheck (500 lines)
 * @checkstyle ParameterNumberCheck (500 lines)
 */
@SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.UnusedLocalVariable"})
public final class UploadSlice implements Slice {
    /**
     * Path to publish.
     */
    static final Pattern PUBLISH = Pattern.compile("(/repos/)?(?<org>.+)?/publish");

    /**
     * Query to publish.
     */
    static final Pattern QUERY = Pattern.compile("replace=(?<replace>true|false)");

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param storage Repository storage.
     */
    public UploadSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final URI uri = new RequestLineFrom(line).uri();
        final Matcher pathmatcher = UploadSlice.PUBLISH
            .matcher(uri.getPath());
        final Matcher querymatcher = UploadSlice.QUERY
            .matcher(uri.getQuery());
        final Response res;
        if (pathmatcher.matches() && querymatcher.matches()) {
            final String org = pathmatcher.group("org");
            final boolean replace = Boolean.parseBoolean(querymatcher.group("replace"));
            final AtomicReference<String> name = new AtomicReference<>();
            final AtomicReference<String> version = new AtomicReference<>();
            final AtomicReference<String> innerchcksum = new AtomicReference<>();
            final AtomicReference<String> outerchcksum = new AtomicReference<>();
            final AtomicReference<byte[]> tarcontent = new AtomicReference<>();
            final AtomicReference<List<PackageOuterClass.Release>> releaseslist =
                new AtomicReference<>();
            final AtomicReference<Key> key = new AtomicReference<>();
            res = new AsyncResponse(UploadSlice.asBytes(body)
                .thenAccept(
                    bytes -> UploadSlice.readVarsFromTar(
                        bytes,
                        name,
                        version,
                        innerchcksum,
                        outerchcksum,
                        tarcontent,
                        key
                    )
                ).thenCompose(
                    nothing -> this.storage.exists(key.get())
                ).thenCompose(
                    packageExists -> {
                        final CompletableFuture<Void> feature;
                        if (packageExists && !replace) {
                            feature = CompletableFuture.completedFuture(null);
                        } else {
                            feature = this.readReleasesListFromStorage(
                                packageExists,
                                releaseslist,
                                key
                            ).thenAccept(
                                nothing -> {
                                    if (packageExists) {
                                        releaseslist.get().removeIf(
                                            release -> version.get().equals(release.getVersion())
                                        );
                                    }
                                }
                            ).thenApply(
                                nothing -> UploadSlice.constructSignedPackage(
                                    name,
                                    version,
                                    innerchcksum,
                                    outerchcksum,
                                    releaseslist
                                )
                            ).thenCompose(
                                signedPackage -> this.saveSignedPackageToStorage(
                                    key,
                                    signedPackage
                                )
                            ).thenCompose(
                                nothing -> this.saveTarContentToStorage(
                                    name,
                                    version,
                                    tarcontent
                                )
                            );
                        }
                        return feature;
                    }
                ).handle(
                    (content, throwable) -> {
                        final Response result;
                        if (throwable == null) {
                            result = new RsFull(
                                RsStatus.CREATED,
                                new Headers.From(
                                    new Header(
                                        "Content-Type",
                                        "application/vnd.hex+erlang; charset=UTF-8"
                                    )
                                ),
                                Content.EMPTY
                            );
                        } else {
                            result = new RsWithBody(
                                new RsWithStatus(RsStatus.INTERNAL_ERROR),
                                throwable.getMessage().getBytes()
                            );
                        }
                        return result;
                    }
                )
            );
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }

    /**
     * Reads variables from tar-content.
     * @param tar Tar archive with Elixir package in byte format
     * @param name Ref for package name
     * @param version Ref for package version
     * @param innerchecksum Ref for package innerChecksum
     * @param outerchecksum Ref for package outerChecksum
     * @param tarcontent Ref for tar archive in byte format
     * @param packagekey Ref for key for store
     */
    private static void readVarsFromTar(
        final byte[] tar,
        final AtomicReference<String> name,
        final AtomicReference<String> version,
        final AtomicReference<String> innerchecksum,
        final AtomicReference<String> outerchecksum,
        final AtomicReference<byte[]> tarcontent,
        final AtomicReference<Key> packagekey
    ) {
        tarcontent.set(tar);
        outerchecksum.set(DigestUtils.sha256Hex(tar));
        final TarReader reader = new TarReader(tar);
        reader
            .readEntryContent(TarReader.METADATA)
            .map(MetadataConfig::new)
            .map(
                metadataConfig -> {
                    final String app = metadataConfig.app();
                    name.set(app);
                    packagekey.set(new Key.From(DownloadSlice.PACKAGES, app));
                    version.set(metadataConfig.version());
                    return metadataConfig;
                }
            ).orElseThrow();
        reader.readEntryContent(TarReader.CHECKSUM)
            .map(
                checksumBytes -> {
                    innerchecksum.set(new String(checksumBytes));
                    return checksumBytes;
                }
            ).orElseThrow();
    }

    /**
     * Reads releasesList from storage.
     * @param packageexist Ref on package exist
     * @param releaseslist Ref for list of releases
     * @param packagekey Ref on key for searching package
     * @return Empty CompletableFuture
     */
    private CompletableFuture<Void> readReleasesListFromStorage(
        final Boolean packageexist,
        final AtomicReference<List<PackageOuterClass.Release>> releaseslist,
        final AtomicReference<Key> packagekey
    ) {
        final CompletableFuture<Void> future;
        if (packageexist) {
            future = this.storage.value(packagekey.get())
                .thenCompose(UploadSlice::asBytes)
                .thenAccept(
                    gzippedBytes -> {
                        final byte[] bytes = UploadSlice.decompressGzip(gzippedBytes);
                        try {
                            final SignedOuterClass.Signed signed =
                                SignedOuterClass.Signed.parseFrom(bytes);
                            final PackageOuterClass.Package pkg =
                                PackageOuterClass.Package.parseFrom(signed.getPayload());
                            releaseslist.set(pkg.getReleasesList());
                        } catch (final InvalidProtocolBufferException ipbex) {
                            throw new ArtipieException("Cannot parse package", ipbex);
                        }
                    }
                );
        } else {
            releaseslist.set(Collections.emptyList());
            future = CompletableFuture.completedFuture(null);
        }
        return future;
    }

    /**
     * Constructs new signed package.
     * @param name Ref on package name
     * @param version Ref on package version
     * @param innerchecksum Ref on package innerChecksum
     * @param outerchecksum Ref on package outerChecksum
     * @param releaseslist Ref on list of releases
     * @return Package wrapped in Signed
     */
    private static SignedOuterClass.Signed constructSignedPackage(
        final AtomicReference<String> name,
        final AtomicReference<String> version,
        final AtomicReference<String> innerchecksum,
        final AtomicReference<String> outerchecksum,
        final AtomicReference<List<PackageOuterClass.Release>> releaseslist
    ) {
        final PackageOuterClass.Release release;
        try {
            release = PackageOuterClass.Release.newBuilder()
                .setVersion(version.get())
                .setInnerChecksum(ByteString.copyFrom(Hex.decodeHex(innerchecksum.get())))
                .setOuterChecksum(ByteString.copyFrom(Hex.decodeHex(outerchecksum.get())))
                .build();
        } catch (final DecoderException dex) {
            throw new ArtipieException("Cannot decode hexed checksum", dex);
        }
        final PackageOuterClass.Package pckg = PackageOuterClass.Package.newBuilder()
            .setName(name.get())
            .setRepository("artipie")
            .addAllReleases(releaseslist.get())
            .addReleases(release)
            .build();
        return SignedOuterClass.Signed.newBuilder()
            .setPayload(ByteString.copyFrom(pckg.toByteArray()))
            .setSignature(ByteString.EMPTY)
            .build();
    }

    /**
     * Save signed package to storage.
     * @param packagekey Ref on key for saving package
     * @param signed Package wrapped in Signed
     * @return Empty CompletableFuture
     */
    private CompletableFuture<Void> saveSignedPackageToStorage(
        final AtomicReference<Key> packagekey,
        final SignedOuterClass.Signed signed
    ) {
        return this.storage.save(
            packagekey.get(),
            new Content.From(UploadSlice.compressGzip(signed.toByteArray()))
        );
    }

    /**
     * Save tar-content to storage.
     * @param name Ref on package name
     * @param version Ref on package version
     * @param tarcontent Ref on tar archive in byte format
     * @return Empty CompletableFuture
     */
    private CompletableFuture<Void> saveTarContentToStorage(
        final AtomicReference<String> name,
        final AtomicReference<String> version,
        final AtomicReference<byte[]> tarcontent
    ) {
        return this.storage.save(
            new Key.From(DownloadSlice.TARBALLS, String.format("%s-%s.tar", name, version)),
            new Content.From(tarcontent.get())
        );
    }

    /**
     * Reads ByteBuffer-contents of Publisher into single byte array.
     * @param body Request body
     * @return CompletionStage with bytes from request body
     */
    private static CompletionStage<byte[]> asBytes(final Publisher<ByteBuffer> body) {
        return new Concatenation(new OneTimePublisher<>(body)).single()
            .to(SingleInterop.get())
            .thenApply(Remaining::new)
            .thenApply(Remaining::bytes);
    }

    /**
     * Compresses data using gzip.
     * @param data Bytes
     * @return Compressed bytes in gzip format
     */
    private static byte[] compressGzip(final byte[] data) {
        try (
            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
            GZIPOutputStream gzipos = new GZIPOutputStream(baos, data.length)
        ) {
            gzipos.write(data);
            return baos.toByteArray();
        } catch (final IOException ioex) {
            throw new ArtipieException("Error when compressing gzip archive", ioex);
        }
    }

    /**
     * Decompresses data using gzip.
     * @param gzippedbytes Compressed bytes in gzip format
     * @return Decompressed bytes
     */
    private static byte[] decompressGzip(final byte[] gzippedbytes) {
        try (
            GZIPInputStream gzipis = new GZIPInputStream(
                new ByteArrayInputStream(gzippedbytes),
                gzippedbytes.length
            );
            ByteArrayOutputStream baos = new ByteArrayOutputStream(gzippedbytes.length)
        ) {
            baos.writeBytes(gzipis.readAllBytes());
            return baos.toByteArray();
        } catch (final IOException ioex) {
            throw new ArtipieException("Error when decompressing gzip archive", ioex);
        }
    }
}
