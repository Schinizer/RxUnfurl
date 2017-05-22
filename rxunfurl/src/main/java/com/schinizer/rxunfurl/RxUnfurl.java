package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.ImageInfo;
import com.schinizer.rxunfurl.model.PreviewData;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Maybe;
import io.reactivex.MaybeSource;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RxUnfurl {

    private OkHttpClient client;
    private Scheduler scheduler;
    private ImageDecoder decoder = new ImageDecoder();

    private RxUnfurl(@NonNull OkHttpClient client, @NonNull Scheduler scheduler) {
        this.client = client;
        this.scheduler = scheduler;
    }

    public Single<PreviewData> generatePreview(@NonNull String url) {
        return Single.just(url)
                .map(new Function<String, Response>() {
                    @Override
                    public Response apply(@NonNull String url) throws Exception {
                        return client.newCall(new Request.Builder().url(url).build()).execute();
                    }
                })
                .subscribeOn(scheduler)
                .flatMap(new Function<Response, SingleSource<AbstractMap.SimpleEntry<PreviewData, List<String>>>>() {
                    @Override
                    public SingleSource<AbstractMap.SimpleEntry<PreviewData, List<String>>> apply(@NonNull Response response) throws Exception {
                        PreviewData previewData = new PreviewData();
                        List<String> extractedURLs = new ArrayList<>();
                        String url = response.request().url().toString();

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

                                                if (!StringUtil.isBlank(content)) {
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
                                }

                                break;
                        }

                        return Single.just(new AbstractMap.SimpleEntry<>(previewData, extractedURLs));
                    }
                })
                .subscribeOn(scheduler)
                .flatMap(new Function<AbstractMap.SimpleEntry<PreviewData, List<String>>, SingleSource<PreviewData>>() {
                    @Override
                    public SingleSource<PreviewData> apply(@NonNull AbstractMap.SimpleEntry<PreviewData, List<String>> pair) throws Exception {
                        return Single.zip(
                                Single.just(pair.getKey()),
                                processImageDimension(pair.getValue()),
                                new BiFunction<PreviewData, List<ImageInfo>, PreviewData>() {
                            @Override
                            public PreviewData apply(@NonNull PreviewData previewData, @NonNull List<ImageInfo> image) throws Exception {
                                previewData.setImages(image);
                                return previewData;
                            }
                        });
                    }
                });
    }

    private Single<List<ImageInfo>> processImageDimension(@NonNull List<String> urls) {
        return Observable.fromIterable(urls)
                // Only query distinct urls
                .distinct()
                // Parse image only for their size
                .concatMapEager(new Function<String, ObservableSource<ImageInfo>>() {
                    @Override
                    public ObservableSource<ImageInfo> apply(@NonNull String url) {
                        return extractImageDimension(url).toObservable();
                    }
                })
                // Sort the results according to resolution
                .toSortedList(new Comparator<ImageInfo>() {
                    @Override
                    public int compare(@NonNull ImageInfo lhs, @NonNull ImageInfo rhs) {
                        Integer lhsRes = lhs.getDimension().getWidth() * lhs.getDimension().getHeight();
                        Integer rhsRes = rhs.getDimension().getWidth() * rhs.getDimension().getHeight();

                        return rhsRes.compareTo(lhsRes);
                    }
                });
    }

    private Maybe<ImageInfo> extractImageDimension(@NonNull String url) {
        return Maybe.just(url)
                .flatMap(new Function<String, MaybeSource<ImageInfo>>() {
                    @Override
                    public MaybeSource<ImageInfo> apply(@NonNull String url) throws Exception {
                        Request request = new Request.Builder()
                                .url(url)
                                .build();
                        Response response = client.newCall(request).execute();

                        try {
                            switch (response.body().contentType().toString()) {
                                case "image/jpeg":
                                    return Maybe.just(new ImageInfo(url, decoder.decodeJpegDimension(response.body().source())));
                                case "image/png":
                                    return Maybe.just(new ImageInfo(url, decoder.decodePngDimension(response.body().source())));
                                case "image/bmp":
                                    return Maybe.just(new ImageInfo(url, decoder.decodeBmpDimension(response.body().source())));
                                case "image/gif":
                                    return Maybe.just(new ImageInfo(url, decoder.decodeGifDimension(response.body().source())));
                                default:
                                    return Maybe.empty();
                            }
                        } finally {
                            if (response != null) response.body().close();
                        }
                    }
                })
                // Chain should not fail if an image fails to load
                // We will just resume with an empty source
                .onErrorResumeNext(new Function<Throwable, MaybeSource<? extends ImageInfo>>() {
                    @Override
                    public MaybeSource<? extends ImageInfo> apply(Throwable throwable) throws Exception {
                        return Maybe.empty();
                    }
                })
                .subscribeOn(scheduler);
    }

    public static final class Builder {
        private OkHttpClient client;
        private Scheduler scheduler;

        public Builder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        public Builder scheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
            return this;
        }

        public RxUnfurl build() {
            if (client == null) {
                client = new OkHttpClient();
            }

            if (scheduler == null) {
                scheduler = Schedulers.trampoline();
            }

            return new RxUnfurl(client, scheduler);
        }
    }
}
