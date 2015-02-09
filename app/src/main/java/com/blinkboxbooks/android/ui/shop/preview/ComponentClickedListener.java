// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.shop.preview;

import com.blinkboxbooks.android.model.ShopItem;

/**
 * Interface for listening for button clicks on preview sub views
 */
public interface ComponentClickedListener {

    /**
     * back button on author fragment was pressed
     */
    public void backFromAuthorPressed();

    /**
     * author name on shop item preview was pressed
     */
    public void authorPressed();

    /**
     * A related book on the author preview was clicked
     */
    public void relatedBookClicked(ShopItem shopItem);

    /**
     * A promoted book on feature page was clicked
     */
    public void promotedBookClicked(ShopItem shopItem);
}
