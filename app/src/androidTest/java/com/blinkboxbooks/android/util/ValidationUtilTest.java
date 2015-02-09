// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.test.AndroidTestCase;

import com.blinkboxbooks.android.util.ValidationUtil;

/**
 * This class tests the reader setting content provider for the Blinkbox books Android app.
 */
public class ValidationUtilTest extends AndroidTestCase {

	/**
	 * Tests basic postcode validation
	 */
	public void testCanValidatePostcode() {
		assertTrue(ValidationUtil.validatePostcode("NW1 6LP"));
		assertFalse(ValidationUtil.validatePostcode("NW1 6LPP"));
		assertTrue(ValidationUtil.validatePostcode("E1 4BA"));
		assertFalse(ValidationUtil.validatePostcode("E1 4BAA"));
	}

	/**
	 * Tests postcode validation without spaces
	 */
	public void testCanValidatePostcodeWithoutSpace() {
		assertTrue(ValidationUtil.validatePostcode("NW16LP"));
		assertFalse(ValidationUtil.validatePostcode("NW16LPP"));
		assertTrue(ValidationUtil.validatePostcode("E14BA"));
		assertFalse(ValidationUtil.validatePostcode("E14BAA"));
	}
}