// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.net.Uri;

import com.blinkboxbooks.android.BuildConfig;

import java.util.List;

/**
 * Contract for use with the CatalogueContentProvider
 */
 public class CatalogueContract {

    private static final String TAG = CatalogueContract.class.getSimpleName();

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID +".provider.catalogue";

    public static final Uri CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static final String EXCEPTION_MESSAGE_INTERNAL_SERVER_ERROR = "internal_server_error";
    public static final String EXCEPTION_MESSAGE_NO_NETWORK = "no_network";
    public static final String EXCEPTION_MESSAGE_NO_RESULTS= "no_results";

    public static class Promotions {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.provider.marketing.promotion";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(CatalogueContract.CONTENT_URI, "promotions");

        public static final String _ID = "_ID";
        public static final String NAME = "NAME";
        public static final String TITLE = "TITLE";
        public static final String SUBTITLE = "SUBTITLE";
        public static final String DISPLAY_NAME = "DISPLAY_NAME";
        public static final String ACTIVATED = "ACTIVATE";
        public static final String DEACTIVATED = "DEACTIVATED";
        public static final String SEQUENCE = "SEQUENCE";
        public static final String LOCATION = "LOCATION";
        public static final String IMAGE = "IMAGE";

        public static final String[] PROJECTION = {_ID, NAME, TITLE, SUBTITLE, DISPLAY_NAME, ACTIVATED, DEACTIVATED, SEQUENCE, LOCATION, IMAGE};

        public static Uri getPromotionWithLocationUri(Integer promotionLocation) {
            return CONTENT_URI.buildUpon().appendPath("promotion_location").appendPath(String.valueOf(promotionLocation)).build();
        }

        /**
         * Gets the promotion location from the Uri
         *
         * @param uri the Uri
         * @return the promotion location
         */
        public static Integer getPromotionLocation(Uri uri) {
            String segment = uri.getLastPathSegment();

            try {
                return Integer.parseInt(segment);
            } catch(NumberFormatException e) {}

            return null;
        }
    }

    public static class Books {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.provider.catalogue.book";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(CatalogueContract.CONTENT_URI, "books");

        public static final String _ID = "_ID";
        public static final String TITLE = "title";
        public static final String AUTHOR_NAME = "author_name";
        public static final String AUTHOR_ID = "author_id";
        public static final String PUBLICATION_DATE = "publication_date";
        public static final String SAMPLE_ELIGIBLE = "sample_eligible";
        public static final String SAMPLE_URI = "sample_uri";
        public static final String COVER_IMAGE_URL = "cover_image_url";
        public static final String PUBLISHER_NAME = "publisher_name";

        public static final String PRICE = "price";
        public static final String DISCOUNT_PRICE = "discounted_price";
        public static final String CLUBCARD_POINTS_AWARDED = "clubcard_points";
        public static final String CURRENCY = "currency";
        public static final String PURCHASE_DATE = "purchase_date";
        public static final String DOWNLOAD_STATUS = "download_status";
        public static final String LIBRARY_ID = "library_id";

        public static final String[] PROJECTION = {_ID, TITLE, AUTHOR_NAME, AUTHOR_ID, PUBLICATION_DATE, SAMPLE_ELIGIBLE, SAMPLE_URI, COVER_IMAGE_URL, PUBLISHER_NAME, PRICE, DISCOUNT_PRICE, CLUBCARD_POINTS_AWARDED, CURRENCY, PURCHASE_DATE, DOWNLOAD_STATUS, LIBRARY_ID};

        /**
         * Builds a Uri for getting books by category name
         *
         * @param categoryName the category name
         * @param addPurchaseInfo if true the 'is purchased' column for each row will be set to reflect the currently signed in user. This
         *                        creates additional work on the query so it should be set to false unless this data is required.
         * @param count the maximum number of books to be returned
         * @param offset the offset within the overall set of data to return the first item from
         * @return the Uri
         */
        public static Uri getBooksForCategoryNameUri(String categoryName, boolean addPurchaseInfo, int count, int offset) {
            Uri uri = CONTENT_URI.buildUpon().appendPath("category_name").appendPath(categoryName).appendPath("count").
                    appendPath(String.valueOf(count)).appendPath("offset").appendPath(String.valueOf(offset)).build();
            if (addPurchaseInfo) {
                uri = uri.buildUpon().appendPath("with_purchase_info").build();
            }
            return uri;
        }

        /**
         * Modifies an existing URI to update the count and offset values and returns the new URI
         * @param uri the URI to modify
         * @param count the new count value to set
         * @param offset the new offset value to set
         * @return the update URI with the new values
         */
        public static Uri refreshUriCountAndOffset(Uri uri, int count, int offset) {
            List<String> segments = uri.getPathSegments();

            Uri.Builder builder = CatalogueContract.CONTENT_URI.buildUpon();

            for (int i = 0; i < segments.size(); i++) {
                builder.appendPath(segments.get(i));
                if (segments.get(i).equals("count")) {
                    // Append the new count value to the new segments and skip over the next index in segments
                    builder.appendPath(String.valueOf(count));
                    i++;
                } else if (segments.get(i).equals("offset")) {
                    // Append the new offset value to the new segments and skip over the next index in segments
                    builder.appendPath(String.valueOf(offset));
                    i++;
                }
            }

            return builder.build();
        }

        /**
         * Builds a Uri for getting books related to the given ISBN
         *
         * @param isbn the isbn of the book
         * @param addPurchaseInfo if true the 'is purchased' column for each row will be set to reflect the currently signed in user. This
         *                        creates additional work on the query so it should be set to false unless this data is required.
         * @param count the new count value to set
         * @param offset the new offset value to set
         * @return the Uri
         */
        public static Uri getBooksRelatedToISBNUri(String isbn, boolean addPurchaseInfo, int count, int offset) {
            Uri uri = CONTENT_URI.buildUpon().appendPath("related").appendPath(isbn).appendPath("count").
                    appendPath(String.valueOf(count)).appendPath("offset").appendPath(String.valueOf(offset)).build();

            if (addPurchaseInfo) {
                uri = uri.buildUpon().appendPath("with_purchase_info").build();
            }
            return uri;
        }

        /**
         * Builds a Uri for getting books by author
         *
         * @param authorId the author id
         * @param addPurchaseInfo if true the 'is purchased' column for each row will be set to reflect the currently signed in user. This
         *                        creates additional work on the query so it should be set to false unless this data is required.
         * @return the Uri
         */
        public static Uri getBooksForAuthorIdUri(String authorId, boolean addPurchaseInfo, int count, int offset) {
            Uri uri = CONTENT_URI.buildUpon().appendPath("author").appendPath(authorId).appendPath("count").
                    appendPath(String.valueOf(count)).appendPath("offset").appendPath(String.valueOf(offset)).build();

            if (addPurchaseInfo) {
                uri = uri.buildUpon().appendPath("with_purchase_info").build();
            }
            return uri;
        }

        /**
         * Builds a Uri for getting books by category location
         * @param categoryLocation the category location
         * @param addPurchaseInfo if true the 'is purchased' column for each row will be set to reflect the currently signed in user. This
         *                        creates additional work on the query so it should be set to false unless this data is required.
         * @return the Uri
         */
        public static Uri getBooksForCategoryLocationUri(int categoryLocation, boolean addPurchaseInfo, int count, int offset) {
            Uri uri = CONTENT_URI.buildUpon().appendPath("category_location").appendPath(String.valueOf(categoryLocation)).appendPath("count").
                    appendPath(String.valueOf(count)).appendPath("offset").appendPath(String.valueOf(offset)).build();

            if (addPurchaseInfo) {
                uri = uri.buildUpon().appendPath("with_purchase_info").build();
            }
            return uri;
        }

        /**
         * Builds a Uri for searching for books
         *
         * @param query the search query. can be an ISBN
         * @param addPurchaseInfo if true the 'is purchased' column for each row will be set to reflect the currently signed in user. This
         *                        creates additional work on the query so it should be set to false unless this data is required.
         * @param count the maximum number of books to be returned
         * @param offset the offset within the overall set of data to return the first item from
         * @return the Uri
         */
        public static Uri getBookSearchUri(String query, boolean addPurchaseInfo, int count, int offset) {
            Uri uri = CONTENT_URI.buildUpon().appendPath("search").appendPath(query).appendPath("count").
                    appendPath(String.valueOf(count)).appendPath("offset").appendPath(String.valueOf(offset)).build();
            if (addPurchaseInfo) {
                uri = uri.buildUpon().appendPath("with_purchase_info").build();
            }
            return uri;
        }

        /**
         * Builds a Uri for getting books via promotion id
         *
         * @param promotionId
         * @param addPurchaseInfo if true the 'is purchased' column for each row will be set to reflect the currently signed in user. This
         *                        creates additional work on the query so it should be set to false unless this data is required.
         * @param count the maximum number of books to be returned
         * @return
         */
        public static Uri getBookForPromotion(int promotionId, boolean addPurchaseInfo, int count) {
            Uri uri = CONTENT_URI.buildUpon().appendPath("promotion").appendPath(String.valueOf(promotionId)).appendPath("count").
                    appendPath(String.valueOf(count)).build();
            if (addPurchaseInfo) {
                uri = uri.buildUpon().appendPath("with_purchase_info").build();
            }
            return uri;
        }

        /**
         * Gets the count value from the Uri
         *
         * @param uri the Uri
         * @return the count value
         */
        public static Integer getCount(Uri uri) {
            String segment = uri.getPathSegments().get(uri.getPathSegments().indexOf("count") + 1);

            try {
                return Integer.parseInt(segment);
            } catch(NumberFormatException e) {}

            return null;
        }

        /**
         * Gets the offset value from the Uri
         *
         * @param uri the Uri
         * @return the offset value
         */
        public static Integer getOffset(Uri uri) {
            String segment = uri.getPathSegments().get(uri.getPathSegments().indexOf("offset") + 1);

            try {
                return Integer.parseInt(segment);
            } catch(NumberFormatException e) {}

            return null;
        }

        /**
         * Gets the search query from the Uri
         *
         * @param uri the Uri
         * @return the search query
         */
        public static String getSearchQuery(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Gets the category name from the Uri
         *
         * @param uri the Uri
         * @return the category name
         */
        public static String getCategoryName(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Gets the author id from the Uri
         *
         * @param uri the Uri
         * @return the author id
         */
        public static String getAuthorId(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Gets the ISBN from the Uri
         *
         * @param uri the Uri
         * @return the isbn
         */
        public static String getISBN(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Gets the category location from the Uri
         *
         * @param uri the Uri
         * @return the category location
         */
        public static Integer getCategoryLocation(Uri uri) {
            String segment = uri.getPathSegments().get(2);

            try {
                return Integer.parseInt(segment);
            } catch(NumberFormatException e) {}

            return null;
        }

        /**
         * Gets the promotion location from the Uri
         *
         * @param uri the Uri
         * @return the promotion location
         */
        public static Integer getPromotionLocation(Uri uri) {
            String segment = uri.getPathSegments().get(2);

            try {
                return Integer.parseInt(segment);
            } catch(NumberFormatException e) {}

            return null;
        }
    }

    public static class SearchSuggestion {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.provider.catalogue.suggestion";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(CatalogueContract.CONTENT_URI, "suggestions");

        public static final String _ID = "_ID";
        public static final String TYPE = "type";
        public static final String TITLE = "title";

        public static final String _ID_LOWERCASE = "_id";
        public static final String SUGGEST_COLUMN_TEXT_1 = "suggest_text_1";
        public static final String SUGGEST_COLUMN_TEXT_2 = "suggest_text_2";
        public static final String SUGGEST_COLUMN_INTENT_DATA = "suggest_intent_data";
        public static final String SUGGEST_COLUMN_INTENT_EXTRA_DATA = "suggest_intent_extra_data";

        public static final String[] PROJECTION = {_ID, TYPE, TITLE};
        public static final String[] SEARCH_INTERFACE_PROJECTION = {_ID_LOWERCASE, SUGGEST_COLUMN_TEXT_1, SUGGEST_COLUMN_TEXT_2, SUGGEST_COLUMN_INTENT_DATA, SUGGEST_COLUMN_INTENT_EXTRA_DATA};

        /**
         * Builds a Uri for getting search suggestions for a query
         *
         * @param query the search query
         * @return the Uri
         */
        public static Uri getSearchSuggestionUri(String query) {
            return CONTENT_URI.buildUpon().appendPath(query).build();
        }

        /**
         * Gets the search suggestion query from the Uri
         *
         * @param uri the Uri
         * @return the search suggestion query
         */
        public static String getSearchSuggestionQuery(Uri uri) {
            return uri.getLastPathSegment();
        }
    }

    public static class Category {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.provider.catalogue.category";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(CatalogueContract.CONTENT_URI, "categories");

        public static final String _ID = "_ID";
        public static final String CATEGORY_NAME = "category_name";
        public static final String DISPLAY_NAME = "display_name";
        public static final String SEQUENCE = "sequence";
        public static final String LOCATION = "location";
        public static final String RECOMMENDED_SEQUENCE = "recommendedSequence";
        public static final String IMAGE_URL = "image_url";

        public static final String[] PROJECTION = {_ID, CATEGORY_NAME, DISPLAY_NAME, SEQUENCE, LOCATION, RECOMMENDED_SEQUENCE, IMAGE_URL};

        /**
         * Builds a Uri for getting all categories
         *
         * @return the Uri
         */
        public static Uri getCategories() {
            return CONTENT_URI;
        }

        /**
         * Builds a Uri for getting a category based on its location id
         *
         * @param locationId
         * @return
         */
        public static Uri getCategoriesForLocationId(int locationId) {
            return CONTENT_URI.buildUpon().appendPath("category_location").appendPath(String.valueOf(locationId)).build();
        }

        /**
         * Gets the category location from the Uri
         *
         * @param uri the Uri
         * @return the category location
         */
        public static Integer getCategoryLocation(Uri uri) {
            String segment = uri.getLastPathSegment();

            try {
                return Integer.parseInt(segment);
            } catch(NumberFormatException e) {}

            return null;
        }
    }

    public static class Author {

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.provider.catalogue.author";

        public static final Uri CONTENT_URI = Uri.withAppendedPath(CatalogueContract.CONTENT_URI, "author");

        public static final String _ID = "_ID";
        public static final String DISPLAY_NAME = "display_name";
        public static final String SORT_NAME = "sort_name";
        public static final String BOOKS_COUNT = "books_count";
        public static final String BIOGRAPHY = "biography";
        public static final String IMAGE_URL = "image_url";

        public static final String[] PROJECTION = {_ID, DISPLAY_NAME, SORT_NAME, BOOKS_COUNT, BIOGRAPHY, IMAGE_URL};

        /**
         * Gets the author id from the Uri
         *
         * @param uri the Uri
         * @return the author id
         */
        public static String getAuthorId(Uri uri) {
            return uri.getLastPathSegment();
        }

        /**
         * Builds a Uri for getting an author
         *
         * @param authorId the author id
         * @return the Uri
         */
        public static Uri getAuthorUri(String authorId) {
            return CONTENT_URI.buildUpon().appendPath(authorId).build();
        }
    }
}