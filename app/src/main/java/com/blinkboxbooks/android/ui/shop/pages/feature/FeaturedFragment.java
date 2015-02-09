// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.pages.feature;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.shop.ImpressionReporterFragment;
import com.blinkboxbooks.android.ui.shop.SortOption;
import com.blinkboxbooks.android.ui.shop.pages.feature.highlight.HighlightFragment;

/**
 * Fragment for showing the shop featured page
 */
public class FeaturedFragment extends ImpressionReporterFragment implements SectionErrorHandler  {

    // The index of the pages of the view flipper
    private static final int VIEW_CONTENT = 0;
    private static final int VIEW_ERROR = 1;

    private static final int PROMOTION_ID = 4;
    private static final int MAX_NUM_BOOKS_TO_REQUEST_FOR_FEATURED = 10;

    private ViewFlipper mViewFlipper;
    private TextView mTextViewError;
    private boolean mHasBeenViewed;

    private static final String ARG_IS_VIEWED = "is_viewed";

    /**
     * Construct a new instance
     * @param isViewed flag to indicate if this fragment is viewed at startup
     * @return a fragment
     */
    public static FeaturedFragment newInstance(boolean isViewed) {

        FeaturedFragment fragment = new FeaturedFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_IS_VIEWED, isViewed);
        fragment.setArguments(args);
        return fragment;
    }

    public FeaturedFragment() {}

    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        mHasBeenViewed = getArguments().getBoolean(ARG_IS_VIEWED, true);
    }

    /**
     * We override the standard 'has been viewed' behaviour to pass on the viewed state to all child fragments.
     * Note that for simplicity we just say all child fragments have been viewed whenever this fragment is viewed because
     * working out exactly what has been shown to the user is extremely complex. This behaviour is consistent with what the
     * website does in some cases because it has similar issues determining exactly what is on screen.
     * @param hasBeenViewed set to true if this fragment has been viewed
     */
    @Override
    public void setHasBeenViewed(boolean hasBeenViewed) {
        // Cache the has been viewed value in case this is called before we have created the fragments
        mHasBeenViewed = hasBeenViewed;
        setViewedStateInternalFragments();
    }

    private void setViewedStateInternalFragments() {
        FragmentManager fragmentManager = getChildFragmentManager();
        setChildFramgentHasBeenViewed((ImpressionReporterFragment) fragmentManager.findFragmentById(R.id.container_highlights), mHasBeenViewed);
        setChildFramgentHasBeenViewed((ImpressionReporterFragment) fragmentManager.findFragmentById(R.id.container_bestsellers_fiction), mHasBeenViewed);
        setChildFramgentHasBeenViewed((ImpressionReporterFragment) fragmentManager.findFragmentById(R.id.container_bestsellers_nonfiction), mHasBeenViewed);
        setChildFramgentHasBeenViewed((ImpressionReporterFragment) fragmentManager.findFragmentById(R.id.container_promoted_category), mHasBeenViewed);
    }

    private void setChildFramgentHasBeenViewed(ImpressionReporterFragment fragment, boolean hasBeenViewed) {
        if (fragment != null) {
            fragment.setHasBeenViewed(hasBeenViewed);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_featured, container, false);
        mTextViewError = (TextView) view.findViewById(R.id.textview_error);
        mViewFlipper = (ViewFlipper) view.findViewById(R.id.view_flipper);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentManager fragmentManager = getChildFragmentManager();

        if(savedInstanceState == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

            //TODO change this to get books by category location when CP-1759 is fixed. will save one API call
            Uri highlightsViewUri = CatalogueContract.Books.getBooksForCategoryNameUri(getString(R.string.shop_highlights_identifier), true, MAX_NUM_BOOKS_TO_REQUEST_FOR_FEATURED, 0);
            Uri bestSellersFictionUri = CatalogueContract.Books.getBooksForCategoryNameUri("bestsellers-fiction", true, MAX_NUM_BOOKS_TO_REQUEST_FOR_FEATURED, 0);
            Uri bestSellersNonFictionUri = CatalogueContract.Books.getBooksForCategoryNameUri("bestsellers-non-fiction", true, MAX_NUM_BOOKS_TO_REQUEST_FOR_FEATURED, 0);

            fragmentTransaction.add(R.id.container_highlights, HighlightFragment.newInstance(highlightsViewUri));
            fragmentTransaction.add(R.id.container_bestsellers_fiction, BookSectionFragment.newInstance(getString(R.string.shop_fiction_bestsellers), 1, bestSellersFictionUri, true, true, SortOption.SEQUENTIAL));
            fragmentTransaction.add(R.id.container_bestsellers_nonfiction, BookSectionFragment.newInstance(getString(R.string.shop_nonfiction_bestsellers), 1, bestSellersNonFictionUri, true, true, SortOption.SEQUENTIAL));
//            fragmentTransaction.add(R.id.container_spotlight, SpotlightFragment.newInstance());
            fragmentTransaction.add(R.id.container_promoted_category, BookSectionFragment.newInstance(PROMOTION_ID, 2, false, false, SortOption.SEQUENTIAL));
            fragmentTransaction.commit();
            fragmentManager.executePendingTransactions();

            setViewedStateInternalFragments();
        }
    }

    @Override
    public void reportError(int errorStringResource) {
        mViewFlipper.setDisplayedChild(VIEW_ERROR);
        mTextViewError.setText(errorStringResource);
    }
}