/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.hex.tarball.HexPackageNameExtractor;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import io.reactivex.Flowable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.reactivestreams.Publisher;

public class LocalHexSlice implements Slice {

    /**
     * Repository storage.
     */
    private final Storage storage;

    /**
     * New local Hex slice.
     *
     * @param storage Repository storage
     */
    public LocalHexSlice(final Storage storage) {
        this.storage = storage;
    }

    @Override
    public Response response(final String line, final Iterable<Map.Entry<String, String>> headers, final Publisher<ByteBuffer> body) {
        System.out.println("line = " + line);//todo debug print
        System.out.println("headers = " + headers);//todo debug print

        if (line.contains("/publish")) {

/*
            final String name = "decimal";
            final String version = "2.0.0";
            final String innerChecksum = "A78296E617B0F5DD4C6CAF57C714431347912FFB1D0842E998E9792B5642D697";//todo CHECKSUM form file
            final String outerChecksum = DigestUtils.sha256Hex(asBytes(body));

            Key.From key = new Key.From("packages", name);
            final AtomicReference<List<PackageOuterClass.Release>> releasesList = new AtomicReference<>();
            this.storage.exists(key)
                .thenApply(exist -> {
                    if (exist) {
                        storage.value(key).thenCompose( content -> {
                            byte[] bytes = decompressGzip(asBytes(content));
                            SignedOuterClass.Signed existSigned = SignedOuterClass.Signed.parseFrom(bytes);
                            PackageOuterClass.Package existPckg = PackageOuterClass.Package.parseFrom(existSigned.getPayload());
                            releasesList.set(existPckg.getReleasesList());
                            return new CompletableFuture<>();
                        });
                    }
                    PackageOuterClass.Release release = PackageOuterClass.Release.newBuilder()
                        .setVersion(version)
                        .setInnerChecksum(ByteString.copyFrom(Hex.decodeHex(innerChecksum)))
                        .setOuterChecksum(ByteString.copyFrom(Hex.decodeHex(outerChecksum)))
                        .build();
                    PackageOuterClass.Package pckg = PackageOuterClass.Package.newBuilder()
                        .setName(name)
                        .setRepository("artipie")//todo repoName
                        .addAllReleases(releasesList.get())
                        .addReleases(release)
                        .build();
                    SignedOuterClass.Signed signed = SignedOuterClass.Signed.newBuilder()
                        .setPayload(ByteString.copyFrom(pckg.toByteArray()))
                        .setSignature(ByteString.EMPTY)
                        .build();
                    return this.storage.save(key, new Content.From(compressGzip(signed.toByteArray())));
                });

*/
            try {
                return new AsyncResponse(
                    CompletableFuture
                        .supplyAsync(() -> {
                                byte[] bytes = asBytes(body);
                                String tarName = HexPackageNameExtractor.extract(bytes).orElseThrow();
                                try {
                                    this.storage.save(
                                        new Key.From(tarName),
                                        new Content.From(bytes)
                                    ).get();
                                } catch (InterruptedException | ExecutionException e) {
                                    throw new RuntimeException(e);
                                }
                                System.out.println("published " + tarName);
                                return bytes;
                            }
                        ).thenApply(nothing -> new RsWithBody(
                            new RsWithStatus(RsStatus.CREATED),
                            "".getBytes()
                            )
                        ));
            } catch (Exception e) {
              System.out.println("e.getMessage() = " + e.getMessage());
              throw new RuntimeException("ERROR in /publish = ", e);
          }
        } else {
            return new RsWithStatus(RsStatus.BAD_REQUEST);
        }
    }
    private static byte[] compressGzip(byte[] data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
        try (GZIPOutputStream gzipos = new GZIPOutputStream(baos, data.length)) {
            gzipos.write(data);
        } catch (IOException exception) {
            throw new RuntimeException("Error when compressing gzip archive", exception);
        }
        return baos.toByteArray();
    }

    private static byte[] decompressGzip(byte[] gzippedBytes) {
        try (GZIPInputStream gzipis = new GZIPInputStream(new ByteArrayInputStream(gzippedBytes), gzippedBytes.length);
             ByteArrayOutputStream baos = new ByteArrayOutputStream(gzippedBytes.length)) {
            baos.writeBytes(gzipis.readAllBytes());
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error when decompressing gzip archive", e);
        }

    }

    /**
     * Reads ByteBuffer-contents of Publisher to byte array
     */
    private byte[] asBytes(Publisher<ByteBuffer> body) {
        final ByteBuffer buffer = Flowable.fromPublisher(body)
            .toList()
            .blockingGet()
            .stream()
            .reduce(
                (left, right) -> {
                    left.mark();
                    right.mark();
                    final ByteBuffer concat = ByteBuffer.allocate(
                        left.remaining() + right.remaining()
                    ).put(left).put(right);
                    left.reset();
                    right.reset();
                    concat.flip();
                    return concat;
                }
            )
            .orElse(ByteBuffer.allocate(0));
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.mark();
        buffer.get(bytes);
        buffer.reset();
        return bytes;
    }
}
