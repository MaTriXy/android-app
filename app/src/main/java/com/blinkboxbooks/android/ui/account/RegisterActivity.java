// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.account;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;

/**
 * This screen allows a user to register for the service.
 */
public class RegisterActivity extends BaseActivity {

    private static final String TAG_BACK_CONFIRM_DIALOG = "back_confirm_dialog";

    public static final int RESULT_CODE_EMAIL_TAKEN = 201;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_register);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Create a new Fragment to be placed in the activity layout
        RegisterFragment registerFragment = new RegisterFragment();

        // Add the fragment to the 'fragment_container' FrameLayout
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, registerFragment).commit();

        registerFragment.setRegisterEventListener(new RegisterFragment.RegisterEventListener() {
            @Override
            public void signInWithEmailPressed(String emailAddress) {
                finish();
                Intent loginIntent = new Intent(RegisterActivity.this, LoginActivity.class);
                loginIntent.putExtra(LoginActivity.EXTRA_PREPOPULATE_EMAIL, emailAddress);
                startActivity(loginIntent);
            }

            @Override
            public void signInComplete(Intent intent) {
                setResult(Activity.RESULT_OK, intent);
                finish();
            }
        });

        mScreenName = AnalyticsHelper.GA_SCREEN_Library_RegistrationScreen;
    }

    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            showBackConfirmDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
        } else if (keyCode == KeyEvent.KEYCODE_BACK) {
            showBackConfirmDialog();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showBackConfirmDialog() {

        final GenericDialogFragment genericDialogFragment = GenericDialogFragment.newInstance(getString(R.string.title_reg_back_confirm), getString(R.string.dialog_reg_back_confirm), getString(R.string.button_leave),
                getString(R.string.button_continue_registration), null,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }

                }, null, null, null
        );

        showDialog(genericDialogFragment, TAG_BACK_CONFIRM_DIALOG, false);
    }
}