// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.app.Activity;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.ui.shop.SearchActivity;

/**
 * Helper class for showing common dialogs
 */
public class DialogUtil {

    public static void showBookNotPartOfSampleDialog(final Activity activity, final Book book) {
        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(activity);
        builder.setTitle(activity.getString(R.string.title_buy_the_full_book_question));
        builder.setMessage(activity.getString(R.string.dialog_this_chapter_is_not_part_of_the_sample));
        builder.setNegativeButton(R.string.no_thanks, null);

        builder.setPositiveButton(R.string.buy_this_book, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(activity, SearchActivity.class);

                intent.putExtra(SearchActivity.ARG_VIEW_TYPE, SearchActivity.ViewType.SEARCH);
                intent.putExtra(SearchManager.QUERY, book.isbn);

                activity.startActivity(intent);
            }
        }).show();
    }
}