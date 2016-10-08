package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.Dimension;
import com.schinizer.rxunfurl.model.ImageInfo;
import com.schinizer.rxunfurl.model.PreviewData;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;

public class RxUnfurl {

    private static final ByteString JPEG_START_MARKER = ByteString.decodeHex("FF");
    private static final ByteString JPEG_BASELINE_MARKER = ByteString.decodeHex("C0");
    private static final ByteString JPEG_PROGRESSIVE_MARKER = ByteString.decodeHex("C2");
    private static volatile OkHttpClient internalClient;

    public static void setInternalClient(OkHttpClient client) {
        if(client == null)
            return;

        if (internalClient == null) {
            synchronized (RxUnfurl.class) {
                if (internalClient == null) {
                    internalClient = client;
                }
            }
        }
    }

    public static Observable<PreviewData> generatePreview(String url) {
        if (internalClient == null) {
            synchronized (RxUnfurl.class) {
                if (internalClient == null) {
                    internalClient = new OkHttpClient();
                }
            }
        }

      if (!url.contains("http://") && !url.contains("https://")) {
        url = "http://" + url;
      }


        return Observable.just(url)
                .flatMap(new Func1<String, Observable<PreviewData>>() {
                    @Override
                    public Observable<PreviewData> call(String s) {
                        return extractData(s, internalClient);
                    }
                });
    }

    private static Observable<PreviewData> extractData(String url, final OkHttpClient client) {
        Request request = new Request.Builder()
                .url(url)
                .build();
        List<String> extractedURLs = new ArrayList<>();
        PreviewData previewData = new PreviewData();

        try {
            Response response = client.newCall(request).execute();

            // Check content type
            switch (response.body().contentType().type()) {
                // If its an image, just extract it
                case "image":
                    extractedURLs.add(url);
                    previewData.setUrl(url);
                    break;
                case "text":
                    switch (response.body().contentType().subtype()) {
                        // If its html, extract the meta tags
                        case "html":
                            Document document = Jsoup.parse(response.body().string(), url);

                            // Extract Open Graph data
                            // If no properties found, infer from existing information
                            for (Element property : document.select("meta[property^=og:]")) {
                                switch (property.attr("property")) {
                                    case "og:title":
                                        previewData.setTitle(property.attr("content"));
                                        break;
                                    case "og:url":
                                        previewData.setUrl(property.attr("abs:content"));
                                        break;
                                    case "og:description":
                                        previewData.setDescription(property.attr("content"));
                                        break;
                                    case "og:image":
                                        extractedURLs.add(property.attr("abs:content"));
                                        break;
                                }
                            }

                            // Fallback to <title>
                            if (StringUtil.isBlank(previewData.getTitle())) {
                                previewData.setTitle(document.title());
                            }
                            // Fallback to param listed url
                            if (StringUtil.isBlank(previewData.getUrl())) {
                                previewData.setUrl(url);
                            }
                            // Fallback to meta description
                            if (StringUtil.isBlank(previewData.getDescription())) {
                                for (Element property : document.select("meta[name=description]")) {
                                    String content = property.attr("content");

                                    if(!StringUtil.isBlank(content)) {
                                        previewData.setDescription(content);
                                        break;
                                    }
                                }
                            }
                            // Fallback to first <p> with text
                            if (StringUtil.isBlank(previewData.getDescription())) {
                                for (Element p : document.select("p")) {
                                    if (!p.text().equals("")) {
                                        previewData.setDescription(p.text());
                                        break;
                                    }
                                }
                            }

                            // Fallback to other media
                            if (extractedURLs.size() == 0) {
                                Elements media = document.select("[src]");
                                for (Element src : media) {
                                    if (src.tagName().equals("img"))
                                        extractedURLs.add(src.attr("abs:src"));
                                }
                            }

                            break;

                        default:
                            return Observable.empty();
                    }

                    break;

                default:
                    return Observable.empty();
            }

            Observable<PreviewData> meta = Observable.just(previewData);
            Observable<List<ImageInfo>> imgInfo = Observable.from(extractedURLs)
                    // Parse image only for their size
                    .flatMap(new Func1<String, Observable<ImageInfo>>() {
                        @Override
                        public Observable<ImageInfo> call(String s) {
                            return extractImageDimension(s, client);
                        }
                    })
                    // Sort the results according to resolution
                    .toSortedList(new Func2<ImageInfo, ImageInfo, Integer>() {
                        @Override
                        public Integer call(ImageInfo lhs, ImageInfo rhs) {
                            Integer lhsRes = lhs.getDimension().getWidth() * lhs.getDimension().getHeight();
                            Integer rhsRes = rhs.getDimension().getWidth() * rhs.getDimension().getHeight();

                            return rhsRes.compareTo(lhsRes);
                        }
                    });

            return Observable.zip(meta, imgInfo, new Func2<PreviewData, List<ImageInfo>, PreviewData>() {
                @Override
                public PreviewData call(PreviewData previewData, List<ImageInfo> images) {
                    previewData.setImages(images);
                    return previewData;
                }
            });
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    private static Observable<ImageInfo> extractImageDimension(String url, OkHttpClient client) {
        Response response = null;
        Request request = new Request.Builder()
                .url(url)
                .build();
        try {
            response = client.newCall(request).execute();
            switch (response.body().contentType().toString()) {
                case "image/jpeg":
                    return Observable.just(new ImageInfo(url, decodeJpegDimension(response.body().byteStream())));
                case "image/png":
                    return Observable.just(new ImageInfo(url, decodePngDimension(response.body().byteStream())));
                case "image/bmp":
                    return Observable.just(new ImageInfo(url, decodeBmpDimension(response.body().byteStream())));
                case "image/gif":
                    return Observable.just(new ImageInfo(url, decodeGifDimension(response.body().byteStream())));
                default:
                    return Observable.empty();
            }
        } catch (IOException e) {
            return Observable.error(e);
        } finally {
            if (response != null) response.body().close();
        }
    }

    private static Dimension decodeJpegDimension(InputStream in) throws IOException {
        // Jpeg stores in big endian
        BufferedSource jpegSource = Okio.buffer(Okio.source(in));
        Dimension dimension;

        while (true) {
            ByteString marker = jpegSource.readByteString(JPEG_START_MARKER.size());

            if (!marker.equals(JPEG_START_MARKER))
                continue;

            marker = jpegSource.readByteString(JPEG_START_MARKER.size());

            if (marker.equals(JPEG_BASELINE_MARKER) || marker.equals(JPEG_PROGRESSIVE_MARKER)) {
                jpegSource.skip(3);
                Short h = jpegSource.readShort();
                Short w = jpegSource.readShort();
                dimension = new Dimension(Integer.valueOf(w), Integer.valueOf(h));
                break;
            }

        }

        return dimension;
    }

    private static Dimension decodePngDimension(InputStream in) throws IOException {
        // Png stores in big endian
        BufferedSource pngSource = Okio.buffer(Okio.source(in));

        pngSource.skip(16);
        int w = pngSource.readInt();
        int h = pngSource.readInt();

        return new Dimension(w, h);
    }

    private static Dimension decodeBmpDimension(InputStream in) throws IOException {
        // Bmp stores in little endian
        BufferedSource bmpSource = Okio.buffer(Okio.source(in));

        bmpSource.skip(18);
        int w = bmpSource.readIntLe();
        int h = bmpSource.readIntLe();

        return new Dimension(w, h);
    }

    private static Dimension decodeGifDimension(InputStream in) throws IOException {
        // Gif stores in little endian
        BufferedSource gifSource = Okio.buffer(Okio.source(in));

        gifSource.skip(6);
        Short w = gifSource.readShortLe();
        Short h = gifSource.readShortLe();

        return new Dimension(Integer.valueOf(w), Integer.valueOf(h));
    }
}
