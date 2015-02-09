package com.blinkboxbooks.android.model.reader;

/**
 * Created by tomh on 16/12/14.
 */
public class HighlightAddedEvent {
    public int code;
    public String message;
    public String call;
    public String CFI;
    public String preview;
    public String chapter;
    public String href;

    public Event createEvent() {
        Event event = new Event();
        event.code = code;
        event.message = message;
        event.call = call;
        event.cfi = new CFI();
        event.cfi.CFI = CFI;
        event.cfi.preview = "";
        event.preview = preview;
        event.href = href;
        return event;
    }
}
