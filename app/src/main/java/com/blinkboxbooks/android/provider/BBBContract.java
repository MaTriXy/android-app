// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.net.Uri;
import android.provider.BaseColumns;

import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.model.Book;

/**
 * <p>
 * The contract between the {@link BBBProvider} and applications. Contains
 * definitions for the supported URIs and columns.
 * </p>
 */
public class BBBContract {

    public interface SyncState {
        /**
         * Sync state type
         */
        public static final int STATE_NORMAL = 0;
        public static final int STATE_ADDED = 1;
        public static final int STATE_DELETED = 2;
        public static final int STATE_UPDATED = 3;
    }

    /**
     * Columns of {@link BBBContract.Books} that refer to
     * properties of the book.
     */
    public interface BooksColumns {
        public static final String BOOK_LIBRARY_ID = "book_library_id";
        public static final String BOOK_SERVER_ID = "book_server_id";
        public static final String BOOK_SYNC_STATE = "book_sync_state";
        public static final String BOOK_AUTHOR = "book_author";
        public static final String BOOK_ISBN = "book_isbn";
        public static final String BOOK_TITLE = "book_title";
        public static final String BOOK_TAGS = "book_tags";
        public static final String BOOK_COVER_URL = "book_cover_url";
        public static final String BOOK_OFFER_PRICE = "book_offer_price";
        public static final String BOOK_NORMAL_PRICE = "book_normal_price";
        public static final String BOOK_DESCRIPTION = "book_description";
        public static final String BOOK_PUBLISHER = "book_publisher";
        public static final String BOOK_PURCHASE_DATE = "book_purchase_date";
        public static final String BOOK_UPDATE_DATE = "book_update_date";
        public static final String BOOK_SYNC_DATE = "book_last_sync_date";
        public static final String BOOK_PUBLICATION_DATE = "book_publication_date";
        public static final String BOOK_STATE = "book_state";
        public static final String BOOK_DOWNLOAD_COUNT = "book_download_count";
        public static final String BOOK_IN_DEVICE_LIBRARY = "book_is_in_device_library";
        public static final String BOOK_IS_EMBEDDED = "book_is_embedded";
        public static final String BOOK_IS_SAMPLE = "book_is_sample";
        public static final String BOOK_FORMAT = "book_format";
        public static final String BOOK_FILE_PATH = "book_file_path";
        public static final String BOOK_MEDIA_PATH = "book_media_path";
        public static final String BOOK_KEY_PATH = "book_key_path";
        public static final String BOOK_SIZE = "book_size";
        public static final String BOOK_DOWNLOAD_STATUS = "book_download_status";
        public static final String BOOK_DOWNLOAD_OFFSET = "book_download_offset";
        public static final String BOOK_ENCRYPTION_KEY = "book_enc_key";
    }

    /**
     * Columns of {@link BBBContract.Bookmarks} that refer to
     * properties of the lastPosition.
     */
    public interface BookmarksColumns {
        public static final String BOOKMARK_CLOUD_ID = "bookmark_cloud_id";
        public static final String BOOKMARK_BOOK_ID = "bookmark_book_id";
        public static final String BOOKMARK_NAME = "bookmark_name";
        public static final String BOOKMARK_CONTENT = "bookmark_content";
        public static final String BOOKMARK_TYPE = "bookmark_type";
        public static final String BOOKMARK_ANNOTATION = "bookmark_annotation";
        public static final String BOOKMARK_STYLE = "bookmark_style";
        public static final String BOOKMARK_UPDATE_BY = "bookmark_create_by";
        public static final String BOOKMARK_UPDATE_DATE = "bookmark_create_date";
        public static final String BOOKMARK_PERCENTAGE = "bookmark_percentage";
        public static final String BOOKMARK_STATE = "bookmark_state";
        public static final String BOOKMARK_COLOR = "bookmark_color";
        public static final String BOOKMARK_ISBN = "bookmark_isbn";
        public static final String BOOKMARK_POSITION = "bookmark_position";
    }

    /**
     * Columns of {@link BBBContract.Sections} that refer to
     * properties of the section.
     */
    public interface SectionsColumns {
        public static final String SECTION_BOOK_ID = "section_book_id";
        public static final String SECTION_PATH = "section_path";
        public static final String SECTION_MEDIA_TYPE = "section_media_type";
        public static final String SECTION_INDEX = "section_index";
        public static final String SECTION_FILE_SIZE = "section_file_size";
    }

    /**
     * Columns of {@link BBBContract.NavPoints} that refer to
     * properties of the navigation point.
     */
    public interface NavPointsColumns {
        public static final String NAVPOINT_BOOK_ID = "navpoint_book_id";
        public static final String NAVPOINT_LABEL = "navpoint_label";
        public static final String NAVPOINT_LINK = "navpoint_link";
        public static final String NAVPOINT_INDEX = "navpoint_index";
        public static final String NAVPOINT_DEPTH = "navpoint_depth";
    }

    /**
     * Columns of {@link BBBContract.Authors} that refer to
     * properties of the author.
     */
    public interface AuthorsColumns {
        public static final String AUTHOR_ID = "author_cloud_id";
        public static final String AUTHOR_BOOK_ID = "author_book_id";
        public static final String AUTHOR_NAME = "author_name";

    }

    /**
     * Columns of {@link BBBContract.BookshelvesColumns} that refer to
     * properties of the bookshelf.
     */
    public interface BookshelvesColumns {
        public static final String BOOKSHELF_NAME = "bookshelf_name";
        public static final String BOOKSHELF_LIBRARY_ID = "bookshelf_library_id";
        public static final String BOOKSHELF_CREATE_DATE = "bookshelf_create_date";
    }

    /**
     * Columns of {@link BBBContract.Libraries} that refer to
     * properties of the library.
     */
    public interface LibrariesColumns {
        public static final String LIBRARY_CREATE_DATE = "library_create_date";
        public static final String LIBRARY_SYNC_DATE = "library_sync_date";
        public static final String LIBRARY_BOOKMARK_SYNC_DATE = "library_bookmark_sync_date";
        public static final String LIBRARY_ACCOUNT = "library_account";
    }

    /**
     * Columns of {@link BBBContract.ReaderSettings} that refer to
     * properties of the reader setting.
     */
    public interface ReaderSettingsColumns {
        public static final String READER_BACKGROUND_COLOR = "reader_background_color";
        public static final String READER_FOREGROUND_COLOR = "reader_foreground_color";
        public static final String READER_FONT_SIZE = "reader_font_size";
        public static final String READER_BRIGHTNESS = "reader_brightness";
        public static final String READER_FONT_TYPEFACE = "reader_font_typeface";
        public static final String READER_LINE_SPACE = "reader_line_space";
        public static final String READER_ORIENTATION_LOCK = "reader_orientation_lock";
        public static final String READER_SHOW_HEADER = "reader_show_header";
        public static final String READER_SHOW_FOOTER = "reader_show_footer";
        public static final String READER_CLOUD_BOOKMARK = "reader_cloud_bookmark";
        public static final String READER_TEXT_ALIGN = "reader_text_align";
        public static final String READER_MARGIN_TOP = "reader_margin_top";
        public static final String READER_MARGIN_BOTTOM = "reader_margin_bottom";
        public static final String READER_MARGIN_LEFT = "reader_margin_left";
        public static final String READER_MARGIN_RIGHT = "reader_margin_right";
        public static final String READER_ORIENTATION = "reader_orientation";
        public static final String READER_ACCOUNT = "reader_account";
        public static final String READER_PUBLISHER_STYLES = "reader_publisher_styles";
    }

    public static final String CONTENT_AUTHORITY = BuildConfig.APPLICATION_ID;

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_ID = "id";
    private static final String PATH_BOOK_ID = "book_id";
    private static final String PATH_LIBRARIES = "libraries";
    private static final String PATH_BOOKS = "books";
    private static final String PATH_LOCAL_BOOK = "local";
    private static final String PATH_CLOUD_BOOK = "cloud";

    private static final String PATH_AUTHORS = "authors";

    private static final String PATH_BOOKMARKS = "bookmarks";
    private static final String PATH_SECTIONS = "sections";
    private static final String PATH_NAVPOINT = "navpoints";

    private static final String PATH_READER_SETTING = "reader_settings";
    private static final String PATH_READING_STATUS = "reading_status";
    private static final String PATH_ACCOUNT = "account";
    private static final String PATH_ALL = "all";
    private static final String PATH_DOWNLOAD = "download";
    private static final String PATH_TYPE = "type";
    private static final String PATH_SERVER_ID = "server_id";
    private static final String PATH_SYNCHRONIZATION = "synchronization";
    private static final String PATH_ACTIVE = "active";

    private static final String PATH_BOOK_STATUS = "status";
    private static final String PATH_BOOK_RECENT_PURCHASE = "recent_purchase";

    /**
     * Class repents book source.
     */
    public static class Books implements BooksColumns, BaseColumns, SyncState {

        public static final String TABLE_NAME = "books";

        /**
         * The constant should be passed when request all books in the library *
         */
        public static final int ALL_BOOKS_REQ = -1;

        final static String BOOKS_JOIN_LIBRARIES = "books "
                + "LEFT OUTER JOIN libraries ON books.book_library_id = libraries._id ";

        final static String BOOKS_JOIN_LIBRARIES_BOOKSHELVES = "books "
                + "LEFT OUTER JOIN libraries ON books.book_library_id = libraries._id "
                + "LEFT OUTER JOIN bookshelves ON books.book_shelf_id = bookshelves._id";

        final static String BOOKS_JOIN_LIBRARIES_BOOKMARKS = "books "
                + "LEFT OUTER JOIN libraries ON books.book_library_id = libraries._id "
                + "LEFT OUTER JOIN (select * from bookmarks where bookmark_type = 0 ) as bookmarks ON books._id = bookmarks.bookmark_book_id ";

        final static String BOOKS_JOIN_BOOKMARKS = "books " + "LEFT JOIN bookmarks ON books._id = bookmarks.bookmark_book_id ";

        /**
         * book status
         */
        public static final int BOOK_STATE_RECENTLY_PURCHASED = 0;
        public static final int BOOK_STATE_READING = 1;
        public static final int BOOK_STATE_UNREAD = 2;
        public static final int BOOK_STATE_FINISHED = 3;

        /**
         * book download status
         */
        public static final int NOT_DOWNLOADED = 0;
        public static final int DOWNLOADING = 1;
        public static final int DOWNLOADED = 2;
        public static final int DOWNLOAD_FAILED = -1;

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOKS).build();
        public static final Uri READING_STATUS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOKS).appendPath(PATH_READING_STATUS).appendPath(PATH_ID).build();
        public static final Uri ACCOUNT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOKS).appendPath(PATH_ACCOUNT).build();
        public static final Uri ACCOUNT_BOOKS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOKS).appendPath(PATH_ACCOUNT).appendPath(PATH_ID).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.ccom.blinkboxbooks.androidbook";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.cocom.blinkboxbooks.androidook";

        /**
         * Default "ORDER BY" clause.
         */
        public static final String DEFAULT_SORT = BaseColumns._ID + " DESC ";
        public static final String LAST_READ_SORT = BooksColumns.BOOK_UPDATE_DATE + " DESC ";
        public static final String TITLE_SORT = BooksColumns.BOOK_TITLE + " COLLATE NOCASE ASC";
        public static final String AUTHOR_SORT = AuthorsColumns.AUTHOR_NAME + " COLLATE NOCASE ASC";
        public static final String PURCHASE_DATE_SORT = BooksColumns.BOOK_PURCHASE_DATE + " DESC ";

        public static final String[] sortTypeList = {LAST_READ_SORT, TITLE_SORT, AUTHOR_SORT, PURCHASE_DATE_SORT};

        /**
         * Read {@link #BOOK_ID} from {@link Books} {@link Uri}.
         */
        public static String getBookId(Uri uri) {
            return uri.getPathSegments().get(3);
        }

        /**
         * Read User Account from {@link Books} {@link Uri}.
         */
        public static String getUserAccount(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Read {@link #BOOK_STATUS} from {@link Books} {@link Uri}.
         */
        public static String getBookStatus(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        /**
         * Read {@link #BOOK_PURCHASE_DATE} from {@link Books} {@link Uri}.
         */
        public static String getBookPurchaseDate(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        /**
         * Read {@link #BOOK_SERVER_ID} from {@link Books} {@link Uri}.
         */
        public static String getBookServerId(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        /**
         * Build {@link Uri} for requested {@link BaseColumns._ID}.
         */
        public static Uri buildBookIdUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(PATH_ID).appendPath(Long.toString(id)).build();
        }

        /**
         * Build {@link Uri} for requested {@link BaseColumns._ID}.
         */
        public static Uri buildDownloadBookIdUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(PATH_DOWNLOAD).appendPath(PATH_ID).appendPath(Long.toString(id)).build();
        }

        /**
         * Build {@link Uri} for requested {@link BaseColumns._ID}.
         */
        public static Uri buildBookReadingStatusIdUri(long id) {
            return CONTENT_URI.buildUpon().appendPath(PATH_READING_STATUS).appendPath(PATH_ID).appendPath(Long.toString(id)).build();
        }

        /**
         * Build {@link Uri} for the book which has already been downloaded
         */
        public static Uri buildBookLocalUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_LOCAL_BOOK).build();
        }

        /**
         * Build {@link Uri} for the cloud library
         */
        public static Uri buildBookCloudUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_CLOUD_BOOK).build();
        }

        /**
         * Build {@link Uri} for requested {@link Libraies.account}.
         */
        public static Uri buildBookAccountUri(String account) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).build();
        }

        /**
         * Build {@link Uri} for requested {@link Libraies.account}.
         */
        public static Uri buildBookAccountUriAll(String account) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_ALL).build();
        }

        /**
         * Gets a URI which will return the books for the active user account or the anonymous library
         *
         * @return th Uri
         */
        public static Uri buildBookActiveAccountUriAll() {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(PATH_ACTIVE).build();
        }

        /**
         * Build {@link Uri} that references any {@link Book} associated with the requested book status and number.
         *
         * @param account,   the account name
         * @param bookStatus
         * @return
         */
        public static Uri buildBookStatusUri(String account, String bookStatus) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_BOOK_STATUS).appendPath(bookStatus).build();
        }

        /**
         * Build {@link Uri} that references any {@link Book} that purchased between the requested time boundaries. but exclude the book
         *
         * @param account,  the account name
         * @param startDate it can be set to {@link #ALL_BOOKS_REQ}, return all books
         * @return
         */
        public static Uri buildBookRecentPurchasedUri(String account, long startDate) {

            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_BOOK_RECENT_PURCHASE).appendPath(Long.toString(startDate)).build();
        }

        /**
         * Build {@link Uri} that references any {@link Book}
         *
         * @param account,  the account name
         * @return
         */
        public static Uri buildBookSynchronizationUri(String account) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_SYNCHRONIZATION).build();
        }

        /**
         * Build {@link Uri} that references any {@link Book}
         *
         * @param account,  the account name
         * @param server_id
         * @return
         */
        public static Uri buildBookServerIdUri(String account, String server_id) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_SERVER_ID).appendPath(server_id).build();
        }

    }

    public static class Libraries implements LibrariesColumns, BaseColumns {
        // table name for the Sqlite database
        public static final String TABLE_NAME = "libraries";

        String LIBRARIES_JOIN_BOOKS = "libraries "
                + "LEFT OUTER JOIN books ON books.book_library_id = libraries._id";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_LIBRARIES).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.comcom.blinkboxbooks.androidbrary";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.com.blinkboxbooks.androidrary";

        /**
         * Read {@link #Library.account} from {@link Libraries} {@link Uri}.
         */
        public static String getAccount(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Read {@link #Library._ID} from {@link Libraries} {@link Uri}.
         */
        public static String getLibraryId(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Build {@link Uri} for requested account.
         */
        public static Uri buildLibrariesIdUri(long libraryId) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ID).appendPath(Long.toString(libraryId)).build();
        }

        /**
         * Build {@link Uri} for requested account.
         */
        public static Uri buildLibrariesAccountUri(String account) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).build();
        }
    }


    public static class Authors implements AuthorsColumns, BaseColumns {
        // table name for the Sqlite database
        public static final String TABLE_NAME = "authors";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_AUTHORS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.bcom.blinkboxbooks.androidor";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.blinkboxbooks.android.author";

        /**
         * Read {@link #AUTHOR._ID} from {@link Authors} {@link Uri}.
         */
        public static String getAuthorId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class Bookmarks implements BookmarksColumns, BaseColumns, SyncState {
        // table name for the Sqlite database
        public static final String TABLE_NAME = "bookmarks";

        public static final String BOOKMARKS_JOIN_BOOKS_LIBRARIES = "bookmarks "
                + "LEFT OUTER JOIN books ON bookmarks.bookmark_book_id = books._id "
                + "LEFT OUTER JOIN libraries ON books.book_library_id = libraries._id ";

        /**
         * Last position type
         */
        public static final int TYPE_LAST_POSITION = 0;
        public static final int TYPE_BOOKMARK = 1;
        public static final int TYPE_HIGHLIGHT = 2;
        public static final int TYPE_NOTE = 3;

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_BOOKMARKS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.lastPosition";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.blinkboxbooks.android.lastPosition";

        /**
         * Read {@link #Bookmark._ID} from {@link Bookmarks} {@link Uri}.
         */
        public static String getBookmarkId(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Read {@link #Book._ID} from the Bookmark Type {@link Uri}.
         */
        public static String getBookId(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        /**
         * Read {@link #Bookmark.type} from the Bookmark Type {@link Uri}.
         */
        public static String getBookmarkType(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Read {@link #Bookmark.cloud_id} from the Bookmark Type {@link Uri}.
         */
        public static String getCloudId(Uri uri) {
            return uri.getPathSegments().get(4);
        }

        /**
         * Build {@link Uri} for requested {@link #Bookmark}.
         */
        public static Uri buildBookmarkIdUri(long bookmarkid) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ID).appendPath(Long.toString(bookmarkid)).build();
        }

        /**
         * Build {@link Uri} for requested {@link #Bookmark.type}.
         */
        public static Uri buildBookmarkTypeUri(int type, long bookid) {
            return CONTENT_URI.buildUpon().appendPath(PATH_TYPE).appendPath(Integer.toString(type)).appendPath(PATH_BOOK_ID).appendPath(Long.toString(bookid)).build();
        }

        /**
         * Build {@link Uri} for synchronisation.
         */
        public static Uri buildBookmarkSynchronizationUri(String account) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_SYNCHRONIZATION).build();
        }

        /**
         * Build {@link Uri} for requested {@link #Bookmark}.
         */
        public static Uri buildBookmarkCloudIdUri(String account, String cloud_id) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).appendPath(PATH_SERVER_ID).appendPath(cloud_id).build();
        }
    }

    public static class Sections implements SectionsColumns, BaseColumns {
        // table name for the Sqlite database
        public static final String TABLE_NAME = "sections";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_SECTIONS).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.section";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.blinkboxbooks.android.section";

        /**
         * Read {@link #NAVPOINT_ISBN} from {@link Sections} {@link Uri}.
         */
        public static String getSectionIsbn(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Read {@link #Sections._ID} from {@link Sections} {@link Uri}.
         */
        public static String getSectionId(Uri uri) {
            return uri.getPathSegments().get(1);
        }

    }

    public static class NavPoints implements NavPointsColumns, BaseColumns {
        // table name for the Sqlite database
        public static final String TABLE_NAME = "navpoints";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_NAVPOINT).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.navpoint";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.blinkboxbooks.android.navpoint";

        /**
         * Read {@link #NavPoints._ID} from {@link NavPoints} {@link Uri}.
         */
        public static String getNavpointId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    public static class ReaderSettings implements ReaderSettingsColumns, BaseColumns {
        // table name for the Sqlite database
        public static final String TABLE_NAME = "readersettings";

        public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_READER_SETTING).build();

        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.blinkboxbooks.android.readersetting";
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.blinkboxbooks.android.readersetting";

        /**
         * Read {@link #READER_ORIENTATION} from {@link ReaderSettings}
         * {@link Uri}.
         */
        public static String getOrientation(Uri uri) {
            return uri.getPathSegments().get(1);
        }

        /**
         * Read {@link #READER_ACCOUNT} from {@link ReaderSettings} {@link Uri}.
         */
        public static String getAccount(Uri uri) {
            return uri.getPathSegments().get(2);
        }

        /**
         * Build {@link Uri} for requested {@link #READER_ACCOUNT}.
         */
        public static Uri buildReaderSettingAccountUri(String account) {
            return CONTENT_URI.buildUpon().appendPath(PATH_ACCOUNT).appendPath(account).build();
        }
    }

    private BBBContract() {
    }
}
