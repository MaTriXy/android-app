// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.OnAccountsUpdateListener;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.AdX.tag.AdXConnect;
import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.net.BBBRequestManagerInterface;
import com.blinkboxbooks.android.model.Library;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.model.helper.ReaderSettingHelper;
import com.blinkboxbooks.android.net.ApiConnector;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.account.ForgottenPasswordFragment;
import com.blinkboxbooks.android.ui.account.LoginActivity;
import com.blinkboxbooks.android.ui.account.LoginFragment;
import com.blinkboxbooks.android.ui.account.RegisterFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.NotificationUtil;
import com.blinkboxbooks.android.util.PreferenceManager;
import com.crashlytics.android.Crashlytics;

/**
 * The controller class for the logging in, logging out, etc
 * <p/>
 * Login process:
 * 1. Check if the db library already exists
 * 2. If the library does not exist ask to transfer the settings
 * 3. Library has been created for id (libraryid)
 * 4. Check if the account already exists
 * 5. Technically new library === new account, so 1 should be the same as 4, check anyway (device may have crashed??).
 * 6. Add account with library id (libraryid) and access token
 * 7. Set the logged in userid shared preference
 */
public class AccountController implements BBBRequestManagerInterface, OnAccountsUpdateListener {

    private static AccountController instance;

    public static synchronized AccountController getInstance() {

        if (instance == null) {
            instance = new AccountController();
        }

        return instance;
    }

    public static final String TAG = AccountController.class.getSimpleName();

    private static final String TAG_LOGIN_FRAGMENT = "tag_login_fragment";
    private static final String TAG_REGISTER_FRAGMENT = "tag_register_fragment";
    private static final String TAG_FORGOT_PASSWORD_FRAGMENT = "tag_forgot_password_fragment";

    public static final String ACTION_LOGGED_IN = "logged_in";
    public static final String PARAM_LIBRARY_ID = "library_id";

    private final AccountManager mAccountManager;

    private String mAccessToken;

    private boolean mSyncing;

    private boolean mNewUser;

    private AccountController() {
        mAccountManager = AccountManager.get(BBBApplication.getApplication());
        mAccountManager.addOnAccountsUpdatedListener(this, null, true);

        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(BBBApplication.getApplication());
        manager.registerReceiver(syncStartedBroadCastReceiver, new IntentFilter(Synchroniser.ACTION_SYNC_STARTED));
        manager.registerReceiver(syncStoppedBroadCastReceiver, new IntentFilter(Synchroniser.ACTION_SYNC_STOPPED));

        String userId = getUserId();

        // If we have a valid signed in user then set it in the analytics helper
        if (userId != null && !userId.equals(BusinessRules.DEFAULT_ACCOUNT_NAME)) {
            AnalyticsHelper.getInstance().setUserId(userId);
        }
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        if(isLoggedIn()) {


            Account account = getLoggedInAccount();

            if(account == null) {
                performLogout();
            }
        }
    }

    /**
     * Check whether the user has registered & logged in for the first time
     *
     * @return true if the user has registered & logged in for the first time
     */
    public boolean isNewUser() {
        return mNewUser;
    }

    /**
     * Sets whether the user is newly registerd
     *
     * @param newUser
     */
    public void setNewUser(boolean newUser) {
        mNewUser = newUser;
    }

    /**
     * Shows the login fragment
     *
     * @param activity
     * @param username pre-populates the username field if supplied
     */
    public void showLoginScreen(final BaseActivity activity, final String username) {
        LoginFragment fragment = LoginFragment.newInstance(username);
        activity.showDialog(fragment, TAG_LOGIN_FRAGMENT, true);
    }

    /**
     * Shows the register fragment
     *
     * @param activity
     */
    public void showRegisterScreen(final BaseActivity activity) {
        RegisterFragment fragment = RegisterFragment.newInstance(false, AnalyticsHelper.GA_SCREEN_Shop_RegistrationScreen, true);
        activity.showDialog(fragment, TAG_REGISTER_FRAGMENT, true);
    }

    /**
     * Shows the forgotten password screen
     *
     * @param activity
     */
    public void showForgottenPasswordScreen(final BaseActivity activity, final String email) {
        ForgottenPasswordFragment fragment = ForgottenPasswordFragment.newInstance(email);
        activity.showDialog(fragment, TAG_FORGOT_PASSWORD_FRAGMENT, true);
    }

    /**
     * Stores the client secret and id to the currently logged in users account
     *
     * @param clientId     the clientId
     * @param clientSecret the clientSecret
     */
    public void setClient(String clientId, String clientSecret) {
        Account account = getLoggedInAccount();

        if (account != null) {
            mAccountManager.setUserData(account, BBBApiConstants.PARAM_CLIENT_ID, clientId);
            mAccountManager.setUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET, clientSecret);
        }
    }

    /**
     * Get the client id for the currently logged in user
     */
    @Override
    public String getClientId() {
        Account account = getLoggedInAccount();

        if (account == null) {
            return null;
        } else {
            return mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_ID);
        }
    }

    /**
     * Get the client secret for the currently logged in user
     */
    @Override
    public String getClientSecret() {
        Account account = getLoggedInAccount();

        if (account == null) {
            return null;
        } else {
            return mAccountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET);
        }
    }

    @Override
    public void elevationRequired() {
        LogUtils.d(TAG, "elevation required");
    }

    @Override
    public void clientInvalidated() {
        LogUtils.d(TAG, "client invalidated");

        setClient(null, null);

        performLogout();

        // Launch the login activity, with a 'logged out' dialog on top
        Application application = BBBApplication.getApplication();
        Intent intent = new Intent(application.getApplicationContext(), LoginActivity.class);
        intent.putExtra(LoginActivity.ACTION_LOGGED_OUT, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        application.startActivity(intent);
    }

    @Override
    public String getRefreshToken() {
        Account account = getLoggedInAccount();

        if (account == null) {
            return null;
        } else {
            return mAccountManager.getUserData(account, BBBApiConstants.PARAM_REFRESH_TOKEN);
        }
    }

    /**
     * Sets a new auth token for the currently logged in user
     */
    @Override
    public void setAccessToken(String accessToken) {
        Account account = getLoggedInAccount();

        if (account != null) {
            setAccessToken(account, accessToken);
        } else {
            mAccessToken = accessToken;
        }
    }

    @Override
    public String getAccessToken() {
        Account account = getLoggedInAccount();

        if (account == null) {
            return mAccessToken;
        } else {
            return mAccountManager.getUserData(account, BBBApiConstants.PARAM_ACCESS_TOKEN);
        }
    }

    /**
     * Saves the access token for the account
     *
     * @param account     the Account you are saving the access token for
     * @param accessToken the access token
     */
    public void setAccessToken(Account account, String accessToken) {

        if (!TextUtils.isEmpty(accessToken)) {
            mAccountManager.setAuthToken(account, BBBApiConstants.AUTHTOKEN_TYPE, accessToken);
            mAccountManager.setUserData(account, BBBApiConstants.PARAM_ACCESS_TOKEN, accessToken);
        }
    }

    /**
     * Saves the refresh token for the account
     *
     * @param account      the Account you are saving the refresh token for
     * @param refreshToken the refresh token
     */
    public void setRefreshToken(Account account, String refreshToken) {
        mAccountManager.setUserData(account, BBBApiConstants.PARAM_REFRESH_TOKEN, refreshToken);
    }

    /**
     * Get the current logged in account
     *
     * @return account, The currently logged in account or null
     */
    public Account getLoggedInAccount() {
        String userId = PreferenceManager.getInstance().getString(PreferenceManager.PREF_KEY_CURRENT_USER, null);

        if (userId == null) {
            return null;
        }

        Account account = getAccountForUserId(userId);

        if (account != null) {
            return account;
        }

        return null;
    }

    /**
     * Gets a stored item of data for the currently logged in account
     *
     * @return the data or null if the user is not logged in
     */
    public String getDataForLoggedInUser(String name) {
        Account account = getLoggedInAccount();

        if (account != null) {
            return mAccountManager.getUserData(account, name);
        }

        return null;
    }

    /**
     * Logout the currently logged in user
     */
    public void performLogout() {
        LogUtils.d(TAG, "performing logout");

        Account account = getLoggedInAccount();

        if (account != null) {
            String refreshToken = mAccountManager.getUserData(account, BBBApiConstants.PARAM_REFRESH_TOKEN);

            if(!TextUtils.isEmpty(refreshToken)) {
                ApiConnector.getInstance().revokeRefreshToken(refreshToken);
            }

            mAccountManager.setAuthToken(account, BBBApiConstants.AUTHTOKEN_TYPE, null);
            mAccountManager.setUserData(account, BBBApiConstants.PARAM_ACCESS_TOKEN, null);
            mAccountManager.setUserData(account, BBBApiConstants.PARAM_REFRESH_TOKEN, null);
        }

        AnalyticsHelper.getInstance().setUserId(null);

        // Hide all notifications
        NotificationUtil.hideNotifications(BBBApplication.getApplication());

        mAccessToken = null;

        PreferenceManager preferenceManager = PreferenceManager.getInstance();
        preferenceManager.setPreference(PreferenceManager.PREF_KEY_CURRENT_USER, (String) null);
        preferenceManager.setPreference(PreferenceManager.PREF_KEY_PREFERRED_NAME, (String) null);
        preferenceManager.setPreference(PreferenceManager.PREF_KEY_REMOVE_SAMPLE_WARNING_COUNT, 0);
        preferenceManager.setPreference(PreferenceManager.PREF_KEY_REMOVE_FROM_DEVICE_WARNING_COUNT, 0);

        Crashlytics.setUserIdentifier(null);
    }

    /**
     * Request synchronisation for the current user
     *
     * @param syncOptions If no syncOptions are specified, everything will be synced otherwise only the items you have specified here will be synced. Options are
     *                    Synchroniser.SYNC_ACCOUNT, Synchroniser.SYNC_LIBRARY, Synchroniser.SYNC_BOOKMARKS_PUSH, Synchroniser.SYNC_BOOKMARKS_PULL
     */
    public void requestSynchronisation(String... syncOptions) {
        Account account = getLoggedInAccount();

        if (account != null) {
            Bundle bundle = new Bundle();

            if (syncOptions != null && syncOptions.length > 0) {
                bundle.putBoolean(Synchroniser.SYNC_SELECTIVE, true);

                for (int i = 0; i < syncOptions.length; i++) {
                    bundle.putBoolean(syncOptions[i], true);
                }
            }

            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            ContentResolver.requestSync(account, BBBContract.CONTENT_AUTHORITY, bundle);
        }
    }

    /**
     * Performs a library sync and downloads the book with the given isbn when the sync is completed
     *
     * @param isbn the ISBN of the book you want to download after sync is complete
     */
    public void requestSynchronisationAndDownloadBook(String isbn) {
        Account account = getLoggedInAccount();

        if (account != null) {
            Bundle bundle = new Bundle();

            bundle.putBoolean(Synchroniser.SYNC_SELECTIVE, true);
            bundle.putBoolean(Synchroniser.SYNC_LIBRARY, true);
            bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
            bundle.putString(BBBApiConstants.PARAM_BOOK, isbn);

            ContentResolver.requestSync(account, BBBContract.CONTENT_AUTHORITY, bundle);
        }
    }

    /**
     * Set the current logged in user
     *
     * @param userName The users username (email address)
     * @param userId   The users unique id
     */
    public void setLoggedIn(String userName, String userId) {
        LogUtils.d(TAG, "Setting user logged in: " + userName + " " + userId);

        PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_CURRENT_USER, userId);
        PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_PREFERRED_NAME, AccountController.createPreferredName(userName));
    }

    /**
     * @return true, if the user is currently logged in
     */
    public boolean isLoggedIn() {
        return PreferenceManager.getInstance().getString(PreferenceManager.PREF_KEY_CURRENT_USER, null) != null;
    }

    /**
     * Return the currently logged in users unique user id
     *
     * @return userId
     */
    public String getUserId() {
        return PreferenceManager.getInstance().getString(PreferenceManager.PREF_KEY_CURRENT_USER, BusinessRules.DEFAULT_ACCOUNT_NAME);
    }

    /**
     * Get account belonging to a specific userId
     *
     * @param userId
     * @return account The account or null
     */
    public Account getAccountForUserId(String userId) {
        try {
            Account[] accounts = mAccountManager.getAccountsByType(BBBApiConstants.ACCOUNT_TYPE);

            for (int i = 0; i < accounts.length; i++) {

                String userData = mAccountManager.getUserData(accounts[i], BBBApiConstants.PARAM_USER_ID);
                if (userData != null && userData.equals(userId)) {
                    return accounts[i];
                }
            }
        } catch (Exception e) {
            // We see very rare DeadObjectExceptions thrown here, we can't capture only DeadObjectExceptions directly
            // as the compiler believes it cannot be thrown. We tell crashylitics about this handled exception so we can get
            // some handle on how frequently this happens
            Crashlytics.logException(new Exception("Failed getAccountForUserId: " + userId + " " + e.toString()));
        }
        return null;
    }

    /**
     * Gets the usernames of all the blinkbox accounts stored on the device
     *
     * @return the array of usernames
     */
    public String[] getAccountUsernames() {
        Account[] accounts = mAccountManager.getAccountsByType(BBBApiConstants.ACCOUNT_TYPE);

        String[] names = new String[accounts.length];

        for (int i = 0; i < accounts.length; i++) {
            names[i] = mAccountManager.getUserData(accounts[i], BBBApiConstants.PARAM_USERNAME);
        }

        return names;
    }

    public interface LoginHandler {
        public void onLoginComplete(String userId, Intent accountData);
    }

    /**
     * Log in the user on this device
     *
     * @param userId
     * @param accountData
     */
    public void performLogin(final String userId, final Intent accountData, boolean isRegistering) {
        LogUtils.d(TAG, "perform login " + userId + " registering: " + isRegistering);

        if (isRegistering) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_REGISTRATION, AnalyticsHelper.GA_EVENT_REGISTRATION_STATUS, AnalyticsHelper.GA_LABEL_REGISTRATION_SUCCESS, null);
            AdXConnect.getAdXConnectEventInstance(BBBApplication.getApplication(), AnalyticsHelper.ADX_EVENT_REGISTER, "", "", userId);
        }

        mNewUser = isRegistering;

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Library library = LibraryHelper.getLibraryForUserId(userId);

                if (library == null) {
                    boolean checkForChanges = BookmarkHelper.hasBookmarks(BusinessRules.DEFAULT_ACCOUNT_NAME)
                            || BookHelper.hasChangedBookStatus(BusinessRules.DEFAULT_ACCOUNT_NAME);

                    createLibrary(userId, checkForChanges, accountData);
                }

                finishLogin(userId, accountData);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                LocalBroadcastManager manager = LocalBroadcastManager.getInstance(BBBApplication.getApplication());
                accountData.setAction(ACTION_LOGGED_IN);
                manager.sendBroadcast(accountData);
            }
        }.execute();

    }

    /**
     * Create the library for the given userId
     *
     * @param userId
     * @param transfer    if true, Transfer the anonymous library
     * @param accountData
     */
    public void createLibrary(String userId, boolean transfer, Intent accountData) {
        if (transfer) {
            LibraryHelper.copyLibrary(BusinessRules.DEFAULT_ACCOUNT_NAME, userId, BBBApplication.getApplication().getContentResolver());

            // Prepare the reader settings
            ReaderSettingHelper.transferReaderSetting(BusinessRules.DEFAULT_ACCOUNT_NAME, userId);
        } else {
            LibraryHelper.copyLibrary(LibraryHelper.TEMPLATE_ACCOUNT, userId, BBBApplication.getApplication().getContentResolver());
        }
    }

    /**
     * Finalize the login on the device
     *
     * @param userId
     * @param intent
     */
    private void finishLogin(String userId, Intent intent) {
        LogUtils.d(TAG, "finish login " + userId);

        Bundle accountData = intent.getExtras();
        Library library = LibraryHelper.getLibraryForUserId(userId);
        accountData.putString(PARAM_LIBRARY_ID, String.valueOf(library.id));

        Account account = getAccountForUserId(userId);

        if (account == null) {
            account = new Account(accountData.getString(BBBApiConstants.PARAM_USERNAME), BBBApiConstants.ACCOUNT_TYPE);

            boolean success = mAccountManager.addAccountExplicitly(account, "", accountData);

            if (success) {
                ContentResolver.setSyncAutomatically(account, BBBContract.CONTENT_AUTHORITY, true);
            } else {
                LogUtils.e(TAG, "There was an error adding the account");
            }
        } else {

            for (String key : accountData.keySet()) {
                mAccountManager.setUserData(account, key, accountData.getString(key));
            }
        }

        AnalyticsHelper.getInstance().setUserId(userId);

        setAccessToken(account, accountData.getString(BBBApiConstants.PARAM_ACCESS_TOKEN));
        setRefreshToken(account, accountData.getString(BBBApiConstants.PARAM_REFRESH_TOKEN));
        setLoggedIn(accountData.getString(BBBApiConstants.PARAM_FIRST_NAME), userId);

        updateBugSenserUser();

        requestSynchronisation();
    }

    /**
     * Gets a blinkbox account identified by email
     *
     * @param email the email address of the Account you are looking for
     * @return the Account object
     */
    public Account getAccountForEmail(String email) {

        if (email == null) {
            return null;
        }

        Account[] accounts = mAccountManager.getAccountsByType(BBBApiConstants.ACCOUNT_TYPE);

        String account_email = null;

        for (int i = 0; i < accounts.length; i++) {
            account_email = mAccountManager.getUserData(accounts[i], BBBApiConstants.PARAM_USERNAME);

            if (email.equalsIgnoreCase(account_email)) {
                return accounts[i];
            }
        }

        return null;
    }

    public static String createPreferredName(String email) {
        int atSymbol = email.indexOf('@');

        if (atSymbol > 0) {
            return email.substring(0, atSymbol);
        }

        return email;
    }

    public void updateBugSenserUser() {

        String accountName = AccountController.getInstance().getDataForLoggedInUser(BBBApiConstants.PARAM_USERNAME);

        if(!TextUtils.isEmpty(accountName)) {
            Crashlytics.setUserIdentifier(accountName);
        }
    }

    /**
     * *Warning* This method will block the current thread. Attempts to get an auth token. If the token has expired, the refresh token is used to obtain a new one.
     *
     * @param account the Account for which you want to retrieve an access token for
     * @return the access token or null if we could not retrieve one.
     */
    public String getAccessToken(Account account) {
        AccountManagerFuture<Bundle> accountManagerFuture = mAccountManager.getAuthToken(account, BBBApiConstants.PARAM_AUTHTOKEN_TYPE, null, false, null, null);

        String accessToken = null;

        try {
            accessToken = accountManagerFuture.getResult().getString(AccountManager.KEY_AUTHTOKEN);
        } catch (Exception e) {
            LogUtils.d(TAG, e.getMessage(), e);
        }

        if (accessToken == null || accessToken.trim().length() == 0) {

            if (!BaseActivity.isAppInBackground()) {
                LogUtils.d(TAG, "Could not get access token. Showing login activity");
                BBBApplication.getApplication().showActivity(LoginActivity.class);
            }

        } else {
            return accessToken;
        }

        return null;
    }

    public void finishLogin(final String userId, String userName, String firstName, String accessToken, String refreshToken, String clientId, String clientSecret, boolean isRegistering) {
        LogUtils.d(TAG, String.format("Finish login: %s %s %s", userId, userName, String.valueOf(isRegistering)));

        final Intent intent = new Intent();

        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, userName);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, BBBApiConstants.ACCOUNT_TYPE);
        intent.putExtra(BBBApiConstants.PARAM_ACCESS_TOKEN, accessToken);
        intent.putExtra(BBBApiConstants.PARAM_REFRESH_TOKEN, refreshToken);
        intent.putExtra(BBBApiConstants.PARAM_USERNAME, userName);
        intent.putExtra(BBBApiConstants.PARAM_FIRST_NAME, firstName);
        intent.putExtra(BBBApiConstants.PARAM_USER_ID, userId);
        intent.putExtra(BBBApiConstants.PARAM_CLIENT_ID, clientId);
        intent.putExtra(BBBApiConstants.PARAM_CLIENT_SECRET, clientSecret);

        performLogin(userId, intent, isRegistering);
    }

    /**
     * Check if a sync is currently active
     *
     * @return true if sync is active else false
     */
    public boolean isSyncActive() {
        return mSyncing;
    }

    private final BroadcastReceiver syncStartedBroadCastReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            mSyncing = true;
        }
    };

    private final BroadcastReceiver syncStoppedBroadCastReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            mSyncing = false;
        }
    };
}