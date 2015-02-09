// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.content.res.Resources;
import android.database.Cursor;
import android.os.Environment;
import android.os.StatFs;
import android.util.DisplayMetrics;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.provider.DictionaryContentProvider;
import com.blinkboxbooks.android.provider.DictionaryContract;
import com.blinkboxbooks.android.util.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;

/**
 * Controller for dictionary
 */
public class DictionaryController {
    private static final String TAG = "DictionaryController";

    private static DictionaryController sInstance = null;

    private static final long UNZIPPED_DICTIONARY_SIZE = 3740071;

    private static final String ZIPFILE_LOCATION = "dictionary/dictionary.db.zip";
    private static final String OUTPUT_FILE_NAME = "dictionary.db";

    private static final int BASE_FONT_SIZE = 18;
    private static final int DICTIONARY_LINE_SPACING = 4;


    /** Exception that is thrown when a definition cannot be found for a search term */
    public class DefinitionNotFoundException extends Exception {}

    /**
     * Static singleton getInstance
     *
     * @return the {@link BookDownloadController} singleton object
     */
    public static DictionaryController getInstance() {

        if (sInstance == null) {
            sInstance = new DictionaryController();
        }

        return sInstance;
    }
    /**
     * Private constructor
     *
     */
    private DictionaryController() {

    }

    /**
     * Extracts dictionary.db.zip file found in assets to given path.
     * Returns true if successful
     * @return boolean
     */
    public boolean unZipDictionary(String destinationPath) {
        LogUtils.d(TAG, "unzipping to "+destinationPath);

        ZipInputStream inStream = null;
        OutputStream outStream = null;

        try {
            //Create input and output streams
            inStream = new ZipInputStream(BBBApplication.getApplication().getAssets().open(ZIPFILE_LOCATION));
            outStream = new FileOutputStream(destinationPath);

            byte[] buffer = new byte[1024];
            int nrBytesRead;

            //Get next zip entry and start reading data
            if ((inStream.getNextEntry()) != null) {

                while ((nrBytesRead = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, nrBytesRead);
                }
            }

        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
            return false;

        } finally {
            try {
                outStream.close();
            } catch (Exception e) {}

            try {
                inStream.close();
            } catch (Exception e) {}
        }

        return true;
    }

    /**
     * Returns true if device has sufficient storage space for the unzipped dictionary
     * @return boolean indicating if device has enough space for the dictionary
     */
    public boolean hasSufficientSpaceForUnzippedDictionary() {
        StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesAvailable = (long)stat.getBlockSize() * (long)stat.getAvailableBlocks();

        if(bytesAvailable > UNZIPPED_DICTIONARY_SIZE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns an HTML String representing the full definition of a dictionary query for the given word
     * @param query the query string to search for
     * @return an HTML page containing formatted dictionary info
     * @throws DefinitionNotFoundException if there are no definitions found for the supplied query string
     */
    public String getFullDefinitionHtml(String query) throws DefinitionNotFoundException {
        Cursor cursorWords = BBBApplication.getApplication().getContentResolver().query(
                DictionaryContract.Words.queryForWord(query), null, null, null, null);

        Cursor cursorDefinitions = BBBApplication.getApplication().getContentResolver().query(
                DictionaryContract.Words.queryForDefinitions(query), null, null, null, null);

        Cursor cursorDerivs = BBBApplication.getApplication().getContentResolver().query(
                DictionaryContract.Words.queryForDerivatives(query), null, null, null, null);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<html><body>");
        stringBuilder.append(getStyle());

        if (cursorWords != null && cursorWords.moveToFirst()) {
            stringBuilder.append("<h3>" + cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.WORD)));

            if (cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.PRONUNCIATION)) != null) {
                stringBuilder.append(" |<i>" + cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.PRONUNCIATION)) + "</i> |");
            }

            stringBuilder.append("</h3>");

            if(cursorDefinitions != null) {
                if (cursorDefinitions.moveToFirst()) {
                    int def_entries = cursorDefinitions.getCount();
                    for (int i = 0; i < def_entries; i++) {
                        stringBuilder.append("<p>");
                        String wordType = cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_WORD_TYPE));
                        if(wordType != null && !wordType.contentEquals(" ")) {
                            stringBuilder.append("<i>" + cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_WORD_TYPE)) + "</i><br>");
                        }
                        if (cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION)) != null) {
                            stringBuilder.append(cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION)));
                            if (cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_EXAMPLE)) != null) {
                                stringBuilder.append(":<i> " + cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_EXAMPLE)) + "</i>.");
                            }
                            stringBuilder.append("</p>");
                        }
                        cursorDefinitions.moveToNext();
                    }
                }
                cursorDefinitions.close();
            }

            if (cursorDerivs != null) {
                if (cursorDerivs.moveToFirst()) {
                    int der_entries = cursorDerivs.getCount();
                    if (der_entries > 0) {

                        if (cursorDerivs.getString(0) != null) {
                            stringBuilder.append("<p><strong>DERIVATIVES</strong><br>");

                            for (int i = 0; i < der_entries; i++) {
                                stringBuilder.append(cursorDerivs.getString(cursorDerivs.getColumnIndex(DictionaryContract.Words.DER_DERIVATIVE)) + " <i>");
                                if (cursorDerivs.getString(cursorDerivs.getColumnIndex(DictionaryContract.Words.DER_TYPE)) != null) {
                                    stringBuilder.append(cursorDerivs.getString(cursorDerivs.getColumnIndex(DictionaryContract.Words.DER_TYPE)));
                                }
                                stringBuilder.append("</i><br>");
                                cursorDerivs.moveToNext();
                            }
                            stringBuilder.append("</p>");
                        }
                    }
                }
                cursorDerivs.close();
            }
            cursorWords.close();
        } else {
            if (cursorWords != null) {
                cursorWords.close();
            }
            closeDictionaryDatabase();
            throw new DefinitionNotFoundException();
        }

        stringBuilder.append("</body></html>");
        closeDictionaryDatabase();

        return stringBuilder.toString();
    }

    /**
     * Returns an HTML String representing the simplified, primary definition of a dictionary query for the given word
     * @param query the query string to search for
     * @return an HTML page containing formatted dictionary info
     * @throws DefinitionNotFoundException if there are no definitions found for the supplied query string
     */
    public String getPrimaryDefinitionHtml(String query) throws DefinitionNotFoundException {
        Cursor cursorWords = BBBApplication.getApplication().getContentResolver().query(
                DictionaryContract.Words.queryForWord(query), null, null, null, null);

        Cursor cursorDefinitions =  BBBApplication.getApplication().getContentResolver().query(
                DictionaryContract.Words.queryForDefinitions(query), null, null, null, null);

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<html><body>");
        stringBuilder.append(getStyle());

        if (cursorWords != null && cursorWords.moveToFirst()) {
            stringBuilder.append("<h3>" + cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.WORD)));

            if (cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.PRONUNCIATION)) != null) {
                stringBuilder.append(" |<i>" + cursorWords.getString(cursorWords.getColumnIndex(DictionaryContract.Words.PRONUNCIATION)) + "</i> |");
            }

            stringBuilder.append("</h3><p>");

            if(cursorDefinitions != null && cursorDefinitions.moveToFirst()){
                String wordType = cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_WORD_TYPE));
                if(wordType != null && !wordType.contentEquals(" ")) {
                    stringBuilder.append("<i>" + wordType + "</i><br>");
                }
                if(cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION)) != null) {
                    stringBuilder.append(cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_DEFINITION)));
                    if(cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_EXAMPLE)) != null) {
                        stringBuilder.append(": <i>" + cursorDefinitions.getString(cursorDefinitions.getColumnIndex(DictionaryContract.Words.DEF_EXAMPLE)) + ".</i>");
                    }
                }
                cursorDefinitions.close();
            }

            stringBuilder.append("</p></body></html>");

            cursorWords.close();

        } else {
            if (cursorWords != null) {
                cursorWords.close();
            }
            closeDictionaryDatabase();
            throw new DefinitionNotFoundException();
        }

        closeDictionaryDatabase();
        return stringBuilder.toString();
    }

    /**
     * Get the text to display if a definition cannot be found for a word.
     * @param resources the Resources object used to look up a string resource
     * @return an string containing the HTML to be displayed when no definitions are found for the search term
     */
    public static String getNoDefinitionFoundText(Resources resources) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<html><body>");
        stringBuilder.append(getStyle());
        stringBuilder.append(resources.getString(R.string.error_no_definition_found));
        stringBuilder.append("</body></html>");
        return stringBuilder.toString();
    }

    /**
     * Returns the full file path location of the dictionary database
     * @return the file path of the database
     */
    public String getDatabaseFileFullPathLocation() {
        return BBBApplication.getApplication().getFilesDir() + File.separator + OUTPUT_FILE_NAME;
    }

    /**
     * Returns true if the dictionary database file exists in its decompressed form
     * @return boolean indicating if the file exists
     */
    public boolean dictionaryFileUnzipped() {
        File file = new File(getDatabaseFileFullPathLocation());
        return file.exists();
    }

    /**
     * Clears the dictionary file if it exists on the device. This can be called when the dictionary does
     * not seem to open in case the database file is corrupt in some way.
     */
    public void clearDictionaryFileIfExists() {
        File file = new File(getDatabaseFileFullPathLocation());
        if (file.exists()) {
            file.delete();
        }
    }

    /**
     * Closes the dictionary database, must be called once dictionary cursors are no longer being used
     */
    private void closeDictionaryDatabase() {
        DictionaryContentProvider.getDatabase().close();
    }

    private static String getStyle() {

        // The font size value will scale based on the current system font size settings
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float textScale = metrics.scaledDensity / metrics.density;
        int fontSize = (int) (BASE_FONT_SIZE * textScale);

        return  "<style>@font-face{\n" +
                "    font-family:avalonbook;\n" +
                "    src:url(fonts/Avalon-Book.otf) format('truetype');\n" +
                "    font-weight:400;\n" +
                "    font-style:normal\n" +
                "    }\n" +
                "\n" +
                "body{\n" +
                "    color:#000;\n" +
                "    font-family:avalonbook,Arial,\"Times New Roman\";\n" +
                "    font-size:" + fontSize + "px;\n" +
                "    line-height:" + (fontSize + DICTIONARY_LINE_SPACING) + "px;\n" +
                "    padding-top:0.3cm;\n" +
                "    padding-bottom:0.3cm;\n" +
                "    padding-right:0.2cm;\n" +
                "    }</style>";
    }
}