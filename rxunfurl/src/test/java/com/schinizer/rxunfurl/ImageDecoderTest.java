package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.Dimension;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;

import static junit.framework.TestCase.assertEquals;

/**
 * Created by DPSUser on 11/13/2016.
 */

public class ImageDecoderTest {
    private ImageDecoder decoder = new ImageDecoder();

    @Test
    public void testDecodeJpegBaseLine() throws IOException
    {
        URL baseline = ImageDecoderTest.class.getClassLoader().getResource("64x64_baseline.jpg");
        FileInputStream inputStream = new FileInputStream((baseline == null) ? "" : baseline.getPath());
        Dimension dimension = decoder.decodeJpegDimension(inputStream);

        assertEquals(dimension.getWidth(), 64);
        assertEquals(dimension.getHeight(), 64);
    }

    @Test
    public void testDecodeJpegProgressive() throws IOException
    {
        URL progressive = ImageDecoderTest.class.getClassLoader().getResource("32x32_progressive.jpg");
        FileInputStream inputStream = new FileInputStream((progressive == null) ? "" : progressive.getPath());
        Dimension dimension = decoder.decodeJpegDimension(inputStream);

        assertEquals(dimension.getWidth(), 32);
        assertEquals(dimension.getHeight(), 32);
    }

    @Test
    public void testDecodePng() throws IOException
    {
        URL png = ImageDecoderTest.class.getClassLoader().getResource("16x16.png");
        FileInputStream inputStream = new FileInputStream((png == null) ? "" : png.getPath());
        Dimension dimension = decoder.decodePngDimension(inputStream);

        assertEquals(dimension.getWidth(), 16);
        assertEquals(dimension.getHeight(), 16);
    }

    @Test
    public void testDecodeGif() throws IOException
    {
        URL gif = ImageDecoderTest.class.getClassLoader().getResource("8x8.gif");
        FileInputStream inputStream = new FileInputStream((gif == null) ? "" : gif.getPath());
        Dimension dimension = decoder.decodeGifDimension(inputStream);

        assertEquals(dimension.getWidth(), 8);
        assertEquals(dimension.getHeight(), 8);
    }

    @Test
    public void testDecodeBmp() throws IOException
    {
        URL bmp = ImageDecoderTest.class.getClassLoader().getResource("4x4.bmp");
        FileInputStream inputStream = new FileInputStream((bmp == null) ? "" : bmp.getPath());
        Dimension dimension = decoder.decodeBmpDimension(inputStream);

        assertEquals(dimension.getWidth(), 4);
        assertEquals(dimension.getHeight(), 4);
    }
}
