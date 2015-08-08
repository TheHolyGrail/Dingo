package io.theholygrail.dingo.navigationbar;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;

import io.theholygrail.jsbridge.JsValueUtil;
import io.theholygrail.dingo.JsonTransformer;
import io.theholygrail.dingo.navigation.NavigationBridge;
import io.theholygrail.jsbridge.JSValue;
import io.theholygrail.jsbridge.JSWebView;

/**
 * Contains the Javascript accessible methods in the navigationBar namespace.
 */
public class NavigationBarBridge {
    private static final String TAG = NavigationBridge.class.getSimpleName();
    public static final String NAMESPACE = "navigationBar";

    JSWebView mWebView;
    Handler mHandler;
    NavigationBarBridgeCallback mCallback;
    JsonTransformer mTransformer;
    Context mContext;

    public NavigationBarBridge(JSWebView webView, JsonTransformer transformer, NavigationBarBridgeCallback callback) {
        mWebView = webView;
        mContext = mWebView.getContext();
        mCallback = callback;
        mTransformer = transformer;
        mHandler = new Handler();
    }

    /**
     * Sets the title for the current page.
     * Example: <pre><code>NativeBridge.navigationBar.setTitle("The Dingo Ate My Title");</code></pre>
     *
     * @param title The new title
     */
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void setTitle(String title) {
        Log.d(TAG, "setTitle(" + title + ")");

        final String titleText = JsValueUtil.parseQuotes(title);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCallback.setTitle(titleText);
            }
        });
    }

    /**
     * Set the navigation bar's buttons with an array of buttons.
     *
     * Example: <pre><code>
     * var buttons = [{
     *    title: "Cancel",
     *    id: "cancel"
     *  }, {
     *    title: "Done",
     *    id: "done"
     *  }];
     *
     *  window.NativeBridge.navigationBar.setButtons(buttons, function (buttonID) {
     *    // handle button tap
     *  });
     * </code></pre>
     *
     * @param param The buttons
     * @param callback callback function when a button is clicked
     */
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void setButtons(String param, String callback) {
        Log.d(TAG, "setButtons: " + param);

        final Button[] buttons = mTransformer.fromJson(param, Button[].class);
        final JSValue callbackValue = new JSValue(callback);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (mCallback != null) {
                    NavigationBarBridgeCallback.OnClickListener clickListener = null;
                    if (buttons != null) {
                        clickListener = new NavigationBarBridgeCallback.OnClickListener() {

                            @Override
                            public void onClick(String id) {
                                if (callbackValue.isFunction()) {
                                    Object args[] = {id};
                                    callbackValue.callFunction(mWebView, args, null);
                                }
                            }
                        };
                    }

                    mCallback.setButtons(buttons, clickListener);
                }
            }
        });
    }
}