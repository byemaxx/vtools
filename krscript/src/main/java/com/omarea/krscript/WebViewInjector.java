package com.omarea.krscript;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import com.omarea.common.shell.KeepShellPublic;
import com.omarea.common.shell.ShellExecutor;
import com.omarea.common.ui.DialogHelper;
import com.omarea.krscript.downloader.Downloader;
import com.omarea.krscript.executor.ExtractAssets;
import com.omarea.krscript.executor.ScriptEnvironmen;
import com.omarea.krscript.model.NodeInfoBase;
import com.omarea.krscript.model.ShellHandlerBase;
import com.omarea.krscript.ui.ParamsFileChooserRender;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public class WebViewInjector {
    private static final java.util.Map<WebView, java.util.concurrent.atomic.AtomicInteger> documents = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private final java.util.concurrent.atomic.AtomicInteger document = new java.util.concurrent.atomic.AtomicInteger();

    public static void onNavigation(WebView view) {
        java.util.concurrent.atomic.AtomicInteger generation = documents.get(view);
        if (generation != null) generation.incrementAndGet();
    }

    private WebView webView;
    private Context context;
    private Activity activity;
    private ParamsFileChooserRender.FileChooserInterface fileChooser;

    @SuppressLint("SetJavaScriptEnabled")
    public WebViewInjector(WebView webView, ParamsFileChooserRender.FileChooserInterface fileChooser) {
        this.webView = webView;
        documents.put(webView, document);
        this.context = webView.getContext();
        this.fileChooser = fileChooser;
    }

    @SuppressLint({"JavascriptInterface", "SetJavaScriptEnabled"})
    public void inject(final Activity activity, final boolean credible) {
        this.activity = activity;
        if (webView != null) {

            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setAllowFileAccess(credible);
            webSettings.setAllowUniversalAccessFromFileURLs(false);
            webSettings.setAllowFileAccessFromFileURLs(false);
            webSettings.setAllowContentAccess(true);
            webSettings.setUseWideViewPort(true);

            webView.addJavascriptInterface(
                    new KrScriptEngine(context),
                    "KrScriptCore" // 由于类名会被混淆，写死吧... KrScriptEngine.class.getSimpleName()
            );
            webView.setDownloadListener(new DownloadListener() {
                @Override
                public void onDownloadStart(final String url, String userAgent, final String contentDisposition, final String mimetype, long contentLength) {
                    if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                        Toast.makeText(context, R.string.kr_write_external_storage, Toast.LENGTH_LONG).show();
                    } else {
                        DialogHelper.Companion.animDialog(new AlertDialog.Builder(context)
                                .setTitle(R.string.kr_download_confirm)
                                .setMessage("" + url + "\n\n" + mimetype + "\n" + contentLength + "Bytes")
                                .setPositiveButton(R.string.btn_confirm, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    new Downloader(context, null).downloadBySystem(url, contentDisposition, mimetype, UUID.randomUUID().toString(), null);
                                    }
                                })
                                .setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                })).setCancelable(false);
                    }
                }
            });
        }
    }

    private static boolean validCallback(String callback) {
        return callback != null && callback.matches("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    }

    // Javascript interfaces also receive requests from embedded frames: never grant a whole page root access.
    private boolean confirmScript(String script) {
        final int requestedDocument = document.get();
        java.util.concurrent.CountDownLatch answered = new java.util.concurrent.CountDownLatch(1);
        java.util.concurrent.atomic.AtomicBoolean allowed = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<AlertDialog> dialogRef = new java.util.concurrent.atomic.AtomicReference<>();
        activity.runOnUiThread(() -> {
            if (activity.isFinishing() || activity.isDestroyed() || requestedDocument != document.get()) {
                answered.countDown();
                return;
            }
            AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.kr_root_request_title)
                .setMessage(activity.getString(R.string.kr_root_request_message) + "\n\n" + webView.getUrl() + "\n\n" + script)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (ignored, which) -> allowed.set(true))
                .create();
            dialog.setOnDismissListener(ignored -> answered.countDown());
            dialogRef.set(dialog);
            dialog.show();
        });
        try {
            if (!answered.await(60, java.util.concurrent.TimeUnit.SECONDS)) return false;
            return allowed.get() && requestedDocument == document.get() && !activity.isFinishing() && !activity.isDestroyed();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            activity.runOnUiThread(() -> {
                AlertDialog dialog = dialogRef.get();
                if (dialog != null) dialog.dismiss();
            });
        }
    }

    private class KrScriptEngine {
        private final Context context;
        private final NodeInfoBase virtualRootNode = new NodeInfoBase("");

        private KrScriptEngine(Context context) {
            this.context = context;
        }

        /**
         * 检查是否具有ROOT权限
         *
         * @return
         */
        @JavascriptInterface
        public boolean rootCheck() {
            return confirmScript("Check root access (su)") && KeepShellPublic.INSTANCE.checkRoot();
        }

        /**
         * 同步执行shell脚本 并返回结果（不包含错误信息）
         *
         * @param script 脚本内容
         * @return 执行过程中的输出内容
         */
        @JavascriptInterface
        public String executeShell(String script) {
            if (script != null && !script.isEmpty() && confirmScript(script)) {
                return ScriptEnvironmen.executeResultRoot(context, script, virtualRootNode);
            }
            return "";
        }

        /**
         * @param script
         * @param callbackFunction
         */
        @JavascriptInterface
        public boolean executeShellAsync(String script, String callbackFunction, String env) {
            if (script == null || script.isEmpty() || !validCallback(callbackFunction) || !confirmScript(script + "\n\nEnvironment:\n" + env)) return false;
            HashMap<String, String> params = new HashMap<>();
            Process process = null;
            try {
                if (env != null && !env.isEmpty()) {
                    JSONObject paramsObject = new JSONObject(env);
                    for (Iterator<String> it = paramsObject.keys(); it.hasNext(); ) {
                        String key = it.next();
                        params.put(key, paramsObject.getString(key));
                    }
                }
                process = ShellExecutor.getSuperUserRuntime();
            } catch (Exception ex) {
                Toast.makeText(context, ex.getMessage(), Toast.LENGTH_SHORT).show();
            }

            if (process != null) {
                final OutputStream outputStream = process.getOutputStream();
                final DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                setHandler(process, callbackFunction, new Runnable() {
                    @Override
                    public void run() {
                    }
                });

                ScriptEnvironmen.executeShell(context, dataOutputStream, script, params, null, null);
                return true;
            } else {
                return false;
            }
        }

        /**
         * 提取assets中的文件
         *
         * @param assets 要提取的文件
         * @return 提取成功后所在的目录
         */
        @JavascriptInterface
        public String extractAssets(String assets) {
            if (assets == null || assets.contains("..") || assets.contains("\\") || assets.startsWith("/")) return null;
            return new ExtractAssets(context).extractResource(assets);
        }

        @JavascriptInterface
        public boolean fileChooser(final String callbackFunction) {
            final int requestedDocument = document.get();
            if (!validCallback(callbackFunction)) return false;
            if (fileChooser != null) {
                return fileChooser.openFileChooser(new ParamsFileChooserRender.FileSelectedInterface() {
                    @Override
                    public int type() {
                        return ParamsFileChooserRender.FileSelectedInterface.Companion.getTYPE_FILE(); // TODO
                    }

                    @Nullable
                    @Override
                    public String suffix() {
                        return null; // TODO
                    }

                    @NotNull
                    @Override
                    public String mimeType() {
                        return "*/*"; // TODO
                    }

                    @Override
                    public void onFileSelected(@Nullable String path) {
                        try {
                            final JSONObject message = new JSONObject();
                            if (path == null || path.isEmpty()) {
                                message.put("absPath", null);
                            } else {
                                message.put("absPath", path);
                            }
                            webView.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (requestedDocument != document.get() || activity.isDestroyed()) return;
                                    webView.evaluateJavascript(callbackFunction + "(" + message.toString() + ")", new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {
                                        }
                                    });
                                }
                            });
                        } catch (Exception ex) {
                        }
                    }
                });
            }
            return false;
        }

        private void setHandler(Process process, final String callbackFunction, final Runnable onExit) {
            final int requestedDocument = document.get();
            final InputStream inputStream = process.getInputStream();
            final InputStream errorStream = process.getErrorStream();
            final Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                        while ((line = bufferedReader.readLine()) != null) {
                            try {
                                final JSONObject message = new JSONObject();
                                message.put("type", ShellHandlerBase.EVENT_REDE);
                                message.put("message", line + "\n");
                                webView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (requestedDocument != document.get() || activity.isDestroyed()) return;
                                    webView.evaluateJavascript(callbackFunction + "(" + message.toString() + ")", new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String value) {

                                            }
                                        });
                                    }
                                });
                            } catch (Exception ex) {
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            final Thread readerError = new Thread(new Runnable() {
                @Override
                public void run() {
                    String line;
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(errorStream, "UTF-8"));
                        while ((line = bufferedReader.readLine()) != null) {
                            try {
                                final JSONObject message = new JSONObject();
                                message.put("type", ShellHandlerBase.EVENT_READ_ERROR);
                                message.put("message", line + "\n");
                                webView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (requestedDocument != document.get() || activity.isDestroyed()) return;
                                    webView.evaluateJavascript(callbackFunction + "(" + message.toString() + ")", new ValueCallback<String>() {
                                            @Override
                                            public void onReceiveValue(String value) {

                                            }
                                        });
                                    }
                                });
                            } catch (Exception ex) {
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            final Process processFinal = process;
            Thread waitExit = new Thread(new Runnable() {
                @Override
                public void run() {
                    int status = -1;
                    try {
                        status = processFinal.waitFor();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            final JSONObject message = new JSONObject();
                            message.put("type", ShellHandlerBase.EVENT_EXIT);
                            message.put("message", "" + status);
                            webView.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (requestedDocument != document.get() || activity.isDestroyed()) return;
                                    webView.evaluateJavascript(callbackFunction + "(" + message.toString() + ")", new ValueCallback<String>() {
                                        @Override
                                        public void onReceiveValue(String value) {

                                        }
                                    });
                                }
                            });
                        } catch (Exception ex) {
                        }

                        if (reader.isAlive()) {
                            reader.interrupt();
                        }
                        if (readerError.isAlive()) {
                            readerError.interrupt();
                        }
                        if (onExit != null) {
                            onExit.run();
                        }
                    }
                }
            });

            reader.start();
            readerError.start();
            waitExit.start();
        }
    }
}
