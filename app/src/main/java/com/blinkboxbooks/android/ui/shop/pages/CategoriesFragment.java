// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.pages;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.loader.CatalogueLoader;
import com.blinkboxbooks.android.provider.CatalogueContract;
import com.blinkboxbooks.android.ui.shop.ImpressionReporterFragment;
import com.blinkboxbooks.android.ui.shop.SearchActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBImageLoader;
import com.blinkboxbooks.android.util.BBBUIUtils;

/**
 * Fragment for displaying categories
 */
public class CategoriesFragment extends ImpressionReporterFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static CategoriesFragment newInstance() {
        return new CategoriesFragment();
    }

    private GridView mGridView;
    private TextView mTextViewError;
    private ProgressBar mProgressBar;

    public CategoriesFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setScreenName(AnalyticsHelper.GA_SCREEN_Shop_CategoriesScreen);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CatalogueLoader loader = new CatalogueLoader(getActivity(), CatalogueContract.Category.getCategories(), null, null, null, null);
        loader.setErrorListener(mErrorListener);

        mProgressBar.setVisibility(View.VISIBLE);
        mTextViewError.setVisibility(View.GONE);

        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        final CategoryAdapter adapter = (CategoryAdapter)mGridView.getAdapter();
        adapter.swapCursor(data);
        mProgressBar.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shop_categories, container, false);

        mGridView = (GridView)view.findViewById(R.id.gridview);
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = ((CursorAdapter)(mGridView.getAdapter())).getCursor();
                cursor.moveToPosition(position);

                String categoryName = cursor.getString(cursor.getColumnIndex(CatalogueContract.Category.CATEGORY_NAME));
                String displayName = cursor.getString(cursor.getColumnIndex(CatalogueContract.Category.DISPLAY_NAME));

                Intent intent = new Intent(getActivity(), SearchActivity.class);
                intent.putExtra(SearchActivity.ARG_VIEW_TYPE, SearchActivity.ViewType.CATEGORY);
                intent.putExtra(SearchActivity.ARG_ID, categoryName);
                intent.putExtra(SearchActivity.ARG_TITLE, displayName);

                getActivity().startActivity(intent);
            }
        });

        mTextViewError = (TextView)view.findViewById(R.id.textview_error);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_loading);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mGridView.setAdapter(new CategoryAdapter(getActivity(),null,false));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        CategoryAdapter adapter = (CategoryAdapter)mGridView.getAdapter();

        if(adapter != null) {
            adapter.swapCursor(null);
        }

        if(mGridView != null) {
            mGridView.setAdapter(null);
            mGridView = null;
        }
    }

    private void showError(int errorMessageResourceId) {
        mTextViewError.setVisibility(View.VISIBLE);
        mTextViewError.setText(errorMessageResourceId);
    }

    private class CategoryAdapter extends CursorAdapter {

        private CategoryAdapter(Context context, Cursor c, boolean autoRequery) {
            super(context, c, autoRequery);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return View.inflate(context, R.layout.shop_category_view, null);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final TextView textView = (TextView)view.findViewById(R.id.category_name);
            final ImageView imageView = (ImageView)view.findViewById(R.id.category_image);

            imageView.setImageBitmap(null);

            String name = cursor.getString(cursor.getColumnIndex(CatalogueContract.Category.DISPLAY_NAME));
            int width = BBBUIUtils.getGridItemWidth(context, mGridView);

            final String imageUrl = BBBImageLoader.injectWidthIntoCoverUrl(cursor.getString(cursor.getColumnIndex(CatalogueContract.Category.IMAGE_URL)), width);
            imageView.setTag(imageUrl);

            BBBImageLoader.getInstance().get(imageUrl, new ImageLoader.ImageListener() {
                @Override
                public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                    String currentUrl = (String) imageView.getTag();

                    if (imageContainer.getRequestUrl().equals(currentUrl)) {
                        imageView.setImageBitmap(imageContainer.getBitmap());
                    }
                }

                @Override
                public void onErrorResponse(VolleyError volleyError) {}

            }, Request.Priority.HIGH);

            textView.setText(name);
        }
    }

    private final CatalogueLoader.ErrorListener mErrorListener = new CatalogueLoader.ErrorListener() {

        @Override
        public void internalServerError() {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        showError(R.string.error_server_message);
                    }
                }
            };

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }

        @Override
        public void noNetworkError() {

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        showError(R.string.error_no_network_shop);
                    }
                }
            };

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }

        @Override
        public void noResults() {

            // We don't expect this to happen so just display the generic server error message
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    if (isResumed()) {
                        showError(R.string.error_server_message);
                    }
                }
            };

            Activity activity = getActivity();
            if (activity != null) {
                activity.runOnUiThread(runnable);
            }
        }
    };
}