// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class DateUtils {
    private static final String TAG = DateUtils.class.getSimpleName();
    /*
     * We specify Locale.US simply to ensure that we get ASCII characters, as the actual format
     * is agnostic of locale.
     */
    private static final DateFormat sShortDate = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final DateFormat sPrettyDate = new SimpleDateFormat("dd MMMMM yyyy", Locale.US);

    public static boolean isFuture(String dateStr) {
        Calendar date = getCalendar(dateStr);
        return date != null && Calendar.getInstance().before(date);
    }

    public static boolean isPast(String dateStr) {
        Calendar date = getCalendar(dateStr);
        return date != null && Calendar.getInstance().after(date);
    }

    public static boolean isFuture(long millis) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        return date != null && Calendar.getInstance().before(date);
    }

    public static boolean isPast(long millis) {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        return date != null && Calendar.getInstance().after(date);
    }

    public static Calendar getCalendar(String dateStr) {
        Calendar calendar = Calendar.getInstance();
        try {
            calendar.setTime(sShortDate.parse(dateStr));
        } catch (ParseException e) {
            LogUtils.w(TAG, "Error parsing date", e);
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar;
    }

    public static String prettify(String shortDate) {
        Calendar date = getCalendar(shortDate);
        String prettyDate = null;
        if (date != null) {
            prettyDate = sPrettyDate.format(date.getTime());
        }
        return prettyDate;
    }
}
