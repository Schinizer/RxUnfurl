package com.schinizer.rxunfurl.sample;

import android.util.Log;

import com.schinizer.rxunfurl.RxUnfurl;
import com.schinizer.rxunfurl.model.PreviewData;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableSingleObserver;

/**
 * Created by Schinizer on 31/8/16.
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
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableSingleObserver<PreviewData>() {
                        @Override
                        public void onSuccess(PreviewData previewData) {
                            data = previewData;
                            view.populateView(data);
                            Log.d("RxUnfurl", data.toString());
                        }

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                        }
                    });
        } else {
            view.populateView(data);
        }
    }
}
