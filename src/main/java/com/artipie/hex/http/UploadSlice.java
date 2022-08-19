/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */

package com.artipie.hex.http;

import com.artipie.asto.*;
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
import com.artipie.http.rs.RsWithStatus;

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

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import hu.akarnokd.rxjava2.interop.SingleInterop;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.reactivestreams.Publisher;

/**
 * This slice creates package meta-info from request body(tar-archive) and saves this tar-archive.
 */
public class UploadSlice implements Slice {
    /**
     * Ctor.
     * @param storage Repository storage.
     */
    public UploadSlice(final Storage storage) {
        this.storage = storage;
    }

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * Path to publish.
     */
    static final Pattern PUBLISH = Pattern.compile("(/repos/)?(?<org>.+)?/publish");

    /**
     * Query to publish.
     */
    static final Pattern QUERY = Pattern.compile("replace=(?<replace>true|false)");

    @Override
    public Response response(
        final String line,
        final Iterable<Map.Entry<String, String>> headers,
        final Publisher<ByteBuffer> body
    ) {
        final URI uri = new RequestLineFrom(line).uri();
        final Matcher pathMatcher = UploadSlice.PUBLISH
            .matcher(uri.getPath());
        final Matcher queryMatcher = UploadSlice.QUERY
            .matcher(uri.getQuery());
        final Response res;
        if (pathMatcher.matches() && queryMatcher.matches()) {
            final String org = pathMatcher.group("org");
            final boolean replace = Boolean.parseBoolean(queryMatcher.group("replace"));
            try {
                final AtomicReference<String> name = new AtomicReference<>();
                final AtomicReference<String> version = new AtomicReference<>();
                final AtomicReference<String> innerChecksum = new AtomicReference<>();
                final AtomicReference<String> outerChecksum = new AtomicReference<>();
                final AtomicReference<byte[]> tarContent = new AtomicReference<>();
                final AtomicReference<List<PackageOuterClass.Release>> releasesList = new AtomicReference<>();
                final AtomicReference<Key> packagesKey = new AtomicReference<>();
                return new AsyncResponse(asBytes(body)
                    .thenAccept(tarBytes -> readVarsFromTar(tarBytes, name, version, innerChecksum, outerChecksum, tarContent, packagesKey))
                    .thenCompose(nothing -> this.storage.exists(packagesKey.get()))
                    .thenCompose(packageExists -> {
                        if(packageExists && !replace) {
                            return CompletableFuture.completedFuture(null);
                        } else {
                            return
                                readReleasesListFromStorage(packageExists, releasesList, packagesKey)
                                .thenAccept(nothing -> {
                                    if(packageExists) {
                                        releasesList.get().removeIf(release -> version.get().equals(release.getVersion()));
                                    }
                                })
                                .thenApply(nothing -> constructSignedPackage(name, version, innerChecksum, outerChecksum, releasesList))
                                .thenCompose(signedPackage -> saveSignedPackageToStorage(packagesKey, signedPackage))
                                .thenCompose(nothing -> saveTarContentToStorage(name, version, tarContent));
                        }
                    }).thenApply(nothing ->
                        new RsFull(
                            RsStatus.OK,
                            new Headers.From(
                                new Header("Content-Type", "application/vnd.hex+erlang; charset=UTF-8")
                            ),
                            Content.EMPTY // todo: return body
                        )
                    )
                );
            } catch (Exception e) {
                System.err.println("e.getMessage() = " + e.getMessage());
                throw new RuntimeException("ERROR in /publish = ", e);
            }
        } else {
            res = new RsWithStatus(RsStatus.BAD_REQUEST);
        }
        return res;
    }

    /**
     * Reads variables from tar-content
     */
    private void readVarsFromTar(final byte[] tarBytes,
                                 final AtomicReference<String> name,
                                 final AtomicReference<String> version,
                                 final AtomicReference<String> innerChecksum,
                                 final AtomicReference<String> outerChecksum,
                                 final AtomicReference<byte[]> tarContent,
                                 final AtomicReference<Key> packagesKey) {
        tarContent.set(tarBytes);
        outerChecksum.set(DigestUtils.sha256Hex(tarBytes));
        final TarReader tarReader = new TarReader(tarBytes);
        tarReader
            .readEntryContent("metadata.config")
            .map(MetadataConfig::create)
            .map(metadataConfig -> {
                final String app = metadataConfig.getApp();
                name.set(app);
                packagesKey.set(new Key.From(DownloadSlice.PACKAGES, app));
                version.set(metadataConfig.getVersion());
                return metadataConfig;
            }).orElseThrow();
        tarReader.readEntryContent("CHECKSUM")
            .map(checksumBytes -> {
                innerChecksum.set(new String(checksumBytes));
                return checksumBytes;
            }).orElseThrow();
    }

    /**
     * Reads releasesList from storage
     */
    private CompletableFuture<Void> readReleasesListFromStorage(final Boolean packageExists,
                                                                final AtomicReference<List<PackageOuterClass.Release>> releasesList,
                                                                final AtomicReference<Key> packagesKey) {
        final CompletableFuture<Void> future;
        if (packageExists) {
            future = storage.value(packagesKey.get())
                .thenCompose(this::asBytes)
                .thenAccept(gzippedBytes -> {
                    final byte[] bytes = decompressGzip(gzippedBytes);
                    try {
                        final SignedOuterClass.Signed signed = SignedOuterClass.Signed.parseFrom(bytes);
                        final PackageOuterClass.Package pkg = PackageOuterClass.Package.parseFrom(signed.getPayload());
                        releasesList.set(pkg.getReleasesList());
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException("Cannot parse package", e);
                    }
                });
        } else {
            releasesList.set(Collections.emptyList());
            future = CompletableFuture.completedFuture(null);
        }
        return future;
    }

    /**
     * Constructs new signed package
     */
    private SignedOuterClass.Signed constructSignedPackage(final AtomicReference<String> name,
                                                           final AtomicReference<String> version,
                                                           final AtomicReference<String> innerChecksum,
                                                           final AtomicReference<String> outerChecksum,
                                                           final AtomicReference<List<PackageOuterClass.Release>> releasesList) {
        final PackageOuterClass.Release release;
        try {
            release = PackageOuterClass.Release.newBuilder()
                .setVersion(version.get())
                .setInnerChecksum(ByteString.copyFrom(Hex.decodeHex(innerChecksum.get())))
                .setOuterChecksum(ByteString.copyFrom(Hex.decodeHex(outerChecksum.get())))
                .build();
        } catch (DecoderException e) {
            throw new RuntimeException("Cannot decode hexed checksum", e);
        }
        final PackageOuterClass.Package pckg = PackageOuterClass.Package.newBuilder()
            .setName(name.get())
            .setRepository("artipie")//todo repoName
            .addAllReleases(releasesList.get())
            .addReleases(release)
            .build();
        return SignedOuterClass.Signed.newBuilder()
            .setPayload(ByteString.copyFrom(pckg.toByteArray()))
            .setSignature(ByteString.EMPTY)
            .build();
    }

    /**
     * Save signed package to storage
     */
    private CompletableFuture<Void> saveSignedPackageToStorage(
        final AtomicReference<Key> packagesKey,
        final SignedOuterClass.Signed signed) {
        return this.storage.save(
            packagesKey.get(),
            new Content.From(compressGzip(signed.toByteArray())));
    }

    /**
     * Save tar-content to storage
     */
    private CompletableFuture<Void> saveTarContentToStorage(final AtomicReference<String> name,
                                                            final AtomicReference<String> version,
                                                            final AtomicReference<byte[]> tarContent) {
        return this.storage.save(
            new Key.From(DownloadSlice.TARBALLS, String.format("%s-%s.tar", name, version)),
            new Content.From(tarContent.get()));
    }

    /**
     * Reads ByteBuffer-contents of Publisher into single byte array
     */
    private CompletionStage<byte[]> asBytes(final Publisher<ByteBuffer> body) {
        return new Concatenation(new OneTimePublisher<>(body)).single()
            .to(SingleInterop.get())
            .thenApply(Remaining::new)
            .thenApply(Remaining::bytes);
    }

    /**
     * Compresses data using gzip
     */
    private static byte[] compressGzip(final byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        try (final GZIPOutputStream gzipos = new GZIPOutputStream(baos, data.length)) {
            gzipos.write(data);
        } catch (IOException exception) {
            throw new RuntimeException("Error when compressing gzip archive", exception);
        }
        return baos.toByteArray();
    }

    /**
     * Decompresses data using gzip
     */
    private static byte[] decompressGzip(final byte[] gzippedBytes) {
        try (final GZIPInputStream gzipis = new GZIPInputStream(new ByteArrayInputStream(gzippedBytes), gzippedBytes.length);
             ByteArrayOutputStream baos = new ByteArrayOutputStream(gzippedBytes.length)) {
            baos.writeBytes(gzipis.readAllBytes());
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error when decompressing gzip archive", e);
        }
    }
}
