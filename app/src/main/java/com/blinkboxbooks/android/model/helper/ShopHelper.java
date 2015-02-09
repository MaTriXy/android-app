// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model.helper;

import android.database.Cursor;

import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.util.BBBCalendarUtil;

import java.util.Calendar;

/**
 * Helper for shop model classes
 */
public class ShopHelper {

    public static ShopItem getShopItem(Cursor cursor) {

        Book book = new Book();
        book.title = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.TITLE));
        book.author = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.AUTHOR_NAME));
        book.authorId = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.AUTHOR_ID));
        book.cover_url = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.COVER_IMAGE_URL));
        book.publisher = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.PUBLISHER_NAME));
        book.isbn = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books._ID));
        book.id = cursor.getLong(cursor.getColumnIndex(CatalogueContract.Books.LIBRARY_ID));
        book.sample_uri = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.SAMPLE_URI));
        book.sample_eligible = cursor.getInt(cursor.getColumnIndex(CatalogueContract.Books.SAMPLE_ELIGIBLE)) == 1;
        book.purchase_date = cursor.getLong(cursor.getColumnIndex(CatalogueContract.Books.PURCHASE_DATE));
        book.download_status = cursor.getInt(cursor.getColumnIndex(CatalogueContract.Books.DOWNLOAD_STATUS));

        String publicationDate = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.PUBLICATION_DATE));
        Calendar publishedDate = BBBCalendarUtil.attemptParse(publicationDate, BBBCalendarUtil.FORMAT_YEAR_MONTH_DAY, BBBCalendarUtil.FORMAT_YEAR, BBBCalendarUtil.FORMAT_TIME_STAMP);

        if (publishedDate != null) {
            book.publication_date = publishedDate.getTimeInMillis();
        }

        BBBBookPrice price = new BBBBookPrice();
        price.price = cursor.getDouble(cursor.getColumnIndex(CatalogueContract.Books.PRICE));
        price.discountPrice = cursor.getDouble(cursor.getColumnIndex(CatalogueContract.Books.DISCOUNT_PRICE));
        price.currency = cursor.getString(cursor.getColumnIndex(CatalogueContract.Books.CURRENCY));
        price.clubcardPointsAward = cursor.getInt(cursor.getColumnIndex(CatalogueContract.Books.CLUBCARD_POINTS_AWARDED));

        ShopItem shopItem = new ShopItem(book, price);

        return shopItem;
    }
}