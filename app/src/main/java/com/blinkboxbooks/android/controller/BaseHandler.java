// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.util.LogUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An abstract base class which performs HTTP transaction marshalling enabling
 * us to register a callback with an HTTP request which will be called
 * when the HTTP response for that specific HTTP request is received.
 * If a second request is received for the same URL, then we don't execute a second
 * request, we simply register it to also receive a callback once we receive a
 * response to the initial request.
 *
 * @param <TYPE> The type of data being returned in the response.
 */
public abstract class BaseHandler<TYPE> extends BBBBasicResponseHandler<TYPE> {
    private static Map<String, List<Callback<?>>> sRequests = new HashMap<String, List<Callback<?>>>();
    private static final String TAG = BaseHandler.class.getSimpleName();
    private final static Map<Controller, Set<String>> sControllerMap = new HashMap<Controller, Set<String>>();

    public interface Controller {
        public void handleError(BBBResponse response);
    }

    /**
     * The Callback interface which defines the methods that get called once the
     * HTTP transaction completes.
     *
     * @param <TYPE>
     */
    public static abstract class Callback<TYPE> {

        private Callback() {
            this(null);
        }

        public Callback(Controller controller) {
            setController(controller);
        }

        public Controller getController() {
            return mController == null ? null : mController.get();
        }

        public void setController(Controller controller) {
            this.mController = controller == null ? null : new WeakReference<Controller>(controller);
        }

        private WeakReference<Controller> mController = null;

        /**
         * Success!
         *
         * @param type The payload from the HTTP response
         */
        public abstract void update(TYPE type);

        public void error(BBBResponse response) {
            Controller controller = mController == null ? null : mController.get();
            if (controller != null) {
                controller.handleError(response);
            }
        }
    }

    /**
     * Executes a request for the specified handler ID, with a Callback instance which will be
     * called once the transaction completes. This will not actually initiate a new HTTP request
     * if one is already active for the specific URL in the request, we may simply get a callback
     * when an existing request for the required content returns.
     *
     * @param request   The new request
     * @param handlerId The handler ID to use
     * @param callback  This object will; receive a callback once the transaction completes
     */
    public void executeRequest(BBBRequest request, String handlerId, Controller controller, Callback<TYPE> callback) {
        String id = getId(request);
        List<Callback<?>> callbacks = sRequests.get(id);
        if (callbacks == null) {
            callbacks = new ArrayList<Callback<?>>();
            executeRequest(handlerId, request);
            sRequests.put(id, callbacks);
        }
        callbacks.add(callback);
        putUrl(controller, request.getUrl());
    }

    @Override
    public void receivedData(BBBResponse response, TYPE type) {
        BBBRequest request = response == null ? null : response.getRequest();
        if (request != null) {
            if (response.getResponseCode() == 200 && validate(type)) {
                updateAll(request, type);
            } else {
                updateAll(request, null);
            }
        }
    }

    private void updateAll(BBBRequest request, TYPE type) {
        List<? extends Callback> callbacks = sRequests.remove(getId(request));
        if (callbacks != null) {
            for (Callback<TYPE> callback : callbacks) {
                Controller controller = callback.getController();
                if (controller != null && hasUrl(controller, request.getUrl())) {
                    callback.update(type);
                }
            }
        }
    }

    @Override
    public void receivedError(BBBResponse response) {
        if (response != null) {
            BBBRequest request = response.getRequest();
            if (request != null) {
                List<? extends Callback> callbacks = sRequests.remove(getId(request));
                for (Callback<TYPE> callback : callbacks) {
                    Controller controller = callback.getController();
                    if (controller != null && hasUrl(controller, request.getUrl())) {
                        callback.error(response);
                    }
                }
            }
            LogUtils.e(TAG, "Unexpected response: " + response.getResponseData());
        }
    }

    private void putUrl(Controller controller, String url) {
        Set<String> urls = sControllerMap.get(controller);
        if (urls == null) {
            urls = new HashSet<String>();
            sControllerMap.put(controller, urls);
        }
        urls.add(url);
    }

    private boolean hasUrl(Controller controller, String url) {
        boolean hasUrl = false;
        Set<String> urls = sControllerMap.get(controller);
        if (urls != null) {
            if (urls.contains(url)) {
                hasUrl = true;
                urls.remove(url);
                if (urls.isEmpty()) {
                    sControllerMap.remove(controller);
                }
            }
        }
        return hasUrl;
    }

    public static void resetController(Controller controller) {
        sControllerMap.remove(controller);
    }

    protected void executeRequest(String handlerId, BBBRequest request) {
        BBBRequestManager.getInstance().executeRequest(handlerId, request);
    }

    protected abstract boolean validate(TYPE type);

    protected String getId(BBBRequest request) {
        return request == null ? null : request.getUrl();
    }
}

