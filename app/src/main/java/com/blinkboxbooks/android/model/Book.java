// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.util.BBBImageLoader;

import java.io.Serializable;

/**
 * Book POJO
 */
@SuppressWarnings("serial")
public class Book implements Serializable {

    public long id;
    public long library_id;
    public int sync_state;
    public String server_id;
    public String authorId;
    public String author;
    public String isbn;
    public String title;
    public String tags;
    public String cover_url;
    public float offer_price;
    public float normal_price;
    public String description;
    public String publisher;
    public long purchase_date;
    public long update_date;
    public long sync_date;
    public long publication_date;
    public int state;
    public int download_count;
    public boolean in_device_library;
    public boolean embedded_book;
    public boolean sample_book;
    public boolean sample_eligible;
    public String sample_uri;
    public String format;
    public String file_path;
    public String media_path;
    public String key_path;
    public long file_size;
    public int download_status;
    public long download_offset;
    public byte[] enc_key;

    //helper variables
    private transient String formatted_fullcover_url;
    private transient String formatted_thumbnail_url;

    private transient String mToString = null;

    public String getFormattedFullcoverUrl() {

        if (embedded_book) {
            return cover_url;
        }

        if (cover_url == null || !(cover_url.startsWith("http") || cover_url.startsWith("file"))) {
            return null;
        }

        if (formatted_fullcover_url == null) {
            formatted_fullcover_url = BBBImageLoader.injectWidthIntoCoverUrl(cover_url, LibraryController.fullScreenImageWidth);
        }

        return formatted_fullcover_url;
    }

    public String getFormattedThumbnailUrl() {

        if (embedded_book) {
            return cover_url;
        }

        if (cover_url == null || !(cover_url.startsWith("http") || cover_url.startsWith("file"))) {
            return null;
        }

        if (formatted_thumbnail_url == null) {
            formatted_thumbnail_url = BBBImageLoader.injectWidthIntoCoverUrl(cover_url, LibraryController.bookCoverWidth);
        }

        return formatted_thumbnail_url;
    }

    @Override
    public String toString() {
        if (mToString == null) {
            StringBuilder builder = new StringBuilder();

            builder.append("id: ");
            builder.append(id);
            builder.append(", isbn: ");
            builder.append(isbn);
            builder.append(", title: ");
            builder.append(title);
            builder.append(", author: ");
            builder.append(author);
            builder.append(", state: ");
            builder.append(state);
            builder.append(", download status: ");
            builder.append(download_status);
            builder.append(", embedded? ");
            builder.append(embedded_book);
            builder.append(", sample? ");
            builder.append(sample_book);
            builder.append(", in device library? ");
            builder.append(in_device_library);
            mToString = builder.toString();
        }

        return mToString;
    }
}