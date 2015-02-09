// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;

/**
 * Helper methods for notifications
 */
public class NotificationUtil {

    public static final int NOTIFICATION_ID_BOOK = 0;

    /**
     * Hide all notifications
     *
     * @param context
     */
    public static void hideNotifications(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }

    /**
     * Hide a particular notification
     *
     * @param context
     * @param id
     */
    public static void hideNotification(Context context, int id) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    /**
     * Update the reading notification
     *
     * @param context
     * @param id
     * @param builder, the existing notification
     */
    public static void updateNotification(final Context context, int id, NotificationCompat.Builder builder) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    /**
     * Show a reading notification
     *
     * @param context
     * @param book
     */
    public static void showReadingNotification(final Context context, final Book book, Bookmark bookmark) {
        // Hide the download notification, if it exists
        hideNotification(context, NOTIFICATION_ID_BOOK);

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(context.getString(R.string.notification_reading_s, book.title))
                .setContentText(context.getString(R.string.notification_s_read, bookmark.percentage));

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return getNotificationBitmap(context, book);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                showNotification(context, NOTIFICATION_ID_BOOK, book, builder, bitmap);
            }
        }.execute();
    }


    /**
     * Show a download complete notification
     *
     * @param context
     * @param book
     */
    public static void showDownloadCompleteNotification(final Context context, final Book book) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setContentTitle(book.author)
                .setContentText(book.title);

        new AsyncTask<Void, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                return getNotificationBitmap(context, book);
            }

            @Override
            protected void onPostExecute(Bitmap bitmap) {
                showNotification(context, NOTIFICATION_ID_BOOK, book, builder, bitmap);
            }
        }.execute();
    }

    private static Bitmap getNotificationBitmap(Context context, Book book) {
        Bitmap bitmap = null;
        String coverUrl = book.getFormattedThumbnailUrl();
        if (coverUrl != null) {
            bitmap = BBBImageLoader.getInstance().getCachedBitmap(coverUrl);
            if (bitmap != null) {
                //Resize bitmap to cropped thumbnail image
                Resources res = context.getResources();
                int height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
                int width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
                //return ThumbnailUtils.extractThumbnail(bitmap, width, height);
                return createThumbnail(bitmap, width, height);
            }
        }
        return bitmap;
    }

    private static void showNotification(Context context, int id, Book book, NotificationCompat.Builder builder, Bitmap bitmap) {
        Intent resultIntent = LibraryController.getOpenBookIntent(context, book);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setSmallIcon(R.drawable.notification_bar_icon)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        if (bitmap != null) {
            builder.setLargeIcon(bitmap);
        }

        updateNotification(context, id, builder);
    }

    private static Bitmap createThumbnail(Bitmap source, int newHeight, int newWidth) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        // Compute the scaling factors to fit the new height and width, respectively.
        // To cover the final image, the final scaling will be the bigger
        // of these two.
        float xScale = (float) newWidth / sourceWidth;
        float yScale = (float) newHeight / sourceHeight;
        float scale = Math.min(xScale, yScale);

        // Now get the size of the source bitmap when scaled
        float scaledWidth = scale * sourceWidth;
        float scaledHeight = scale * sourceHeight;

        // Let's find out the upper left coordinates if the scaled bitmap
        // should be centered in the new size give by the parameters
        float left = (newWidth - scaledWidth) / 2;
        float top = (newHeight - scaledHeight) / 2;

        // The target rectangle for the new, scaled version of the source bitmap will now be
        RectF targetRect = new RectF(left, top, left + scaledWidth, top + scaledHeight);

        // Finally, we create a new bitmap of the specified size and draw our new,
        // scaled bitmap onto it.
        Bitmap dest = Bitmap.createBitmap(newWidth, newHeight, source.getConfig());
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(source, null, targetRect, null);

        return dest;
    }
}