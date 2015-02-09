// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.sync;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.blinkboxbooks.android.util.LogUtils;

/**
 * Implementation of an AbstractThreadedSyncAdapter for syncing local data with the server. This class retrieves data from the REST API and saves it to the content provider.
 * The GUI component should register a ContentObserver to receive notifications of changes to the underlying data it is displaying.
 */
public class BBBSyncAdapter extends AbstractThreadedSyncAdapter {

    private final Synchroniser mSynchroniser;

    /**
     * Creates an AbstractThreadedSyncAdapter
     *
     * @param context        the Context that this is running within.
     * @param autoInitialize if true then sync requests that have SYNC_EXTRAS_INITIALIZE set will be internally handled by AbstractThreadedSyncAdapter by calling setIsSyncable(android.accounts.Account, String, int) with 1 if it is currently set to <0.
     */
    public BBBSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);

        mSynchroniser = new Synchroniser();
    }

    /**
     * Creates an AbstractThreadedSyncAdapter
     * This form of the constructor maintains compatibility with Android 3.0
     * and later platform versions
     *
     * @param context            the Context that this is running within.
     * @param autoInitialize     if true then sync requests that have SYNC_EXTRAS_INITIALIZE set will be internally handled by AbstractThreadedSyncAdapter by calling setIsSyncable(android.accounts.Account, String, int) with 1 if it is currently set to <0.
     * @param allowParallelSyncs if true then allow syncs for different accounts to run at the same time, each in their own thread. This must be consistent with the setting in the SyncAdapter's configuration file.
     */
    public BBBSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);

        mSynchroniser = new Synchroniser();
    }

    /**
     * Perform a sync for this account. SyncAdapter-specific parameters may be specified in extras, which is guaranteed to not be null. Invocations of this method are guaranteed to be serialized.
     *
     * @param account    the account that should be synced
     * @param extras     SyncAdapter-specific parameters
     * @param authority  the authority of this sync request
     * @param provider   a ContentProviderClient that points to the ContentProvider for this authority
     * @param syncResult SyncAdapter-specific parameters
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(getContext());

        broadcastManager.sendBroadcastSync(new Intent(Synchroniser.ACTION_SYNC_STARTED));
        LogUtils.d(Synchroniser.TAG, String.format("Synchronisation started"));

        mSynchroniser.performSync(getContext(), account, extras, authority, syncResult);

        broadcastManager.sendBroadcastSync(new Intent(Synchroniser.ACTION_SYNC_STOPPED));
        LogUtils.d(Synchroniser.TAG, String.format("Synchronisation stopped"));
    }
}