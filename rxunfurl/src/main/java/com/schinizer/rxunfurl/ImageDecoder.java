package com.schinizer.rxunfurl;

import com.schinizer.rxunfurl.model.Dimension;

import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

import okio.BufferedSource;
import okio.ByteString;

/**
 * Created by Schinizer on 11/11/2016.
 */

class ImageDecoder {

    private static final ByteString JPEG_START_MARKER = ByteString.decodeHex("FF");
    private static final ByteString JPEG_BASELINE_MARKER = ByteString.decodeHex("C0");
    private static final ByteString JPEG_PROGRESSIVE_MARKER = ByteString.decodeHex("C2");

    Dimension decodeJpegDimension(BufferedSource jpegSource) throws IOException {
        // Jpeg stores in big endian
        Dimension dimension;

        while (true) {
            ByteString marker = jpegSource.readByteString(JPEG_START_MARKER.size());

            if (!marker.equals(JPEG_START_MARKER))
                continue;

            marker = jpegSource.readByteString(JPEG_START_MARKER.size());

            if (marker.equals(JPEG_BASELINE_MARKER) || marker.equals(JPEG_PROGRESSIVE_MARKER)) {
                jpegSource.skip(3);
                Short h = jpegSource.readShort();
                Short w = jpegSource.readShort();

                if(h < 0 || w < 0) {
                    throw new InvalidPropertiesFormatException("Invalid width and height");
                }

                dimension = new Dimension(Integer.valueOf(w), Integer.valueOf(h));
                break;
            }

        }

        return dimension;
    }

    Dimension decodePngDimension(BufferedSource pngSource) throws IOException {
        // Png stores in big endian
        pngSource.skip(16);
        int w = pngSource.readInt();
        int h = pngSource.readInt();

        return new Dimension(w, h);
    }

    Dimension decodeBmpDimension(BufferedSource bmpSource) throws IOException {
        // Bmp stores in little endian
        bmpSource.skip(18);
        int w = bmpSource.readIntLe();
        int h = bmpSource.readIntLe();

        return new Dimension(w, h);
    }

    Dimension decodeGifDimension(BufferedSource gifSource) throws IOException {
        // Gif stores in little endian
        gifSource.skip(6);
        Short w = gifSource.readShortLe();
        Short h = gifSource.readShortLe();

        return new Dimension(Integer.valueOf(w), Integer.valueOf(h));
    }
}
