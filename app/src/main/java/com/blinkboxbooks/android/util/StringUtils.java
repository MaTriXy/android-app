// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;

import com.blinkboxbooks.android.R;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Helper class for performing string related functions
 */
public class StringUtils {

    private static final StrikethroughSpan STRIKETHROUGH_SPAN = new StrikethroughSpan();
    private static final StyleSpan STYLE_SPAN_BOLD = new StyleSpan(Typeface.BOLD);

    /**
     * Injects some parameters into the middle of a URL after the host and before the path
     *
     * @param url
     * @param params
     * @return
     */
    public static String injectIntoResourceUrl(String url, String params) {

        if (url == null) {
            return url;
        }

        if (url.startsWith("http")) {
            final Uri uri = Uri.parse(url);
            StringBuilder stringBuilder = new StringBuilder(uri.getScheme());
            stringBuilder.append("://");
            stringBuilder.append(uri.getHost());
            stringBuilder.append(params);
            stringBuilder.append(uri.getPath());
            return stringBuilder.toString();
        } else {
            return url;
        }
    }

    /**
     * Gets the last component of a URL
     *
     * @param url
     * @return
     */
    public static String getLastPathSegment(String url) {

        if (url == null) {
            return null;
        }

        return Uri.parse(url).getLastPathSegment();
    }

    /**
     * Formats a book title and author
     *
     * @param resources
     * @param title
     * @param author
     * @return
     */
    public static Spannable formatTitleAuthor(Resources resources, String title, String author) {
        return formatTitleAuthor(resources,title,author,true);
    }

    /**
     * Formats a book title and author
     *
     * @param resources
     * @param title
     * @param author
     * @param boldTitle
     * @return
     */
    public static Spannable formatTitleAuthor(Resources resources, String title, String author, boolean boldTitle) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(resources.getString(R.string.shop_title_author, title, author));

        if ((boldTitle)&&(title != null)) {
            ssb.setSpan(STYLE_SPAN_BOLD, 0, title.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        }

        return ssb;
    }

    /**
     * Formats a price with the given currency
     *
     * @param currency the currency code. passing null will cause this to use the default local currency
     * @param price    the price you want to format
     * @param freeString    the optional string which should be returned if the price is free (may be null)
     * @return the formatted currency object
     */
    public static String formatPrice(String currency, double price, String freeString) {
        NumberFormat format = NumberFormat.getCurrencyInstance();

        if (price <= 0.0 && !BBBTextUtils.isEmpty(freeString)) {
            return freeString;
        }

        if (currency == null) {
            format.setCurrency(Currency.getInstance(Locale.getDefault()));
        } else {
            format.setCurrency(Currency.getInstance(currency));
        }

        return format.format(price);
    }

    /**
     * Formats a discount with the given currency. The supplied string resource ID will be loaded and
     * searched for the price, which will then have a strikethru span applied. Only the first
     * occurrence of the price will be matched.
     *
     * @param resources       The Resources object from which the string resource will be obtained.
     * @param stringId        The resource ID of the format string to use.
     * @param currency        the currency code. passing null will cause this to use the default local currency
     * @param price           the full price of the item
     * @param discountedPrice the discounted price of the item
     * @return the formatted discount string, or null if the price or discountedPrice is 0.0
     */
    public static Spanned formatDiscount(Resources resources, int stringId, String currency, double price, Double discountedPrice) {
        Spanned str = null;

        // Check price is greater than 0 to avoid divide by zero
        if (discountedPrice != null && price > 0.0) {
            NumberFormat format = NumberFormat.getCurrencyInstance();

            if (currency == null) {
                format.setCurrency(Currency.getInstance(Locale.getDefault()));
            } else {
                format.setCurrency(Currency.getInstance(currency));
            }

            SpannableStringBuilder sb = new SpannableStringBuilder();
            String p = format.format(price);
            String s = resources.getString(stringId, p, (int) Math.round((1.0 - (discountedPrice / price)) * 100));
            sb.append(s);
            int start = s.indexOf(p);

            if (start >= 0) {
                sb.setSpan(STRIKETHROUGH_SPAN, start, start + p.length(), Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            }

            str = sb;
        }

        return str;
    }

    public static String getCurrencySymbol(String currency) {
        if(currency.startsWith("GBP")) {return "£";}
        if(currency.startsWith("USD")) {return "$";}
        if(currency.startsWith("EUR")) {return "€";}
        return currency;
    }

    /**
     * The standard trim doesn't get rid of new lines, so we have our own custom trim functionality to remove
     * all 'white space' characters from a CharSequence.
     * @param string the text string
     * @param start the index to start trimming from
     * @param end the index to end trimming
     * @return a CharSequence with all whitespace trimmed
     */
    public static CharSequence trimAllWhiteSpace(CharSequence string, int start, int end) {
        while (start < end && Character.isWhitespace(string.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(string.charAt(end - 1))) {
            end--;
        }

        return string.subSequence(start, end);
    }
}
