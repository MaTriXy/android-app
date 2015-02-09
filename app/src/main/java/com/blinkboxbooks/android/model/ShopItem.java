// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.model.helper.BookHelper;

import java.io.Serializable;

/**
 * Helper object which represents a book, its price and synopsis
 */
@SuppressWarnings("serial")
public class ShopItem implements Serializable {

    public final Book book;

    public BBBBookPrice price;

    public Integer position;

    public ShopItem(BBBBookInfo bookInfo) {
        book = BookHelper.createBook(bookInfo);
    }

    public ShopItem(Book book) {
        this.book = book;
    }

    public ShopItem(Book book, BBBBookPrice price) {
        this.book = book;
        this.price = price;
    }
}
