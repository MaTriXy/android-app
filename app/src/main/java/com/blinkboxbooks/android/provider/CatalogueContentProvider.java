// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBBookInfoList;
import com.blinkboxbooks.android.api.model.BBBBookPrice;
import com.blinkboxbooks.android.api.model.BBBBookPriceList;
import com.blinkboxbooks.android.api.model.BBBCategory;
import com.blinkboxbooks.android.api.model.BBBCategoryList;
import com.blinkboxbooks.android.api.model.BBBContributor;
import com.blinkboxbooks.android.api.model.BBBImage;
import com.blinkboxbooks.android.api.model.BBBLink;
import com.blinkboxbooks.android.api.model.BBBPromotion;
import com.blinkboxbooks.android.api.model.BBBPromotionList;
import com.blinkboxbooks.android.api.model.BBBSuggestion;
import com.blinkboxbooks.android.api.model.BBBSuggestionList;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.net.ApiConnector;
import com.blinkboxbooks.android.net.exceptions.APIConnectorException;
import com.blinkboxbooks.android.net.exceptions.APIConnectorRuntimeException;
import com.blinkboxbooks.android.net.exceptions.NoResultsException;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Provider for shop data
 */
public class CatalogueContentProvider extends ContentProvider {

    private static final int DEFAULT_COUNT = 100;
    private int mCursorId = 1;

    private static final String TAG = CatalogueContentProvider.class.getSimpleName();

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int BOOKS_FOR_CATEGORY_LOCATION = 1;
    private static final int BOOKS_FOR_CATEGORY_LOCATION_WITH_PURCHASE_INFO = 101;
    private static final int BOOKS_FOR_CATEGORY_NAME = 2;
    private static final int BOOKS_FOR_CATEGORY_NAME_WITH_PURCHASE_INFO = 102;
    private static final int BOOKS_FOR_PROMOTION = 3;
    private static final int BOOKS_FOR_PROMOTION_WITH_PURCHASE_INFO = 103;
    private static final int BOOKS_FOR_AUTHOR = 4;
    private static final int BOOKS_FOR_AUTHOR_WITH_PURCHASE_INFO = 104;
    private static final int SEARCH = 5;
    private static final int SEARCH_WITH_PURCHASE_INFO = 105;
    private static final int SUGGESTIONS = 6;
    private static final int CATEGORIES = 7;
    private static final int CATEGORIES_FOR_LOCATION = 8;
    private static final int PROMOTIONS_FOR_PROMOTION_LOCATION = 9;
    private static final int SUGGESTIONS_SEARCH_INTERFACE = 10;
    // The search provider is invoked on screen rotations when we have empty text in the search field which causes the normal suggestions
    // pattern match to fail because it expects some text. This is provided to avoid warnings being output.
    private static final int EMPTY_SUGGESTIONS_SEARCH_INTERFACE = 110;
    private static final int AUTHOR = 11;
    private static final int BOOKS_RELATED = 12;
    private static final int BOOKS_RELATED_WITH_PURCHASE_INFO = 112;
    private static final int BOOKS_FOR_CATEGORY_NAME_WITHOUT_PAGINATION = 13;
    private static final int SEARCH_WITHOUT_PAGINATION = 14;

    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);

        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/category_location/*/count/#/offset/#/with_purchase_info", BOOKS_FOR_CATEGORY_LOCATION_WITH_PURCHASE_INFO);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/category_location/*/count/#/offset/#", BOOKS_FOR_CATEGORY_LOCATION);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/category_name/*/count/#/offset/#/with_purchase_info", BOOKS_FOR_CATEGORY_NAME_WITH_PURCHASE_INFO);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/category_name/*/count/#/offset/#", BOOKS_FOR_CATEGORY_NAME);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/category_name/*", BOOKS_FOR_CATEGORY_NAME_WITHOUT_PAGINATION); //This is used externally so must always be handled
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/promotion/*/count/#/with_purchase_info", BOOKS_FOR_PROMOTION_WITH_PURCHASE_INFO);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/promotion/*/count/#/", BOOKS_FOR_PROMOTION);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/author/*/count/#/offset/#/with_purchase_info", BOOKS_FOR_AUTHOR_WITH_PURCHASE_INFO);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/author/*/count/#/offset/#", BOOKS_FOR_AUTHOR);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/related/*/count/#/offset/#/with_purchase_info", BOOKS_RELATED_WITH_PURCHASE_INFO);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/related/*/count/#/offset/#", BOOKS_RELATED);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/search/*/count/#/offset/#/with_purchase_info", SEARCH_WITH_PURCHASE_INFO);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/search/*/count/#/offset/#/", SEARCH);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "books/search/*", SEARCH_WITHOUT_PAGINATION); //This is used externally so must always be handled
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "suggestions/*", SUGGESTIONS);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "categories", CATEGORIES);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "categories/category_location/*", CATEGORIES_FOR_LOCATION);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "promotions/promotion_location/*", PROMOTIONS_FOR_PROMOTION_LOCATION);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "search_suggest_query/", EMPTY_SUGGESTIONS_SEARCH_INTERFACE);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "search_suggest_query/*", SUGGESTIONS_SEARCH_INTERFACE);
        matcher.addURI(CatalogueContract.CONTENT_AUTHORITY, "author/*", AUTHOR);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        return false;
    }

    @Override
    public String getType(Uri uri) {

        switch (sUriMatcher.match(uri)) {

            case BOOKS_FOR_PROMOTION: //falls through
            case BOOKS_FOR_PROMOTION_WITH_PURCHASE_INFO: // falls through
            case BOOKS_FOR_CATEGORY_LOCATION: //falls through
            case BOOKS_FOR_CATEGORY_LOCATION_WITH_PURCHASE_INFO:
            case BOOKS_FOR_CATEGORY_NAME:  //falls through
            case BOOKS_FOR_CATEGORY_NAME_WITH_PURCHASE_INFO: // falls through
            case BOOKS_FOR_CATEGORY_NAME_WITHOUT_PAGINATION:  //falls through
            case BOOKS_FOR_AUTHOR: //falls through
            case BOOKS_FOR_AUTHOR_WITH_PURCHASE_INFO: // falls through
            case BOOKS_RELATED: //falls through
            case BOOKS_RELATED_WITH_PURCHASE_INFO: // falls through
            case SEARCH: // falls through
            case SEARCH_WITH_PURCHASE_INFO:
            case SEARCH_WITHOUT_PAGINATION:
                return CatalogueContract.Books.CONTENT_TYPE;
            case SUGGESTIONS:
                return CatalogueContract.SearchSuggestion.CONTENT_TYPE;
            case CATEGORIES_FOR_LOCATION: //falls through
            case CATEGORIES:
                return CatalogueContract.Category.CONTENT_TYPE;
            case PROMOTIONS_FOR_PROMOTION_LOCATION:
                return CatalogueContract.Promotions.CONTENT_TYPE;
            case AUTHOR:
                return CatalogueContract.Author.CONTENT_TYPE;
        }

        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {

        try {
            switch (sUriMatcher.match(uri)) {

                case BOOKS_FOR_PROMOTION: {
                    Integer promotionId = CatalogueContract.Books.getPromotionLocation(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForPromotionId(promotionId, projection, selection, selectionArgs, sortOrder, false, count);
                }
                case BOOKS_FOR_PROMOTION_WITH_PURCHASE_INFO: {
                    Integer promotionId = CatalogueContract.Books.getPromotionLocation(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForPromotionId(promotionId, projection, selection, selectionArgs, sortOrder, true, count);
                }
                case BOOKS_FOR_CATEGORY_LOCATION: {
                    Integer categoryLocation = CatalogueContract.Books.getCategoryLocation(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForCategoryLocation(categoryLocation, projection, selection, selectionArgs, sortOrder, false, offset, count);
                }
                case BOOKS_FOR_CATEGORY_LOCATION_WITH_PURCHASE_INFO: {
                    Integer categoryLocation = CatalogueContract.Books.getCategoryLocation(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForCategoryLocation(categoryLocation, projection, selection, selectionArgs, sortOrder, true, offset, count);
                }
                case BOOKS_FOR_CATEGORY_NAME: {
                    String categoryName = CatalogueContract.Books.getCategoryName(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForCategoryName(categoryName, projection, selection, selectionArgs, sortOrder, false, offset, count);
                }
                case BOOKS_FOR_CATEGORY_NAME_WITH_PURCHASE_INFO: {
                    String categoryName = CatalogueContract.Books.getCategoryName(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForCategoryName(categoryName, projection, selection, selectionArgs, sortOrder, true, offset, count);
                }
                case BOOKS_FOR_CATEGORY_NAME_WITHOUT_PAGINATION: {
                    String categoryName = CatalogueContract.Books.getCategoryName(uri);
                    return getBookForCategoryName(categoryName, projection, selection, selectionArgs, sortOrder, false, 0, DEFAULT_COUNT);
                }
                case BOOKS_FOR_AUTHOR: {
                    String authorId = CatalogueContract.Books.getAuthorId(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForAuthorId(authorId, projection, selection, selectionArgs, sortOrder, false, offset, count);
                }
                case BOOKS_FOR_AUTHOR_WITH_PURCHASE_INFO: {
                    String authorId = CatalogueContract.Books.getAuthorId(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    return getBookForAuthorId(authorId, projection, selection, selectionArgs, sortOrder, true, offset, count);
                }
                case SEARCH: {
                    String searchQuery = CatalogueContract.Books.getSearchQuery(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    return getBooksForSearch(searchQuery, projection, selection, selectionArgs, sortOrder, false, offset, count);
                }
                case SEARCH_WITH_PURCHASE_INFO: {
                    String searchQuery = CatalogueContract.Books.getSearchQuery(uri);
                    int count = CatalogueContract.Books.getCount(uri);
                    int offset = CatalogueContract.Books.getOffset(uri);
                    return getBooksForSearch(searchQuery, projection, selection, selectionArgs, sortOrder, true, offset, count);
                }
                case SEARCH_WITHOUT_PAGINATION: {
                    String searchQuery = CatalogueContract.Books.getSearchQuery(uri);
                    return getBooksForSearch(searchQuery, projection, selection, selectionArgs, sortOrder, false, 0, DEFAULT_COUNT);
                }
                case BOOKS_RELATED: {
                    final String isbn = CatalogueContract.Books.getISBN(uri);
                    final int offset = CatalogueContract.Books.getOffset(uri);
                    final int count = CatalogueContract.Books.getCount(uri);
                    return getBookRelatedToISBN(isbn, projection, selection, selectionArgs, sortOrder, false, offset, count);
                }
                case BOOKS_RELATED_WITH_PURCHASE_INFO: {
                    final String isbn = CatalogueContract.Books.getISBN(uri);
                    final int offset = CatalogueContract.Books.getOffset(uri);
                    final int count = CatalogueContract.Books.getCount(uri);
                    return getBookRelatedToISBN(isbn, projection, selection, selectionArgs, sortOrder, true, offset, count);
                }
                case SUGGESTIONS: {
                    String suggestionQuery = CatalogueContract.SearchSuggestion.getSearchSuggestionQuery(uri);
                    return getSearchSuggestions(suggestionQuery, projection, selection, selectionArgs, sortOrder);
                }
                case CATEGORIES: {
                    return getCategories(projection, selection, selectionArgs, sortOrder);
                }
                case CATEGORIES_FOR_LOCATION: {
                    Integer categoryLocation = CatalogueContract.Category.getCategoryLocation(uri);
                    return getCategory(categoryLocation, projection, selection, selectionArgs, sortOrder);
                }
                case PROMOTIONS_FOR_PROMOTION_LOCATION: {
                    Integer promotionLocation = CatalogueContract.Promotions.getPromotionLocation(uri);
                    return getPromotionForLocation(promotionLocation, projection, selection, selectionArgs, sortOrder);
                }
                case SUGGESTIONS_SEARCH_INTERFACE: {
                    String suggestionSearchInterfaceQuery = CatalogueContract.SearchSuggestion.getSearchSuggestionQuery(uri);
                    return getSuggestionsForSearchInterface(suggestionSearchInterfaceQuery, projection, selection, selectionArgs, sortOrder);
                }
                case EMPTY_SUGGESTIONS_SEARCH_INTERFACE: {
                    // No point doing any query as we know the text is empty, just return an empty cursor
                    return new MatrixCursor(CatalogueContract.SearchSuggestion.SEARCH_INTERFACE_PROJECTION);
                }
                case AUTHOR: {
                    String authorId = CatalogueContract.Author.getAuthorId(uri);
                    return getAuthor(authorId, projection);
                }
                default:
                    throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        } catch (APIConnectorException e) {
            // Because we cannot throw a standard check exception from this method, we create our special
            // runtime exception that acts as a wrapper for the real APIConnectorException.
            throw new APIConnectorRuntimeException(e);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("This provider does not support the insert operation");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider does not support the delete operation");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("This provider does not support the update operation");
    }

    private Cursor getCategory(int locationId, String[] projection, String selection, String[] selectionArgs, String sortOrder)
            throws APIConnectorException {
        BBBCategoryList categoryList = ApiConnector.getInstance(getContext()).getCategory(locationId);
        return createCursorFromCategoryList(categoryList, projection);
    }

    private Cursor getAuthor(String authorId, String[] projection) throws APIConnectorException {
        BBBContributor contributor = ApiConnector.getInstance(getContext()).getAuthor(authorId);
        return createCursorFromContributor(contributor, projection);
    }

    private Cursor getCategories(String[] projection, String selection, String[] selectionArgs, String sortOrder) throws APIConnectorException {
        BBBCategoryList list = ApiConnector.getInstance(getContext()).getCategories();
        return createCursorFromCategoryList(list, projection);
    }

    private Cursor getBookForAuthorId(String authorId, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean addPurchaseInfo, int offset, int count) throws APIConnectorException {
        Boolean desc = parseSortDirection(sortOrder);
        sortOrder = parseSortParameter(sortOrder);

        BBBBookInfoList list = ApiConnector.getInstance(getContext()).getBooksForAuthorId(authorId, offset, count, desc, sortOrder, null, null);
        BBBBookPriceList priceList = ApiConnector.getInstance(getContext()).getBookPriceList(getISBNs(list));
        return createCursorFromBookList(list, priceList, projection, addPurchaseInfo);
    }

    private Cursor getBookRelatedToISBN(String isbn, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean addPurchaseInfo, int offset, int count) throws APIConnectorException {
        BBBBookInfoList list = ApiConnector.getInstance(getContext()).getBooksRelatedToISBN(isbn, offset, count);
        BBBBookPriceList priceList = ApiConnector.getInstance(getContext()).getBookPriceList(getISBNs(list));
        return createCursorFromBookList(list, priceList, projection, addPurchaseInfo);
    }

    private Cursor getBookForCategoryName(String categoryName, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean addPurchaseInfo, int offset, int count) throws APIConnectorException {
        Boolean desc = parseSortDirection(sortOrder);
        sortOrder = parseSortParameter(sortOrder);

        BBBBookInfoList list = ApiConnector.getInstance(getContext()).getBooksForCategoryName(categoryName, offset, count, desc, sortOrder, null, null);
        BBBBookPriceList priceList = ApiConnector.getInstance(getContext()).getBookPriceList(getISBNs(list));
        return createCursorFromBookList(list, priceList, projection, addPurchaseInfo);
    }

    private Cursor getBookForCategoryLocation(Integer categoryId, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean addPurchaseInfo, int offset, int count) throws APIConnectorException {
        Boolean desc = parseSortDirection(sortOrder);
        sortOrder = parseSortParameter(sortOrder);

        BBBBookInfoList list = ApiConnector.getInstance(getContext()).getBooksForCategoryLocation(categoryId, offset, count, desc, sortOrder, null, null);
        BBBBookPriceList priceList = ApiConnector.getInstance(getContext()).getBookPriceList(getISBNs(list));
        return createCursorFromBookList(list, priceList, projection, addPurchaseInfo);
    }

    private Cursor getBookForPromotionId(Integer promotion, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean addPurchaseInfo, int count) throws APIConnectorException {
        Boolean desc = parseSortDirection(sortOrder);
        sortOrder = parseSortParameter(sortOrder);

        BBBBookInfoList list = ApiConnector.getInstance(getContext()).getBooksForPromotion(promotion, count, desc, sortOrder);
        BBBBookPriceList priceList = ApiConnector.getInstance(getContext()).getBookPriceList(getISBNs(list));
        return createCursorFromBookList(list, priceList, projection, addPurchaseInfo);
    }

    private Cursor getBooksForSearch(String query, String[] projection, String selection, String[] selectionArgs, String sortOrder, boolean addPurchaseInfo, int offset, int count) throws APIConnectorException {
        Boolean desc = parseSortDirection(sortOrder);
        sortOrder = parseSortParameter(sortOrder);

        BBBBookInfoList list = ApiConnector.getInstance(getContext()).searchBooks(query, sortOrder, desc, offset, count);
        BBBBookPriceList priceList = ApiConnector.getInstance(getContext()).getBookPriceList(getISBNs(list));
        return createCursorFromBookList(list, priceList, projection, addPurchaseInfo);
    }

    private Cursor getPromotionForLocation(Integer promotionLocation, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws APIConnectorException {
        BBBPromotionList promotionList = ApiConnector.getInstance(getContext()).getPromotionWithLocation(promotionLocation);
        return createCursorFromPromotionList(promotionList, projection);
    }

    private String parseSortParameter(String sortString) {

        if(sortString == null || !sortString.contains(":")) {
            return sortString;
        }

        return sortString.substring(0, sortString.indexOf(':'));
    }

    private Boolean parseSortDirection(String sortString) {

        if(sortString == null || !sortString.contains(":")) {
            return null;
        }

        String direction = sortString.substring(sortString.indexOf(':')+1, sortString.length());
        return Boolean.parseBoolean(direction);
    }

    private Cursor createCursorFromContributor(BBBContributor contributor, String[] projection) throws APIConnectorException {

        // If the supplied contributor is null then we just return an empty cursor
        if(contributor == null) {
            throw new NoResultsException();
        }

        //TODO use the given projection instead of always using the default
        MatrixCursor cursor = new MatrixCursor(CatalogueContract.Author.PROJECTION);
        Object[] row = new Object[cursor.getColumnCount()];

        row[cursor.getColumnIndex(CatalogueContract.Author.DISPLAY_NAME)] = contributor.displayName;
        row[cursor.getColumnIndex(CatalogueContract.Author.SORT_NAME)] = contributor.sortName;
        row[cursor.getColumnIndex(CatalogueContract.Author.BIOGRAPHY)] = contributor.biography;
        row[cursor.getColumnIndex(CatalogueContract.Author.BOOKS_COUNT)] = contributor.booksCount;

        BBBLink imageLink = contributor.getLinkData(BBBApiConstants.URN_IMAGE_CONTRIBUTOR);

        if(imageLink != null ) {
            row[cursor.getColumnIndex(CatalogueContract.Author.IMAGE_URL)] = imageLink.href;
        }

        cursor.addRow(row);
        return cursor;
    }

    private Cursor createCursorFromPromotionList(BBBPromotionList list, String[] projection) throws APIConnectorException {

        // If an empty list is supplied then we by definition will have no results
        if(list == null || list.items == null || list.items.length == 0) {
            throw new NoResultsException();
        }

        //TODO use the given projection instead of always using the default
        MatrixCursor matrixCursor= new MatrixCursor(CatalogueContract.Promotions.PROJECTION);

        if(list.items != null) {
            for(int i=0; i<list.items.length; i++) {
                addPromotionToCursor(matrixCursor, list.items[i]);
            }
        }

        return matrixCursor;
    }

    private Cursor createCursorFromCategoryList(BBBCategoryList list, String[] projection) throws APIConnectorException {

        // If an empty list is supplied then we by definition will have no results
        if(list == null || list.items == null || list.items.length == 0) {
            throw new NoResultsException();
        }

        // TODO use the given projection instead of always using the default
        MatrixCursor matrixCursor= new MatrixCursor(CatalogueContract.Category.PROJECTION);

        if(list.items != null) {

            Arrays.sort(list.items, new Comparator<BBBCategory>() {
                @Override
                public int compare(BBBCategory lhs, BBBCategory rhs) {
                    return lhs.displayName.compareTo(rhs.displayName);
                }
            });

            for(int i=0; i<list.items.length; i++) {
                addCategoryToCursor(matrixCursor, list.items[i]);
            }
        }

        return matrixCursor;
    }

    private static class LibraryBookInfo {
        final private long mPurchaseDate;
        final private long mId;
        final private int mDownloadStatus;

        private LibraryBookInfo(long id, long purchaseDate, int downloadStatus) {
            mId = id;
            mPurchaseDate = purchaseDate;
            mDownloadStatus = downloadStatus;
        }
    }

    private Cursor createCursorFromBookList(BBBBookInfoList list, BBBBookPriceList priceList, String[] projection, boolean addPurchaseInfo) throws APIConnectorException {

        // If the price list is empty then just return an empty cursor
        if (priceList == null || priceList.items == null || priceList.items.length == 0) {
            throw new NoResultsException();
        }

        // If the book info list is empty then just return an empty cursor
        if (list == null || list.items == null || list.items.length == 0) {
            throw new NoResultsException();
        }

        // TODO use the given projection instead of always using the default
        MatrixCursor matrixCursor= new MatrixCursor(CatalogueContract.Books.PROJECTION);

        HashMap<String, BBBBookPrice> isbnMap = new HashMap<String, BBBBookPrice>();

        String isbn;

        for (int i = 0; i < priceList.items.length; i++) {
            isbn = priceList.items[i].id;
            isbn = isbn.replaceAll("[a-zA-Z+]+", "");

            isbnMap.put(isbn, priceList.items[i]);
        }

        Map<String,LibraryBookInfo> userLibraryISBNToLibraryData = new HashMap<String,LibraryBookInfo>();

        // If this request requires setting the is_purchased flag we request all purchased content from the user's
        // library and add them to the userLibraryPurchasedBookToDate map
        if (addPurchaseInfo) {
            String userId = AccountController.getInstance().getUserId();
            String[] libraryProjection = {BBBContract.BooksColumns.BOOK_ISBN};
            Uri uri = BBBContract.Books.buildBookAccountUriAll(userId);
            String selection = BBBContract.BooksColumns.BOOK_IS_SAMPLE + " = ?";
            String[] args = new String[]{"0"}; // 0 to indicate false

            Cursor libraryCursor = getContext().getContentResolver().query(uri, projection, selection, args, null);

            // Add all books in the library in to a map with key ISBN and value of purchase date
            for (libraryCursor.moveToFirst(); !libraryCursor.isAfterLast(); libraryCursor.moveToNext()) {
                //TODO: See ALA-1508 - we shouldn't have this hard coded column index, but
                //because of the nature of the table join, we have to as theree are 3 columns in this
                //cursor with the name "_id" and using getColumnIndex will return the wrong one.
                final long id = libraryCursor.getLong(0);
                final String bookISBN = libraryCursor.getString(libraryCursor.getColumnIndex(BBBContract.BooksColumns.BOOK_ISBN));
                final long purchaseDate = libraryCursor.getLong(libraryCursor.getColumnIndex(BBBContract.BooksColumns.BOOK_PURCHASE_DATE));
                final int downloadStatus = libraryCursor.getInt(libraryCursor.getColumnIndex(BBBContract.BooksColumns.BOOK_DOWNLOAD_STATUS));
                userLibraryISBNToLibraryData.put(bookISBN, new LibraryBookInfo(id, purchaseDate, downloadStatus));
            }

            libraryCursor.close();
        }

        if(list.items != null) {
            for (BBBBookInfo item : list.items) {
                final LibraryBookInfo bookInfo = userLibraryISBNToLibraryData.get(item.id);
                addBookInfoToCursor(matrixCursor, item, isbnMap.get(item.id),
                        bookInfo == null ? 0L : bookInfo.mId,
                        bookInfo == null ? 0L : bookInfo.mPurchaseDate,
                        bookInfo == null ? 0 : bookInfo.mDownloadStatus);
            }
        }

        return matrixCursor;
    }

    private Cursor getSearchSuggestions(String query, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws APIConnectorException {
        BBBSuggestionList list = ApiConnector.getInstance(getContext()).getSuggestions(query, null);

        //TODO use the given projection instead of always using the default
        MatrixCursor matrixCursor= new MatrixCursor(CatalogueContract.SearchSuggestion.PROJECTION);

        if(list.items != null) {

            for(int i=0; i<list.items.length; i++) {
                addSuggestionToCursor(matrixCursor, list.items[i]);
            }
        }

        return matrixCursor;
    }

    private void addPromotionToCursor(MatrixCursor cursor, BBBPromotion promotion) {
        Object[] row = new Object[cursor.getColumnCount()];

        try {
            long id = Long.parseLong(promotion.id);
            row[cursor.getColumnIndex(CatalogueContract.Promotions._ID)] = id;
        } catch (NumberFormatException e) {
            LogUtils.e(TAG, e.getMessage(), e);
            return;
        }

        row[cursor.getColumnIndex(CatalogueContract.Promotions.NAME)] = promotion.name;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.TITLE)] = promotion.title;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.SUBTITLE)] = promotion.subtitle;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.DISPLAY_NAME)] = promotion.displayName;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.ACTIVATED)] = promotion.activated;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.DEACTIVATED)] = promotion.deactivated;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.SEQUENCE)] = promotion.sequence;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.LOCATION)] = promotion.location;
        row[cursor.getColumnIndex(CatalogueContract.Promotions.IMAGE)] = promotion.image;

        cursor.addRow(row);
    }

    //Create cursor with the required tables to be used with the search interface
    private Cursor getSuggestionsForSearchInterface(String query, String[] projection, String selection, String[] selectionArgs, String sortOrder) throws APIConnectorException {
        BBBSuggestionList list = ApiConnector.getInstance(getContext()).getSuggestions(query, null);

        MatrixCursor matrixCursor= new MatrixCursor(CatalogueContract.SearchSuggestion.SEARCH_INTERFACE_PROJECTION);

        if(list.items != null) {

            for(int i=0; i<list.items.length; i++) {
                addSuggestionToSearchInterfaceCursor(matrixCursor, list.items[i]);
            }
        }
        return matrixCursor;
    }

    private void addSuggestionToSearchInterfaceCursor(MatrixCursor cursor, BBBSuggestion suggestion) {
        Object[] row = new Object[cursor.getColumnCount()];

        try {
            long id = Long.parseLong(suggestion.id);
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion._ID_LOWERCASE)] = id;
        } catch(NumberFormatException e) {
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion._ID_LOWERCASE)] = mCursorId; mCursorId++;
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.SUGGEST_COLUMN_INTENT_EXTRA_DATA)] = suggestion.id;
        }

        row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.SUGGEST_COLUMN_TEXT_1)] = suggestion.title;

        if(suggestion.authors != null) {
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.SUGGEST_COLUMN_TEXT_2)] = "by " + suggestion.authors[0];
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.SUGGEST_COLUMN_INTENT_DATA)] = suggestion.title + " - " + suggestion.authors[0];
        } else {
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.SUGGEST_COLUMN_INTENT_DATA)] = suggestion.title;
        }

        cursor.addRow(row);
    }

    private void addCategoryToCursor(MatrixCursor cursor, BBBCategory category) {
        Object[] row = new Object[cursor.getColumnCount()];

        try {
            long id = Long.parseLong(category.id);
            row[cursor.getColumnIndex(CatalogueContract.Category._ID)] = id;
        } catch(NumberFormatException e) {
            LogUtils.e(TAG, e.getMessage(), e);
            return;
        }

        row[cursor.getColumnIndex(CatalogueContract.Category.CATEGORY_NAME)] = category.slug;
        row[cursor.getColumnIndex(CatalogueContract.Category.DISPLAY_NAME)] = category.displayName;
        row[cursor.getColumnIndex(CatalogueContract.Category.SEQUENCE)] = category.sequence;
        row[cursor.getColumnIndex(CatalogueContract.Category.RECOMMENDED_SEQUENCE)] = category.recommendedSequence;
        row[cursor.getColumnIndex(CatalogueContract.Category.IMAGE_URL)] = category.categoryImage;

        cursor.addRow(row);
    }

    private void addBookInfoToCursor(MatrixCursor cursor, BBBBookInfo book,
                                     BBBBookPrice price, long libraryId, long purchaseDate, int downloadStatus) {
        Object[] row = new Object[cursor.getColumnCount()];

        try {
            long id = Long.parseLong(book.id);
            row[cursor.getColumnIndex(CatalogueContract.Books._ID)] = id;
        } catch(NumberFormatException e) {
            return;
        }

        row[cursor.getColumnIndex(CatalogueContract.Books.TITLE)] = book.title;

        BBBLink authorLink = book.getLinkData(BBBApiConstants.URN_CONTRIBUTOR);

        if(authorLink != null) {
            row[cursor.getColumnIndex(CatalogueContract.Books.AUTHOR_NAME)] = authorLink.title;
            row[cursor.getColumnIndex(CatalogueContract.Books.AUTHOR_ID)] = StringUtils.getLastPathSegment(authorLink.href);
        }

        BBBLink sampleLink = book.getLinkData(BBBApiConstants.URN_SAMPLE_MEDIA);

        if(sampleLink != null) {
            row[cursor.getColumnIndex(CatalogueContract.Books.SAMPLE_URI)] = StringUtils.injectIntoResourceUrl(sampleLink.href, "/params;v=0");
        }

        row[cursor.getColumnIndex(CatalogueContract.Books.PUBLICATION_DATE)] = book.publicationDate;
        row[cursor.getColumnIndex(CatalogueContract.Books.SAMPLE_ELIGIBLE)] = book.sampleEligible ? 1 : 0;

        BBBImage coverImage = book.getImageData(BBBApiConstants.URN_IMAGE_COVER);

        if(coverImage != null) {
            row[cursor.getColumnIndex(CatalogueContract.Books.COVER_IMAGE_URL)] = coverImage.src;
        }

        BBBLink publisherLink = book.getLinkData(BBBApiConstants.URN_PUBLISHER);

        if(publisherLink != null) {
            row[cursor.getColumnIndex(CatalogueContract.Books.PUBLISHER_NAME)] = publisherLink.title;
        }

        if(price != null) {
            row[cursor.getColumnIndex(CatalogueContract.Books.CURRENCY)] = price.currency;
            row[cursor.getColumnIndex(CatalogueContract.Books.PRICE)] = price.price;
            row[cursor.getColumnIndex(CatalogueContract.Books.DISCOUNT_PRICE)] = price.discountPrice;
            row[cursor.getColumnIndex(CatalogueContract.Books.CLUBCARD_POINTS_AWARDED)] = price.clubcardPointsAward;
        }

        row[cursor.getColumnIndex(CatalogueContract.Books.PURCHASE_DATE)] = purchaseDate;
        row[cursor.getColumnIndex(CatalogueContract.Books.DOWNLOAD_STATUS)] = downloadStatus;
        row[cursor.getColumnIndex(CatalogueContract.Books.LIBRARY_ID)] = libraryId;

        cursor.addRow(row);
    }

    private void addSuggestionToCursor(MatrixCursor cursor, BBBSuggestion suggestion) {
        Object[] row = new Object[cursor.getColumnCount()];

        try {
            long id = Long.parseLong(suggestion.id);
            row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion._ID)] = id;
        } catch(NumberFormatException e) {
            return;
        }

        row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.TYPE)] = suggestion.type;
        row[cursor.getColumnIndex(CatalogueContract.SearchSuggestion.TITLE)] = suggestion.title;

        cursor.addRow(row);
    }

    private String[] getISBNs(BBBBookInfoList list) {

        // If we get an empty list supplied then we return an empty array of ISBNs
        if (list == null || list.items == null || list.items.length == 0) {
            return new String[] {};
        }

        String[] isbns = new String[list.items.length];

        for (int i = 0; i < list.items.length; i++) {
            isbns[i] = list.items[i].id;
        }

        return isbns;
    }
}