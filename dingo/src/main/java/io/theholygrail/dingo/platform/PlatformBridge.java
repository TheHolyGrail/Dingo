package io.theholygrail.dingo.platform;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.JavascriptInterface;

import io.theholygrail.dingo.JsonTransformer;
import io.theholygrail.jsbridge.JSValue;
import io.theholygrail.jsbridge.JSWebView;

/**
 * Exposed methods in the main platform namespace
 *
 */
public class PlatformBridge {
    private static final String TAG = PlatformBridge.class.getSimpleName();
    public static final String NAMESPACE = "platform";

    private Context mContext;
    private JSWebView mWebView;
    private Handler mHandler;
    private JsonTransformer mJsonTransformer;

    public PlatformBridge(Context context, JSWebView webView, JsonTransformer transformer) {
        mContext = context;
        mWebView = webView;
        mJsonTransformer = transformer;
        mHandler = new Handler();
    }

    /**
     * Provides information that identifies the device and platform.
     *
     * @param callback The object containing the device and platform info
     */
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void info(String callback) {
        Log.d(TAG, "info()");

        final JSValue callbackValue = new JSValue(callback);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String appVersion = "Unknown";
                String model = Build.MODEL;
                String os = String.valueOf(Build.VERSION.SDK_INT);
                try {
                    appVersion = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "Failed to get appversion: ", e);
                }

                if (callbackValue.isFunction()) {
                    Info info = new Info();
                    info.appVersion = appVersion;
                    info.platform = os;
                    info.device = model;
                    Object args[] = {mJsonTransformer.toJson(info)};
                    callbackValue.callFunction(mWebView, args, null);
                }
            }
        });
    }

    /**
     * Shows a dialogSets the title for the current page.
     * @param param The new title
     */
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void dialog(String param, String callback) {

        final DialogData dialogData = mJsonTransformer.fromJson(param, DialogData.class);
        final JSValue callbackValue = new JSValue(callback);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext)
                        .setTitle(dialogData.title)
                        .setMessage(dialogData.message);
                boolean showDialog = false;
                boolean positiveButtonSet = false;
                for (final DialogData.Action action : dialogData.actions) {
                    if ("cancel".equalsIgnoreCase(action.id)) {
                        builder.setNegativeButton(action.label, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "negative button clicked!");
                                sendToJs("", action.id);
                            }
                        });
                        showDialog = true;
                    } else if (!TextUtils.isEmpty(action.id)) {
                        DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Log.i(TAG, action.id + " button clicked!");
                                sendToJs("", action.id);
                            }
                        };

                        if (positiveButtonSet) {
                            builder.setPositiveButton(action.label, clickListener);
                        } else {
                            builder.setNeutralButton(action.label, clickListener);
                            positiveButtonSet = true;
                        }

                        showDialog = true;
                    }
                }

                if (showDialog) {
                    builder.show();
                } else {
                    sendToJs("Could not create dialog", "");
                    Log.w(TAG, "Could not create Dialog!");
                }
            }

            private void sendToJs(String error, String id) {
                Log.d(TAG, "error: " + error + " id: " + id);
                if (callbackValue.isFunction()) {
                    Object args[] = {error, new JSValue(id)};
                    callbackValue.callFunction(mWebView, args, null);
                }
            }
        });
    }

    /**
     * Simple sharing of a message, and an URL, using the platforms built in sharing mechanism.
     *
     * @param options Object containing the message, and url to share.
     */
    @SuppressWarnings("unused")
    @JavascriptInterface
    public void share(String options) {
        ShareData shareData = mJsonTransformer.fromJson(options, ShareData.class);

        final String message = shareData.message;
        String url = shareData.url;

        final Intent share = new Intent(android.content.Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, message);
        share.putExtra(Intent.EXTRA_TEXT, url);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mContext.startActivity(Intent.createChooser(share, message));
            }
        });
    }
}