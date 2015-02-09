// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;
import android.widget.TextView;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.ReaderSetting;
import com.blinkboxbooks.android.ui.BaseDialogFragment;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.ColorValue;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.FontType;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.ReaderSettingListener;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.SettingValue;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.widget.BBBSwitch;

/**
 * Fragment to adjust the reader settings
 */
public class EPub2ReaderSettingsFragment extends BaseDialogFragment implements ReaderSettingListener {

    private EPub2ReaderSettingController mEPub2ReaderSettingController;
    private FontType[] mFonts;
    private SeekBar mSeekBarBrightness;
    private Spinner mSpinnerFont;

    private Button mToggleButtonWhite;
    private Button mToggleButtonBlack;
    private Button mToggleButtonSepia;
    private ImageButton mToggleButtonLeftAligned;
    private ImageButton mToggleButtonJustified;
    private ImageButton mToggleButtonWide;
    private ImageButton mToggleButtonMedium;
    private ImageButton mToggleButtonNarrow;
    private ImageButton mToggleButtonLineSpace1;
    private ImageButton mToggleButtonLineSpace2;
    private ImageButton mToggleButtonLineSpace3;

    private ImageView mButtonFontSizeDecrease;
    private ImageView mButtonFontSizeIncrease;
    private ImageView[] mImageViewFontSize;

    private TextView mTextViewFontSize;
    private String mFontSizeFormat;

    private BBBSwitch mSwitchPublisherStyles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mFragmentName = AnalyticsHelper.GA_SCREEN_Reader_SettingsScreen;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader_epub2_settings, container, false);

        mEPub2ReaderSettingController = EPub2ReaderSettingController.getInstance();

        mSwitchPublisherStyles = (BBBSwitch) view.findViewById(R.id.switch_styling);
        mSwitchPublisherStyles.setOnCheckedChangeListener(onCheckedChangeListenerPublisherStyles);

        mTextViewFontSize = (TextView) view.findViewById(R.id.textview_fontsize);
        mFontSizeFormat = getString(R.string.font_size_format);

        mToggleButtonLineSpace1 = (ImageButton) view.findViewById(R.id.button_line_space1);
        mToggleButtonLineSpace2 = (ImageButton) view.findViewById(R.id.button_line_space2);
        mToggleButtonLineSpace3 = (ImageButton) view.findViewById(R.id.button_line_space3);

        if (mToggleButtonLineSpace1 != null) {
            mToggleButtonLineSpace1.setOnClickListener(onClickListenerLineSpacing);
            mToggleButtonLineSpace2.setOnClickListener(onClickListenerLineSpacing);
            mToggleButtonLineSpace3.setOnClickListener(onClickListenerLineSpacing);
        }

        mButtonFontSizeDecrease = (ImageView) view.findViewById(R.id.button_fontsize_decrease);
        mButtonFontSizeIncrease = (ImageView) view.findViewById(R.id.button_fontsize_increase);

        mImageViewFontSize = new ImageView[EPub2ReaderSettingController.READING_SETTING_FONT_SIZE.length];
        mImageViewFontSize[0] = (ImageView) view.findViewById(R.id.imageview_fontsize_1);
        mImageViewFontSize[1] = (ImageView) view.findViewById(R.id.imageview_fontsize_2);
        mImageViewFontSize[2] = (ImageView) view.findViewById(R.id.imageview_fontsize_3);
        mImageViewFontSize[3] = (ImageView) view.findViewById(R.id.imageview_fontsize_4);
        mImageViewFontSize[4] = (ImageView) view.findViewById(R.id.imageview_fontsize_5);
        mImageViewFontSize[5] = (ImageView) view.findViewById(R.id.imageview_fontsize_6);

        mButtonFontSizeDecrease.setOnClickListener(onClickListenerFontSize);
        mButtonFontSizeIncrease.setOnClickListener(onClickListenerFontSize);

        if (view.findViewById(R.id.container_fontsize_1) != null) {
            view.findViewById(R.id.container_fontsize_1).setOnClickListener(onClickListenerFontSize);
            view.findViewById(R.id.container_fontsize_2).setOnClickListener(onClickListenerFontSize);
            view.findViewById(R.id.container_fontsize_3).setOnClickListener(onClickListenerFontSize);
            view.findViewById(R.id.container_fontsize_4).setOnClickListener(onClickListenerFontSize);
            view.findViewById(R.id.container_fontsize_5).setOnClickListener(onClickListenerFontSize);
            view.findViewById(R.id.container_fontsize_6).setOnClickListener(onClickListenerFontSize);
        }

        mToggleButtonLeftAligned = (ImageButton) view.findViewById(R.id.button_left_aligned);
        mToggleButtonJustified = (ImageButton) view.findViewById(R.id.button_justified);

        if (mToggleButtonLeftAligned != null) {
            mToggleButtonLeftAligned.setOnClickListener(onClickListenerTextAlign);
            mToggleButtonJustified.setOnClickListener(onClickListenerTextAlign);
        }

        mToggleButtonWide = (ImageButton) view.findViewById(R.id.button_wide);
        mToggleButtonMedium = (ImageButton) view.findViewById(R.id.button_medium);
        mToggleButtonNarrow = (ImageButton) view.findViewById(R.id.button_narrow);

        if (mToggleButtonWide != null) {
            mToggleButtonWide.setOnClickListener(onClickListenerPageMargins);
            mToggleButtonMedium.setOnClickListener(onClickListenerPageMargins);
            mToggleButtonNarrow.setOnClickListener(onClickListenerPageMargins);
        }

        mToggleButtonWhite = (Button) view.findViewById(R.id.button_white);
        mToggleButtonBlack = (Button) view.findViewById(R.id.button_black);
        mToggleButtonSepia = (Button) view.findViewById(R.id.button_sepia);
        mToggleButtonWhite.setOnClickListener(onClickListenerColors);
        mToggleButtonBlack.setOnClickListener(onClickListenerColors);
        mToggleButtonSepia.setOnClickListener(onClickListenerColors);

        mSeekBarBrightness = (SeekBar) view.findViewById(R.id.seekbar_brightness);

        if (mSeekBarBrightness != null) {
            mSeekBarBrightness.setMax(EPub2ReaderSettingController.SettingValue.BRIGHTNESS.getProgressMax());
            mSeekBarBrightness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

                public void onStopTrackingTouch(SeekBar seekBar) {
                }

                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser) {
                        mEPub2ReaderSettingController.setBrightness(SettingValue.BRIGHTNESS.getValue(progress));
                    }
                }
            });
        }

        mFonts = EPub2ReaderSettingController.FontType.values();
        mSpinnerFont = (Spinner) view.findViewById(R.id.spinner_font);

        final ArrayAdapter<FontType> fontAdapter = new ArrayAdapter<FontType>(getActivity(), android.R.layout.simple_spinner_item, mFonts) {

            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                setFontType(view, position);

                return view;
            }

            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                setFontType(view, position);
                return view;
            }

            private void setFontType(View view, int position) {
                TextView textview = (TextView) view.findViewById(android.R.id.text1);
                final FontType font = mFonts[position];
                textview.setText(font.label);
                textview.setTypeface(font.font_typeface);
            }
        };

        fontAdapter.setDropDownViewResource(android.R.layout.simple_list_item_1);
        mSpinnerFont.setAdapter(fontAdapter);
        mSpinnerFont.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mEPub2ReaderSettingController.setFontTypeface(mFonts[position]);
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_FONT_SELECTION,getString(mFonts[position].label), null);
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        mEPub2ReaderSettingController.loadReaderSetting(this);
        getDialog().setCanceledOnTouchOutside(true);
        return view;
    }

    private final CompoundButton.OnCheckedChangeListener onCheckedChangeListenerPublisherStyles = new CompoundButton.OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            mEPub2ReaderSettingController.setPublisherStylesEnabled(isChecked);
        }
    };

    private final OnClickListener onClickListenerColors = new OnClickListener() {

        public void onClick(View v) {
            ReaderSetting settings = mEPub2ReaderSettingController.getReaderSetting();

            switch (v.getId()) {
                case R.id.button_white:
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_PAGE_COLOUR,"Light", null);
                    mEPub2ReaderSettingController.setReaderTheme(ColorValue.WHITE.background_color, ColorValue.WHITE.foreground_color);
                    break;
                case R.id.button_black:
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_PAGE_COLOUR,"Dark", null);
                    mEPub2ReaderSettingController.setReaderTheme(ColorValue.BLACK.background_color, ColorValue.BLACK.foreground_color);
                    break;
                case R.id.button_sepia:
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_PAGE_COLOUR,"Sepia", null);
                    mEPub2ReaderSettingController.setReaderTheme(ColorValue.SEPIA.background_color, ColorValue.SEPIA.foreground_color);
                    break;
            }

            setColorButtonStates(settings);
        }
    };

    private final OnClickListener onClickListenerFontSize = new OnClickListener() {

        public void onClick(View v) {
            ReaderSetting settings = mEPub2ReaderSettingController.getReaderSetting();

            switch (v.getId()) {
                case R.id.button_fontsize_decrease: {
                    settings.font_size = mEPub2ReaderSettingController.setReaderFontSize(settings.font_size - SettingValue.FONT_SIZE.stepValue);
                    break;
                }

                case R.id.button_fontsize_increase: {
                    settings.font_size = mEPub2ReaderSettingController.setReaderFontSize(settings.font_size + SettingValue.FONT_SIZE.stepValue);
                    break;
                }

                case R.id.container_fontsize_1: {
                    settings.font_size = EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[0];
                    mEPub2ReaderSettingController.setReaderFontSize(settings.font_size);
                    break;
                }

                case R.id.container_fontsize_2: {
                    settings.font_size = EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[1];
                    mEPub2ReaderSettingController.setReaderFontSize(settings.font_size);
                    break;
                }

                case R.id.container_fontsize_3: {
                    settings.font_size = EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[2];
                    mEPub2ReaderSettingController.setReaderFontSize(settings.font_size);
                    break;
                }

                case R.id.container_fontsize_4: {
                    settings.font_size = EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[3];
                    mEPub2ReaderSettingController.setReaderFontSize(settings.font_size);
                    break;
                }

                case R.id.container_fontsize_5: {
                    settings.font_size = EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[4];
                    mEPub2ReaderSettingController.setReaderFontSize(settings.font_size);
                    break;
                }

                case R.id.container_fontsize_6: {
                    settings.font_size = EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[5];
                    mEPub2ReaderSettingController.setReaderFontSize(settings.font_size);
                    break;
                }
            }

            setFontSizeButtonStates(settings);
        }
    };

    private final OnClickListener onClickListenerLineSpacing = new OnClickListener() {

        public void onClick(View v) {
            ReaderSetting settings = mEPub2ReaderSettingController.getReaderSetting();

            switch (v.getId()) {

                case R.id.button_line_space1: {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_SPACING,"1", null);
                    settings.line_space = EPub2ReaderSettingController.READING_SETTING_LINE_SPACES[0];
                    settings.line_space = mEPub2ReaderSettingController.setLineSpace(settings.line_space);
                    break;
                }

                case R.id.button_line_space2: {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_SPACING,"2", null);
                    settings.line_space = EPub2ReaderSettingController.READING_SETTING_LINE_SPACES[1];
                    settings.line_space = mEPub2ReaderSettingController.setLineSpace(settings.line_space);
                    break;
                }

                case R.id.button_line_space3: {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_SPACING,"3", null);
                    settings.line_space = EPub2ReaderSettingController.READING_SETTING_LINE_SPACES[2];
                    settings.line_space = mEPub2ReaderSettingController.setLineSpace(settings.line_space);
                    break;
                }
            }

            setLinespaceButtonStates(settings);
        }
    };

    private final OnClickListener onClickListenerTextAlign = new OnClickListener() {

        public void onClick(View v) {
            ReaderSetting settings = mEPub2ReaderSettingController.getReaderSetting();

            switch (v.getId()) {
                case R.id.button_left_aligned:
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_ALIGNMENT,"Left", null);
                    mEPub2ReaderSettingController.setReaderTextAlign(EPub2ReaderSettingController.READER_SETTING_TEXT_ALIGN_LEFT);
                    break;
                case R.id.button_justified:
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_ALIGNMENT,"Justify", null);
                    mEPub2ReaderSettingController.setReaderTextAlign(EPub2ReaderSettingController.READER_SETTING_TEXT_ALIGN_JUSTIFY);
                    break;
            }

            setTextAlignStates(settings);
        }
    };

    private final OnClickListener onClickListenerPageMargins = new OnClickListener() {

        public void onClick(View v) {
            ReaderSetting settings = mEPub2ReaderSettingController.getReaderSetting();

            switch (v.getId()) {
                case R.id.button_narrow:
                    mEPub2ReaderSettingController.setReaderTextMargin(EPub2ReaderSettingController.READER_SETTING_TEXT_MARGIN_NARROW);
                    break;
                case R.id.button_medium:
                    mEPub2ReaderSettingController.setReaderTextMargin(EPub2ReaderSettingController.READER_SETTING_TEXT_MARGIN_MEDIUM);
                    break;
                case R.id.button_wide:
                    mEPub2ReaderSettingController.setReaderTextMargin(EPub2ReaderSettingController.READER_SETTING_TEXT_MARGIN_WIDE);
                    break;
            }

            setMarginButtonStates(settings);
        }
    };

    @Override
    public void onReaderSettingLoaded(ReaderSetting readerSetting) {

        if (mSeekBarBrightness != null) {
            mSeekBarBrightness.setProgress(SettingValue.BRIGHTNESS.getProgress(readerSetting.brightness));
        }

        for (int i = 0; i < mFonts.length; i++) {

            if (mFonts[i].font_family.equals(readerSetting.font_typeface)) {
                mSpinnerFont.setSelection(i);
                break;
            }
        }

        setTextAlignStates(readerSetting);
        setColorButtonStates(readerSetting);
        setMarginButtonStates(readerSetting);
        setLinespaceButtonStates(readerSetting);
        setFontSizeButtonStates(readerSetting);

        mSwitchPublisherStyles.setChecked(readerSetting.publisher_styles);
    }

    private void setTextAlignStates(ReaderSetting readerSetting) {

        if (mToggleButtonLeftAligned != null) {

            if (EPub2ReaderSettingController.READER_SETTING_TEXT_ALIGN_LEFT.equals(readerSetting.text_align)) {
                mToggleButtonLeftAligned.setActivated(true);
                mToggleButtonJustified.setActivated(false);
            } else {
                mToggleButtonLeftAligned.setActivated(false);
                mToggleButtonJustified.setActivated(true);
            }
        }
    }

    private void setColorButtonStates(ReaderSetting readerSetting) {

        if (readerSetting.background_color == ColorValue.WHITE.background_color) {
            mToggleButtonWhite.setActivated(true);
            mToggleButtonBlack.setActivated(false);
            mToggleButtonSepia.setActivated(false);
        } else if (readerSetting.background_color == ColorValue.BLACK.background_color) {
            mToggleButtonWhite.setActivated(false);
            mToggleButtonBlack.setActivated(true);
            mToggleButtonSepia.setActivated(false);
        } else {
            mToggleButtonWhite.setActivated(false);
            mToggleButtonBlack.setActivated(false);
            mToggleButtonSepia.setActivated(true);
        }
    }

    private void setMarginButtonStates(ReaderSetting readerSetting) {

        if (mToggleButtonWide != null) {

            if (readerSetting.margin_left > BBBApplication.getApplication().getResources().getInteger(R.integer.default_reader_margin)) {
                mToggleButtonWide.setActivated(true);
                mToggleButtonMedium.setActivated(false);
                mToggleButtonNarrow.setActivated(false);
            } else if (readerSetting.margin_left < BBBApplication.getApplication().getResources().getInteger(R.integer.default_reader_margin)) {
                mToggleButtonNarrow.setActivated(true);
                mToggleButtonMedium.setActivated(false);
                mToggleButtonWide.setActivated(false);
            } else {
                mToggleButtonMedium.setActivated(true);
                mToggleButtonWide.setActivated(false);
                mToggleButtonNarrow.setActivated(false);
            }
        }
    }

    private void setLinespaceButtonStates(ReaderSetting readerSetting) {

        if (mToggleButtonLineSpace1 != null) {
            mToggleButtonLineSpace1.setActivated(false);
            mToggleButtonLineSpace2.setActivated(false);
            mToggleButtonLineSpace3.setActivated(false);

            if (readerSetting.line_space <= EPub2ReaderSettingController.READING_SETTING_LINE_SPACES[0]) {
                mToggleButtonLineSpace1.setActivated(true);
            } else if (readerSetting.line_space == EPub2ReaderSettingController.READING_SETTING_LINE_SPACES[1]) {
                mToggleButtonLineSpace2.setActivated(true);
            } else if (readerSetting.line_space >= EPub2ReaderSettingController.READING_SETTING_LINE_SPACES[2]) {
                mToggleButtonLineSpace3.setActivated(true);
            }
        }
    }

    private void setFontSizeButtonStates(ReaderSetting readerSetting) {
        mButtonFontSizeIncrease.setEnabled(true);
        mButtonFontSizeDecrease.setEnabled(true);

        if (mImageViewFontSize != null && mImageViewFontSize[0] != null) {
            for (int i = 0; i < EPub2ReaderSettingController.READING_SETTING_FONT_SIZE.length; i++) {
                if (readerSetting.font_size < EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[i]) {
                    mImageViewFontSize[i].setSelected(false);
                } else if (readerSetting.font_size == EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[i]) {
                    mImageViewFontSize[i].setSelected(true);
                } else if (i < EPub2ReaderSettingController.READING_SETTING_FONT_SIZE.length - 1 && readerSetting.font_size < EPub2ReaderSettingController.READING_SETTING_FONT_SIZE[i + 1]) {
                    mImageViewFontSize[i].setSelected(true);
                } else {
                    mImageViewFontSize[i].setSelected(false);
                }
            }
        }

        if (readerSetting.font_size <= SettingValue.FONT_SIZE.minValue) {
            mButtonFontSizeDecrease.setEnabled(false);
        } else if (readerSetting.font_size >= SettingValue.FONT_SIZE.maxValue) {
            mButtonFontSizeIncrease.setEnabled(false);
        }

        if (mTextViewFontSize != null) {
            float range = SettingValue.FONT_SIZE.maxValue - SettingValue.FONT_SIZE.minValue;
            float percentage = ((readerSetting.font_size - SettingValue.FONT_SIZE.minValue) / range) * 100;

            mTextViewFontSize.setText(String.format(mFontSizeFormat,percentage));
        }
    }
}