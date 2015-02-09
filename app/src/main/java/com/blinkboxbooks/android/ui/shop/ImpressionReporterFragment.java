package com.blinkboxbooks.android.ui.shop;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;

import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.util.AnalyticsHelper;

/**
 * Abstract class that should be extended by fragments within the top level shop to enable functionality
 * for reporting book impressions for Google Analytics.
 *
 * The important things to know are:
 * Any fragment that extends this should report its screen name before reporting impressions (via the setScreenName) method.
 * The extending fragment should call reportShopItemImpression OR reportBookInfoImpression to report books when they are displayed.
 * The setHasBeenViewed flag must be set for this fragment to report the list of impressions.
 * The list of impressions is reported at the point this fragment is destroyed.
 */
public abstract class ImpressionReporterFragment extends Fragment {

    private final String KEY_HAS_BEEN_VIEWED = "has_been_viewed";
    private final String KEY_SCREEN_NAME = "screen_name";

    private boolean mHasBeenViewed = false;
    private String mScreenName;

    // For Google Analytics to track impressions
    private SparseArray<ShopItem> mGAShopItemImpressions = new SparseArray<ShopItem>(200);
    private SparseArray<BBBBookInfo> mGABookInfoImpressions = new SparseArray<BBBBookInfo>(200);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mHasBeenViewed = savedInstanceState.getBoolean(KEY_HAS_BEEN_VIEWED, false);
            mScreenName = savedInstanceState.getString(KEY_SCREEN_NAME, "");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        bundle.putBoolean(KEY_HAS_BEEN_VIEWED, mHasBeenViewed);
        bundle.putString(KEY_SCREEN_NAME, mScreenName);
    }

    /** Called to inform that this fragment has been viewed by the user */
    public void setHasBeenViewed(boolean hasBeenViewed) {
        mHasBeenViewed = hasBeenViewed;
    }

    /**
     * Set the screen name to report along with impressions.
     */
    public void setScreenName(String screenName) {
        mScreenName = screenName;
    }

    /**
     * Get the screen name to report along with impressions.
    */
    public String getScreenName() {
        return mScreenName;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // We send off all impressions at the point the fragment is destroyed. Note that on devices where
        // we support rotation the impressions will be reported again after rotation and similarly if a
        // fragment is within a viewpager such that it is swiped away (out of memory) and later swiped back
        // into view we will report impressions twice. These issues are not bugs but a limitation of this
        // reporting mechanism (to avoid making it overly complex).
        if (mHasBeenViewed) {
            if (mGAShopItemImpressions.size() > 0) {
                AnalyticsHelper.getInstance().sendImpressions(mScreenName, mGAShopItemImpressions);
            } else if (mGABookInfoImpressions.size() > 0) {
                AnalyticsHelper.getInstance().sendBookInfoImpressions(mScreenName, mGABookInfoImpressions);
            } else {
                // If we have no impressions, just log that we have visited the page. This allows us to use this to track
                // page views for items that may have no actual shop items within (such as a category list)
                AnalyticsHelper.getInstance().startTrackingUIComponent(mScreenName);
                AnalyticsHelper.getInstance().stopTrackingUIComponent(mScreenName);
            }
        }
    }

    /**
     * Add a new shop item impression to be reported to GA
     * @param index the index of the shop item (starting from 0)
     * @param shopItem the shop item to report
     */
    protected void reportShopItemImpression(int index, ShopItem shopItem) {
        // index starts at zero so the position is always +1 (as we want position to start at 1)
        mGAShopItemImpressions.put(index + 1, shopItem);
    }

    /**
     * Add a new shop item impression to be reported to GA
     * @param index the index of the shop item (starting from 0)
     * @param bookInfo the bookInfo item to report
     */
    protected void reportBookInfoImpression(int index, BBBBookInfo bookInfo) {
        // index starts at zero so the position is always +1 (as we want position to start at 1)
        mGABookInfoImpressions.put(index + 1, bookInfo);
    }
}
