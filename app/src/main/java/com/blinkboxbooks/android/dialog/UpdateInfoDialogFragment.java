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
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;

/**
 * A DialogFragment for displaying whats new information to list one or more features that are present in a new release.
 */
public class UpdateInfoDialogFragment extends DialogFragment {

    private static final String ARG_UPDATE_INFO = "update_info";
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
     * @param updates a list of strings that describe the update
     * @param width set to a specific value in pixels to force a particular width (or 0 to use default)
     * @return a RadioOptionsDialogFragment
     */
    public static UpdateInfoDialogFragment newInstance(String[] updates, int width) {
        UpdateInfoDialogFragment fragment = new UpdateInfoDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putStringArray(ARG_UPDATE_INFO, updates);
        arguments.putInt(ARG_WIDTH, width);
        fragment.setArguments(arguments);
        return fragment;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);

        View layout = View.inflate(getActivity(), R.layout.fragment_dialog_update_info, null);
        Button closeButton = (Button) layout.findViewById(R.id.button_update_info_close);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        String[] updateInfo = (String[]) getArguments().get(ARG_UPDATE_INFO);
        ListView list = (ListView) layout.findViewById(R.id.fragment_dialog_update_info_list);
        int width = getArguments().getInt(ARG_WIDTH);

        ArrayAdapter<String> adapter = new UpdateInfoAdapter(getActivity(), updateInfo);
        list.setAdapter(adapter);
        dialog.setContentView(layout);

        int widthToUse = (width == 0) ? WindowManager.LayoutParams.WRAP_CONTENT : width;
        dialog.getWindow().setLayout(widthToUse, WindowManager.LayoutParams.WRAP_CONTENT);
        return dialog;
    }

    private class UpdateInfoAdapter extends ArrayAdapter<String> {

        public UpdateInfoAdapter(Context context, String[] options) {
            super(context, R.layout.list_item_update_info, options);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = View.inflate(getContext(), R.layout.list_item_update_info, null);
                holder.updateText = (TextView) convertView.findViewById(R.id.list_item_update_info_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final String optionText = getItem(position);
            holder.updateText.setText(optionText);

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView updateText;
    }
}
