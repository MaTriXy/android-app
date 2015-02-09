// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.net;

import android.content.Context;
import android.text.TextUtils;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBBookInfoList;
import com.blinkboxbooks.android.api.model.BBBBookPriceList;
import com.blinkboxbooks.android.api.model.BBBBookSearchResponse;
import com.blinkboxbooks.android.api.model.BBBBookmark;
import com.blinkboxbooks.android.api.model.BBBBookmarkList;
import com.blinkboxbooks.android.api.model.BBBBookmarkResponse;
import com.blinkboxbooks.android.api.model.BBBCategory;
import com.blinkboxbooks.android.api.model.BBBCategoryList;
import com.blinkboxbooks.android.api.model.BBBClientInformation;
import com.blinkboxbooks.android.api.model.BBBContributor;
import com.blinkboxbooks.android.api.model.BBBPromotionList;
import com.blinkboxbooks.android.api.model.BBBReadingStatus;
import com.blinkboxbooks.android.api.model.BBBSuggestionList;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBResponseHandler;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.net.exceptions.APIConnectorException;
import com.blinkboxbooks.android.net.exceptions.InternalServerErrorException;
import com.blinkboxbooks.android.net.exceptions.JsonParsingException;
import com.blinkboxbooks.android.net.exceptions.NoNetworkException;
import com.blinkboxbooks.android.net.exceptions.NoResultsException;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.SyncState;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.DebugUtils;
import com.blinkboxbooks.android.util.LogUtils;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Helper class for making calls to the REST API
 */
public class ApiConnector {

    private static ApiConnector instance;

    public static ApiConnector getInstance() {

        if (instance == null) {
            instance = new ApiConnector(BBBApplication.getApplication());
        }

        return instance;
    }

    public static ApiConnector getInstance(Context context) {

        if (instance == null) {
            instance = new ApiConnector(context);
        }

        return instance;
    }

    private static final String TAG = ApiConnector.class.getSimpleName();

    private static final int BOOKINFO_DOWNLOAD_LIMIT = 110;

    private static final int DEFAULT_BOOK_LIMIT = 100;
    private static final int DEFAULT_SUGGESTION_LIMIT = 10;

    private ApiConnector(Context context) {
        BBBRequestFactory.getInstance().setHostDefault(context.getString(R.string.rest_server_host));
        BBBRequestFactory.getInstance().setHostAuthentication(context.getString(R.string.auth_server_host));
    }

    /**
     * Fire and forget method that attempts to revoke a refresh token
     *
     * @param refreshToken
     */
    public void revokeRefreshToken(String refreshToken) {
        BBBRequest request = BBBRequestFactory.getInstance().createRevokeRefreshTokenRequest(refreshToken);

        BBBRequestManager.getInstance().executeRequest(request, new BBBResponseHandler() {
            @Override
            public void receivedResponse(BBBResponse response) {

                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    LogUtils.d(TAG, "successfully revoked refresh token");
                } else {
                    LogUtils.e(TAG, "error revoking refresh token: " + response.getResponseCode());
                }
            }

            @Override
            public void connectionError(BBBRequest bbbRequest) {
                LogUtils.e(TAG, "connection error revoking refresh token");
            }
        });
    }

    /**
     * Registers the users device to the server and store the client secret in the account object
     *
     * @return true if the device was successfully registered else false
     */
    public BBBResponse registerDevice(String clientName, String clientBrand, String clientModel, String clientOs) {
        LogUtils.i(TAG, "registering device");

        BBBRequest request = BBBRequestFactory.getInstance().createRegisterClientRequest(clientName, clientBrand, clientModel, clientOs);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BBBClientInformation clientInformation = new Gson().fromJson(response.getResponseData(), BBBClientInformation.class);

            if (!TextUtils.isEmpty(clientInformation.client_id) && !TextUtils.isEmpty(clientInformation.client_secret)) {
                AccountController.getInstance().setClient(clientInformation.client_id, clientInformation.client_secret);

                return response;
            }
        }

        return response;
    }

    /**
     * Uploads a users tesco clubcard to the server
     *
     * @param clubcardNumber the clubcard number
     * @return true if the clubcard was successfully uploaded else false
     */
    public boolean uploadClubcardNumber(String clubcardNumber) {
        LogUtils.i(TAG, "uploading clubcard number: " + clubcardNumber);

        BBBRequest request = BBBRequestFactory.getInstance().createAddClubcardRequest(clubcardNumber);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        return response.getResponseCode() == HttpURLConnection.HTTP_CREATED;
    }

    /**
     * Updates the reading status of a book
     *
     * @param book the Book object we are updating
     * @return true if the book was successfully updated else false
     */
    public boolean updateReadingStatus(Book book) {
        BBBReadingStatus status = null;

        switch (book.state) {
            case BBBContract.Books.BOOK_STATE_FINISHED:
                status = BBBReadingStatus.FINISHED;
                break;
            case BBBContract.Books.BOOK_STATE_READING:
                status = BBBReadingStatus.READING;
                break;
            case BBBContract.Books.BOOK_STATE_UNREAD:
                status = BBBReadingStatus.UNREAD;
                break;
            default:
                return false;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createUpdateLibraryItemRequest(book.server_id, status);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        return response.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    /**
     * Removes a book from the users library. If the book is a sample we delete it otherwise we can only archive it
     *
     * @param book the Book we are removing
     * @return true if the book was successfully removed else false
     */
    public boolean removeBook(Book book) {
        BBBRequest request;

        if (book.sample_book) {
            request = BBBRequestFactory.getInstance().createDeleteLibraryItemRequest(book.server_id);
        } else {
            // ALA-1807, we should NEVER be archiving full version books, so we have disabled the archive request below.
            //request = BBBRequestFactory.getInstance().createArchiveItemRequest(book.server_id);

            // Note that we do not ever expect to come in here since the fix for ALA-1750 has been applied, so we add a Crashlytics
            // handled exception just in case we do so at least we can track that we are trying to.
            Crashlytics.log("isbn:" + book.isbn);
            Crashlytics.logException(new Exception("Unexpected attempt to archive book"));
            return true;
        }

        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        return response.getResponseCode() == HttpURLConnection.HTTP_OK;
    }

    /**
     * Add a new bookmark
     *
     * @param bookmark the Bookmark we are adding
     * @return true if the bookmark was successfully added else false
     */
    public boolean addBookmark(Bookmark bookmark) {
        String bookmarkType = BookmarkHelper.getBookmarkType(bookmark.type);

        String position = bookmark.position;
        if (TextUtils.isEmpty(position)) {
            position = BookmarkHelper.NO_CFI;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createAddBookmarkRequest(bookmark.isbn, bookmarkType, bookmark.update_by, position, bookmark.name,
                bookmark.annotation, bookmark.style, bookmark.color, bookmark.percentage, bookmark.content);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        String bookmarkId = null;
        if (response.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            try {
                BBBBookmark bookmarkResponse = new Gson().fromJson(response.getResponseData(), BBBBookmark.class);
                bookmarkId = bookmarkResponse.id;
                bookmark.update_date = BBBCalendarUtil.parseDate(bookmarkResponse.createdDate, BBBCalendarUtil.FORMAT_TIME_STAMP).getTimeInMillis();
            } catch (IllegalStateException| JsonSyntaxException e) {
                // if the parsing of the returned JSON failed then just return false to indicate that adding the bookmark failed
                return false;
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
            // Here be dragons (CP-814): re-open CP-396 if this occurs
            // Ideally we need the bookmark id in the response.
            // bookmarkId = response.getBookmarkGUID();

            request = BBBRequestFactory.getInstance().createGetBookmarksRequest(bookmark.isbn, bookmark.position, null, bookmarkType);
            response = BBBRequestManager.getInstance().executeRequestSynchronously(request);
            try {
                BBBBookmarkList bookmarkList = new Gson().fromJson(response.getResponseData(), BBBBookmarkList.class);

                if (bookmarkList != null && bookmarkList.numberOfResults == 1) {
                    bookmarkId = bookmarkList.bookmarks[0].id;
                } else {
                    DebugUtils.handleException("Bookmark sync problems detected");
                }
            } catch (IllegalStateException| JsonSyntaxException e) {
                // if the parsing of the returned JSON failed then just return false to indicate that adding the bookmark failed
                return false;
            }
        }

        if (bookmarkId != null) {
            bookmark.cloud_id = bookmarkId;
            bookmark.state = SyncState.STATE_NORMAL;

            return true;
        }

        return false;
    }

    /**
     * Removes a bookmark
     *
     * @param bookmark the Bookmark we are removing
     * @return true if the book was successfully removed else false
     */
    public boolean removeBookmark(Bookmark bookmark, String deletedBy) {
        BBBRequest request = BBBRequestFactory.getInstance().createDeleteBookmarkRequest(bookmark.cloud_id, deletedBy);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        // A successful delete operation should return HTTP_NO_CONTENT. If the server thinks the bookmark doesn't exist it will
        // return HTTP_OK, in this case we also treat this as a success as from the client point of view the bookmark should be treated as gone.
        return (response.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT || response.getResponseCode() == HttpURLConnection.HTTP_OK);
    }

    /**
     * Updates a lastPosition
     *
     * @param bookmark    the Bookmark we are removing
     * @return true if the book was successfully removed else false
     */
    public boolean updateBookmark(Bookmark bookmark) {
        String bookmarkType = BookmarkHelper.getBookmarkType(bookmark.type);

        String position = bookmark.position;
        if (TextUtils.isEmpty(position)) {
            position = BookmarkHelper.NO_CFI;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createUpdateBookmarkRequest(bookmark.cloud_id, bookmarkType, bookmark.update_by,
                position, bookmark.name, bookmark.annotation, bookmark.style, bookmark.color, bookmark.percentage, bookmark.content);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookmarkResponse bookmarkResponse = new Gson().fromJson(response.getResponseData(), BBBBookmarkResponse.class);
                if (bookmarkResponse != null && bookmarkResponse.updatedTimeDate != null) {
                    bookmark.update_date = BBBCalendarUtil.parseDate(bookmarkResponse.updatedTimeDate, BBBCalendarUtil.FORMAT_TIME_STAMP).getTimeInMillis();
                    return true;
                }
            } catch (IllegalStateException| JsonSyntaxException e) {
                // if the parsing of the returned JSON failed then just return false to indicate that updating the bookmark failed
                return false;
            }
            return false;
        } else {
            return false;
        }
    }

    /**
     * Gets the decryption key for a particular book
     *
     * @param url       the url to the key for the book
     * @param publicKey the public key used for encrypting the book decryption key
     * @return BBBResponse object containing the key or failure reason if the operation fails
     */
    public BBBResponse getDecryptionKey(String url, String publicKey) {
        BBBRequest request = BBBRequestFactory.getInstance().createGetKeyRequest(url, publicKey);

        return BBBRequestManager.getInstance().executeRequestSynchronously(request);
    }

    /**
     * Downloads the book information for the specified books. Splits the request if the list of books is too large.
     *
     * @param ids the ISBNs of the books you want to download information for
     * @return an ArrayList of BBBBookInfo objects or null, it no book information could be obtained
     */
    public ArrayList<BBBBookInfo> getBookInfo(String[] ids) {

        if (ids == null) {
            return null;
        }

        ArrayList<BBBBookInfo> books = new ArrayList<BBBBookInfo>(ids.length);

        boolean done = false;

        int startIndex = 0, endIndex = 0;
        String[] idSubset;

        while (!done) {
            startIndex = endIndex;
            endIndex = startIndex + BOOKINFO_DOWNLOAD_LIMIT > ids.length ? ids.length : startIndex + BOOKINFO_DOWNLOAD_LIMIT;

            if (startIndex >= ids.length || startIndex > endIndex) {
                break;
            }

            idSubset = Arrays.copyOfRange(ids, startIndex, endIndex);

            BBBRequest request = BBBRequestFactory.getInstance().createGetBooksRequest(idSubset.length, null, idSubset);
            BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

            try {

                if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    try {
                        BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);

                        if (list.items != null) {
                            books.addAll(Arrays.asList(list.items));
                        }
                    }  catch (IllegalStateException| JsonSyntaxException e) {
                        return null;
                    }

                } else {
                    return null;
                }
            } catch (Exception e) {
                return null;
            }
        }

        return books;
    }

    /**
     * A common handler for translating a BBBRequest response code into an appropriate APIConnectorException
     * @param responseCode the response code for the request
     * @return an APIConnectorException
     */
    private APIConnectorException getExceptionForCommonAPIErrors(int responseCode) throws InternalServerErrorException, NoNetworkException {
        if (responseCode == BBBApiConstants.ERROR_PARSER) {
            return new InternalServerErrorException();
        } else {
            // Fall back on the no network message for any other cases
            return new NoNetworkException();
        }
    }

    /**
     * Gets books for the given category name
     *
     * @param categoryName
     * @param offset
     * @param count
     * @param desc
     * @param order
     * @param minPublicationDate
     * @param maxPublicationDate
     * @return a BookInfoList object containing a list of books for the category
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList getBooksForCategoryName(String categoryName, Integer offset, Integer count, Boolean desc, String order, String minPublicationDate, String maxPublicationDate)
            throws APIConnectorException {

        if(categoryName == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetCategoryRequest(categoryName);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BBBCategoryList categoryList = null;
            try {
                categoryList = new Gson().fromJson(response.getResponseData(), BBBCategoryList.class);
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }

            if(categoryList != null && categoryList.items != null) {
                BBBCategory category = categoryList.items[0];
                return getBooksForCategoryId(Integer.parseInt(category.id), offset, count, desc, order, minPublicationDate, maxPublicationDate);
            } else {
                throw new NoResultsException();
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets books for the given promotion
     *
     * @param promotionId
     * @param count
     * @param order
     * @return a BookInfoList object containing a list of books for the category
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList getBooksForPromotion(Integer promotionId, Integer count, Boolean desc, String order) throws APIConnectorException {

        if(promotionId == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetBooksForPromotionRequest(promotionId, count == null ? DEFAULT_BOOK_LIMIT : count, desc, order);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets books for the given category location
     *
     * @param categoryLocation
     * @param offset
     * @param count
     * @param desc
     * @param order
     * @param minPublicationDate
     * @param maxPublicationDate
     * @return a BookInfoList object containing a list of books for the category
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList getBooksForCategoryLocation(Integer categoryLocation, Integer offset, Integer count, Boolean desc, String order, String minPublicationDate, String maxPublicationDate)
        throws APIConnectorException {

        if(categoryLocation == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetBooksForCategoryLocationRequest(categoryLocation, offset, count == null ? DEFAULT_BOOK_LIMIT : count, desc, order, minPublicationDate, maxPublicationDate);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets books for the given category id
     *
     * @param categoryId
     * @param offset
     * @param count
     * @param desc
     * @param order
     * @param minPublicationDate
     * @param maxPublicationDate
     * @return a BookInfoList object containing a list of books for the category
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList getBooksForCategoryId(Integer categoryId, Integer offset, Integer count, Boolean desc, String order, String minPublicationDate, String maxPublicationDate)
            throws APIConnectorException {

        if(categoryId == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetBooksForCategoryIdRequest(categoryId, offset, count == null ? DEFAULT_BOOK_LIMIT : count, desc, order, minPublicationDate, maxPublicationDate);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets books for a particular author
     *
     * @param authorId
     * @param offset
     * @param count
     * @param desc
     * @param order
     * @param minPublicationDate
     * @param maxPublicationDate
     * @return a BookInfoList object containing a list of books for the category
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList getBooksForAuthorId(String authorId, Integer offset, Integer count, Boolean desc, String order, String minPublicationDate, String maxPublicationDate)
            throws APIConnectorException {

        if(authorId == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetBooksForContributorIdRequest(authorId, offset, count == null ? DEFAULT_BOOK_LIMIT : count, desc, order, minPublicationDate, maxPublicationDate);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets books related to a particular ISBN
     *
     * @param isbn
     * @param offset
     * @param count
     * @return
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList getBooksRelatedToISBN(String isbn, Integer offset, Integer count) throws APIConnectorException {

        if(isbn == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetRelatedBooksRequest(isbn, offset, count == null ? DEFAULT_BOOK_LIMIT : count);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data */
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Searches for books based on the given query
     *
     * @param query
     * @param order
     * @param desc
     * @param offset
     * @param count
     * @return a BookInfoList object containing a list of books for the search query
     * @throws APIConnectorException if the request failed
     */
    public BBBBookInfoList searchBooks(String query, String order, Boolean desc, Integer offset, Integer count) throws APIConnectorException {

        if(query == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createSearchRequest(query, order, desc, offset, count == null ? DEFAULT_BOOK_LIMIT : count);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BBBBookSearchResponse searchResponse = new Gson().fromJson(response.getResponseData(), BBBBookSearchResponse.class);

            if(searchResponse != null && searchResponse.books != null) {
                String[] ids = new String[searchResponse.books.length];

                for (int i = 0; i < searchResponse.books.length; i++) {
                    ids[i] = searchResponse.books[i].id;
                }

                request = BBBRequestFactory.getInstance().createGetBooksRequest(null, null, ids);
                response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

                try {
                    BBBBookInfoList list = new Gson().fromJson(response.getResponseData(), BBBBookInfoList.class);
                    return list;
                } catch (IllegalStateException | JsonSyntaxException e) {
                    throw new JsonParsingException(e.getMessage());
                }
            } else {
                // throw an exception to indicate that there were no results
                throw new NoResultsException();
            }
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets search suggestions based on a query
     *
     * @param query
     * @param limit
     * @return a BBBSuggestionList object containing a list of suggestions for the search query
     * @throws APIConnectorException if the request failed
     */
    public BBBSuggestionList getSuggestions(String query, Integer limit) throws APIConnectorException {

        if(query == null) {
            return null;
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetSuggestionsRequest(query, limit == null ? DEFAULT_SUGGESTION_LIMIT : limit);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBSuggestionList list = new Gson().fromJson(response.getResponseData(), BBBSuggestionList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets all catalogue categories
     *
     * @return a BBBCategoryList object containing all available categories
     * @throws APIConnectorException if the request failed
     */
    public BBBCategoryList getCategories() throws APIConnectorException {
        BBBRequest request = BBBRequestFactory.getInstance().createGetAllCategoriesRequest(null);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBCategoryList list = new Gson().fromJson(response.getResponseData(), BBBCategoryList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            // A bad request typically means that we have exceeded the allowed offset when requesting data
            // throw an exception to indicate that there were no results
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets the prices for the given isbns
     *
     * @param isbns
     * @return a BBBBookPriceList containing a list of prices for all supplied isbns
     * @throws APIConnectorException if the request failed
     */
    public BBBBookPriceList getBookPriceList(String... isbns) throws APIConnectorException {

        BBBRequest request = BBBRequestFactory.getInstance().createGetPricesRequest(isbns);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBBookPriceList list = new Gson().fromJson(response.getResponseData(), BBBBookPriceList.class);
                return list;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new InternalServerErrorException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets a category based on its location id
     *
     * @param locationId
     * @return a BBBCategoryList object containing the category at the specified location
     * @throws APIConnectorException if the request failed
     */
    public BBBCategoryList getCategory(int locationId) throws APIConnectorException {
        BBBRequest request = BBBRequestFactory.getInstance().createGetCategoryWithLocationIdRequest(locationId);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBCategoryList categoryList = new Gson().fromJson(response.getResponseData(), BBBCategoryList.class);
                return categoryList;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new InternalServerErrorException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets the promotion with the given location
     *
     * @param promotionLocation
     * @return a BBBPromotionList with the promotion at the current location
     * @throws APIConnectorException if the request failed
     */
    public BBBPromotionList getPromotionWithLocation(int promotionLocation) throws APIConnectorException {
        BBBRequest request = BBBRequestFactory.getInstance().createGetPromotionsByLocationRequest(promotionLocation, null, null);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBPromotionList promotion = new Gson().fromJson(response.getResponseData(), BBBPromotionList.class);
                return promotion;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new InternalServerErrorException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Gets the author with the given id
     *
     * @param authorId the author id to lookup
     * @return a BBBContributor object
     * @throws APIConnectorException if the request failed
     */
    public BBBContributor getAuthor(String authorId) throws APIConnectorException {

        // If you ask for a null author id then you will get no results!
        if (authorId == null) {
            throw new NoResultsException();
        }

        BBBRequest request = BBBRequestFactory.getInstance().createGetContributorRequest(authorId);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                BBBContributor contributor = new Gson().fromJson(response.getResponseData(), BBBContributor.class);
                return contributor;
            } catch (IllegalStateException | JsonSyntaxException e) {
                throw new JsonParsingException(e.getMessage());
            }
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {
            throw new InternalServerErrorException();
        } else if (response.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
            throw new NoResultsException();
        } else {
            // Fall back to our common error handler
            throw getExceptionForCommonAPIErrors(response.getResponseCode());
        }
    }

    /**
     * Checks the size of the file at the given URL
     *
     * @param url the URL of the file
     * @return the size of the file in bytes
     */
    public long getFileSize(String url) {
        BBBRequest request = BBBRequestFactory.getInstance().createCheckFileSizeHeadRequest(url);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if(response == null || response.getHeaders() == null) {
            return -1;
        }

        List<String> contentLength = response.getHeaders().get("Content-Length");

        if(contentLength != null && contentLength.size() > 0) {
            String length = contentLength.get(0);

            try {
                return Long.parseLong(length);
            } catch(ArrayIndexOutOfBoundsException e) {}
        }

        return -1;
    }

    /**
     * Request a BBBCreditResponse from the server. Caller must handle the Exception
     * @return BBBCreditResponse object which includes the user's credit information
     */
    public BBBResponse getCreditResponse() throws Exception {
        BBBRequest request = BBBRequestFactory.getInstance().createGetCreditOnAccountRequest();
        return BBBRequestManager.getInstance().executeRequestSynchronously(request);
    }
}