package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.PreviewData;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.Okio;

/**
 * Created by Schinizer on 9/5/2016.
 */

public class RxUnfurlTest {
    @Rule
    public final MockWebServer server = new MockWebServer();
    private RxUnfurl rxUnfurl = new RxUnfurl.Builder().scheduler(Schedulers.trampoline()).build();

    @Test
    public void testExtractOpenGraphTags() {
        final String mockHtml = "<!DOCTYPE html><html> <meta property=\"og:url\" content=\"http://someurl.io\"/> <meta property=\"og:title\" content=\"title\"/> <meta property=\"og:description\" content=\"description\"/> <meta property=\"og:image\" content=\"/64x64_baseline.jpg\"/></html>";

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                switch (request.getPath()) {
                    case "/64x64_baseline.jpg":
                        URL res = RxUnfurlTest.class.getClassLoader().getResource("64x64_baseline.jpg");
                        return fileResponse((res == null) ? "" : res.getPath());
                    default:
                        return new MockResponse()
                                .setStatus("HTTP/1.1 200")
                                .setBody(mockHtml)
                                .addHeader("content-type: text/html")
                                .addHeader("content-length", mockHtml.length());
                }
            }
        });

        rxUnfurl.generatePreview(server.url("/").toString())
                .test()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(new Predicate<PreviewData>() {
                    @Override
                    public boolean test(PreviewData previewData) throws Exception {
                        return "http://someurl.io".equals(previewData.getUrl()) &&
                                "title".equals(previewData.getTitle()) &&
                                "description".equals(previewData.getDescription()) &&
                                previewData.getImages().size() == 1 &&
                                (server.url("/").toString() + "64x64_baseline.jpg")
                                        .equals(previewData.getImages().get(0).getSource()) &&
                                previewData.getImages().get(0).getDimension().getHeight() == 64 &&
                                previewData.getImages().get(0).getDimension().getWidth() == 64;
                    }
                });
    }

    @Test
    public void testExtractFallbacks() {
        final String mockHtml = "<!DOCTYPE html><html><meta name=\"description\" content=\"fallback description\" />\n<title>fallback title</title><body> <img src=\"64x64_baseline.jpg\"/> </body></html>";

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                switch (request.getPath()) {
                    case "/64x64_baseline.jpg":
                        URL baseline = RxUnfurlTest.class.getClassLoader().getResource("64x64_baseline.jpg");
                        return fileResponse((baseline == null) ? "" : baseline.getPath());
                    default:
                        return new MockResponse()
                                .setStatus("HTTP/1.1 200")
                                .setBody(mockHtml)
                                .addHeader("content-type: text/html")
                                .addHeader("content-length", mockHtml.length());
                }
            }
        });

        rxUnfurl.generatePreview(server.url("/").toString())
                .test()
                .assertNoErrors()
                .assertValueCount(1)
                .assertValue(new Predicate<PreviewData>() {
                    @Override
                    public boolean test(@NonNull PreviewData previewData) throws Exception {
                        return server.url("/").toString().equals(previewData.getUrl()) &&
                                "fallback title".equals(previewData.getTitle()) &&
                                "fallback description".equals(previewData.getDescription()) &&
                                previewData.getImages().size() == 1 &&
                                (server.url("/").toString() + "64x64_baseline.jpg")
                                        .equals(previewData.getImages().get(0).getSource()) &&
                                previewData.getImages().get(0).getDimension().getHeight() == 64 &&
                                previewData.getImages().get(0).getDimension().getWidth() == 64;
                    }
                });
    }

    @Test
    public void testParagraphFallback() {
        final String mockHtml = "<!DOCTYPE html><html><p>fallback description</p></body></html>";

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                return new MockResponse()
                        .setStatus("HTTP/1.1 200")
                        .setBody(mockHtml)
                        .addHeader("content-type: text/html")
                        .addHeader("content-length", mockHtml.length());
            }
        });

        rxUnfurl.generatePreview(server.url("/").toString())
                .test().assertNoErrors()
                .assertValueCount(1)
                .assertValue(new Predicate<PreviewData>() {
                    @Override
                    public boolean test(@NonNull PreviewData previewData) throws Exception {
                        return previewData.getDescription().equals("fallback description");
                    }
                });
    }

    private String contentType(String path) {
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg")) return "image/jpeg";
        if (path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".gif")) return "image/gif";
        if (path.endsWith(".bmp")) return "image/bmp";
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".txt")) return "text/plain; charset=utf-8";
        return "application/octet-stream";
    }

    private MockResponse fileResponse(String url) {
        File file = new File(url);
        try {
            return new MockResponse()
                    .setStatus("HTTP/1.1 200")
                    .addHeader("content-type: " + contentType(url))
                    .setBody(fileToBytes(file));
        } catch (IOException e) {
            return new MockResponse()
                    .setStatus("HTTP/1.1 404")
                    .addHeader("content-type: text/plain; charset=utf-8")
                    .setBody("NOT FOUND: " + url);
        }
    }

    private Buffer fileToBytes(File file) throws IOException {
        Buffer result = new Buffer();
        result.writeAll(Okio.source(file));
        return result;
    }
}
