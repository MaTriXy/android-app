// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.Library;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.provider.BBBContract.Libraries;
import com.blinkboxbooks.android.provider.BBBContract.SyncState;
import com.blinkboxbooks.android.provider.BBBProvider;

/**
 * This class tests the content provider for the Blinkbox books Android app.
 */
public class BBBProviderTest extends ProviderTestCase2<BBBProvider> {

	// Contains a reference to the mocked content resolver for the provider
	// under test.
	private MockContentResolver mMockResolver;

	// Contains an SQLite database, used as test data
	private SQLiteDatabase mDb;

	// Contains the test data, as an array of Book instances.
	private final Book[] TEST_BOOKS = {
			// library 1
			new TestBook(1, 1, "server_id1", "Eric YUAN", "1323423342", "ERIC'S FIRST BOOK", "", "book cover url", 109, 23, "Eric's first book", "mobcast",
					100, 2, 1, 0, 1, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id2", "Chirag Patel", "999993342", "Chirag's FIRST BOOK", "", "book cover url", 129, 38, "Chirag's first book", "mobcast",
					101, 2, 1, 0, 1, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id3", "Tim", "13234233332", "Tim'S FIRST BOOK", "", "book cover url", 109, 23, "Tim's first book", "mobcast",
					102, 2, 1, 0, 2, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id4", "Vandad", "9999933999", "Vandad's Tenth BOOK", "", "book cover url", 129, 38, "Vandad's first book", "mobcast",
					103, 2, 1, 0, 3, 1, false, false, "epub", "filepath", 100, 0, 0),
			// Library 2
			new TestBook(2, 1, "server+id5", "Eric YUAN", "1323423342", "ERIC'S FIRST BOOK", "", "book cover url", 109, 23, "Eric's first book", "mobcast",
					100, 2, 1, 0, 1, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(2, 1, "server_id6", "Chirag Patel", "999993342", "Chirag's FIRST BOOK", "", "book cover url", 129, 38, "Chirag's first book", "mobcast",
					101, 2, 1, 0, 1, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(2, 1, "server_id7", "Tim", "13234233332", "Tim'S FIRST BOOK", "", "book cover url", 109, 23, "Tim's first book", "mobcast",
					102, 2, 1, 0, 2, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(2, 1, "server_id8", "Vandad", "9999933999", "Vandad's Tenth BOOK", "", "book cover url", 129, 38, "Vandad's first book", "mobcast",
					103, 2, 1, 0, 3, 1, false, false, "epub", "filepath", 100, 0, 0)
	};


	// Contains the test data, as an array of Library instances.
	private final Library[] TEST_LIBRARIES = {
			new TestLibrary("567", "890", BusinessRules.DEFAULT_ACCOUNT_NAME),
			new TestLibrary("123", "234", "Eric")
	};


	/**
	 * Constructor for the test case class. Calls the super constructor with the
	 * class name of the provider under test and the authority name of the
	 * provider.
	 */
	public BBBProviderTest() {
		super(BBBProvider.class, BBBContract.CONTENT_AUTHORITY);
	}

	/*
	 * Sets up the test environment before each test method. Creates a mock
	 * content resolver, gets the provider under test, and creates a new
	 * database for the provider.
	 */
	@Override
	protected void setUp() throws Exception {
		// Calls the base class implementation of this method.
		super.setUp();

		// Gets the resolver for this test.
		mMockResolver = getMockContentResolver();
		
		/*
		 * Gets a handle to the database underlying the provider. Gets the
		 * provider instance created in super.setUp(), gets the
		 * DatabaseOpenHelper for the provider, and gets a database object from
		 * the helper.
		 */
		mDb = getProvider().getOpenHelperForTest().getReadableDatabase();
		mDb.delete(Books.TABLE_NAME, null, null);
		mDb.delete(Libraries.TABLE_NAME, null, null);
		mDb.close();
	}

	/*
	 * Tests the provider's Library table
	 */
	public void testGetLibraryIdFromAccount() {
		String account = "Chirag";
		Uri uri = Libraries.buildLibrariesAccountUri(account);
		Cursor cursor = mMockResolver.query(uri, null, null, null, null);

		// Asserts that the returned cursor contains no records
		assertEquals(0, cursor.getCount());
		cursor.close();

		Library library = new Library();
		library.account = account;

		// Insert the record and assert it is returned
		mMockResolver.insert(uri, LibraryHelper.getContentValues(library));
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Insert the record again assert only 1 exists
		mMockResolver.insert(uri, LibraryHelper.getContentValues(library));
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();
	}

	/*
	 * Tests the provider's public API for querying data in the table, using the
	 * URI for a dataset of records.
	 */
	public void testQueriesOnBooksUri() {

		String account = "Eric";
		// If there are no records in the table, the returned cursor from a
		// query should be empty.
		Cursor cursor = mMockResolver.query(Books.buildBookAccountUri(account), null, null, null, null);

		// Asserts that the returned cursor contains no records
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Inserts the test data into the provider's underlying data source
		insertData();

		// Gets all the columns for all the rows in the table
		cursor = mMockResolver.query(Books.buildBookAccountUri(account), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows as
		// the size of the
		// test data array.
		assertEquals(4, cursor.getCount());
		cursor.close();
	}

	/*
	 * Tests queries against the provider, using the book server_id URI
	 */
	public void testQueriesOnServerIdUri() {
		// Insert the test data
		insertData();

		Uri serverIdUri = Books.buildBookServerIdUri("Eric", "server+id5");
		Cursor cursor = mMockResolver.query(serverIdUri, null, null, null, null);

		// Asserts that the returned cursor contains only one row.
		assertEquals(1, cursor.getCount());

		// Moves to the cursor's first row, and asserts that this did not fail.
		assertTrue(cursor.moveToFirst());
		cursor.close();

		mMockResolver.delete(serverIdUri, null, null);
		cursor = mMockResolver.query(serverIdUri, null, null, null, null);

		// Asserts that the record was deleted
		assertEquals(0, cursor.getCount());
		cursor.close();
	}

	/*
	 * Tests queries against the provider, using the book id URI. This URI
	 * encodes a single record ID. The provider should only return 0 or 1
	 * record.
	 */
	public void testQueriesOnBookIdUri() {

		// Inserts the test data into the provider's underlying data source and
		// get the _id
		long bookId = insertData();

		// Constructs a URI that matches the provider's book id URI pattern
		// using an non-exist value of 0 as the book Id.
		Uri bookIdUri = Books.buildBookIdUri(bookId);

		// Queries the table using the book id URI, which should returns a
		// single
		// record with the specified book id.
		Cursor cursor = mMockResolver.query(bookIdUri, null, null, null, null);

		// Asserts that the returned cursor contains only one row.
		assertEquals(1, cursor.getCount());

		// Moves to the cursor's first row, and asserts that this did not fail.
		assertTrue(cursor.moveToFirst());

		// Asserts that the book id passed to the provider is the same as the
		// book id returned.
		assertEquals(bookId, cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)));
		cursor.close();

	}

	/*
	 * Tests the provider's public API for querying books by given status and
	 * numbers.
	 */
	public void testQueriesOnBooksByPurchaseDateUri() {

		String account = "Eric";

		// If there are no records in the table, the returned cursor from a
		// query should be empty.
		Cursor cursor = mMockResolver.query(Books.buildBookRecentPurchasedUri(account, 100), null, null, null, null);

		// Asserts that the returned cursor contains no records
		assertEquals(0, cursor.getCount());

		// Inserts the test data into the provider's underlying data source
		cursor.close();
		insertData();

		// Gets all the books by the given purchase date
		cursor = mMockResolver.query(Books.buildBookRecentPurchasedUri(account, 100), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Gets all the books by the given purchase date
		cursor = mMockResolver.query(Books.buildBookRecentPurchasedUri(account, 101), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Gets all book by the given purchase date
		cursor = mMockResolver.query(Books.buildBookRecentPurchasedUri(account, 103), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(0, cursor.getCount());
		cursor.close();

	}

	/*
	 * Tests the provider's public API for querying books by given purchase date
	 * and numbers.
	 */
	public void testQueriesOnBooksByStatusUri() {

		String account = "Eric";

		// If there are no records in the table, the returned cursor from a
		// query should be empty.
		Cursor cursor = mMockResolver.query(Books.buildBookStatusUri(account, "1"), null, null, null, null);

		// Asserts that the returned cursor contains no records
		assertEquals(0, cursor.getCount());

		// Inserts the test data into the provider's underlying data source
		cursor.close();
		insertData();

		// Gets all the books with status "0"
		cursor = mMockResolver.query(Books.buildBookStatusUri(account, "1"), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(2, cursor.getCount());
		cursor.close();

		// Gets all the books with status "1"
		cursor = mMockResolver.query(Books.buildBookStatusUri(account, "2"), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(1, cursor.getCount());
		cursor.close();


		// Gets all the books with status "3"
		cursor = mMockResolver.query(Books.buildBookStatusUri(account, "3"), null, null, null, null);

		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(1, cursor.getCount());
		cursor.close();

	}

	/*
	 * Tests the provider's public API for querying books by given purchase date
	 * and numbers.
	 */
	public void testBasicSynchronisation() {
		long bookId = insertData();
		String account = "Eric";

		Cursor cursor = mMockResolver.query(Books.buildBookStatusUri(account, "3"), null, null, null, null);
		// Asserts that the returned cursor contains the same number of rows in
		// test data array.
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Test that no sync records appear
		cursor = mMockResolver.query(Books.buildBookSynchronizationUri(account), null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Delete the book from the app (mark as STATE_DELETED)
		mMockResolver.delete(Books.buildBookIdUri(bookId), null, null);

		// Test that sync STATE_DELETED records appear here
		cursor = mMockResolver.query(Books.buildBookSynchronizationUri(account), null, null, null, null);
		cursor.moveToFirst();
		Book book = BookHelper.createBook(cursor);
		assertEquals(1, cursor.getCount());
		assertEquals(bookId, book.id);
		assertEquals(Books.STATE_DELETED, book.sync_state);
		cursor.close();

		// Test that sync STATE_DELETED records do not appear
		cursor = mMockResolver.query(Books.buildBookStatusUri(account, "3"), null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();
	}


	/*
	 * Tests the provider's public API for updating book by given book id.
	 */
	public void testUpdateOnBookByIdUri() {
		// Inserts the test data into the provider's underlying data source and
		// get the _id
		long bookId = insertData();

		// Constructs a URI that matches the provider's book id URI pattern
		// using an non-exist value of 0 as the book Id.
		Uri bookIdUri = Books.buildBookIdUri(bookId);

		// Queries the table using the book id URI, which should returns a
		// single
		// record with the specified book id.
		Cursor cursor = mMockResolver.query(bookIdUri, null, null, null, null);

		// Asserts that the returned cursor contains only one row.
		assertEquals(1, cursor.getCount());
		cursor.moveToFirst();
		Book book = BookHelper.createBook(cursor);
		cursor.close();

		book.author = "test";

		mMockResolver.update(Books.buildBookIdUri(book.id), BookHelper.getContentValues(book), null, null);

		cursor = mMockResolver.query(bookIdUri, null, null, null, null);
		cursor.moveToFirst();

		book = BookHelper.createBook(cursor);
		assertEquals(8, book.id);
		assertEquals("test", book.author);
		cursor.close();

	}

	/*
	 * Tests the provider's public API for updating book by given book id.
	 */
	public void testUpdateBookLastReadingPosition() {
		// Inserts the test data into the provider's underlying data source and
		// get the _id
		long bookId = insertData();
		Bookmark bookmark = new Bookmark();
		bookmark.book_id = bookId;
		bookmark.type = Bookmarks.TYPE_LAST_POSITION;
		bookmark.update_by = "Tim";
		bookmark.isbn = "12312312";
		bookmark.position = "/6/4[chap04ref]!/3:10,8IASKJGNKA";

		Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_LAST_POSITION, bookId);
		Cursor cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		mMockResolver.insert(uri, BookmarkHelper.getContentValues(bookmark));

		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		mMockResolver.insert(uri, BookmarkHelper.getContentValues(bookmark));

		cursor = mMockResolver.query(uri, null, null, null, null);
		cursor.moveToFirst();

		assertEquals(1, cursor.getCount());
		Bookmark newBookmark = BookmarkHelper.createBookmark(cursor);
		assertEquals(Bookmarks.TYPE_LAST_POSITION, newBookmark.type);
		assertEquals("Tim", newBookmark.update_by);
		cursor.close();
	}

	/*
	 * Tests the provider's public API for updating book by given book id.
	 */
	public void testInsertAndQueryBookmarks() {
		// Inserts the test data into the provider's underlying data source and
		// get the _id
		long bookId = insertData();
		String account = "Eric";
		Bookmark bookmark = new Bookmark();
		bookmark.book_id = bookId;
		bookmark.type = Bookmarks.TYPE_BOOKMARK;
		bookmark.update_by = account;
		bookmark.isbn = "12312312";
		bookmark.position = "/6/4[chap04ref]!/3:10,8IASKJGNKA";

		// Assert there are no bookmarks
		Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, bookId);
		Cursor cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Insert a lastPosition and insert there is 1
		mMockResolver.insert(uri, BookmarkHelper.getContentValues(bookmark));
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Insert another lastPosition and validate it
		Uri bookmarkUri = mMockResolver.insert(uri, BookmarkHelper.getContentValues(bookmark));
		cursor = mMockResolver.query(uri, null, null, null, null);
		cursor.moveToFirst();
		assertEquals(2, cursor.getCount());
		Bookmark newBookmark = BookmarkHelper.createBookmark(cursor);
		assertEquals(Bookmarks.TYPE_BOOKMARK, newBookmark.type);
		assertEquals(account, newBookmark.update_by);
		cursor.close();

		// Assert both bookmarks require synchronisation
		Uri syncUri = Bookmarks.buildBookmarkSynchronizationUri(account);
		cursor = mMockResolver.query(syncUri, null, null, null, null);
		assertEquals(2, cursor.getCount());
		cursor.moveToFirst();
		newBookmark = BookmarkHelper.createBookmark(cursor);
		assertEquals(SyncState.STATE_ADDED, newBookmark.state);
		long bookmarkid = cursor.getLong(0);
		cursor.close();

		// Mark the lastPosition as successfully synced
		uri = Bookmarks.buildBookmarkIdUri(bookmarkid);
		newBookmark.cloud_id = "1299";
		newBookmark.state = SyncState.STATE_NORMAL;
		ContentValues values = BookmarkHelper.getContentValues(newBookmark);
		mMockResolver.update(uri, values, null, null);

		// Assert it was updated
		uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, bookId);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(2, cursor.getCount());
		cursor.moveToFirst();
		newBookmark = BookmarkHelper.createBookmark(cursor);
		assertEquals("1299", newBookmark.cloud_id);
		assertEquals(SyncState.STATE_NORMAL, newBookmark.state);
		cursor.close();

		// Mark the 2nd lastPosition as deleted
		mMockResolver.delete(bookmarkUri, null, null);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Delete all bookmarks
		mMockResolver.delete(uri, null, null);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Assert the bookmarks require synchronisation (STATE_DELETED)
		cursor = mMockResolver.query(syncUri, null, null, null, null);
		assertEquals(2, cursor.getCount());
		cursor.moveToFirst();
		newBookmark = BookmarkHelper.createBookmark(cursor);
		assertEquals(SyncState.STATE_DELETED, newBookmark.state);
		cursor.close();
	}

	/*
	 * Tests the provider's public API for highlights and notes
	 */
	public void testInsertAndQueryNotes() {
		// Inserts the test data into the provider's underlying data source and
		// get the _id
		long bookId = insertData();

		Bookmark bookmark = new Bookmark();
		bookmark.book_id = bookId;
		bookmark.type = Bookmarks.TYPE_HIGHLIGHT;
		bookmark.annotation = "This highlight has an annotation";
		bookmark.update_by = "Tim";
		bookmark.isbn = "12312312";
		bookmark.position = "/6/4[chap04ref]!/3:10,8IASKJGNKA";

		// Insert a highlight with an annotation and assert it appears in both the note and highlight list
		Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_HIGHLIGHT, bookId);
		mMockResolver.insert(uri, BookmarkHelper.getContentValues(bookmark));
		Cursor cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		Uri uriNote = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_NOTE, bookId);
		cursor = mMockResolver.query(uriNote, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();
	}

	/*
	 * Sets up test data. The test data is in an SQL database. It is created in
	 * setUp() without any data, and populated in insertData if necessary.
	 * 
	 * return the {@Code _id} of the last record added.
	 */
	private long insertData() {
		mDb = getProvider().getOpenHelperForTest().getReadableDatabase();
		long id = -1;
		// Sets up test data
		for (int index = 0; index < TEST_LIBRARIES.length; index++) {
			// Adds a record to the database.
			id = mDb.insertOrThrow(Libraries.TABLE_NAME, Libraries.LIBRARY_ACCOUNT, LibraryHelper.getContentValues(TEST_LIBRARIES[index]));
		}
		for (int index = 0; index < TEST_BOOKS.length; index++) {
			// Adds a record to the database.
			id = mDb.insertOrThrow(Books.TABLE_NAME, Books.BOOK_TITLE, BookHelper.getContentValues(TEST_BOOKS[index]));
		}
		mDb.close();
		return id;
	}

	/*
	 * A help class to create {@link Book} object
	 */
	private static class TestBook extends Book {

		private static final long serialVersionUID = -8460095106089401766L;

		/*
		 * Constructor for a TestBook instance.
		 */
		public TestBook(long library_id, long book_shelf_id, String server_id, String author, String isbn, String title, String tags, String cover_url, float offer_price,
						float normal_price, String description, String publisher, int purchase_date, int update_date, int sync_date, long publication_date,
						int state, int download_count, boolean embedded_book, boolean sample_book, String format, String file_path, long file_size, int download_status,
						long download_offset) {
			this.library_id = library_id;
			this.server_id = server_id;
			this.author = author;
			this.isbn = isbn;
			this.title = title;
			this.tags = tags;
			this.cover_url = cover_url;
			this.offer_price = offer_price;
			this.normal_price = normal_price;
			this.description = description;
			this.publisher = publisher;
			this.purchase_date = purchase_date;
			this.update_date = update_date;
			this.sync_date = sync_date;
			this.publication_date = publication_date;
			this.state = state;
			this.download_count = download_count;
			this.embedded_book = embedded_book;
			this.sample_book = sample_book;
			this.format = format;
			this.file_path = file_path;
			this.file_size = file_size;
			this.download_status = download_status;
			this.download_offset = download_offset;

		}
	}


	/*
	 * A help class to create {@link Library} object
	 */
	private static class TestLibrary extends Library {

		/*
		 * Constructor for a TestBook instance.
		 */
		public TestLibrary(String date_created, String date_last_sync, String account) {
			this.date_created = date_created;
			this.date_library_last_sync = date_last_sync;
			this.account = account;
		}
	}
}
