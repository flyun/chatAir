package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UndoView;

import java.util.ArrayList;

/**
 * Created by flyun on 2025/1/9.
 */
class RenderMarkdownActivity extends BaseFragment {

    String message;
    private WebView webView;

    private View loadingView;

    FrameLayout frameLayout;

    @Override
    public boolean onFragmentCreate() {

        message = arguments.getString("message", "");

        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
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

    private UndoView undoView;

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
