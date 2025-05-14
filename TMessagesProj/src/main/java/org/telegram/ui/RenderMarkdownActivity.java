package org.telegram.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
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
import org.telegram.ui.ActionBar.ActionBarMenuItem;
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

    private ActionBarMenuItem headerItem;

    private final static int copy_button = 1;
    private final static int share_button = 2;

    private final static int share_button_html = 21;
    private final static int share_button_md = 22;

    private final static int share_button_img = 23;

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
            webView = null;
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
                } else if (id == share_button_html) {
                    AndroidUtilities.logEvent("renderMarkdown", "share");
                    AndroidUtilities.logEvent("renderMarkdown", "share_html");
                    shareContent(ShareType.HTML);
                } else if (id == share_button_md) {
                    AndroidUtilities.logEvent("renderMarkdown", "share");
                    AndroidUtilities.logEvent("renderMarkdown", "share_markdown");
                    shareContent(ShareType.MARDKDOWN);
                } else if (id == share_button_img) {
                    AndroidUtilities.logEvent("renderMarkdown", "share");
                    AndroidUtilities.logEvent("renderMarkdown", "share_image");
                    shareContent(ShareType.IMAGE);
                }
            }
        });

        ActionBarMenu menu = actionBar.createMenu();
        copyButton = menu.addItemWithWidth(copy_button, R.drawable.msg_copy,
                AndroidUtilities.dp(56), LocaleController.getString("Copy", R.string.Copy));

        headerItem = menu.addItem(share_button, R.drawable.msg_shareout, null);
        headerItem.setContentDescription(LocaleController.getString("ShareFile",
                R.string.ShareFile));

        headerItem.lazilyAddSubItem(share_button_html, 0,
                LocaleController.getString("ExportAsHtml", R.string.ExportAsHtml));
        headerItem.lazilyAddSubItem(share_button_md, 0,
                LocaleController.getString("ExportAsMarkdown", R.string.ExportAsMarkdown));
        headerItem.lazilyAddSubItem(share_button_img, 0,
                LocaleController.getString("ExportAsImage", R.string.ExportAsImage));


        frameLayout = new FrameLayout(context);
        fragmentView = frameLayout;
        fragmentView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        fragmentView.setOnTouchListener((v, event) -> true);

        // 创建占位视图，并设置背景色
        loadingView = new View(context);
        loadingView.setBackgroundColor(Theme.isCurrentThemeDark()
                ? Color.parseColor("#1e1e1e") : Color.WHITE);
        frameLayout.addView(loadingView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,
                LayoutHelper.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            WebView.enableSlowWholeDocumentDraw();
        }

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
            @JavascriptInterface
            public String getMarkdownContent() {
                return markdownContent;
            }

            @JavascriptInterface
            public String getAndroidFontSize() {
                return String.valueOf(SharedConfig.fontSize);
            }

            @JavascriptInterface
            public String getNightMode() {
                return String.valueOf(Theme.isCurrentThemeDark());
            }

            @JavascriptInterface
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

    private void shareContent(ShareType shareType) {

        final Context context = weakReference != null ? weakReference.get() : null;

        if (context == null) return;

        AlertDialog progressDialog = new AlertDialog(getParentActivity(),
                AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.setCanCancel(false);
        progressDialog.show();

        switch (shareType) {
            case HTML:
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

                        if (file == null || weakReference == null || weakReference.get() == null) {
                            progressDialog.dismiss();
                            return;
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            try {
                                progressDialog.dismiss();
                                shareFile(context, file, "text/html");
                            } catch (Exception ignore) {
                            }
                        });
                    });

                }, "");
                break;
            case MARDKDOWN:
                Utilities.globalQueue.postRunnable(() -> {
                    File file = saveContentToCacheFile(message, "ChatAir_share.md");
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        if (file != null) {
                            shareFile(context, file, "text/markdown");
                        }
                    });
                });
                break;
            case IMAGE:
                generateImage(context, progressDialog);
                break;
            default:
                break;
        }
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
                "        code { font-family: Consolas, Monaco, 'Courier New', monospace; " +
                "font-size: 0.8em; }\n" +
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

    private void generateImage(Context context, AlertDialog progressDialog) {
        if (webView == null) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
            return;
        }

        AndroidUtilities.runOnUIThread(() -> {

            int widthSpec = 0;

            int currentWebViewWidth = webView.getWidth();
            if (currentWebViewWidth <= 0) {
                widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            } else {
                widthSpec = View.MeasureSpec.makeMeasureSpec(currentWebViewWidth,
                        View.MeasureSpec.EXACTLY);
            }

            int heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

            webView.measure(widthSpec, heightSpec);

            int contentWidth = webView.getMeasuredWidth();
            if (currentWebViewWidth > 0) {
                contentWidth = currentWebViewWidth;
            }
            if (contentWidth <= 0) {
                android.util.DisplayMetrics displayMetrics =
                        context.getResources().getDisplayMetrics();
                contentWidth = displayMetrics.widthPixels;
                widthSpec = View.MeasureSpec.makeMeasureSpec(contentWidth,
                        View.MeasureSpec.EXACTLY);
                webView.measure(widthSpec, heightSpec);
            }


            int contentHeight = webView.getMeasuredHeight();

            if (contentWidth <= 0 || contentHeight <= 0) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                return;
            }

            Bitmap bitmap = null;
            try {
                bitmap = Bitmap.createBitmap(contentWidth, contentHeight, Bitmap.Config.ARGB_8888);
            } catch (OutOfMemoryError e) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                return;
            }

            Canvas canvas = new Canvas(bitmap);

            Paint paint = new Paint();
            int backgroundColor = Theme.getColor(Theme.key_windowBackgroundWhite);
            if (webView.getBackground() == null || webView.getBackground().getOpacity() != android.graphics.PixelFormat.OPAQUE) {
                canvas.drawColor(backgroundColor);
            } else {
            }

            int initialScrollX = webView.getScrollX();
            int initialScrollY = webView.getScrollY();

            // 将 WebView 滚动到顶部开始绘制，确保从 (0,0) 开始捕获
            webView.scrollTo(0, 0);

            // 需要临时禁用硬件加速来确保 draw() 的正确性，特别是对于旧版 WebView
            int currentLayerType = webView.getLayerType();
            boolean changedLayerType = false;
            if (currentLayerType != View.LAYER_TYPE_SOFTWARE) {
                webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
                changedLayerType = true;
            }

            // 关键：在调用 draw 之前，需要强制 WebView 重新布局以适应完整内容的高度
            // 这一步非常重要，确保 WebView 内部知道它需要绘制那么大
            webView.layout(0, 0, contentWidth, contentHeight);

            webView.draw(canvas);

            // 恢复硬件加速设置
            if (changedLayerType) {
                webView.setLayerType(currentLayerType, null);
            }
            // 恢复滚动位置
            webView.scrollTo(initialScrollX, initialScrollY);

            // 重新请求布局，使其恢复到屏幕上的原始尺寸和状态
            // 这很重要，否则 WebView 可能会在屏幕上保持拉伸后的大小
            webView.requestLayout();

            final Bitmap finalBitmap = addWatermarkIconToBitmap(context, bitmap);
            final Bitmap originalBitmapToRecycle = bitmap;

            Utilities.globalQueue.postRunnable(() -> {
                File imageFile = saveBitmapToCacheFile(finalBitmap,
                        "ChatAir_share.png");

                if (finalBitmap != null && !finalBitmap.isRecycled()) {
                    if (originalBitmapToRecycle != finalBitmap && originalBitmapToRecycle != null && !originalBitmapToRecycle.isRecycled()) {
                        originalBitmapToRecycle.recycle();
                    }
                    finalBitmap.recycle();
                } else if (originalBitmapToRecycle != null && !originalBitmapToRecycle.isRecycled()) {
                    originalBitmapToRecycle.recycle();
                }


                // 6. 返回 UI 线程处理结果
                AndroidUtilities.runOnUIThread(() -> {
                    if (progressDialog != null && progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (imageFile != null && imageFile.exists()) {
                        shareFile(context, imageFile, "image/png");
                    } else {
                    }
                });
            });
        });
    }


    public Bitmap addWatermarkIconToBitmap(Context context, Bitmap originalBitmap) {
        // 1. 检查原始Bitmap是否有效
        if (originalBitmap == null) {
            return null;
        }

        // 获取原始Bitmap的配置，如果为null则默认使用ARGB_8888
        Bitmap.Config config = originalBitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }


        final int ICON_TARGET_DIAMETER_DP = 50;
        final int ICON_TARGET_DIAMETER_PX = dpToPx(context, ICON_TARGET_DIAMETER_DP);

        // 水印文字及其尺寸
        String watermarkText = "ChatAir";
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int textColor = Theme.isCurrentThemeDark() ? Color.parseColor("#d7d7d7") : Color.BLACK;
        textPaint.setColor(textColor);
        textPaint.setTextSize(spToPx(context, 14));
        textPaint.setTextAlign(Paint.Align.CENTER); // 文字居中对齐

        // 计算文字边界，获取文字高度
        Rect textBounds = new Rect();
        textPaint.getTextBounds(watermarkText, 0, watermarkText.length(), textBounds);
        int textHeight = textBounds.height();

        int watermarkAreaBottomMarginPx = dpToPx(context, 20);
        int iconTextSpacingPx = dpToPx(context, 2);

        int watermarkIconResId = Theme.isCurrentThemeDark() ? R.mipmap.ic_watermark_dark :
                R.mipmap.ic_watermark;
        Bitmap rawIconBitmap = BitmapFactory.decodeResource(context.getResources(),
                watermarkIconResId);

        if (rawIconBitmap == null) {
            return originalBitmap;
        }

        // 缩放图标到目标尺寸
        Bitmap scaledIconBitmap;
        if (rawIconBitmap.getWidth() != ICON_TARGET_DIAMETER_PX || rawIconBitmap.getHeight() != ICON_TARGET_DIAMETER_PX) {
            scaledIconBitmap = Bitmap.createScaledBitmap(rawIconBitmap, ICON_TARGET_DIAMETER_PX,
                    ICON_TARGET_DIAMETER_PX, true);
            rawIconBitmap.recycle();
        } else {
            scaledIconBitmap = rawIconBitmap;
        }

        // 将缩放后的图标处理成圆形
        Bitmap circularIconBitmap = Bitmap.createBitmap(ICON_TARGET_DIAMETER_PX,
                ICON_TARGET_DIAMETER_PX, Bitmap.Config.ARGB_8888);
        Canvas iconCanvas = new Canvas(circularIconBitmap);
        Paint iconPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        iconCanvas.drawCircle(ICON_TARGET_DIAMETER_PX / 2f, ICON_TARGET_DIAMETER_PX / 2f,
                ICON_TARGET_DIAMETER_PX / 2f, iconPaint);

        iconPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        iconCanvas.drawBitmap(scaledIconBitmap, 0, 0, iconPaint);

        if (!scaledIconBitmap.isRecycled()) {
            scaledIconBitmap.recycle();
        }

        int iconHeight = circularIconBitmap.getHeight();
        // 水印区域总高度 = 图标高度 + 图标文字间距 + 文字高度 + 水印区域底边距
        int totalWatermarkAreaHeight =
                iconHeight + iconTextSpacingPx + textHeight + watermarkAreaBottomMarginPx;


        Bitmap resultBitmap = Bitmap.createBitmap(originalBitmap.getWidth(),
                originalBitmap.getHeight() + totalWatermarkAreaHeight, config);
        Canvas canvas = new Canvas(resultBitmap);

        canvas.drawBitmap(originalBitmap, 0, 0, null);

        Paint backgroundPaint = new Paint();
        int backgroundColor = Theme.isCurrentThemeDark() ? Color.parseColor("#1e1e1e") :
                Color.WHITE;
        backgroundPaint.setColor(backgroundColor); // 设置填充颜色
        canvas.drawRect(0, originalBitmap.getHeight(),
                resultBitmap.getWidth(), resultBitmap.getHeight(),
                backgroundPaint);

        // 图标的X坐标：在新Bitmap宽度范围内居中
        float iconX = (canvas.getWidth() - circularIconBitmap.getWidth()) / 2f;

        // 图标的Y坐标：在水印区域内定位。
        // 水印区域的顶部是 watermarkAreaStartY。
        // 水印的整体是图标在上面，文字在下面。
        // 水印整体的底部是 canvas.getHeight() - watermarkAreaBottomMarginPx。
        // 水印整体的高度是 iconHeight + iconTextSpacingPx + textHeight。
        // 水印整体的顶部Y坐标 = canvas.getHeight() - watermarkAreaBottomMarginPx - (iconHeight +
        // iconTextSpacingPx + textHeight)
        float watermarkBlockStartY = canvas.getHeight() - watermarkAreaBottomMarginPx
                        - (iconHeight + iconTextSpacingPx + textHeight);

        // 图标位于水印块的顶部
        float iconY = watermarkBlockStartY;


        // 文字的X坐标：在新Bitmap宽度范围内居中
        float textX = canvas.getWidth() / 2f;

        // 文字的基线Y坐标：位于图标下方，间隔 iconTextSpacingPx，然后减去文字顶部到基线的距离 textBounds.top
        // 文字块的顶部Y = iconY + iconHeight + iconTextSpacingPx
        // 文字的基线Y = 文字块的顶部Y - textBounds.top
        float textBaselineY = iconY + iconHeight + iconTextSpacingPx - textBounds.top;


        canvas.drawBitmap(circularIconBitmap, iconX, iconY, null);

        canvas.drawText(watermarkText, textX, textBaselineY, textPaint);

        if (!circularIconBitmap.isRecycled()) {
            circularIconBitmap.recycle();
        }

        return resultBitmap;
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static int spToPx(Context context, int sp) {
        return (int) (sp * context.getResources().getDisplayMetrics().scaledDensity + 0.5f);
    }

    private File saveContentToCacheFile(String content, String fileName) {
        File cacheDir = ApplicationLoader.applicationContext.getCacheDir();
        if (cacheDir == null) return null;
        File file = new File(cacheDir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(content.getBytes(StandardCharsets.UTF_8));
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    private File saveBitmapToCacheFile(Bitmap bitmap, String fileName) {
        if (bitmap == null) return null;
        File cacheDir = ApplicationLoader.applicationContext.getCacheDir();
        if (cacheDir == null) return null;
        File file = new File(cacheDir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            return file;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Js调用Android的方法
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

    private static void shareFile(Context context, File file, String mimeType) {

        if (context == null || file == null) return;
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
            i.setType(mimeType);
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

        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND
                , null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND,
                null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR
                , null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR
                , null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar,
                ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null,
                Theme.key_actionBarDefaultSelector));

        return themeDescriptions;
    }


    private enum ShareType {
        HTML,
        MARDKDOWN,
        IMAGE,
    }
}

