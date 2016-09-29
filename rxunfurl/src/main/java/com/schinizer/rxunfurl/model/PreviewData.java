package com.schinizer.rxunfurl.model;

import java.util.List;

/**
 * Created by tinkerbox on 18/8/16.
 */

public class PreviewData {

    private String url = "";
    private String title = "";
    private String description = "";
    private List<ImageInfo> images;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ImageInfo> getImages() {
        return images;
    }

    public void setImages(List<ImageInfo> images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "PreviewData{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", images=" + images +
                '}';
    }
}