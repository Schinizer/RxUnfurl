package com.schinizer.rxunfurl.sample;

import com.schinizer.rxunfurl.model.PreviewData;

/**
 * Created by tinkerbox on 31/8/16.
 */

interface UrlPreviewContract {

    interface View extends BaseView<Presenter> {
        void populateView(PreviewData data);
    }

    interface Presenter extends BasePresenter<View> {
        void GeneratePreview(String url);
    }
}
