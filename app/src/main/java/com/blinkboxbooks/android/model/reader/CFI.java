// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model.reader;

/**
 * POJO for CFI object returned by cross platform library
 */
public class CFI {

    /**
     * the CFI string
     */
    public String CFI;

    /**
     * a preview of the contents the CFI points to
     */
    public String preview;

    /**
     * the current chapter
     */
    public String chapter;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        builder.append("cfi: ");
        builder.append(CFI);
        builder.append(' ');
        builder.append("preview: ");
        builder.append(preview);

        return builder.toString();
    }
}
