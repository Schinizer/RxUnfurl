package com.schinizer.rxunfurl.sample;

import com.schinizer.rxunfurl.model.PreviewData;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

public class UrlPreviewItemPresenterTest {

    @Mock
    UrlPreviewContract.View view;

    @Test
    public void populateViewTest() throws Exception {
        initMocks(this);

        UrlPreviewItemPresenter presenter = new UrlPreviewItemPresenter(new PreviewData());
        presenter.setView(view);
        presenter.GeneratePreview("http://asd.com");

        verify(view).populateView(Mockito.any(PreviewData.class));
    }
}