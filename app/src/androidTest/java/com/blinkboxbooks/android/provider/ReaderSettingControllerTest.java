// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.provider;

import android.test.AndroidTestCase;

import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.model.ReaderSetting;
import com.blinkboxbooks.android.model.helper.ReaderSettingHelper;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettings;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController;

/**
 * This class tests the reader setting content provider for the Blinkbox books Android app.
 */
public class ReaderSettingControllerTest extends AndroidTestCase {

	private static final String SIGNED_OUT_ACCOUNT = BusinessRules.DEFAULT_ACCOUNT_NAME;
	private static final String ACCOUNT_A = "Account A";
	private static final String ACCOUNT_B = "Account B";
	private static final float BRIGHTNESS_A = 0.25f;
	private static final float BRIGHTNESS_B = 0.25f;

	private EPub2ReaderSettingController mEPub2ReaderSettingController = EPub2ReaderSettingController.getInstance();
	private ReaderSetting mReaderSetting;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		getContext().getContentResolver().delete(ReaderSettings.buildReaderSettingAccountUri(SIGNED_OUT_ACCOUNT), null, null);
		getContext().getContentResolver().delete(ReaderSettings.buildReaderSettingAccountUri(ACCOUNT_A), null, null);
		getContext().getContentResolver().delete(ReaderSettings.buildReaderSettingAccountUri(ACCOUNT_B), null, null);
		mReaderSetting = null;
	}

	/**
	 * Tests that the default settings are applied to an Anonymous user
	 */
	public void testCanReadDefaultSettings() {
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(SIGNED_OUT_ACCOUNT);
		assertEquals(EPub2ReaderSettingController.READER_SETTING_DEFAULT_BRIGHTNESS, mReaderSetting.brightness);
	}

	/**
	 * Tests that the Anonymous user can modify the settings
	 */
	public void testCanUpdateDefaultSettings() {
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(SIGNED_OUT_ACCOUNT);
		assertEquals(EPub2ReaderSettingController.READER_SETTING_DEFAULT_BRIGHTNESS, mReaderSetting.brightness);
		mEPub2ReaderSettingController.setBrightness(BRIGHTNESS_A);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(SIGNED_OUT_ACCOUNT);
		assertEquals(BRIGHTNESS_A, mReaderSetting.brightness);
		mEPub2ReaderSettingController.setBrightness(BRIGHTNESS_B);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(SIGNED_OUT_ACCOUNT);
		assertEquals(BRIGHTNESS_B, mReaderSetting.brightness);
	}

	/**
	 * Tests that after an Anonymous logs in his modified settings are transferred
	 */
	public void testDefaultSettingsAreTransferredToUser() {
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(SIGNED_OUT_ACCOUNT);
		assertEquals(EPub2ReaderSettingController.READER_SETTING_DEFAULT_BRIGHTNESS, mReaderSetting.brightness);
		mEPub2ReaderSettingController.setBrightness(BRIGHTNESS_A);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}

		// User logs in as account A
		ReaderSettingHelper.transferReaderSetting(SIGNED_OUT_ACCOUNT, ACCOUNT_A);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(ACCOUNT_A);

		// User should have the modified settings
		assertEquals(BRIGHTNESS_A, mReaderSetting.brightness);
		assertEquals(EPub2ReaderSettingController.READER_SETTING_DEFAULT_FONT_SIZE, mReaderSetting.font_size);

		// Anonymous user should have the default settings again
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(SIGNED_OUT_ACCOUNT);
		assertEquals(BRIGHTNESS_A, mReaderSetting.brightness);
	}

	/**
	 * Tests that different users should have separate settings
	 */
	public void testTwoUsersCanHaveSeparateSettings() {
		// Set the brightness for A
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(ACCOUNT_A);
		assertEquals(EPub2ReaderSettingController.READER_SETTING_DEFAULT_BRIGHTNESS, mReaderSetting.brightness);
		mEPub2ReaderSettingController.setBrightness(BRIGHTNESS_A);

		// Set the brightness for B
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(ACCOUNT_B);
		assertEquals(EPub2ReaderSettingController.READER_SETTING_DEFAULT_BRIGHTNESS, mReaderSetting.brightness);
		mEPub2ReaderSettingController.setBrightness(BRIGHTNESS_B);
		try {
			Thread.sleep(1000);
		} catch (Exception e) {
		}

		// The two accounts should have different settings
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(ACCOUNT_A);
		assertEquals(BRIGHTNESS_A, mReaderSetting.brightness);
		mReaderSetting = mEPub2ReaderSettingController.getReaderSetting(ACCOUNT_B);
		assertEquals(BRIGHTNESS_B, mReaderSetting.brightness);
	}
}