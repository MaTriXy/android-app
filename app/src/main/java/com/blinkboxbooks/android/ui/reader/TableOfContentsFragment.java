// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.blinkbox.java.book.json.BBBTocItem;
import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.list.ContentsListAdapter;
import com.blinkboxbooks.android.loader.BookFileLoader;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.util.DialogUtil;
import com.blinkboxbooks.android.util.StringUtils;

import java.util.LinkedList;
import java.util.List;

/**
 * Fragment that will be overlayed on the Reader Activity to display the books table of contents
 */
public class TableOfContentsFragment extends Fragment {

    public static class TOCEntry {
        public String href;
        public String label;
        public boolean currentPosition;
        public boolean active;
        public int depth;
    }

    // The container Activity must implement this interface so the fragment can deliver messages
    public interface TableOfContentsListener {

        /**
         * Get the current book
         */
        public Book getBook();

        /**
         * Called when a table of contents item is selected
         */
        public void onTableOfContentsItemSelected(String url, boolean hasLastReadingPosition);
    }

    private TableOfContentsListener mTableOfContentsListener;
    private Book mBook;
    private boolean mHasLastPosition;
    private List<TOCEntry> mTableOfContents;

    private TextView mTextViewError;
    private ProgressBar mProgressBar;
    private ListView mListView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tableofcontents, container, false);

        mTextViewError = (TextView)view.findViewById(R.id.textview_error);
        mProgressBar = (ProgressBar)view.findViewById(R.id.progress_loading);
        mListView = (ListView)view.findViewById(R.id.listview);

        mListView.setOnItemClickListener(mItemClickListener);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mTableOfContentsListener = (TableOfContentsListener) getActivity();
        mBook = mTableOfContentsListener.getBook();

        final TextView header = (TextView) View.inflate(getActivity(), R.layout.list_item_table_of_contents_header, null);
        if (mBook.author != null) {
            header.setText(StringUtils.formatTitleAuthor(getResources(), mBook.title, mBook.author, false));
        } else {
            header.setText(mBook.title);
        }
        mListView.addHeaderView(header, null, false);

        new AsyncTask<Void, Void, List<TOCEntry>>() {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                mProgressBar.setVisibility(View.VISIBLE);
                mListView.setVisibility(View.GONE);
                mTextViewError.setVisibility(View.GONE);
            }

            @Override
            protected List<TOCEntry> doInBackground(Void... params) {
                Bookmark lastPosition = BookmarkHelper.getBookmark(BBBContract.Bookmarks.TYPE_LAST_POSITION, mBook.id);
                mHasLastPosition = (lastPosition != null);

                BBBEPubBook epub = BookFileLoader.loadBook(getActivity(), mBook);

                List<TOCEntry> tocList = new LinkedList<TOCEntry>();
                if (epub != null && epub.getBookInfo() != null) {
                    List<BBBTocItem> nodeTree = epub.getBookInfo().getToc();
                    flattenNavigationTree(0, tocList, nodeTree);
                }
                return tocList;
            }

            @Override
            protected void onPostExecute(List<TOCEntry> result) {
                mProgressBar.setVisibility(View.GONE);

                // The activity can potentially be null at this point, so we just ignore the post execute in this case to avoid possible crashes.
                if (getActivity() != null) {
                    if (result != null && result.size() > 0) {
                        mListView.setVisibility(View.VISIBLE);

                        mTableOfContents = result;
                        ListAdapter listAdapter = new ContentsListAdapter(getActivity(), R.layout.list_item_table_of_contents, result);
                        mListView.setAdapter(listAdapter);
                    } else {
                        mTextViewError.setVisibility(View.VISIBLE);
                    }
                }
            }
        }.execute();
    }

    /**
     * Preorder tree travesal
     *
     * @param depth
     * @param tocList
     * @param tocs
     */
    private static void flattenNavigationTree(int depth, List<TOCEntry> tocList, List<BBBTocItem> tocs) {

        if (tocs == null) {
            return;
        }

        for (BBBTocItem toc : tocs) {

            if (toc != null) {
                TOCEntry entry = new TOCEntry();
                entry.label = toc.label;
                entry.href = toc.href;
                entry.active = toc.active;
                entry.depth = depth;
                tocList.add(entry);

                if(toc.children != null) {
                    flattenNavigationTree(depth + 1, tocList, toc.children);
                }
            }
        }
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            TOCEntry entry = null;

            int index = position - 1;

            if (index >= 0 && index < mTableOfContents.size()) {
                entry = mTableOfContents.get(index);
            }

            if (entry != null && entry.active) {
                String url = entry.href;
                mTableOfContentsListener.onTableOfContentsItemSelected(url, mHasLastPosition);
            } else {
                DialogUtil.showBookNotPartOfSampleDialog(getActivity(), mBook);
            }
        }
    };
}