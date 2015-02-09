// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.widget.Toast;

import com.blinkboxbooks.android.R;

/**
 * Simple helper class for opening the email client
 */
public class EmailUtil {

    public static void openEmailClient(Context context, String email, String subject, String text, String chooserTitle) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType(context.getString(R.string.email_messagerf));

        i.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, text);

        try {
            context.startActivity(Intent.createChooser(i, chooserTitle));
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(context, context.getString(R.string.no_email_clients), Toast.LENGTH_SHORT).show();
        }
    }

    public static String getFooter(Context context) {
        StringBuilder builder = new StringBuilder();

        builder.append("Version " + getVersionString(context) + "\n");
        builder.append("Device " + DeviceUtils.getClientName(context) + "\n");

        return builder.toString();
    }

    public static String getVersionString(Context context) {

        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
        }

        return "";
    }
}