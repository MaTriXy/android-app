// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.purchase;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBillingAddress;
import com.blinkboxbooks.android.controller.PurchaseController;
import com.blinkboxbooks.android.model.CreditCard;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.ValidationUtil;
import com.blinkboxbooks.android.widget.BBBButton;
import com.blinkboxbooks.android.widget.BBBEditText;
import com.blinkboxbooks.android.widget.BBBTextView;

import java.util.Calendar;

/**
 * Fragment for allowing user to register a new credit card
 */
@SuppressLint("InflateParams")
public class AddNewCardFragment extends BaseDialogFragment {

    private static final int STAGE_ENTER_CARD_NUMBER = 0;
    private static final int STAGE_ENTER_CARD_DETAILS = 1;
    private static final int STAGE_ENTER_NAME_ON_CARD = 2;
    private static final int STAGE_EDIT_MODE = 3;

    private ScrollView mScrollView;
    private SwitchCompat mSwitchSaveDetails;
    private BBBEditText mEditTextCardnumber;
    private ImageView mImageViewVisa;
    private ImageView mImageViewMastercard;
    private BBBEditText mEditTextMonth;
    private BBBEditText mEditTextYear;
    private BBBEditText mEditTextCvv;
    private BBBEditText mEditTextNameOnCard;
    private BBBEditText mEditTextAddressLine1;
    private BBBEditText mEditTextAddressLine2;
    private BBBEditText mEditTextLocality;
    private BBBEditText mEditTextPostcode;
    private BBBTextView mTextViewCardOverseas;
    private BBBTextView mTextViewCardOverseasText;
    private BBBTextView mTextViewErrorGeneric;
    private BBBButton mButtonContinue;

    private ColorMatrixColorFilter mGreyScaleFilter;
    private int mMode = STAGE_ENTER_CARD_NUMBER;
    private TextWatcher mCardNumberWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String cardNumber = s.toString();
            String cardType = ValidationUtil.getCreditCardType(cardNumber);
            if (BBBApiConstants.PARAM_CARDTYPE_VISA.equals(cardType)) {
                mImageViewVisa.setColorFilter(null);
                mImageViewMastercard.setColorFilter(mGreyScaleFilter);
            } else if (BBBApiConstants.PARAM_CARDTYPE_MASTERCARD.equals(cardType)) {
                mImageViewVisa.setColorFilter(mGreyScaleFilter);
                mImageViewMastercard.setColorFilter(null);
            } else {
                mImageViewVisa.setColorFilter(mGreyScaleFilter);
                mImageViewMastercard.setColorFilter(mGreyScaleFilter);
            }

            if (cardNumber.length() < ValidationUtil.VALIDATION_MIN_CARD_LENGTH) {
                mEditTextCardnumber.setErrorState(false);
            } else if (ValidationUtil.validateCreditCardNumber(cardNumber)) {
                // Skip to next element
                mEditTextMonth.requestFocus();
                mEditTextCardnumber.setErrorState(false);
            } else {
                mEditTextCardnumber.setErrorState(true);
            }
            updateMode();
        }
    };

    private TextWatcher mMonthWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String monthString = s.toString();
            if (monthString.length() > 0) {
                if (ValidationUtil.validateMonth(monthString)) {
                    // Prevent premature skipping of field
                    int month = Integer.valueOf(monthString);
                    if (month > 2) {
                        mEditTextYear.requestFocus();
                    }

                    mEditTextMonth.setErrorState(false);

                    if (mEditTextYear.getText().length() == 4) {
                        validateExpiryDate();
                    }
                } else {
                    mEditTextMonth.setErrorState(true);
                }
            } else {
                mEditTextMonth.setErrorState(false);
            }
            updateMode();
        }
    };

    private TextWatcher mYearWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String yearString = s.toString();
            if (yearString.length() == 4) {
                if (validateExpiryDate()) {
                    mEditTextCvv.requestFocus();
                } else {
                    mEditTextYear.setErrorState(true);
                }
            } else {
                mEditTextYear.setErrorState(true);
            }
            updateMode();
        }
    };

    private TextWatcher mCVVWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            String cvvString = s.toString();
            if (cvvString.length() == 3) {
                mEditTextCvv.setErrorState(false);
            } else {
                mEditTextCvv.setErrorState(true);
            }
            updateMode();
        }
    };

    /**
     * Creates a new instance of this dialog fragment
     *
     * @return the Fragment
     */
    public static AddNewCardFragment newInstance() {
        return new AddNewCardFragment();
    }

    public AddNewCardFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_add_new_card, null);

        // Prevent screenshots in production app
        if (!BuildConfig.DEBUG) {
            getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        }

        mScrollView = (ScrollView) view.findViewById(R.id.scroll_view);
        mSwitchSaveDetails = (SwitchCompat) view.findViewById(R.id.switch_save_details);
        mEditTextCardnumber = (BBBEditText) view.findViewById(R.id.edittext_cardnumber);
        mImageViewVisa = (ImageView) view.findViewById(R.id.imageview_visa);
        mImageViewMastercard = (ImageView) view.findViewById(R.id.imageview_mastercard);
        mEditTextMonth = (BBBEditText) view.findViewById(R.id.edittext_month);
        mEditTextYear = (BBBEditText) view.findViewById(R.id.edittext_year);
        mEditTextCvv = (BBBEditText) view.findViewById(R.id.edittext_cvv);
        mEditTextNameOnCard = (BBBEditText) view.findViewById(R.id.edittext_name_on_card);
        mEditTextAddressLine1 = (BBBEditText) view.findViewById(R.id.edittext_address_line_1);
        mEditTextAddressLine2 = (BBBEditText) view.findViewById(R.id.edittext_address_line_2);
        mEditTextLocality = (BBBEditText) view.findViewById(R.id.edittext_locality);
        mEditTextPostcode = (BBBEditText) view.findViewById(R.id.edittext_postcode);
        mTextViewCardOverseas = (BBBTextView) view.findViewById(R.id.textview_card_overseas);
        mTextViewCardOverseasText = (BBBTextView) view.findViewById(R.id.textview_card_overseas_text);
        mButtonContinue = (BBBButton) view.findViewById(R.id.button_continue);
        mTextViewErrorGeneric = (BBBTextView) view.findViewById(R.id.textview_error_generic);

        mTextViewCardOverseas.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG | Paint.ANTI_ALIAS_FLAG);
        mTextViewCardOverseas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int visible = mTextViewCardOverseasText.getVisibility();
                if (visible == View.VISIBLE) {
                    visible = View.GONE;
                } else {
                    visible = View.VISIBLE;
                }
                mTextViewCardOverseasText.setVisibility(visible);
            }
        });

        mEditTextPostcode.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    continueClicked();
                    return true;
                }

                return false;
            }
        });

        mEditTextCvv.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {

                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    String cvv = mEditTextCvv.getText().toString();

                    if (ValidationUtil.validateCVC(cvv)) {
                        mEditTextCvv.setErrorState(false);
                    } else {
                        mEditTextCvv.setErrorState(true);
                        return true;
                    }
                }

                return false;
            }
        });

        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        mGreyScaleFilter = new ColorMatrixColorFilter(colorMatrix);
        mImageViewMastercard.setColorFilter(mGreyScaleFilter);
        mImageViewVisa.setColorFilter(mGreyScaleFilter);

        mEditTextCardnumber.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ValidationUtil.VALIDATION_MIN_CARD_LENGTH)});
        mEditTextCvv.setFilters(new InputFilter[]{new InputFilter.LengthFilter(ValidationUtil.VALIDATION_CVV_LENGTH)});
        mEditTextMonth.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        mEditTextYear.setFilters(new InputFilter[]{new InputFilter.LengthFilter(4)});

        mButtonContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                continueClicked();
            }
        });

        mEditTextCardnumber.addTextChangedListener(mCardNumberWatcher);
        mEditTextYear.addTextChangedListener(mYearWatcher);
        mEditTextMonth.addTextChangedListener(mMonthWatcher);
        mEditTextCvv.addTextChangedListener(mCVVWatcher);

        mEditTextCardnumber.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (!hasFocus) {
                    mEditTextCardnumber.setErrorState(!ValidationUtil.validateCreditCardNumber(mEditTextCardnumber.getText().toString()));
                }
            }
        });

        mEditTextYear.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (!hasFocus && mMode > STAGE_ENTER_CARD_NUMBER) {
                    validateExpiryDate();
                }
            }
        });

        mEditTextCvv.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                if (!hasFocus && mMode == STAGE_EDIT_MODE) {
                    mEditTextCardnumber.setErrorState(!ValidationUtil.validateCreditCardNumber(mEditTextCardnumber.getText().toString()));
                }
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return dialog;
    }

    private void continueClicked() {
        boolean valid;

        valid = ValidationUtil.validateCreditCardNumber(mEditTextCardnumber.getText().toString());
        mEditTextCardnumber.setErrorState(!valid);
        boolean validated = valid;

        valid = validateExpiryDate();
        validated &= valid;

        valid = ValidationUtil.validateCVC(mEditTextCvv.getText().toString());
        mEditTextCvv.setErrorState(!valid);
        validated &= valid;

        valid = ValidationUtil.validateField(mEditTextNameOnCard.getText().toString());
        mEditTextNameOnCard.setErrorState(!valid);
        validated &= valid;

        valid = ValidationUtil.validateField(mEditTextAddressLine1.getText().toString());
        mEditTextAddressLine1.setErrorState(!valid);
        validated &= valid;

        valid = ValidationUtil.validateField(mEditTextLocality.getText().toString());
        mEditTextLocality.setErrorState(!valid);
        validated &= valid;

        String postCode = mEditTextPostcode.getText().toString();
        valid = ValidationUtil.validatePostcode(postCode);
        mEditTextPostcode.setErrorState(!valid);
        validated &= valid;

        if (!validated) {
            mTextViewErrorGeneric.setVisibility(View.VISIBLE);
            // Scroll back to the top so the user can clearly see that there is an error
            mScrollView.smoothScrollTo(0, 0);
            return;
        } else {
            mTextViewErrorGeneric.setVisibility(View.GONE);
        }

        CreditCard creditCard = new CreditCard();
        creditCard.saveNewCard = mSwitchSaveDetails.isChecked();
        creditCard.number = mEditTextCardnumber.getText().toString();
        creditCard.maskedNumber = creditCard.number.substring(Math.max(creditCard.number.length() - 4, 0));
        creditCard.cvv = mEditTextCvv.getText().toString();
        creditCard.cardholderName = mEditTextNameOnCard.getText().toString();
        creditCard.expirationMonth = Integer.valueOf(mEditTextMonth.getText().toString());
        creditCard.expirationYear = Integer.valueOf(mEditTextYear.getText().toString());

        creditCard.billingAddress = new BBBBillingAddress();
        creditCard.billingAddress.line1 = mEditTextAddressLine1.getText().toString();
        creditCard.billingAddress.line2 = mEditTextAddressLine2.getText().toString();
        creditCard.billingAddress.locality = mEditTextLocality.getText().toString();
        creditCard.billingAddress.postcode = mEditTextPostcode.getText().toString();

        dismiss();
        PurchaseController.getInstance().setCreditCard(creditCard);
        PurchaseController.getInstance().showPurchaseConfirmationFragment();
    }

    private boolean validateExpiryDate() {
        String month = mEditTextMonth.getText().toString();
        String year = mEditTextYear.getText().toString();
        Calendar currentDate = Calendar.getInstance();

        if (TextUtils.isEmpty(month)) {
            mEditTextMonth.setErrorState(true);
            return false;
        }

        int m = Integer.parseInt(month);

        if (m < 1 || m > 12) {
            mEditTextMonth.setErrorState(true);
            return false;
        }

        if (TextUtils.isEmpty(year)) {
            mEditTextYear.setErrorState(true);
            return false;
        }

        if (year.trim().length() < 4) {
            mEditTextYear.setErrorState(true);
            return false;
        }

        Calendar date = Calendar.getInstance();
        date.set(Calendar.MONTH, m);
        date.set(Calendar.YEAR, Integer.parseInt(year));

        if (currentDate.after(date)) {
            mEditTextYear.setErrorState(true);
            return false;
        }

        mEditTextYear.setErrorState(false);
        return true;
    }

    private void updateMode() {
        int oldMode = mMode;
        if (mMode == STAGE_ENTER_CARD_NUMBER) {
            String cardNumber = mEditTextCardnumber.getText().toString();

            if (ValidationUtil.validateCreditCardNumber(cardNumber)) {
                mMode = STAGE_ENTER_CARD_DETAILS;
            }
        }

        if (mMode == STAGE_ENTER_CARD_DETAILS) {
            String year = mEditTextYear.getText().toString();
            String month = mEditTextMonth.getText().toString();
            String cvv = mEditTextCvv.getText().toString();

            if (ValidationUtil.validateMonth(month) && ValidationUtil.validateYear(year) && ValidationUtil.validateCVC(cvv)) {
                mMode = STAGE_ENTER_NAME_ON_CARD;
                mEditTextCvv.setErrorState(false);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        AnalyticsHelper.getInstance().stopTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Shop_PaymentScreen_EditCard);
        AnalyticsHelper.getInstance().stopTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Shop_PaymentScreen_CardDetails);
        AnalyticsHelper.getInstance().stopTrackingUIComponent(AnalyticsHelper.GA_SCREEN_Shop_PaymentScreen_PersonalDetails);
    }
}