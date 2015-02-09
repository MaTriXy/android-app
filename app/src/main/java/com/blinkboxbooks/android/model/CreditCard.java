// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

import com.blinkboxbooks.android.api.model.BBBCreditCard;

import java.io.Serializable;

/**
 * Credit card POJO
 */
public class CreditCard extends BBBCreditCard implements Serializable {

    private static final long serialVersionUID = 1L;

    public boolean saveNewCard;
    public String number;
    public String cvv;
}