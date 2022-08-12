/*
 * The MIT License (MIT) Copyright (c) 2020-2022 artipie.com
 * https://github.com/artipie/hexpm-adapter/blob/master/LICENSE.txt
 */
package com.artipie.hex.http;

import com.artipie.asto.Content;
import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.Slice;
import com.artipie.http.async.AsyncResponse;
import com.artipie.http.headers.Header;
import com.artipie.http.rs.RsFull;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithBody;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.http.slice.ContentWithSize;
import io.reactivex.Flowable;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.reactivex.Single;
import io.reactivex.internal.operators.flowable.FlowableMap;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

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

        if (line.contains("/tarballs/decimal-2.0.0.tar")) {
            System.out.println("inside tarballs");//todo debug print
            try {
                return
                    new AsyncResponse(
                        this.storage.value(new Key.From("binary", "decimal-2.0.0.tar"))
                            .thenApply(
                                value -> new RsFull(
                                    RsStatus.OK,
                                    new Headers.From(
                                        new Header("Content-Type", "application/octet-stream")
                                        ),
                                    value
                                )
                            )
                    );
            } catch (Exception e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                throw new RuntimeException("ERROR in /tarballs/ = ", e);
            }
        } else if (line.contains("/packages/decimal")) {
            System.out.println("inside packages");
            HttpURLConnection con = null;
            try {
//                con = (HttpURLConnection) new URL("https://repo.hex.pm/packages/aba").openConnection();//todo proxy to hexpm with archive
//                con.setRequestMethod("GET");
//                con.setDoInput(true);
//                InputStream inputStr = con.getInputStream();
//                byte[] response = inputStr.readAllBytes();

//                PackageOuterClass.Package package1 =
//                    PackageOuterClass.Package.parseFrom(response);
//
//                PackageOuterClass.Package package2 =
//                    PackageOuterClass.Package.parseFrom(accept(this.storage.value(new Key.From("binary", "decimal")).get()).get());

                return
                    new AsyncResponse(
                        this.storage.value(new Key.From("binary", "decimal"))
                            .thenApply(
                                value -> new RsFull(
                                    RsStatus.OK,
                                    new Headers.From(
                                        new Header("Content-Type", "application/octet-stream")
                                    ),
                                    value
                                )
                            )
                    );
            } catch (Exception e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                throw new RuntimeException(String.format("ERROR in /packages/ = %s", e.getMessage()), e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else if (line.contains("/users/me")) {
            System.out.println("inside packages");
            HttpURLConnection con = null;
            try {
//                con = (HttpURLConnection) new URL("https://hex.pm/api/users/swizbiz").openConnection();//todo proxy to hexpm
//                con.setRequestMethod("GET");
//                con.setRequestProperty("Accept", "application/vnd.hex+erlang");
//                con.setRequestProperty("Authorization", "824650fa6e7687b54a95c43d2bbe8e59");
//                con.setDoInput(true);
//                InputStream inputStr = con.getInputStream();
//                byte[] response = inputStr.readAllBytes();

                return new RsFull(RsStatus.NO_CONTENT, new Headers.From(//todo workaround with NO_CONTENT
//                    new Header("Content-length", String.valueOf(response.length)),
                    new Header("Content-Type", "application/vnd.hex+erlang; charset=UTF-8") //todo application/vnd.hex+<needed format> implement this headers
                ),
                    Content.EMPTY
//                    new Content.From(response)
                );
            } catch (Exception e) {
                System.out.println("e.getMessage() = " + e.getMessage());
                throw new RuntimeException("ERROR in /users/ = ", e);
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        } else if (line.contains("/publish")) {
            System.out.println("inside publish");
            HttpURLConnection con = null;
            try {
//                con = (HttpURLConnection) new URL("https://hex.pm/api/packages/aba").openConnection();
//                con.setRequestMethod("GET");
//                con.setRequestProperty("Accept", "application/vnd.hex+erlang");
//                con.setDoInput(true);
//                InputStream inputStr = con.getInputStream();
//                byte[] response = inputStr.readAllBytes();

//                bodyAsBytes(body)
//                    .thenAcceptAsync(bytes -> {
//                        System.out.println(new String(bytes));
//                    }).get();

                AsyncResponse res = new AsyncResponse(
                    this.storage.save(
                        new Key.From("kv"),
                        new ContentWithSize(body, headers)
                    ).thenApply(nothing -> new RsWithBody(
                            new RsWithStatus(RsStatus.CREATED),
                            """
                                    {
                                        "version": "0.1.0",
                                        "has_docs": false,
                                        "url": "https://hex.pm/api/packages/kv/releases/0.1.0",
                                        "package_url": "https://hex.pm/api/packages/kv",
                                        "html_url": "https://hex.pm/packages/kv/0.1.0",
                                        "docs_html_url": "https://hexdocs.pm/kv/0.1.0",
                                        "meta": {
                                          "build_tools": ["mix"]
                                        },
                                        "dependencies": {
                                          "cowboy": {
                                            "requirement": "~> 1.0",
                                            "optional": true,
                                            "app": "cowboy"
                                          }
                                        }
                                        "downloads": 16,
                                        "inserted_at": "2014-04-23T18:58:54Z",
                                        "updated_at": "2014-04-23T18:58:54Z"
                                      }
                                """.getBytes()
/*
                                    """
                                            {
                                                "version": "0.1.0",
                                                "has_docs": false,
                                                "url": "http://localhost:8080/packages/kv/releases/0.1.0",
                                                "package_url": "http://localhost:8080/packages/kv",
                                                "html_url": "https://hex.pm/packages/kv/0.1.0",
                                                "docs_html_url": "https://hexdocs.pm/kv/0.1.0",
                                                "meta": {
                                                  "build_tools": ["mix"]
                                                },
                                                "downloads": 16,
                                                "inserted_at": "2014-04-23T18:58:54Z",
                                                "updated_at": "2014-04-23T18:58:54Z"
                                              }
                                        """.getBytes()
*/
                        )
                    )
                );
                System.out.println();
                return res;
            } catch (Exception e) {
              System.out.println("e.getMessage() = " + e.getMessage());
              throw new RuntimeException("ERROR in /publish = ", e);
          }
        } else {
            return new RsWithStatus(RsStatus.BAD_REQUEST);
        }
    }

    private CompletableFuture<byte[]> bodyAsBytes(Publisher<ByteBuffer> body) {
        return CompletableFuture.supplyAsync(
            () -> {
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
        );
    }

    public CompletableFuture<ByteBuffer> accept(final Publisher<ByteBuffer> body) {//todo need only for testing protobuf
        return CompletableFuture.supplyAsync(() -> {
            final ByteBuffer buffer = Flowable.fromPublisher(body).toList().blockingGet().stream().reduce((left, right) -> {
                left.mark();
                right.mark();
                final ByteBuffer concat = ByteBuffer.allocate(left.remaining() + right.remaining()).put(left).put(right);
                left.reset();
                right.reset();
                concat.flip();
                return concat;
            }).orElse(ByteBuffer.allocate(0));
//                final byte[] bytes = new byte[buffer.remaining()];
//                buffer.mark();
//                buffer.get(bytes);
//                buffer.reset();
//                AtomicReference<byte[]> container = new AtomicReference<>();
//                container.set(bytes);
            return buffer;
        });

    }
}
