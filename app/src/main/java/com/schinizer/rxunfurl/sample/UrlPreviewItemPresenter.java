package com.schinizer.rxunfurl.sample;

import android.util.Log;

import com.schinizer.rxunfurl.RxUnfurl;
import com.schinizer.rxunfurl.model.PreviewData;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by tinkerbox on 31/8/16.
 */

class UrlPreviewItemPresenter implements UrlPreviewContract.Presenter {

    private UrlPreviewContract.View view;
    private PreviewData data = null;
    private RxUnfurl rxUnfurl;

    UrlPreviewItemPresenter(RxUnfurl rxUnfurl) {
        this.rxUnfurl = rxUnfurl;
    }

    UrlPreviewItemPresenter(PreviewData data) {
        this.data = data;
    }

    @Override
    public void setView(UrlPreviewContract.View view) {
        this.view = view;
    }

    @Override
    public void GeneratePreview(String url) {
        if (data == null) {
            rxUnfurl.generatePreview(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<PreviewData>() {
                        @Override
                        public void accept(PreviewData previewData) {
                            data = previewData;
                            view.populateView(data);
                            Log.d("RxUnfurl", data.toString());
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) {
                            Log.d("RxUnfurl", throwable.toString());
                        }
                    });
        } else {
            view.populateView(data);
        }
    }
}
