// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.library;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.model.BBBCredit;
import com.blinkboxbooks.android.api.model.BBBCreditResponse;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.DrawerMenuController;
import com.blinkboxbooks.android.controller.DrawerMenuController.OnMenuItemSelectedListener;
import com.blinkboxbooks.android.list.MenuAdapter;
import com.blinkboxbooks.android.model.MenuListItem;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.StringUtils;

/**
 * The Drawer Menu implementation
 */
public class DrawerMenuFragment extends ListFragment {
    private static final String CREDIT_INFORMATION_HANDLER_ID = "credit_information_handler";

    private MenuAdapter mMenuAdapter;
    private DrawerMenuController mDrawerMenuController;
    private OnMenuItemSelectedListener mOnMenuItemSelectedListener;
    private int mNumItemsCurrentlyReading;
    private int mNumItemsMyLibrary;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mOnMenuItemSelectedListener = (OnMenuItemSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnMenuItemSelectedListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_drawer, container);
    }


    /**
     * Set the number of items for the currently reading tab
     * @param numberOfItems the number of items
     */
    public void setNumberOfItemsCurrentlyReading(int numberOfItems) {
        mNumItemsCurrentlyReading = numberOfItems;
        createMenuItems();
    }

    /**
     * Set the number of items for the my library tab
     * @param numberOfItems the number of items
     */
    public void setNumberOfItemsMyLibrary(int numberOfItems) {
        mNumItemsMyLibrary = numberOfItems;
        createMenuItems();
    }

    /**
     * Construct a the main menu list based on data from the
     * {@link DrawerMenuController}
     */
    public void createMenuItems() {
        // Check that we still have an activity as this could potentially be called (in edge cases) after the activity is killed.
        Activity activity = getActivity();
        if (activity != null) {
            mDrawerMenuController = DrawerMenuController.getInstance(activity);
            mDrawerMenuController.setInnerItemsEnabled(NetworkUtils.hasInternetConnectivity(getActivity()));
            mMenuAdapter = new MenuAdapter(activity, R.layout.menu_list_item, mDrawerMenuController.getMenuItems(), mNumItemsCurrentlyReading, mNumItemsMyLibrary);
            setListAdapter(mMenuAdapter);
            if (AccountController.getInstance().isLoggedIn()) {
                sendCreditRequest();
            } else {
                DrawerMenuController.getInstance(getActivity()).setCreditText("");
            }
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MenuListItem listItem = mMenuAdapter.getItem(position);
        mOnMenuItemSelectedListener.onMenuItemSelected(listItem);
    }

    @Override
    public void onStart() {
        super.onStart();
        createMenuItems();

        BBBRequestManager.getInstance().addResponseHandler(CREDIT_INFORMATION_HANDLER_ID, creditInformationHandler);
    }

    @Override
    public void onStop() {
        super.onStop();
        BBBRequestManager.getInstance().removeResponseHandler(CREDIT_INFORMATION_HANDLER_ID);
    }

    private void sendCreditRequest() {
        BBBRequest request = BBBRequestFactory.getInstance().createGetCreditOnAccountRequest();
        BBBRequestManager.getInstance().executeRequest(CREDIT_INFORMATION_HANDLER_ID, request);
    }

    private final BBBBasicResponseHandler<BBBCreditResponse> creditInformationHandler = new BBBBasicResponseHandler<BBBCreditResponse>() {
        @Override
        public void receivedData(BBBResponse bbbResponse, BBBCreditResponse bbbCreditResponse) {
            BBBCredit currentCredit = bbbCreditResponse.credit;

           String currency = StringUtils.getCurrencySymbol(currentCredit.currency);
           String creditText = String.format(getString(R.string.you_have_credit), currency, currentCredit.amount);

            if(currentCredit.amount > 0) {
                DrawerMenuController.getInstance(getActivity()).setCreditText(creditText);
            } else {
                DrawerMenuController.getInstance(getActivity()).setCreditText("");
            }

            mMenuAdapter.notifyDataSetChanged();
        }
        //A error response will most commonly be because the user is not signed in, in this case, we set the credit text to be empty
        @Override
        public void receivedError(BBBResponse bbbResponse) {
            DrawerMenuController.getInstance(getActivity()).setCreditText("");
            mMenuAdapter.notifyDataSetChanged();
        }
    };
}
