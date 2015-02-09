// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model;

import java.io.Serializable;

/**
 * Bookmark POJO
 */
@SuppressWarnings("serial")
public class Bookmark implements Serializable {

    public long id;
    public String cloud_id;
    public long book_id;
    public String name;
    public String content;
    public int type;
    public String annotation;
    public String style;
    public String update_by;
    public long update_date;
    public int percentage;
    public int state;
    public String color;
    public String isbn;
    public String position;
}