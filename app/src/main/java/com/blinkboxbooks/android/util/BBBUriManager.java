// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.widget.Toast;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.WebContentActivity;
import com.blinkboxbooks.android.ui.shop.ShopActivity;

/**
 * This is a helper class for fetch blinkbox domain urls
 */
public class BBBUriManager {
    private final static String TAG = BBBUriManager.class.getSimpleName();

    public final static String BBB_URI_MANAGER_HOST = "urimanager";

    private static BBBUriManager sInstance;
    private final Uri mBaseUri;

    /**
     * Enumerates the urls at the blinkbox domain
     */
    public enum BBBUri {
        DEVICES("#!/account/your-devices"), CONTACT("contact"),
        TERMSANDCONDITIONS("#!/terms-conditions"), FEEDBACK("feedback"), SHOP("shop"),
        ABOUT("about"), ON_YOUR_DEVICE("value_device"), IN_YOUR_CLOUD("value_cloud"),
        REFRESH_YOUR_LIBRARY("refresh"), FORCE_REFRESH_YOUR_LIBRARY("force_refresh"), SIGN_OUT("logout"), SIGN_IN("login"), FAQ("help");

        public String stringValue;

        private BBBUri(String toString) {
            stringValue = toString;
        }
    }

    /**
     * Static singleton getInstance
     *
     * @return the {@link BBBUriManager} singleton object
     */
    public static BBBUriManager getInstance() {

        if (sInstance == null) {
            init(BBBApplication.getApplication());
        }

        return sInstance;
    }

    /**
     * Initializes this class with the given context.
     *
     * @param context
     */
    public static void init(Context context) {
        sInstance = new BBBUriManager(context);
    }

    /**
     * Private constructor
     *
     * @param context
     */
    private BBBUriManager(Context context) {
        this.mBaseUri = Uri.parse(context.getString(R.string.base_web_url));
    }

    /**
     * Get a specific Uri on the blinkbox domain given a specific path
     *
     * @param path
     * @return
     */
    public Uri getUri(String path) {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return Uri.withAppendedPath(mBaseUri, path);
    }

    /**
     * Handle a BBBUri
     *
     * @param context
     * @param bbbUri
     */
    public void handleUri(Context context, BBBUri bbbUri) {
        Uri uri = getUri(bbbUri.stringValue);
        launchUri(context, uri);
    }

    /**
     * Handle a deep link Uri
     *
     * @param context
     * @param actionUri
     */
    public void handleUri(Context context, String actionUri) {
        Uri uri = Uri.parse(actionUri);
        LogUtils.i(TAG,"action URI:"+uri.toString());

        String lastPathSegment = uri.getLastPathSegment();
        if (BBBUriManager.BBB_URI_MANAGER_HOST.equals(uri.getHost())) {

            if (BBBUri.FEEDBACK.stringValue.equals(lastPathSegment)) {
                EmailUtil.openEmailClient(context, context.getString(R.string.email_feedback_email), context.getString(R.string.email_feedback_subject),
                        EmailUtil.getFooter(context), context.getString(R.string.feedback));

                return;
            } else if (BBBUri.CONTACT.stringValue.equals(lastPathSegment)) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_CONTACT_US, "", null);
                EmailUtil.openEmailClient(context, context.getString(R.string.email_contact_us_email), context.getString(R.string.email_contact_us_subject),
                        EmailUtil.getFooter(context), context.getString(R.string.contact_us));

                return;
            } else if (BBBUri.ABOUT.stringValue.equals(lastPathSegment)) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_INFO, "", null);
                Activity activity = (Activity) context;

                Intent intent = new Intent(activity, WebContentActivity.class);
                intent.putExtra(WebContentActivity.PARAM_FILE, "html/information.html");

                try {
                    PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

                    String[] args = {packageInfo.versionName, context.getString(R.string.cpr_version)};
                    intent.putExtra(WebContentActivity.PARAM_TITLE, context.getString(R.string.info));
                    intent.putExtra(WebContentActivity.PARAM_FORMAT_ARGS, args);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }

                activity.startActivity(intent);
                return;
            } else if (BBBUri.SHOP.stringValue.equals(lastPathSegment)) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_SHOP_MORE_BOOKS, "", null);
                Activity activity = (Activity) context;

                Intent intent = new Intent(activity, ShopActivity.class);
                activity.startActivity(intent);

                return;
            }

            Uri.Builder builder = uri.buildUpon();
            builder.scheme(mBaseUri.getScheme());
            builder.authority(mBaseUri.getAuthority());
            uri = builder.build();
        } else if (BBBUri.ON_YOUR_DEVICE.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_ON_YOUR_DEVICE, "", null);
        } else if (BBBUri.IN_YOUR_CLOUD.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_IN_YOUR_CLOUD, "", null);
        } else if (BBBUri.REFRESH_YOUR_LIBRARY.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_REFRESH_YOUR_LIBRARY, "", null);
        } else if (BBBUri.FORCE_REFRESH_YOUR_LIBRARY.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_FORCE_REFRESH_YOUR_LIBRARY, "", null);
        } else if (BBBUri.SIGN_IN.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_SIGN_IN, "", null);
        } else if (BBBUri.SIGN_OUT.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_SIGN_OUT, "", null);
        } else if (BBBUri.FAQ.stringValue.equals(lastPathSegment)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper
                    .CATEGORY_LIBRARY_MENU_CLICK, AnalyticsHelper.GA_EVENT_FAQ, "", null);
        }

        launchUri(context, uri);
    }

    /**
     * Launch a deep link Uri, prompt the user to leave the app
     *
     * @param context
     * @param uri
     */
    private static void launchUri(final Context context, Uri uri) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);

        ComponentName componentName = intent.resolveActivity(context.getPackageManager());

        if (componentName == null) {
            Toast.makeText(context, R.string.application_not_available, Toast.LENGTH_LONG).show();
        } else if (context.getPackageName().equals(componentName.getPackageName())) { // Launch our application activity
            context.startActivity(intent);
        } else { // Prompt the user

            if (NetworkUtils.hasInternetConnectivity(context)) {
                context.startActivity(intent);
            } else {
                BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(context);
                builder.setMessage(R.string.error_no_network_opening_link);
                builder.setPositiveButton(R.string.ok, new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
            }
        }
    }
}