// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.Context;
import android.content.res.XmlResourceParser;

import com.blinkboxbooks.android.model.MenuSpecItem;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is helper function for loading config from XML resources
 */
public class MainMenuLoader {
    /**
     * Load a menu specification from a config file in the resources
     *
     * @param context
     * @param configXmlId
     * @return
     */
    public static List<MenuSpecItem> loadMainMenu(final Context context, int configXmlId) {
        XmlResourceParser parser = context.getResources().getXml(configXmlId);
        try {
            while (parser.getEventType() != XmlPullParser.END_DOCUMENT) {
                if (parser.getEventType() == XmlPullParser.START_TAG) {
                    String itemName = parser.getName();
                    if ("main_menu".equals(itemName)) {
                        final List<MenuSpecItem> menuSpecItemList = new ArrayList<MenuSpecItem>();
                        iterateOverArray(parser, "main_menu", new ItemParser() {
                            @Override
                            public void parseItem(XmlResourceParser parser) {
                                MenuSpecItem item = new MenuSpecItem();
                                item.tag = parser.getAttributeValue(null, "tag");
                                item.header = parser.getAttributeBooleanValue(null, "header", false);
                                item.additional = parser.getAttributeValue(null, "additional");
                                item.titleResourceId = getResId("title", parser);
                                item.iconResourceId = parser.getAttributeResourceValue(null, "drawable", 0);
                                item.actionUri = parser.getAttributeValue(null, "actionUri");
                                item.isDependentOnNetwork = parser.getAttributeBooleanValue(null, "isDependentOnNetwork", false);
                                item.isVisibleSignedIn = parser.getAttributeBooleanValue(null, "isVisibleSignedIn", true);
                                item.isVisibleSignedOut = parser.getAttributeBooleanValue(null, "isVisibleSignedOut", true);
                                item.isMyDevice = parser.getAttributeBooleanValue(null, "isMyDevice", false);
                                menuSpecItemList.add(item);
                            }
                        });
                        return menuSpecItemList;
                    }
                }
                parser.next();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected config file error.", ex);
        }
        return null;
    }

    private static int getResId(String key, XmlResourceParser parser) {
        int resId = parser.getAttributeResourceValue(null, key, 0);
        if (resId == 0) {
            throw new IllegalStateException("Error in config files. Can't find resource  [" + parser.getAttributeValue(null, key) + "].");
        }
        return resId;
    }

    private interface ItemParser {
        /**
         * This function will be invoked for every <item> in an array
         *
         * @param parser
         */
        void parseItem(XmlResourceParser parser);
    }

    /**
     * Iterate over an xml array and invoke the {@link ItemParser} interface for every item found.
     *
     * @param parser
     * @param tagName
     * @param itemParser
     * @throws XmlPullParserException
     * @throws IOException
     */
    private static void iterateOverArray(XmlResourceParser parser, String tagName, ItemParser itemParser) throws XmlPullParserException, IOException {
        while (!(parser.getEventType() == XmlPullParser.END_TAG && tagName.equals(parser.getName()))) {
            if (parser.getEventType() == XmlPullParser.START_TAG && "item".equals(parser.getName())) {
                itemParser.parseItem(parser);
            }
            parser.next();
        }
    }
}
