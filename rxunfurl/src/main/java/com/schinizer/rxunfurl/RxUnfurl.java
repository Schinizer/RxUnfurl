package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.ImageInfo;
import com.schinizer.rxunfurl.model.PreviewData;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

public class RxUnfurl {

    private OkHttpClient client;
    private Scheduler scheduler;
    private ImageDecoder decoder = new ImageDecoder();

    private RxUnfurl(OkHttpClient client, Scheduler scheduler)
    {
        this.client = client;
        this.scheduler = scheduler;
    }

    public Observable<PreviewData> generatePreview(String url) {
        return extractData(url);
    }

    private Observable<PreviewData> extractData(String url) {
        return Observable.just(url)
                .flatMap(new Func1<String, Observable<AbstractMap.SimpleEntry<PreviewData, List<String>>>>() {
                    @Override
                    public Observable<AbstractMap.SimpleEntry<PreviewData, List<String>>> call(String url) {
                        Request request = new Request.Builder()
                                .url(url)
                                .build();
                        PreviewData previewData = new PreviewData();
                        List<String> extractedURLs = new ArrayList<>();

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
                                                    if (src.tagName().equals("img")) {
                                                        extractedURLs.add(src.attr("abs:src"));
                                                    }
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

                            return Observable.just(new AbstractMap.SimpleEntry<>(previewData, extractedURLs));
                        } catch (IOException e) {
                            return Observable.error(e);
                        }
                    }
                })
                .flatMap(new Func1<AbstractMap.SimpleEntry<PreviewData, List<String>>, Observable<PreviewData>>() {
                    @Override
                    public Observable<PreviewData> call(AbstractMap.SimpleEntry<PreviewData, List<String>> pair) {
                        Observable<PreviewData> meta = Observable.just(pair.getKey());
                        Observable<List<ImageInfo>> imgInfo = processImageDimension(pair.getValue());

                        return Observable.zip(meta, imgInfo, new Func2<PreviewData, List<ImageInfo>, PreviewData>() {
                            @Override
                            public PreviewData call(PreviewData previewData, List<ImageInfo> images) {
                                previewData.setImages(images);
                                return previewData;
                            }
                        });
                    }
                });
    }

    private Observable<List<ImageInfo>> processImageDimension(List<String> urls)
    {
        return Observable.from(urls)
                // Only query distinct urls
                .distinct()
                // Parse image only for their size
                .concatMapEager(new Func1<String, Observable<ImageInfo>>() {
                    @Override
                    public Observable<ImageInfo> call(String url) {
                        return extractImageDimension(url);
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
                })
                .subscribeOn(scheduler);
    }

    private Observable<ImageInfo> extractImageDimension(String url) {
        return Observable.just(url)
                .flatMap(new Func1<String, Observable<ImageInfo>>() {
                    @Override
                    public Observable<ImageInfo> call(String url) {
                        Response response = null;
                        Request request = new Request.Builder()
                                .url(url)
                                .build();
                        try {
                            response = client.newCall(request).execute();
                            switch (response.body().contentType().toString()) {
                                case "image/jpeg":
                                    return Observable.just(new ImageInfo(url, decoder.decodeJpegDimension(response.body().byteStream())));
                                case "image/png":
                                    return Observable.just(new ImageInfo(url, decoder.decodePngDimension(response.body().byteStream())));
                                case "image/bmp":
                                    return Observable.just(new ImageInfo(url, decoder.decodeBmpDimension(response.body().byteStream())));
                                case "image/gif":
                                    return Observable.just(new ImageInfo(url, decoder.decodeGifDimension(response.body().byteStream())));
                                default:
                                    return Observable.empty();
                            }
                        }
                        catch (IOException e) {
                            return Observable.empty();
                        }
                        finally {
                            if (response != null) response.body().close();
                        }
                    }
                })
                .subscribeOn(scheduler);
    }

    public static final class Builder {
        private OkHttpClient client;
        private Scheduler scheduler;

        public Builder client(OkHttpClient client)
        {
            this.client = client;
            return this;
        }

        public Builder scheduler(Scheduler scheduler)
        {
            this.scheduler = scheduler;
            return this;
        }

        public RxUnfurl build()
        {
            if(client == null)
            {
                client = new OkHttpClient();
            }

            if(scheduler == null)
            {
                scheduler = Schedulers.immediate();
            }

            return new RxUnfurl(client, scheduler);
        }
    }
}
