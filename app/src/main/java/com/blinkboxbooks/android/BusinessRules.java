// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android;

import com.blinkboxbooks.android.util.BBBCalendarUtil;

/**
 * Class for storing business constants
 */
public class BusinessRules {

    /**
     * If a book has a purchase date that falls within this time period in the past, we can consider it to be a 'recent' book
     */
    public static final long RECENT_BOOKS_TIME_PERIOD = BBBCalendarUtil.TIME_PERIOD_1_DAY * 3;

    /**
     * The default account name for a user that is not logged in
     */
    public static final String DEFAULT_ACCOUNT_NAME = "Anonymous";

    /**
     * The maximum length of a name
     */
    public static final int NAME_MAX_LENGTH = 50;

    /**
     * The minimum length of a name
     */
    public static final int NAME_MIN_LENGTH = 1;

    /**
     * The minimum length of a password
     */
    public static final int PASSWORD_MIN_LENGTH = 6;

    /**
     * Regular expression defining a valid email
     */
    public static final String REGULAR_EXPRESSION_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";

    /**
     * Regular expression defining a valid uk postcode
     */
    public static final String REGULAR_EXPRESSION_UK_POSTCODE = "^(GIR ?0AA)|((([A-PR-UWYZa-pr-uwyz][0-9][0-9]?)|(([A-PR-UWYZa-pr-uwyz][A-HK-Ya-hk-y][0-9][0-9]?)|(([A-PR-UWYZa-pr-uwyz][0-9][A-HJKPSTUWa-hjkpstuw])|([A-PR-UWYZa-pr-uwyz][A-HK-Ya-hk-y][0-9][ABEHMNPRVWXYabehmnprvwxy])))) ?[0-9][ABD-HJLNP-UW-Zabd-hjlnp-uw-z]{2})$";
}
