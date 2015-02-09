// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.SparseArray;

import com.AdX.tag.AdXConnect;
import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.model.ShopItem;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;
import com.google.android.gms.analytics.ecommerce.Product;
import com.google.android.gms.analytics.ecommerce.ProductAction;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for Google Analytics
 */
public class AnalyticsHelper {

    private static AnalyticsHelper instance;

    public static AnalyticsHelper getInstance() {

        if (instance == null) {
            instance = new AnalyticsHelper();
        }

        return instance;
    }

    private static final String TAG = AnalyticsHelper.class.getSimpleName();

    public static final String AFFILIATION = "BBB Android App Shop";

    private static final String BLINKBOX_EMAIL_SUFFIX = "@blinkbox.com";

    //Screens
    public static final String GA_SCREEN_App_WelcomeScreen = "App_WelcomeScreen";
    public static final String GA_SCREEN_App_ResetPasswordScreen = "App_ResetPasswordScreen";
    public static final String GA_SCREEN_App_ResetPasswordScreen_EmailConfirmation = "App_ResetPasswordScreen_EmailConfirmation";

    public static final String GA_SCREEN_Library_RegistrationScreen = "Library_RegistrationScreen";
    public static final String GA_SCREEN_Library_SignInScreen = "Library_SignInScreen";
    public static final String GA_SCREEN_Library_YourLibraryScreen_Reading = "Library_YourLibraryScreen_Reading";
    public static final String GA_SCREEN_Library_YourLibraryScreen_MyLibrary = "Library_YourLibraryScreen_MyLibrary";
    public static final String GA_SCREEN_Library_YourLibraryScreen_Reading_Anonymous = "Library_YourLibraryScreen_Reading_Anonymous";
    public static final String GA_SCREEN_Library_YourLibraryScreen_MyLibrary_Anonymous = "Library_YourLibraryScreen_MyLibrary_Anonymous";
    public static final String GA_SCREEN_Library_FAQScreen = "Library_FAQScreen";

    public static final String GA_SCREEN_Reader = "Reader";
    public static final String GA_SCREEN_Reader_EndOfSampleScreen = "Reader_EndOfSampleScreen";
    public static final String GA_SCREEN_Reader_EndOfBookScreen = "Reader_EndOfBookScreen";
    public static final String GA_SCREEN_Reader_TOCScreen = "Reader_TOCScreen";
    public static final String GA_SCREEN_Reader_AboutThisBookScreen = "Reader_AboutThisBookScreen";
    public static final String GA_SCREEN_Reader_MyBookmarksScreen = "Reader_MyBookmarksScreen";
    public static final String GA_SCREEN_Reader_SettingsScreen = "Reader_SettingsScreen";

    public static final String GA_SCREEN_Shop_CategoriesScreen = "Shop_CategoriesScreen";
    public static final String GA_SCREEN_Shop_BookDetailsScreen = "Shop_BookDetailsScreen/"; // + Book ISBN
    public static final String GA_SCREEN_Shop_SoftLoginScreen_PasswordConfirmation = "Shop_SoftLoginScreen_PasswordConfirmation";
    public static final String GA_SCREEN_Shop_PaymentScreen_CardDetails = "Shop_PaymentScreen_CardDetails";
    public static final String GA_SCREEN_Shop_PaymentScreen_PersonalDetails = "Shop_PaymentScreen_PersonalDetails";
    public static final String GA_SCREEN_Shop_PaymentScreen_AddNewCard = "Shop_PaymentScreen_AddNewCard";
    public static final String GA_SCREEN_Shop_PaymentScreen_ConfirmOrder = "Shop_PaymentScreen_ConfirmOrder";
    public static final String GA_SCREEN_Shop_PaymentScreen_EditCard = "Shop_PaymentScreen_EditCard";
    public static final String GA_SCREEN_Shop_PaymentScreen_Success = "Shop_PaymentScreen_Success";
    public static final String GA_SCREEN_Shop_RegistrationScreen = "Shop_RegistrationScreen";
    public static final String GA_SCREEN_Shop_SignInScreen = "Shop_SignInScreen";
    public static final String GA_SCREEN_Shop_Category = "Shop_Category/"; // + category name
    public static final String GA_SCREEN_Shop_Search_Query_Prefix = "Shop_Search?q="; // + search term
    public static final String GA_SCREEN_Shop_Author = "Shop_Author";
    public static final String GA_SCREEN_Shop_Related = "Shop_Related";
    public static final String GA_SCREEN_Shop_Featured = "Shop_Featured";
    public static final String GA_SCREEN_Shop_Highlights = "Shop_Highlights";
    public static final String GA_SCREEN_Shop_Bestsellers_Fiction = "Shop_BestSellers_Fiction";
    public static final String GA_SCREEN_Shop_Bestsellers_Non_Fiction = "Shop_BestSellers-Non-Fiction";
    public static final String GA_SCREEN_Shop_Free = "Shop_Free_Ebooks";
    public static final String GA_SCREEN_Shop_New_Released = "Shop_New_Releases";

    //Events
    public static final String GA_EVENT_REGISTRATION_STATUS = "RegistrationStatus";
    public static final String GA_EVENT_PURCHASE_STATUS = "PurchaseStatus";
    public static final String GA_EVENT_SAVE_SAMPLE_BOOK = "SaveSampleBook";
    public static final String GA_EVENT_FREE_BOOK = "FreeBook";
    public static final String GA_EVENT_DOWNLOAD_STATUS_SUCCESS = "DownloadStatus-Success";
    public static final String GA_EVENT_DOWNLOAD_STATUS_FAILED = "DownloadStatus-Failed";
    public static final String GA_EVENT_FINISHED_READING_FULL_BOOK = "FinishedReadingFullBook";
    public static final String GA_EVENT_FINISHED_READING_SAMPLE_BOOK = "FinishedReadingSampleBook";
    public static final String GA_EVENT_CLICK_BUY_BUTTON = "ClickBuyButton";
    public static final String GA_EVENT_REMOVE_SAMPLE = "RemoveSample";
    public static final String GA_EVENT_DOWNLOAD_ERROR = "DownloadError";
    public static final String GA_EVENT_SIGN_IN_ERROR = "SignInError";
    public static final String GA_EVENT_REGISTRATION_ERROR = "RegistrationError";
    public static final String GA_EVENT_ON_YOUR_DEVICE = "OnYourDevice";
    public static final String GA_EVENT_IN_YOUR_CLOUD = "InYourCloud";
    public static final String GA_EVENT_SIGN_IN = "SignIn";
    public static final String GA_EVENT_SIGN_OUT = "SignOut";
    public static final String GA_EVENT_REFRESH_YOUR_LIBRARY = "RefreshYourLibrary";
    public static final String GA_EVENT_FORCE_REFRESH_YOUR_LIBRARY = "ForceRefreshYourLibrary";
    public static final String GA_EVENT_SHOP_MORE_BOOKS = "ShopMoreBooks";
    public static final String GA_EVENT_FAQ = "FAQ";
    public static final String GA_EVENT_CONTACT_US = "ContactUs";
    public static final String GA_EVENT_INFO = "Info";
    public static final String GA_EVENT_SHOP = "shop";
    public static final String GA_EVENT_FORCED_UPGRADE_ACCEPTED = "ForcedUpgradeAccepted";
    public static final String GA_EVENT_FORCED_UPGRADE_DISMISSED = "ForcedUpgradeDismissed";
    public static final String GA_EVENT_FRIENDLY_UPGRADE_ACCEPTED = "FriendlyUpgradeAccepted";
    public static final String GA_EVENT_FRIENDLY_UPGRADE_DISMISSED = "FriendlyUpgradeDismissed";
    public static final String GA_EVENT_FIND_MORE_BOOKS_CLICKED = "FindMoreBooksClicked";

    // LibraryOptionsMenu events

    public static final String GA_EVENT_BUY_FULL_BOOK = "BuyFullBook";
    public static final String GA_EVENT_ABOUT_BOOK = "AboutBook";
    public static final String GA_EVENT_READ_BOOK = "ReadBook";
    public static final String GA_EVENT_DOWNLOAD_BOOK = "DownloadBook";
    public static final String GA_EVENT_SEE_CONTENTS = "SeeContents";
    public static final String GA_EVENT_READ_SAMPLE = "ReadSample";
    public static final String GA_EVENT_MARK_FINISHED = "MarkFinished";
    public static final String GA_EVENT_REMOVE_BOOK_ON_DEVICE = "RemoveBookOnDevice";

    public static final String GA_EVENT_FICTION_TOP100 = "FictionTop100";
    public static final String GA_EVENT_NONFICTION_TOP100 = "NonFictionTop100";
    public static final String GA_EVENT_CATEGORIES = "Categories";
    public static final String GA_EVENT_BOX = "Box";
    public static final String GA_EVENT_CATEGORY_MENU = "CategoryMenu";

    // ProductPage events
    public static final String GA_EVENT_AUTHOR = "Auhor";
    public static final String GA_EVENT_COVER = "Cover";
    public static final String GA_EVENT_DESCRIPTION = "Description";
    public static final String GA_EVENT_SAMPLE = "Sample";
    public static final String GA_EVENT_BUY = "Buy";

    // end of sample events
    public static final String GA_EVENT_GO_TO_LIBRARY = "GotoLibrary";
    public static final String GA_EVENT_MOREBOOKS = "MoreBooks";

    // Reader Settings events
    public static final String GA_EVENT_FONT_SELECTION = "FontSelection";
    public static final String GA_EVENT_ALIGNMENT = "Alignment";
    public static final String GA_EVENT_PAGE_COLOUR = "PageColour";
    public static final String GA_EVENT_SPACING = "Spacing";
    public static final String GA_EVENT_ZOOM = "Zoom";
    public static final String GA_EVENT_BRIGHTNESS = "Brightness";

    // Search event
    public static final String GA_EVENT_SEARCH_RESULT_NOT_FOUND = "SearchResultNotFound";
    public static final String GA_EVENT_SEARCH_RESULT_PAGE = "SearchResultPage";
    public static final String GA_EVENT_SEARCH_RESULT = "SearchResult";

    // Dictionary event
    public static final String GA_EVENT_DICTIONARY_INVOKED = "DictionaryInvoked";

    //Labels
    public static final String GA_LABEL_FORM_INTERACTION = "FormInteraction";
    public static final String GA_LABEL_REGISTRATION_SUCCESS = "RegistrationSuccess";
    public static final String GA_LABEL_PURCHASE_FAILED = "PurchaseFailed";
    public static final String GA_LABEL_END_OF_SAMPLE_SCREEN = "EndOfSampleScreen";
    public static final String GA_LABEL_BOOK_OPTIONS_LIBRARY_SCREEN = "BookOptions_LibraryScreen";
    public static final String GA_LABEL_EXCEED_DOWNLOAD_LIMIT = "ExceedDownloadLimit";
    public static final String GA_LABEL_DOWNLOAD_FAILED = "DownloadFailed";
    public static final String GA_LABEL_NO_NETWORK = "NoNetwork";
    public static final String GA_LABEL_NO_EMAIL = "NoEmail";
    public static final String GA_LABEL_INCORRECT_LOGIN = "IncorrectLogin";
    public static final String GA_LABEL_EXCEEDED_DEVICE_LIMIT = "ExceededDeviceLimit";
    public static final String GA_LABEL_ALREADY_REGISTERED = "AlreadyRegistered";
    public static final String GA_LABEL_WRONG_EMAIL_FORMAT = "WrongEmailFormat";
    public static final String GA_LABEL_PASSWORD_MISMATCH = "PasswordMismatch";
    public static final String GA_LABEL_SHORT_PASSWORD = "ShortPassword";
    public static final String GA_LABEL_NO_PASSWORD = "NoPassword";

    //Categories
    public static final String CATEGORY_NAME_USERINTERFACE = "user_interface";
    public static final String CATEGORY_EBOOK = "eBook";
    public static final String CATEGORY_REGISTRATION = "Registration";
    public static final String CATEGORY_BOOK_PURCHASE = "BookPurchase";
    public static final String CATEGORY_BOOK_SEARCH = "BookSearch";
    public static final String CATEGORY_BOOK_DOWNLOADS = "BookDownloads";
    public static final String CATEGORY_READING = "Reading";
    public static final String CATEGORY_CALL_TO_ACTIONS = "CallToActions";
    public static final String CATEGORY_ERROR = "Error";
    public static final String CATEGORY_LIBRARY_MENU_CLICK = "LibraryMenuClick";
    public static final String CATEGORY_LIBRARY_GESTURE = "LibraryGesture";
    public static final String CATEGORY_LIBRARY_OPTIONS_MENU = "LibraryOptionsMenu";
    public static final String CATEGORY_SHOP_TOP_NAV = "ShopTopNav";
    public static final String CATEGORY_TOP_FICTION = "TopFiction";
    public static final String CATEGORY_TOP_NONFICTION = "TopNonFiction";
    public static final String CATEGORY_CATEGORIES = "Categories";
    public static final String CATEGORY_PRODUCT_PAGE = "ProductPage";
    public static final String CATEGORY_ENDOF_SAMPLE = "EndOfSample";
    public static final String CATEGORY_CATEGORY_LISTINGS = "CategoryListings";
    public static final String CATEGORY_SEARCH = "Search";
    public static final String CATEGORY_READER_ABOUT_THIS_BOOK = "ReaderAboutThisBook";
    public static final String CATEGORY_READER_MENU = "ReaderMenu";
    public static final String CATEGORY_READER_SETTINGS = "ReaderSettings";
    public static final String CATEGORY_LIBRARY_ITEM_CLICK = "LibraryItemClick";

    //Custom dimensions
    public static final int CD_NAME_USER_ID = 1;
    public static final int CD_NAME_USER_ROLE = 2;
    public static final int CD_NAME_LOGIN_STATUS = 3;
    public static final int CD_NAME_USER_TYPE = 4;

    public static final String CD_VALUE_REGISTERED = "Registered";
    public static final String CD_VALUE_NON_REGISTERED = "NonRegistered";
    public static final String CD_VALUE_LOGGED_IN = "LoggedIn";
    public static final String CD_VALUE_NOT_LOGGED_IN = "NotLoggedIn";
    public static final String CD_VALUE_BLINKBOX_STAFF = "BlinkboxStaff";
    public static final String CD_VALUE_NON_BLINKBOX_STAFF = "NonBlinkboxStaff";

    //ADX Event names
    public static final String ADX_EVENT_LAUNCH = "Launch";
    public static final String ADX_EVENT_REGISTER = "Register";
    public static final String ADX_EVENT_VIEW_PRODUCT = "ViewProduct";
    public static final String ADX_EVENT_SALE = "Sale";
    public static final String ADX_EVENT_SALE_FREE = "Sale_Free";
    public static final String ADX_EVENT_SALE_PAID = "Sale_Paid";

    // Enhanced eCommerce related constants
    public static final String EE_SCREEN_NAME_PURCHASE_WITH_CREDIT = "purchase with credit";
    public static final String EE_SCREEN_NAME_PURCHASE_WITH_CARD = "purchase with card";
    public static final String EE_SCREEN_NAME_END_OF_BOOK = "end of book";
    public static final String EE_SCREEN_NAME_END_OF_SAMPLE = "end of sample";

    // The maximum number of impressions to report in one batch. We need to limit the amount of reports
    // in one report as the GA message seems to get truncated.
    private static final int EE_IMPRESSION_BATCH_SIZE = 10;

    private final HashMap<String, Long> timings;

    private Tracker mTracker;

    private AnalyticsHelper() {
        timings = new HashMap<String, Long>();

        Context context = BBBApplication.getApplication();

        String key = context.getResources().getString(R.string.google_analytics_key);

        mTracker = GoogleAnalytics.getInstance(BBBApplication.getApplication()).newTracker(key);
        mTracker.enableAdvertisingIdCollection(true);

        LogUtils.d(TAG, "Initialising analytics with key: " + key);

        if (BuildConfig.DEBUG) {
            setDebug();
        }
    }

    @SuppressWarnings("deprecation")
    private void setDebug() {
        GoogleAnalytics.getInstance(BBBApplication.getApplication()).getLogger().setLogLevel(Logger.LogLevel.VERBOSE);
        GoogleAnalytics.getInstance(BBBApplication.getApplication()).setLocalDispatchPeriod(60);
    }

    // We have five pretty much identical private methods to set the custom dimensions for each builder type.
    // Each builder type inherits the same setCustomDimension method but sadly that is within an internal protected class
    // and is therefore not accessible at this level. It is preferable to keep these five methods together (rather than
    // distribute the code in each send method) because if we decide to change or add a custom dimension we can edit
    // all methods here and more easily identify if one has been forgotten etc.

    private void setCustomDimensionsForEvent(HitBuilders.EventBuilder builder) {
        AccountController controller = AccountController.getInstance();
        builder.setCustomDimension(CD_NAME_USER_ID, controller.getUserId());
        builder.setCustomDimension(CD_NAME_USER_ROLE, controller.isLoggedIn() ? CD_VALUE_REGISTERED : CD_VALUE_NON_REGISTERED);
        builder.setCustomDimension(CD_NAME_LOGIN_STATUS, controller.isLoggedIn() ? CD_VALUE_LOGGED_IN : CD_VALUE_NOT_LOGGED_IN);
        builder.setCustomDimension(CD_NAME_USER_TYPE, getUserType(controller));
    }

    private void setCustomDimensionsForItem(HitBuilders.ItemBuilder builder) {
        AccountController controller = AccountController.getInstance();
        builder.setCustomDimension(CD_NAME_USER_ID, controller.getUserId());
        builder.setCustomDimension(CD_NAME_USER_ROLE, controller.isLoggedIn() ? CD_VALUE_REGISTERED : CD_VALUE_NON_REGISTERED);
        builder.setCustomDimension(CD_NAME_LOGIN_STATUS, controller.isLoggedIn() ? CD_VALUE_LOGGED_IN : CD_VALUE_NOT_LOGGED_IN);
        builder.setCustomDimension(CD_NAME_USER_TYPE, getUserType(controller));
    }

    private void setCustomDimensionsForTiming(HitBuilders.TimingBuilder builder) {
        AccountController controller = AccountController.getInstance();
        builder.setCustomDimension(CD_NAME_USER_ID, controller.getUserId());
        builder.setCustomDimension(CD_NAME_USER_ROLE, controller.isLoggedIn() ? CD_VALUE_REGISTERED : CD_VALUE_NON_REGISTERED);
        builder.setCustomDimension(CD_NAME_LOGIN_STATUS, controller.isLoggedIn() ? CD_VALUE_LOGGED_IN : CD_VALUE_NOT_LOGGED_IN);
        builder.setCustomDimension(CD_NAME_USER_TYPE, getUserType(controller));
    }

    private void setCustomDimensionsForTransaction(HitBuilders.TransactionBuilder builder) {
        AccountController controller = AccountController.getInstance();
        builder.setCustomDimension(CD_NAME_USER_ID, controller.getUserId());
        builder.setCustomDimension(CD_NAME_USER_ROLE, controller.isLoggedIn() ? CD_VALUE_REGISTERED : CD_VALUE_NON_REGISTERED);
        builder.setCustomDimension(CD_NAME_LOGIN_STATUS, controller.isLoggedIn() ? CD_VALUE_LOGGED_IN : CD_VALUE_NOT_LOGGED_IN);
        builder.setCustomDimension(CD_NAME_USER_TYPE, getUserType(controller));
    }

    private void setCustomDimensionsForScreenView(HitBuilders.ScreenViewBuilder builder) {
        AccountController controller = AccountController.getInstance();
        builder.setCustomDimension(CD_NAME_USER_ID, controller.getUserId());
        builder.setCustomDimension(CD_NAME_USER_ROLE, controller.isLoggedIn() ? CD_VALUE_REGISTERED : CD_VALUE_NON_REGISTERED);
        builder.setCustomDimension(CD_NAME_LOGIN_STATUS, controller.isLoggedIn() ? CD_VALUE_LOGGED_IN : CD_VALUE_NOT_LOGGED_IN);
        builder.setCustomDimension(CD_NAME_USER_TYPE, getUserType(controller));
    }

    private String getUserType(AccountController controller) {
        String userName = AccountController.getInstance().getDataForLoggedInUser(BBBApiConstants.PARAM_USERNAME);

        if(!TextUtils.isEmpty(userName) && userName.endsWith(BLINKBOX_EMAIL_SUFFIX)) {
            return CD_VALUE_BLINKBOX_STAFF;
        } else {
            return CD_VALUE_NON_BLINKBOX_STAFF;
        }
    }

    /**
     * Set the user userId that is associated with the tracker.
     * @param userId the userId to set (null to clear the user)
     */
    public void setUserId(String userId) {
        mTracker.set("&uid", userId);
    }

    /**
     * Send an event to Google Analytics.
     *
     * @param eventCategory Event Category (required)
     * @param eventAction   Event Action (required)
     * @param eventLabel    Event Label
     * @param eventValue    Event Value
     */
    public void sendEvent(String eventCategory, String eventAction, String eventLabel, Long eventValue) {

        HitBuilders.EventBuilder eventBuilder =
                new HitBuilders.EventBuilder()
                        .setCategory(eventCategory)
                        .setAction(eventAction)
                        .setLabel(eventLabel)
                        .setValue(eventValue != null ? eventValue : 0l);

        setCustomDimensionsForEvent(eventBuilder);

        Map<String, String> eventParams =  eventBuilder.build();

        mTracker.send(eventParams);

        LogUtils.d(TAG, String.format("Tracking event category: %s, event action: %s, event label: %s, event value: %s", eventCategory, eventAction, eventLabel, eventValue));
    }

    /**
     * Sends a successful transaction event
     *
     * @param transactionId the id of the transaction
     * @param price         the total price that was paid
     * @param currencyCode  the currency code of the price
     * @param productName   the name of the book/product
     * @param sku           the ISBN of the book
     */
    public void sendTransaction(String transactionId, double price, String currencyCode, String productName, String sku) {

        HitBuilders.TransactionBuilder transactionBuilder =
                new HitBuilders.TransactionBuilder()
                        .setTransactionId(transactionId)
                        .setAffiliation(AFFILIATION)
                        .setRevenue(price)
                        .setCurrencyCode(currencyCode);

        setCustomDimensionsForTransaction(transactionBuilder);

        Map<String, String> transactionParams = transactionBuilder.build();


        HitBuilders.ItemBuilder itemBuilder =
                new HitBuilders.ItemBuilder()
                        .setTransactionId(transactionId)
                        .setName(productName).setSku(sku)
                        .setCategory(CATEGORY_EBOOK)
                        .setPrice(price)
                        .setQuantity(1l)
                        .setCurrencyCode(currencyCode);

        setCustomDimensionsForItem(itemBuilder);

        Map<String, String> itemParams = itemBuilder.build();

        mTracker.send(transactionParams);
        mTracker.send(itemParams);

        LogUtils.d(TAG, String.format("Tracking transaction id: %s, price: %f, currency code: %s, book title: %s, isbn: %s", transactionId, price, currencyCode, productName, sku));
    }

    /**
     * Start tracking the first view of a ui component.
     *
     * @param name the category name this event falls into
     * @param name the name of the event
     */
    public void startTrackingUIComponent(String name) {

        if (!timings.containsKey(name)) {
            long time = System.currentTimeMillis();
            timings.put(name, time);
        }

        HitBuilders.ScreenViewBuilder screenViewBuilder =
                new HitBuilders.ScreenViewBuilder();

        setCustomDimensionsForScreenView(screenViewBuilder);

        mTracker.setScreenName(name);
        mTracker.send(screenViewBuilder.build());
    }

    /**
     * Marks that a userinterface component is not being viewed anymore.
     *
     * @param name the category name this event falls into
     * @param name the name of the event
     */
    public void stopTrackingUIComponent(String name) {

        if (timings.containsKey(name)) {
            long time = System.currentTimeMillis() - timings.get(name);

            HitBuilders.TimingBuilder timingBuilder =
                new HitBuilders.TimingBuilder()
                            .setCategory(CATEGORY_NAME_USERINTERFACE)
                            .setValue(time)
                            .setVariable(name);

            setCustomDimensionsForTiming(timingBuilder);

            Map<String, String> timingParams = timingBuilder.build();

            mTracker.send(timingParams);

            LogUtils.d(TAG, String.format("Tracking event name: %s, time: %d", name, time));

            timings.remove(name);
        }
    }

    ////////////////// Enhanced eCommerce /////////////////

    private Product createProductFromShopItem(ShopItem shopItem) {

        Product product = new Product();

        if (shopItem != null && shopItem.book != null) {
            product.setId(shopItem.book.isbn != null ? shopItem.book.isbn : "");
            product.setName(shopItem.book.title != null ? shopItem.book.title : "");
            product.setBrand(shopItem.book.publisher != null ? shopItem.book.publisher : "");
        }

        if (shopItem != null && shopItem.price != null) {
            // If the shop item has a valid discount price then we report that, else just report the standard price
            if (shopItem.price.discountPrice >= 0 && shopItem.price.discountPrice < shopItem.price.price) {
                product.setPrice(shopItem.price.discountPrice);
            } else {
                product.setPrice(shopItem.price.price);
            }
        }

        return product;
    }

    private Product createProductFromBookInfo(BBBBookInfo bookInfo) {
        Product product = new Product().
                setId(bookInfo.id).
                setName(bookInfo.title);
        return product;
    }

    /**
     * Send an enhanced eCommerce event when the user sees a group of shop item impressions.
     * @param screenName the screen name that the impressions apply to
     * @param shopItems the shop items to report impressions for
     */
    public void sendImpressions(String screenName, SparseArray<ShopItem> shopItems) {

        SparseArray<Product> productArray = new SparseArray<Product>();
        for (int i = 0; i < shopItems.size(); i++) {
            int key = shopItems.keyAt(i);
            productArray.put(key, createProductFromShopItem(shopItems.get(key)));
        }
        sendProductImpressions(screenName, productArray);
    }

    /**
     * Send an enhanced eCommerce event when the user sees a group of book info impressions.
     * @param screenName the screen name that the impressions apply to
     * @param bookInfos the book info list to report impressions for
     */
    public void sendBookInfoImpressions(String screenName, SparseArray<BBBBookInfo> bookInfos) {

        SparseArray<Product> productArray = new SparseArray<Product>();
        for (int i = 0; i < bookInfos.size(); i++) {
            int key = bookInfos.keyAt(i);
            productArray.put(key, createProductFromBookInfo(bookInfos.get(key)));
        }
        sendProductImpressions(screenName, productArray);
    }

    // Send a list of product impressions to GA
    private void sendProductImpressions(String screenName, SparseArray<Product> products) {
        // We break the impression reports into batches as we could potentially
        // have hundreds of impressions at a time and the GA calls seem to fail if we send to much data at once
        for(int i = 0; i < products.size(); i += EE_IMPRESSION_BATCH_SIZE) {

            HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder();

            for (int j = 0; j < EE_IMPRESSION_BATCH_SIZE && ((i + j) < products.size()); j++) {
                int key = products.keyAt(i + j);
                Product product = products.get(key);
                product.setPosition(key);
                builder.addImpression(product, screenName);
            }

            LogUtils.d(TAG, "Tracking impressions: [" + screenName + ": " + (i + 1) + " - "  + Math.min((i + EE_IMPRESSION_BATCH_SIZE), products.size()) + " / " + products.size() + "] " );

            mTracker.setScreenName(screenName);
            mTracker.send(builder.build());
        }
    }

    /**
     * Send an enhanced eCommerce event when the user clicks on a product within the shop context
     * @param screenName the name of the screen that they have clicked from
     * @param shopItem the shop item they have clicked on
     */
    public void sendClickOnProduct(String screenName, ShopItem shopItem) {
        Product product = createProductFromShopItem(shopItem);

        ProductAction productAction = new ProductAction(ProductAction.ACTION_CLICK);

        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(productAction);

        LogUtils.d(TAG, "Tracking click on product: [" + screenName + "] " + shopItem.book.title);

        mTracker.setScreenName(screenName);
        mTracker.send(builder.build());
    }

    /**
     * Send an enhanced eCommerce event when the user clicks on a product with a book info
     * @param screenName the name of the screen that they have clicked from
     * @param bookInfo the book item they have clicked on
     */
    public void sendClickOnProduct(String screenName, BBBBookInfo bookInfo) {
        Product product = createProductFromBookInfo(bookInfo);

        ProductAction productAction = new ProductAction(ProductAction.ACTION_CLICK);

        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(productAction);

        LogUtils.d(TAG, "Tracking click on product: [" + screenName + "] " + bookInfo.title + " " + bookInfo.id);

        mTracker.setScreenName(screenName);
        mTracker.send(builder.build());
    }

    /**
     * Send an enhanced eCommerce event when the user views a product page for an individual product
     * @param screenName the screen name that the product was viewed
     * @param shopItem the shop item that was viewed
     */
    public void sendViewProductPage(String screenName, ShopItem shopItem) {

        Product product = createProductFromShopItem(shopItem);
        ProductAction productAction = new ProductAction(ProductAction.ACTION_DETAIL);

        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(productAction);

        LogUtils.d(TAG, "Tracking view product page: [" + screenName + "] " + shopItem.book.title);

        mTracker.setScreenName(screenName);
        mTracker.send(builder.build());
    }

    /**
     * Send an enhanced eCommerce event when a shop item was added to cart. Since we don't really have
     * the concept of a shopping basket this step reports two GA messages. Firstly adding the item to
     * the cart and secondly invoking the checkout process.
     * @param screenName the screen name
     * @param shopItem the shop item that was added to cart
     */
    public void sendAddToCart(String screenName, ShopItem shopItem) {
        Product product = createProductFromShopItem(shopItem);

        // First send the add to basket action.
        HitBuilders.ScreenViewBuilder addToBasketBuilder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(new ProductAction(ProductAction.ACTION_ADD));

        LogUtils.d(TAG, "Tracking add to cart: [" + screenName + "] " + shopItem.book.title);

        mTracker.setScreenName(screenName);
        mTracker.send(addToBasketBuilder.build());

        // Now send the check out action
        HitBuilders.ScreenViewBuilder checkoutBuilder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(new ProductAction(ProductAction.ACTION_CHECKOUT));

        LogUtils.d(TAG, "Tracking checkout: [" + screenName + "] " + shopItem.book.title);

        mTracker.setScreenName(screenName);
        mTracker.send(checkoutBuilder.build());
    }

    /**
     * Send an enhanced eCommerce event when a shop item is purchased
     * @param screenName the screen name
     * @param shopItem the shop item that was bought
     */
    public void sendPurchase(String screenName, ShopItem shopItem) {
        Product product = createProductFromShopItem(shopItem);
        ProductAction productAction = new ProductAction(ProductAction.ACTION_PURCHASE);

        HitBuilders.ScreenViewBuilder builder = new HitBuilders.ScreenViewBuilder()
                .addProduct(product)
                .setProductAction(productAction);

        LogUtils.d(TAG, "Tracking purchase: [" + screenName + "] " + shopItem.book.title);

        mTracker.setScreenName(screenName);
        mTracker.send(builder.build());
    }

    /**
     * Method to inspect an intent from a deep link and set appropriate Adx parameters if the deep link came
     * from an appropriate marketing source.
     * @param context the context where the intent came from
     * @param intent an Intent object which may contain AD-X information
     */
    public static void handleAdXDeepLink(Context context, Intent intent) {
        if (intent != null) {
            Uri data = intent.getData();
            if ((data != null)&&(data.isHierarchical())) {
                String adxId = data.getQueryParameter("ADXID");
                if (adxId != null && adxId.length() > 0) {
                    AdXConnect.getAdXConnectEventInstance(context, "DeepLinkLaunch", adxId, "");
                }
            }
        }
    }
}