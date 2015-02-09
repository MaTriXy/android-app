// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * SyncAdapter service for performing syncs in the background
 */
public class BBBSyncService extends Service {
    // Storage for an instance of the sync adapter
    private static BBBSyncAdapter sSyncAdapter = null;
    // Object to use as a thread-safe lock
    private static final Object sSyncAdapterLock = new Object();

    @Override
    public void onCreate() {
        super.onCreate();
        // Create the sync adapter as a singleton. Set the sync adapter as syncable Disallow parallel syncs
        synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new BBBSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    /**
     * Return an object that allows the system to invoke the sync adapter.
     */
    @Override
    public IBinder onBind(Intent intent) {

    	/*
         * Get the object that allows external processes to call onPerformSync(). The object is created in the base class code when the SyncAdapter
         * constructors call super()
         */
        return sSyncAdapter.getSyncAdapterBinder();
    }
}