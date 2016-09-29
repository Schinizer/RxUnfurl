package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.PreviewData;

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import okio.Okio;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;

/**
 * Created by DPSUser on 9/5/2016.
 */

public class RxUnfurlTest {
    @Rule public final MockWebServer server = new MockWebServer();

    @Test
    public void testExtractOpenGraphTags()
    {
        final String mockHtml = "<!DOCTYPE html><html> <meta property=\"og:url\" content=\"http://someurl.io\"/> <meta property=\"og:title\" content=\"title\"/> <meta property=\"og:description\" content=\"description\"/> <meta property=\"og:image\" content=\"/64x64_baseline.jpg\"/></html>";

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                switch (request.getPath())
                {
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

        Observable<PreviewData> observable = RxUnfurl.generatePreview(server.url("/").toString());
        TestSubscriber<PreviewData> testSubscriber = new TestSubscriber<>();

        observable.subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        PreviewData data = testSubscriber.getOnNextEvents().get(0);
        assertEquals(data.getUrl(), "http://someurl.io");
        assertEquals(data.getTitle(), "title");
        assertEquals(data.getDescription(), "description");
        assertEquals(data.getImages().size(), 1);
        assertEquals(data.getImages().get(0).getSource(), server.url("/").toString() + "64x64_baseline.jpg");
        assertEquals(data.getImages().get(0).getDimension().getWidth(), 64);
        assertEquals(data.getImages().get(0).getDimension().getHeight(), 64);
    }

    @Test
    public void testExtractFallbacks()
    {
        final String mockHtml = "<!DOCTYPE html><html><meta name=\"description\" content=\"fallback description\" />\n<title>fallback title</title><body> <img src=\"64x64_baseline.jpg\"/> </body></html>";

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                switch (request.getPath())
                {
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

        Observable<PreviewData> observable = RxUnfurl.generatePreview(server.url("/").toString());
        TestSubscriber<PreviewData> testSubscriber = new TestSubscriber<>();

        observable.subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        PreviewData data = testSubscriber.getOnNextEvents().get(0);
        assertEquals(data.getUrl(), server.url("/").toString());
        assertEquals(data.getTitle(), "fallback title");
        assertEquals(data.getDescription(), "fallback description");

        assertEquals(data.getImages().size(), 1);
        assertEquals(data.getImages().get(0).getSource(), server.url("/").toString() + "64x64_baseline.jpg");
        assertEquals(data.getImages().get(0).getDimension().getWidth(), 64);
        assertEquals(data.getImages().get(0).getDimension().getHeight(), 64);
    }

    @Test
    public void testParagraphFallback()
    {
        final String mockHtml = "<!DOCTYPE html><html><p>fallback description</p></body></html>";

        server.enqueue(new MockResponse()
            .setStatus("HTTP/1.1 200")
            .setBody(mockHtml)
            .addHeader("content-type: text/html")
            .addHeader("content-length", mockHtml.length()));

        Observable<PreviewData> observable = RxUnfurl.generatePreview(server.url("/").toString());
        TestSubscriber<PreviewData> testSubscriber = new TestSubscriber<>();

        observable.subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        PreviewData data = testSubscriber.getOnNextEvents().get(0);
        assertEquals(data.getDescription(), "fallback description");
    }

    @Test
    public void testFileFormatExtractDimensions()
    {
        final String mockHtml = "<!DOCTYPE html><html><body><img src=\"64x64_baseline.jpg\"/> <img src=\"32x32_progressive.jpg\"/> <img src=\"16x16.png\"/> <img src=\"8x8.gif\"/> <img src=\"4x4.bmp\"/></body></html>";

        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                switch (request.getPath())
                {
                    case "/64x64_baseline.jpg":
                        URL baseline = RxUnfurlTest.class.getClassLoader().getResource("64x64_baseline.jpg");
                        return fileResponse((baseline == null) ? "" : baseline.getPath());
                    case "/32x32_progressive.jpg":
                        URL progressive = RxUnfurlTest.class.getClassLoader().getResource("32x32_progressive.jpg");
                        return fileResponse((progressive == null) ? "" : progressive.getPath());
                    case "/16x16.png":
                        URL png = RxUnfurlTest.class.getClassLoader().getResource("16x16.png");
                        return fileResponse((png == null) ? "" : png.getPath());
                    case "/8x8.gif":
                        URL gif = RxUnfurlTest.class.getClassLoader().getResource("8x8.gif");
                        return fileResponse((gif == null) ? "" : gif.getPath());
                    case "/4x4.bmp":
                        URL bmp = RxUnfurlTest.class.getClassLoader().getResource("4x4.bmp");
                        return fileResponse((bmp == null) ? "" : bmp.getPath());
                    default:
                        return new MockResponse()
                                .setStatus("HTTP/1.1 200")
                                .setBody(mockHtml)
                                .addHeader("content-type: text/html")
                                .addHeader("content-length", mockHtml.length());
                }
            }
        });

        Observable<PreviewData> observable = RxUnfurl.generatePreview(server.url("/").toString());
        TestSubscriber<PreviewData> testSubscriber = new TestSubscriber<>();

        observable.subscribe(testSubscriber);
        testSubscriber.assertNoErrors();
        testSubscriber.assertValueCount(1);

        PreviewData data = testSubscriber.getOnNextEvents().get(0);

        assertEquals(data.getImages().size(), 5);

        // Images are sorted to total pixel counts
        // Test all supported image format
        assertEquals(data.getImages().get(0).getSource(), server.url("/").toString() + "64x64_baseline.jpg");
        assertEquals(data.getImages().get(0).getDimension().getWidth(), 64);
        assertEquals(data.getImages().get(0).getDimension().getHeight(), 64);
        assertEquals(data.getImages().get(1).getSource(), server.url("/").toString() + "32x32_progressive.jpg");
        assertEquals(data.getImages().get(1).getDimension().getWidth(), 32);
        assertEquals(data.getImages().get(1).getDimension().getHeight(), 32);
        assertEquals(data.getImages().get(2).getSource(), server.url("/").toString() + "16x16.png");
        assertEquals(data.getImages().get(2).getDimension().getWidth(), 16);
        assertEquals(data.getImages().get(2).getDimension().getHeight(), 16);
        assertEquals(data.getImages().get(3).getSource(), server.url("/").toString() + "8x8.gif");
        assertEquals(data.getImages().get(3).getDimension().getWidth(), 8);
        assertEquals(data.getImages().get(3).getDimension().getHeight(), 8);
        assertEquals(data.getImages().get(4).getSource(), server.url("/").toString() + "4x4.bmp");
        assertEquals(data.getImages().get(4).getDimension().getWidth(), 4);
        assertEquals(data.getImages().get(4).getDimension().getHeight(), 4);
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

    private MockResponse fileResponse(String url)
    {
        File file = new File(url);
        try {
            return new MockResponse()
                    .setStatus("HTTP/1.1 200")
                    .addHeader("content-type: " + contentType(url))
                    .setBody(fileToBytes(file));
        }
        catch (IOException e)
        {
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
