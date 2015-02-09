// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import com.blinkbox.java.book.json.BBBTocItem;

import java.util.ArrayList;

/**
 * Util for dealing with epub files
 */
public class EPubUtil {

    /**
     * flattens TOC into list
     *
     * @param depth
     * @param tocs
     */
    public static ArrayList<BBBTocItem> flattenNavigationTree(int depth, ArrayList<BBBTocItem> tocs, ArrayList<BBBTocItem> all) {

        if (tocs == null) {
            return null;
        }

        if(all == null) {
            all = new ArrayList<BBBTocItem>(tocs.size());
        }

        for (BBBTocItem toc : tocs) {

            if (toc != null) {
                all.add(toc);

                if(toc.children != null) {
                    flattenNavigationTree(depth + 1, toc.children, all);
                }
            }
        }

        return all;
    }
}
