// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
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
import com.blinkboxbooks.android.provider.BBBProvider;

/**
 * This class tests the content provider for the Blinkbox books Android app.
 */
public class BBBProviderLibraryTest extends ProviderTestCase2<BBBProvider> {

	// Contains a reference to the mocked content resolver for the provider
	// under test.
	private MockContentResolver mMockResolver;

	// Contains an SQLite database, used as test data
	private SQLiteDatabase mDb;

	// Contains the test data, as an array of Book instances.
	private final Book[] TEST_BOOKS = {
			// template library
			new TestBook(1, 1, "server_id1", "Eric YUAN", "1323423342", "ERIC'S FIRST BOOK", "", "book cover url", 109, 23, "Eric's first book", "mobcast",
					100, 2, 1, 0, 0, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id2", "Chirag Patel", "999993342", "Chirag's FIRST BOOK", "", "book cover url", 129, 38, "Chirag's first book", "mobcast",
					101, 2, 1, 0, 0, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id3", "Tim", "13234233332", "Tim'S FIRST BOOK", "", "book cover url", 109, 23, "Tim's first book", "mobcast",
					102, 2, 1, 0, 1, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id4", "Vandad", "9999933999", "Vandad's Tenth BOOK", "", "book cover url", 129, 38, "Vandad's first book", "mobcast",
					103, 2, 1, 0, 2, 1, false, false, "epub", "filepath", 100, 0, 0),
			new TestBook(1, 1, "server_id2", "Derek", "999993342", "Derek's FIRST BOOK", "", "book cover url", 129, 38, "Derek's first book", "mobcast",
					101, 2, 1, 0, 0, 1, false, false, "epub", "filepath", 100, 0, 0),
	};

	// Contains the test data, as an array of Library instances.
	private final Library[] TEST_LIBRARIES = {
			new TestLibrary("567", "890", LibraryHelper.TEMPLATE_ACCOUNT),
	};

	/**
	 * Constructor for the test case class. Calls the super constructor with the class name of the provider under test and the authority name of the provider.
	 */
	public BBBProviderLibraryTest() {
		super(BBBProvider.class, BBBContract.CONTENT_AUTHORITY);
	}

	/*
	 * Sets up the test environment before each test method. Creates a mock content resolver, gets the provider under test, and creates a new database for the provider.
	 */
	@Override
	protected void setUp() throws Exception {
		// Calls the base class implementation of this method.
		super.setUp();

		// Gets the resolver for this test.
		mMockResolver = getMockContentResolver();

		/*
		 * Gets a handle to the database underlying the provider. Gets the provider instance created in super.setUp(), gets the DatabaseOpenHelper for the provider, and gets a
		 * database object from the helper.
		 */
		mDb = getProvider().getOpenHelperForTest().getWritableDatabase();
		mDb.delete(Books.TABLE_NAME, null, null);
		mDb.delete(Libraries.TABLE_NAME, null, null);
		mDb.close();
	}

	/**
	 * Test the library uris are correct
	 */
	public void testLibraryUri() {
		String account = "testy";
		Uri libraryUri = Libraries.buildLibrariesAccountUri(account);
		assertEquals(account, Libraries.getAccount(libraryUri));

		long libraryId = 23;
		Uri libraryIdUri = Libraries.buildLibrariesIdUri(libraryId);
		assertEquals(libraryId, (long) Long.valueOf(Libraries.getLibraryId(libraryIdUri)));
	}

	/**
	 * Tests that the anonymous users library can be created from the template
	 */
	public void testCreateAnonymousUser() {
		insertData();

		// Assert that the Anonymous library is empty
		Uri uri = Libraries.buildLibrariesAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME);
		Cursor cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Transfer the Anonymous library from the template
		LibraryHelper.createAnonymousLibrary(mMockResolver);

		// Assert that the Anonymous library has been created
		uri = Libraries.buildLibrariesAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Gets all the books for the anonymous user
		cursor = mMockResolver.query(Books.buildBookAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null, null, null);
		// Asserts that the returned cursor contains the same number of rows as the template data
		assertEquals(5, cursor.getCount());
		cursor.close();

		// Attempt to create another anonymous library
		LibraryHelper.createAnonymousLibrary(mMockResolver);

		// Assert that there is still only 1 Anonymous library
		uri = Libraries.buildLibrariesAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();
	}

	/**
	 * Tests that the anonymous users library is transferred after login
	 */
	public void testTransferUser() {
		insertData();

		String changedAuthor = "Eric New";
		String account = "12312";

		// Assert that the users library is empty
		Uri uri = Libraries.buildLibrariesAccountUri(account);
		Cursor cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Create the anonymous library
		LibraryHelper.createAnonymousLibrary(mMockResolver);

		// Update the Anonymous library
		cursor = mMockResolver.query(Books.buildBookAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null, null, null);
		cursor.moveToFirst();
		Book book = BookHelper.createBook(cursor);
		assertEquals("Eric YUAN", book.author);
		assertEquals(6, book.id);
		cursor.close();

		book.author = changedAuthor;
		int rows = mMockResolver.update(Books.buildBookIdUri(book.id), BookHelper.getContentValues(book), null, null);
		assertEquals(1, rows);

		// Transfer the users library from the anonymous library
		LibraryHelper.copyLibrary(BusinessRules.DEFAULT_ACCOUNT_NAME, account, mMockResolver);

		// Assert that the Anonymous library has been created
		uri = Libraries.buildLibrariesAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Gets all the books for the anonymous user
		cursor = mMockResolver.query(Books.buildBookAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null, null, null);
		// Asserts that the returned cursor contains the same number of rows as the template data
		assertEquals(5, cursor.getCount());

		// Assert the book id is correct
		cursor.moveToFirst();
		book = BookHelper.createBook(cursor);
		assertEquals(changedAuthor, book.author);
		assertEquals(6, book.id);
		cursor.close();

		// Assert that the users library has been created
		uri = Libraries.buildLibrariesAccountUri(account);
		cursor = mMockResolver.query(uri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Gets all the books for the user
		cursor = mMockResolver.query(Books.buildBookAccountUri(account), null, null, null, null);
		// Asserts that the returned cursor contains the same number of rows as the template data
		assertEquals(5, cursor.getCount());

		// Assert the book id is correct
		cursor.moveToFirst();
		book = BookHelper.createBook(cursor);
		assertEquals(11, book.id);
		assertEquals(changedAuthor, book.author);
		cursor.close();
	}


	/**
	 * Tests that the anonymous users library is transferred after login
	 */
	public void testResetToOriginalSettings() {
		insertData();

		String changedAuthor = "Eric New";

		// Create the anonymous library
		LibraryHelper.createAnonymousLibrary(mMockResolver);

		// Update the Anonymous library
		Cursor cursor = mMockResolver.query(Books.buildBookStatusUri(BusinessRules.DEFAULT_ACCOUNT_NAME, "0"), null, null, null, null);
		assertEquals(3, cursor.getCount());
		cursor.moveToFirst();
		Book book = BookHelper.createBook(cursor);
		assertEquals("Eric YUAN", book.author);
		assertEquals(6, book.id);
		cursor.close();
		book.author = changedAuthor;

		int rows = mMockResolver.update(Books.buildBookIdUri(book.id), BookHelper.getContentValues(book), null, null);
		assertEquals(1, rows);
		rows = mMockResolver.delete(Books.buildBookIdUri(book.id + 1), null, null);
		assertEquals(1, rows);

		Bookmark bookmark = new Bookmark();
		bookmark.book_id = book.id;
		bookmark.type = Bookmarks.TYPE_BOOKMARK;
		bookmark.update_by = "Tim";
		bookmark.isbn = "12312312";
		bookmark.position = "/6/4[chap04ref]!/3:10,8IASKJGNKA";
		Uri bookmarkUri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, book.id);
		bookmarkUri = mMockResolver.insert(bookmarkUri, BookmarkHelper.getContentValues(bookmark));

		// Assert the anonymous library is setup correctly
		cursor = mMockResolver.query(Books.buildBookAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null, null, null);
		assertEquals(5, cursor.getCount());
		cursor.moveToFirst();
		book = BookHelper.createBook(cursor);
		assertEquals(changedAuthor, book.author);
		assertEquals(6, book.id);
		cursor.close();

		cursor = mMockResolver.query(bookmarkUri, null, null, null, null);
		assertEquals(1, cursor.getCount());
		cursor.close();

		// Delete the anonymous library
		rows = mMockResolver.delete(Libraries.buildLibrariesAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null);
		assertEquals(1, rows);

		// Assert the anonymous library was deleted
		cursor = mMockResolver.query(Books.buildBookAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		cursor = mMockResolver.query(bookmarkUri, null, null, null, null);
		assertEquals(0, cursor.getCount());
		cursor.close();

		// Recreate
		LibraryHelper.createAnonymousLibrary(mMockResolver);

		// Assert created
		cursor = mMockResolver.query(Books.buildBookAccountUri(BusinessRules.DEFAULT_ACCOUNT_NAME), null, null, null, null);
		assertEquals(5, cursor.getCount());
		cursor.close();
	}

	/*
	 * Sets up test data. The test data is in an SQL database. It is created in setUp() without any data, and populated in insertData if necessary. return the {@Code _id} of the
	 * last record added.
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
