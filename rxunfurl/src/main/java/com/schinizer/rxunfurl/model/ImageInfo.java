package com.schinizer.rxunfurl.model;

/**
 * Created by tinkerbox on 17/8/16.
 */

public class ImageInfo {

    private String source;
    private Dimension dimension;

    public ImageInfo(String source, Dimension dimension)
    {
        this.source = source;
        this.dimension = dimension;
    }

    public String getSource()
    {
        return source;
    }

    public Dimension getDimension()
    {
        return dimension;
    }

    @Override
    public String toString() {
        return "ImageInfo{" +
                "source='" + source + '\'' +
                ", dimension=" + dimension +
                '}';
    }
}
