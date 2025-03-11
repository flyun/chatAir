package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.apache.commons.text.StringEscapeUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UndoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import androidx.core.content.FileProvider;

/**
 * Created by flyun on 2025/1/9.
 */
class RenderMarkdownActivity extends BaseFragment {

    String message;
    private WebView webView;

    private View loadingView;

    private UndoView undoView;

    FrameLayout frameLayout;

    private View copyButton;
    private View shareButton;

    private final static int copy_button = 1;
    private final static int share_button = 2;

    private WeakReference<Context> weakReference;

    @Override
    public boolean onFragmentCreate() {

        message = arguments.getString("message", "");

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        if (weakReference != null) {
            weakReference.clear();
            weakReference = null;
        }
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
            webView.destroy();
        }
    }

    public RenderMarkdownActivity(Bundle args) {
        super(args);
    }

    @Override
    public View createView(Context context) {
        weakReference = new WeakReference<>(RenderMarkdownActivity.this.getContext());

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("RenderAdvancedMarkdown",
                R.string.RenderAdvancedMarkdown));
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setAllowOverlayTitle(false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == copy_button) {
                    AndroidUtilities.logEvent("renderMarkdown", "copy");
                    AndroidUtilities.addToClipboard(message);

                    //创造复制成功提示
                    createUndoView();
                    if (undoView == null) {
                        return;
                    }
                    //显示复制成功提示
                    undoView.showWithAction(0, UndoView.ACTION_MESSAGE_COPIED, null);
                } else if (id == share_button) {
                    AndroidUtilities.logEvent("renderMarkdown", "share");
                    shareContent();
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        copyButton = menu.addItemWithWidth(copy_button, R.drawable.msg_copy,
                AndroidUtilities.dp(56), LocaleController.getString("Copy", R.string.Copy));
        shareButton = menu.addItemWithWidth(share_button, R.drawable.msg_shareout,
                AndroidUtilities.dp(56), LocaleController.getString("ShareFile", R.string.ShareFile));


        frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        fragmentView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fragmentView.setOnTouchListener((v, event) -> true);

        // 创建占位视图，并设置背景色
        loadingView = new View(context);
        loadingView.setBackgroundColor(Theme.isCurrentThemeDark()
                ? Color.parseColor("#1e1e1e") : Color.WHITE);
        frameLayout.addView(loadingView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        webView = new WebView(context);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        // 将 marked.js 放在 assets 文件夹中
        webView.getSettings().setAllowFileAccess(true);
        // 加载本地 HTML 文件（用来引入 marked.js）
        webView.loadUrl("file:///android_asset/markdown_renderer.html");
        // Markdown 内容
        String markdownContent = message;
        // 远程debug
//        WebView.setWebContentsDebuggingEnabled(true);

        // 设置 WebView 背景色为透明
        webView.setBackgroundColor(Color.TRANSPARENT);

        // 设置 WebViewClient，在页面加载完成后隐藏占位视图
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (loadingView != null) {
                    frameLayout.removeView(loadingView);
                    loadingView = null;
                }
            }

            // 添加 shouldOverrideUrlLoading 方法
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // 尝试打开链接
                try {
                    Browser.openUrl(getParentActivity(), url);
                    return true;
                } catch (Exception e) {
                    // 如果尝试打开链接失败，则直接使用webview加载
                }
                // 如果没有Activity可以处理，则交由webview加载
                return false;
            }
        });

        // 使用 WebView 的 JavaScriptInterface 将 Markdown 内容传递到 WebView
        webView.addJavascriptInterface(new Object() {
            @android.webkit.JavascriptInterface
            public String getMarkdownContent() {
                return markdownContent;
            }
            @android.webkit.JavascriptInterface
            public String getAndroidFontSize() {
                return String.valueOf(SharedConfig.fontSize);
            }
            @android.webkit.JavascriptInterface
            public String getNightMode() {
                return String.valueOf(Theme.isCurrentThemeDark());
            }
            @android.webkit.JavascriptInterface
            public void copy(String content) {
                AndroidUtilities.runOnUIThread(() -> {
                    if (!content.isEmpty()) {
                        AndroidUtilities.addToClipboard(content);
                        createUndoView();
                        undoView.showWithAction(0, UndoView.ACTION_TEXT_COPIED, null);
                    }
                });
            }
        }, "AndroidInterface");

        frameLayout.addView(webView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT, Gravity.CENTER, 0, 0, 0, 0));

        return fragmentView;
    }

    private void shareContent() {

        AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();
        callJavaScriptFunction("getShareContent", value -> {

            if (value == null) {
                progressDialog.dismiss();
                return;
            }
            String handleContent = removeQuotesIfPresent(value);
            String decodedContent = StringEscapeUtils.unescapeJava(handleContent);

            if (decodedContent == null) {
                progressDialog.dismiss();
                return;
            }

            String htmlContent = buildFullHtml(decodedContent);

            Utilities.globalQueue.postRunnable(() -> {

                File file = saveHtmlToCacheFile(htmlContent);

                if (file == null|| weakReference == null || weakReference.get() == null) {
                    progressDialog.dismiss();
                    return;
                }
                Context context = weakReference.get();

                AndroidUtilities.runOnUIThread(() -> {
                    try {
                        progressDialog.dismiss();
                        shareFile(context, file);
                    } catch (Exception ignore) {
                    }
                });
            });

        }, "");
    }

    private static String removeQuotesIfPresent(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // 检查字符串是否以双引号开头
        if (input.startsWith("\"")) {
            // 使用正则表达式进行替换，只替换字符串首尾的双引号
            return input.replaceAll("^\"|\"$", "");
        } else {
            return input;
        }
    }

    private String buildFullHtml(String content) {
        // 确保样式与 WebView 中的显示效果一致
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>ChatAir Share Content</title>\n" +
                "    <style>\n" +
                "        body { font-size: 16px; }\n" +
                "        table { border-collapse: collapse; width: 100%; }\n" +
                "        th, td { border: 1px solid black; padding: 8px; text-align: left; }\n" +
                "        th { background-color: #f2f2f2; }\n" +
                "        code { font-family: Consolas, Monaco, 'Courier New', monospace; font-size: 0.8em; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"content\">\n" +
                content + // 插入从 WebView 获取的内容
                "</div>\n" +
                "</body>\n" +
                "</html>";
        return html;
    }

    private File saveHtmlToCacheFile(String htmlContent) {
        File cacheDir = ApplicationLoader.applicationContext.getCacheDir();
        File htmlFile = new File(cacheDir, "ChatAir_shared_content.html");

        try (FileOutputStream outputStream = new FileOutputStream(htmlFile)) {
            outputStream.write(htmlContent.getBytes(StandardCharsets.UTF_8));
            return htmlFile;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     *  Js调用Android的方法
     */
    public void callJavaScriptFunction(final String functionName, ValueCallback<String> callback,
                                       final String... params) {
        if (webView == null) return;
        webView.post(() -> {
            StringBuilder jsCode = new StringBuilder();
            jsCode.append("javascript:");
            jsCode.append(functionName);
            jsCode.append("(");
            if (params != null && params.length > 0) {
                for (int i = 0; i < params.length; i++) {
                    jsCode.append("'").append(params[i]).append("'");
                    if (i < params.length - 1) {
                        jsCode.append(",");
                    }
                }
            }
            jsCode.append(")");

            if (webView == null) return;
            webView.evaluateJavascript(jsCode.toString(), callback);
        });
    }

    private static void shareFile(Context context, File file) {

        try {
            Uri uri;
            if (Build.VERSION.SDK_INT >= 24) {
                uri = FileProvider.getUriForFile(context,
                        ApplicationLoader.getApplicationId() + ".provider", file);
            } else {
                uri = Uri.fromFile(file);
            }

            Intent i = new Intent(Intent.ACTION_SEND);
            if (Build.VERSION.SDK_INT >= 24) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            i.setType("text/html");
            // 设置分享标题
            i.putExtra(Intent.EXTRA_SUBJECT, "ChatAir Share Content");
            i.putExtra(Intent.EXTRA_STREAM, uri);
            context.startActivity(Intent.createChooser(i, "ChatAir Share Content"));

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/html");

        } catch (IllegalArgumentException e) {
        }
    }

    //创建底部提示tips
    private void createUndoView() {
        if (undoView != null || getContext() == null) {
            return;
        }
        undoView = new UndoView(getContext(), this, false, null);
        undoView.setAdditionalTranslationY(AndroidUtilities.dp(51));
        frameLayout.addView(undoView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));

        return themeDescriptions;
    }

}
