package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.blinkboxbooks.android.R;

/**
 * A DialogFragment for displaying a list of options with radio buttons to indicate the currently
 * selected option.
 */
public class RadioOptionsDialogFragment extends DialogFragment {

    private static final String ARG_OPTIONS = "options";
    private static final String ARG_TITLE = "title";
    private static final String ARG_SELECTED_INDEX = "selected_index";
    private static final String ARG_WIDTH = "width";

    private OptionSelectedListener mOptionSelectedListener;

    /**
     * Interface that a class can implement to listen for the user selecting an option
     */
    public interface OptionSelectedListener {

        /**
         * Callback when the user selects an option
         * @param selectionIndex the index that was selected
         * @param option the option text that was selected
         */
        public void onOptionSelected(int selectionIndex, String option);
    }

    /**
     * Create a new instance of a RadioOptionsDialogFragment
     * @param title the title of the dialog
     * @param options the array of options
     * @param selectedIndex the initial selected index
     * @param width set to a specific value in pixels to force a particular width (or 0 to use default)
     * @return a RadioOptionsDialogFragment
     */
    public static RadioOptionsDialogFragment newInstance(String title, String[] options, int selectedIndex, int width) {
        RadioOptionsDialogFragment fragment = new RadioOptionsDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_TITLE, title);
        arguments.putStringArray(ARG_OPTIONS, options);
        arguments.putInt(ARG_SELECTED_INDEX, selectedIndex);
        arguments.putInt(ARG_WIDTH, width);
        fragment.setArguments(arguments);
        return fragment;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View layout = View.inflate(getActivity(), R.layout.fragment_dialog_radio_options, null);
        TextView titleView = (TextView) layout.findViewById(R.id.fragment_dialog_radio_options_title);

        String title = getArguments().getString(ARG_TITLE);
        String[] options = getArguments().getStringArray(ARG_OPTIONS);
        int selectedIndex = getArguments().getInt(ARG_SELECTED_INDEX);
        int width = getArguments().getInt(ARG_WIDTH);

        titleView.setText(title);

        ListView list = (ListView) layout.findViewById(R.id.fragment_dialog_radio_options_list);

        ArrayAdapter<String> adapter = new LibrarySortOptionsAdapter(getActivity(), options, selectedIndex);
        list.setAdapter(adapter);

        dialog.setContentView(layout);

        int widthToUse = (width == 0) ? WindowManager.LayoutParams.WRAP_CONTENT : width;
        dialog.getWindow().setLayout(widthToUse, WindowManager.LayoutParams.WRAP_CONTENT);

        return dialog;
    }

    /**
     * Set the option selected listener to receive callbacks when the user clicks on an option
     * @param listener the listener to set
     */
    public void setOptionSelectedListener(OptionSelectedListener listener) {
        mOptionSelectedListener = listener;
    }

    class LibrarySortOptionsAdapter extends ArrayAdapter<String> {

        private final int padding;
        private int selectedIndex;

        public LibrarySortOptionsAdapter(Context context, String[] options, int initialSelection) {
            super(context, R.layout.list_item_radio_option, options);
            padding = context.getResources().getDimensionPixelOffset(R.dimen.gap_medium);
            selectedIndex = initialSelection;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(getContext(), R.layout.list_item_radio_option, null);
                holder.radioButton = (RadioButton) convertView.findViewById(R.id.list_item_radio_button);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final String optionText = getItem(position);
            holder.radioButton.setText(getItem(position));
            holder.radioButton.setChecked(selectedIndex == position);

            holder.radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOptionSelectedListener != null) {
                        mOptionSelectedListener.onOptionSelected(position, optionText);
                    }
                    dismiss();
                }
            });

            return convertView;
        }
    }

    private static class ViewHolder {
        RadioButton radioButton;
    }
}
