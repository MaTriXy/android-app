package com.blinkboxbooks.android.ui.library;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.BookDownloadController;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.ui.AboutBookActivity;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.reader.TableOfContentsActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.PreferenceManager;

/**
 * Helper class for displaying the options associated with a book within the library and handling the user
 * selection of the option.
 */
public class LibraryBookOptionsHelper {

    // menu id constants for context menu
    private static final int MENU_ID_READ_SAMPLE = 1;
    private static final int MENU_ID_ABOUT_THIS_BOOK = 2;
    private static final int MENU_ID_READ_BOOK = 3;
    private static final int MENU_ID_DOWNLOAD_BOOK = 4;
    private static final int MENU_ID_READ_LATER= 5;
    private static final int MENU_ID_REMOVE_BOOK = 6;
    private static final int MENU_ID_REMOVE_SAMPLE = 7;
    private static final int MENU_ID_TABLE_OF_CONTENTS = 8;
    private static final int MENU_ID_BUY_THIS_BOOK = 9;

    private static final String TAG_REMOVE_FROM_DEVICE_WARNING_DIALOG = "remove_from_device_warning_dialog";

    // The first few times the user selects the remove from device option we show a warning. This value determines how
    // many times we will show the warning message.
    private static final int REMOVE_FROM_DEVICE_NUM_WARNINGS_TO_SHOW = 2;
    private static final int REMOVE_SAMPLE_NUM_WARNINGS_TO_SHOW = 2;

    private PopupMenu mPopupMenu;

    /**
     * Construct a new LibraryOptions Helper class
     */
    public LibraryBookOptionsHelper() {}

    /**
     * Display the list of options for the specified book item
     * @param activity the parent activity where any UI controls will be displayed
     * @param view the view to anchor the menu to
     * @param bookItem the book item to display options for
     */
    public void displayOptionsForBook(final BaseActivity activity, final View view, final BookItem bookItem, final int filterType) {

        mPopupMenu = new PopupMenu(activity, view);

        populatePopupMenuMenuForBook(activity, mPopupMenu.getMenu(), bookItem.book, filterType);

        mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                onMenuItemSelected(activity, item);
                return true;
            }
        });

        mPopupMenu.show();
    }

    /**
     * Hides the popup menu if it is currently being displayed.
     */
    public void hidePopupMenu() {
        if (mPopupMenu != null) {
            mPopupMenu.dismiss();
            mPopupMenu = null;
        }
    }

    /**
     * Populates a PopupMenu based on the state of a Book object
     *
     * @param context the Context object
     * @param menu the Menu to populate
     * @param book the Book to configure from
     * @param filterType the type of filter that is applied to the book in question
     */
    public void populatePopupMenuMenuForBook(Context context, Menu menu, Book book, int filterType) {
        if (filterType == BBBContract.Books.BOOK_STATE_READING) {

            if (! book.sample_book) {
                if (book.download_status == BBBContract.Books.DOWNLOADED) {
                    // Menu items for purchased and downloaded books when filtering books in the reading state
                    addAboutMenuOption(context, menu, book);
                    addContentsMenuOption(context, menu, book);
                    addReadMenuOption(context, menu, book);
                    addReadLaterMenuOption(context, menu, book);
                    addRemoveMenuOption(context, menu, book);
                } else {
                    // Menu items for purchased (but non-downloaded) books when filtering books in the reading state
                    addDownloadMenuOption(context, menu, book);
                    addAboutMenuOption(context, menu, book);
                    addReadLaterMenuOption(context, menu, book);
                }
            } else {
                if (book.download_status == BBBContract.Books.DOWNLOADED) {
                    // Menu items for downloaded samples when filtering books in the reading state
                    addBuyThisBookMenuOption(context, menu, book);
                    addAboutMenuOption(context, menu, book);
                    addContentsMenuOption(context, menu, book);
                    addReadMenuOption(context, menu, book);
                    addReadLaterMenuOption(context, menu, book);
                    addRemoveSampleMenuOption(context, menu, book);
                } else {
                    // Menu items for non-downloaded samples when filtering books in the reading state
                    addBuyThisBookMenuOption(context, menu, book);
                    addDownloadMenuOption(context, menu, book);
                    addAboutMenuOption(context, menu, book);
                    addReadLaterMenuOption(context, menu, book);
                    addRemoveSampleMenuOption(context, menu, book);
                }
            }

        } else if (filterType == LibraryBooksFragment.NO_FILTER) {
            if (! book.sample_book) {
                if (book.download_status == BBBContract.Books.DOWNLOADED) {
                    // Menu items for purchased and downloaded books in the unfiltered view
                    addAboutMenuOption(context, menu, book);
                    addContentsMenuOption(context, menu, book);
                    addReadMenuOption(context, menu, book);
                    addRemoveMenuOption(context, menu, book);
                } else {
                    // Menu items for purchased (but non-downloaded) books in the unfiltered view
                    addDownloadMenuOption(context, menu, book);
                    addAboutMenuOption(context, menu, book);
                }
            } else {
                if (book.download_status == BBBContract.Books.DOWNLOADED) {
                    // Menu items for downloaded samples when filtering books in the unfiltered view
                    addBuyThisBookMenuOption(context, menu, book);
                    addAboutMenuOption(context, menu, book);
                    addContentsMenuOption(context, menu, book);
                    addReadMenuOption(context, menu, book);
                    addRemoveSampleMenuOption(context, menu, book);
                } else {
                    // Menu items for non-downloaded samples when filtering books in the unfiltered view
                    addBuyThisBookMenuOption(context, menu, book);
                    addDownloadMenuOption(context, menu, book);
                    addAboutMenuOption(context, menu, book);
                    addRemoveSampleMenuOption(context, menu, book);
                }
            }
        }
    }

    private void addContentsMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_TABLE_OF_CONTENTS, Menu.NONE, R.string.menu_item_contents).setIntent(getOpenTOCIntent(context, book));
    }

    private void addAboutMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_ABOUT_THIS_BOOK, Menu.NONE, R.string.menu_item_about).setIntent(getOpenAboutIntent(context, book));
    }

    private void addReadMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_READ_BOOK, Menu.NONE, R.string.menu_item_read).setIntent(LibraryController.getOpenBookIntent(context, book));
    }

    private void addRemoveMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_REMOVE_BOOK, Menu.NONE, R.string.menu_item_remove).setIntent(getBookIntent(book));
    }

    // Remove sample is treated as a different option to remove book as we will treat it slightly differently
    private void addRemoveSampleMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_REMOVE_SAMPLE, Menu.NONE, R.string.menu_item_remove).setIntent(getBookIntent(book));
    }

    private void addReadLaterMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_READ_LATER, Menu.NONE, R.string.menu_item_read_later).setIntent(getBookIdAndIsbnIntent(book.id, book.isbn));
    }

    private void addDownloadMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_DOWNLOAD_BOOK, Menu.NONE, R.string.menu_item_download).setIntent(getBookIntent(book));
    }

    private void addBuyThisBookMenuOption(Context context, Menu menu, Book book) {
        menu.add(Menu.NONE, MENU_ID_BUY_THIS_BOOK, Menu.NONE, R.string.menu_item_buy_full_ebook).setIntent(getBookIntent(book));
    }

    /**
     * Handle the user selection of a menu item
     * @param activity the parent activity where the menu lives
     * @param item the menu item that was selected
     */
    private void onMenuItemSelected(final BaseActivity activity, final MenuItem item) {

        switch (item.getItemId()) {
            case MENU_ID_READ_BOOK: {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_READ_BOOK, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                startOpenBookIntent(activity, item.getIntent());
                break;
            }
            case MENU_ID_READ_SAMPLE: {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_READ_SAMPLE, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                startOpenBookIntent(activity, item.getIntent());
                break;
            }
            case MENU_ID_BUY_THIS_BOOK: {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_CLICK_BUY_BUTTON, AnalyticsHelper.GA_LABEL_BOOK_OPTIONS_LIBRARY_SCREEN, null);
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_BUY_FULL_BOOK, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                Book book = (Book) item.getIntent().getSerializableExtra(Book.class.getSimpleName());
                ShopItem shopItem = new ShopItem(book);
                PurchaseController.getInstance().buyPressed(shopItem);
                break;
            }
            case MENU_ID_ABOUT_THIS_BOOK: {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_ABOUT_BOOK, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                activity.startActivity(item.getIntent());
                break;
            }
            case MENU_ID_READ_LATER:
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_MARK_FINISHED, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                long id = item.getIntent().getLongExtra(BBBApiConstants.PARAM_ID, -1);
                BookHelper.updateBookReadingStatus(id, BBBContract.Books.BOOK_STATE_UNREAD);
                break;
            case MENU_ID_TABLE_OF_CONTENTS: {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_SEE_CONTENTS, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                activity.startActivity(item.getIntent());
                break;
            }
            case MENU_ID_REMOVE_BOOK: {
                final Book book = (Book) item.getIntent().getSerializableExtra(Book.class.getSimpleName());
                int removeWarningCount = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_KEY_REMOVE_FROM_DEVICE_WARNING_COUNT, 0);
                if (removeWarningCount < REMOVE_FROM_DEVICE_NUM_WARNINGS_TO_SHOW) {
                    PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_REMOVE_FROM_DEVICE_WARNING_COUNT, ++removeWarningCount);

                    GenericDialogFragment.newInstance(activity.getString(R.string.menu_item_remove), activity.getString(R.string.remove_book_warning_message), activity.getString(R.string.ok), activity.getString(R.string.button_cancel), null,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_REMOVE_BOOK_ON_DEVICE, book.isbn, null);
                                    deleteBook(book);
                                }
                            }, null, null, null).show(activity.getSupportFragmentManager(), TAG_REMOVE_FROM_DEVICE_WARNING_DIALOG);
                } else {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_REMOVE_BOOK_ON_DEVICE, book.isbn, null);
                    deleteBook(book);
                }
                break;
            }
            case MENU_ID_REMOVE_SAMPLE: {
                final Book book = (Book) item.getIntent().getSerializableExtra(Book.class.getSimpleName());
                int removeWarningCount = PreferenceManager.getInstance().getInt(PreferenceManager.PREF_KEY_REMOVE_SAMPLE_WARNING_COUNT, 0);
                if (removeWarningCount < REMOVE_SAMPLE_NUM_WARNINGS_TO_SHOW) {
                    PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_REMOVE_SAMPLE_WARNING_COUNT, ++removeWarningCount);

                    GenericDialogFragment.newInstance(activity.getString(R.string.menu_item_remove), activity.getString(R.string.remove_sample_warning_message), activity.getString(R.string.ok), activity.getString(R.string.button_cancel), null,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_REMOVE_SAMPLE, book.isbn, null);
                                    removeSample(book);
                                }
                            }, null, null, null).show(activity.getSupportFragmentManager(), TAG_REMOVE_FROM_DEVICE_WARNING_DIALOG);
                } else {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_REMOVE_SAMPLE, book.isbn, null);
                    removeSample(book);
                }
                break;
            }
            case MENU_ID_DOWNLOAD_BOOK: {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_OPTIONS_MENU, AnalyticsHelper.GA_EVENT_DOWNLOAD_BOOK, item.getIntent().getStringExtra(BBBApiConstants.PARAM_ISBN), null);
                Book book = (Book) item.getIntent().getSerializableExtra(Book.class.getSimpleName());
                BookDownloadController.getInstance(activity).startDownloadClicked(activity, book);
                break;
            }
        }
    }

    /**
     * Remove a sample from the user's library and delete the physical book download
     * @param book the book to remove from library
     */
    private void removeSample(final Book book) {
        eraseBook(book, true);
    }

    /**
     * Remove a downloaded book from the device
     * @param book the book whose download to remove
     */
    private void deleteBook(final Book book) {
        eraseBook(book, false);
    }

    /**
     * Clears a books download file (and removes from library if requested)
     * @param book the book to delete from the device
     * @param removeBook true if the book should be totally removed from the user's library
     */
    private void eraseBook(final Book book, final boolean removeBook) {
        // Do everything in an ASyncTask to avoid blocking the UI thread
        new AsyncTask<Void, Void, Void>() {
            protected Void doInBackground(Void... params) {
                Uri bookUri = BBBContract.Books.buildBookIdUri(book.id);
                ContentResolver contentResolver = BBBApplication.getApplication().getContentResolver();

                // Update the books status to be not downloaded for this user
                book.in_device_library = false;
                book.download_status = BBBContract.Books.NOT_DOWNLOADED;
                ContentValues values = BookHelper.getContentValues(book);
                contentResolver.update(bookUri, values, null, null);

                BookHelper.updateBookReadingStatus(book.id, BBBContract.Books.BOOK_STATE_UNREAD);

                BookHelper.deletePhysicalBook(book);

                // For samples we also delete the book uri from the content resolver
                if (removeBook) {
                    contentResolver.delete(bookUri, null, null);
                }
                return null;
            }
        }.execute();
    }

    private Intent getBookIntent(Book book) {
        Intent intent = new Intent();
        intent.putExtra(Book.class.getSimpleName(), book);
        return intent;
    }

    private Intent getOpenAboutIntent(Context context, Book book) {
        Intent intent = new Intent(context, AboutBookActivity.class);
        intent.putExtra(AboutBookActivity.PARAM_BOOK, book);
        return intent;
    }

    private Intent getOpenTOCIntent(Context context, Book book) {
        Intent intent = new Intent(context, TableOfContentsActivity.class);
        intent.putExtra(TableOfContentsActivity.PARAM_BOOK, book);
        return intent;
    }

    private void startOpenBookIntent(Context context, Intent intent) {
        AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
        context.startActivity(intent);
    }

    /**
     * Get a new {@link android.content.Intent} with the book id and isbn
     *
     * @param id   {@link com.blinkboxbooks.android.provider.BBBContract.Books} id
     * @param isbn {@link com.blinkboxbooks.android.provider.BBBContract.Books} isbn
     * @return
     */
    private Intent getBookIdAndIsbnIntent(long id, String isbn) {
        Intent intent = new Intent();
        intent.putExtra(BBBApiConstants.PARAM_ID, id);
        intent.putExtra(BBBApiConstants.PARAM_ISBN, isbn);
        return intent;
    }
}
