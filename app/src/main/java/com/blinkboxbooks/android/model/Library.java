// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

/**
 * Library POJO
 */
public class Library {

    public long id;
    public String date_created;
    public String date_library_last_sync;
    public String date_bookmark_last_sync;
    public String account;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append(id);
        builder.append(", ");
        builder.append(account);
        builder.append(", ");
        builder.append(date_created);
        builder.append(", ");
        builder.append(date_library_last_sync);
        builder.append(", ");
        builder.append(date_bookmark_last_sync);

        return builder.toString();
    }
}