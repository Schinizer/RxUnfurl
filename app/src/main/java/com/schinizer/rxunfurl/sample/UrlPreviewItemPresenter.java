package com.schinizer.rxunfurl.sample;

import android.util.Log;

import com.schinizer.rxunfurl.RxUnfurl;
import com.schinizer.rxunfurl.model.PreviewData;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by tinkerbox on 31/8/16.
 */

class UrlPreviewItemPresenter implements UrlPreviewContract.Presenter {

    private UrlPreviewContract.View view;
    private PreviewData data = null;

    UrlPreviewItemPresenter() {}

    UrlPreviewItemPresenter(PreviewData data)
    {
        this.data = data;
    }

    @Override
    public void setView(UrlPreviewContract.View view) {
        this.view = view;
    }

    @Override
    public void GeneratePreview(String url) {
        if (data == null) {
            RxUnfurl.generatePreview(url)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<PreviewData>() {
                        @Override
                        public void call(PreviewData previewData) {
                            data = previewData;
                            view.populateView(data);
                            Log.d("RxUnfurl", data.toString());
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.d("RxUnfurl", throwable.toString());
                        }
                    });
        } else {
            view.populateView(data);
        }
    }
}
