package com.blinkboxbooks.android.model.reader;

/**
 * Created by tomh on 16/12/14.
 */
public class HighlightClickedEvent {
    public int code;
    public String message;
    public String call;
    public String cfi;
    public float clientX;
    public float clientY;

    public Event createEvent() {
        Event event = new Event();
        event.code = code;
        event.message = message;
        event.call = call;
        event.cfi = new CFI();
        event.cfi.CFI = cfi;
        event.cfi.preview = "";
        event.clientX = clientX;
        event.clientY = clientY;
        return event;
    }
}
