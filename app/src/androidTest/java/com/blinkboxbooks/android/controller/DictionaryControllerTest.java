// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;

import com.blinkboxbooks.android.BBBApplication;

import java.io.File;

public class DictionaryControllerTest extends AndroidTestCase {

    private Context context;
    private DictionaryController mDController;
    private static String mDestination;

    @Override
    protected void setUp() throws Exception {
        // Calls the base class implementation of this method.
        super.setUp();

        // Gets the context for this test.
        context = getContext();

        mDestination = mContext.getFilesDir() + File.separator + "dictionary.db";
    }

    public void testDictionaryUnzip() {
        mDController = DictionaryController.getInstance();

        File destFile = new File(mDestination);
        String destination = BBBApplication.getApplication().getFilesDir() + File.separator + "dictionary.db";
        mDController.unZipDictionary(destination);

        assert(destFile.isFile());

        SQLiteDatabase mDatabase = SQLiteDatabase.openDatabase(
                mDestination, null, SQLiteDatabase.OPEN_READWRITE);
        assertTrue(mDatabase.isOpen());
        assertTrue(mDatabase.getPageSize() > 0);
    }


}
