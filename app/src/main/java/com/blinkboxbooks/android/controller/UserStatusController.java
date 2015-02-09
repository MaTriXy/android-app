// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.content.Context;
import android.view.Menu;

import com.blinkboxbooks.android.R;

/**
 * Controller for the user status
 */
public class UserStatusController {

    //menu id constants for the popup menu
    public static final int MENU_ID_SIGN_OUT = 1;

    /**
     * Configures the PopupMenu that pops up when the user clicks on the account icon
     *
     * @param context
     * @param menu    The menu to be inflated
     */
    public static void configurePopupMenu(Context context, Menu menu) {
        menu.clear();
        boolean hasPurchased = true;
        menu.add(Menu.NONE, 0, 0, context.getString(R.string.update_my_personal_details));
        menu.add(Menu.NONE, 0, 0, context.getString(R.string.manage_my_devices));
        if (hasPurchased) {
            menu.add(Menu.NONE, 0, 0, context.getString(R.string.order_payment_history));
            menu.add(Menu.NONE, 0, 0, context.getString(R.string.manage_my_payment));
        }
        menu.add(Menu.NONE, MENU_ID_SIGN_OUT, 0, context.getString(R.string.sign_out));
    }
}