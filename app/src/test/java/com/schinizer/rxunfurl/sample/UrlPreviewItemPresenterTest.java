package com.schinizer.rxunfurl.sample;

import com.schinizer.rxunfurl.model.PreviewData;

import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.mockito.Mockito.verify;

public class UrlPreviewItemPresenterTest {

    @Mock
    UrlPreviewContract.View view;

    @Test
    public void populateViewTest() throws Exception {
        MockitoAnnotations.initMocks(this);

        UrlPreviewItemPresenter presenter = new UrlPreviewItemPresenter(new PreviewData());
        presenter.setView(view);
        presenter.GeneratePreview("http://asd.com");

        verify(view).populateView(Mockito.any(PreviewData.class));
    }
}