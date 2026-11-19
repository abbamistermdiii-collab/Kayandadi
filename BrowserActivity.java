package ng.com.shafinbatsa.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** The in-app browser: own toolbar (close, back, forward, address, reload, open in Chrome). */
public class BrowserActivity extends Activity {

    private WebView web;
    private ProgressBar bar;
    private TextView address, backBtn, fwdBtn;
    private float dp;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        final String url = getIntent().getStringExtra("url");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#120914"));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(Color.parseColor("#14091A"));

        TextView close = button("\u2715");
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        backBtn = button("\u2039");
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (web.canGoBack()) web.goBack(); }
        });
        fwdBtn = button("\u203A");
        fwdBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (web.canGoForward()) web.goForward(); }
        });

        address = new TextView(this);
        address.setTextColor(Color.parseColor("#E8D9E4"));
        address.setTextSize(13);
        address.setSingleLine(true);
        address.setEllipsize(TextUtils.TruncateAt.END);
        address.setPadding((int) (8 * dp), 0, (int) (8 * dp), 0);
        address.setGravity(Gravity.CENTER_VERTICAL);

        TextView reload = button("\u21BB");
        reload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { web.reload(); }
        });
        TextView external = button("\u2197");
        external.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String cur = web.getUrl() != null ? web.getUrl() : url;
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(cur))); } catch (Exception ignored) { }
            }
        });

        toolbar.addView(close);
        toolbar.addView(backBtn);
        toolbar.addView(fwdBtn);
        toolbar.addView(address, new LinearLayout.LayoutParams(0, (int) (48 * dp), 1f));
        toolbar.addView(reload);
        toolbar.addView(external);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF4D83")));

        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);

        root.addView(toolbar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (48 * dp)));
        root.addView(bar, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int) (3 * dp)));
        root.addView(web, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                bar.setProgress(progress);
                bar.setVisibility(progress >= 100 ? View.INVISIBLE : View.VISIBLE);
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String scheme = u.getScheme() == null ? "" : u.getScheme();
                if (scheme.equals("http") || scheme.equals("https")) return false;
                if (scheme.equals("intent")) {
                    try {
                        Intent it = Intent.parseUri(u.toString(), Intent.URI_INTENT_SCHEME);
                        it.addCategory(Intent.CATEGORY_BROWSABLE);
                        it.setComponent(null);
                        it.setSelector(null);
                        startActivity(it);
                    } catch (Exception e) {
                        try {
                            Intent it = Intent.parseUri(u.toString(), Intent.URI_INTENT_SCHEME);
                            String fb = it.getStringExtra("browser_fallback_url");
                            if (fb != null) view.loadUrl(fb);
                        } catch (Exception ignored) { }
                    }
                    return true;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String u, Bitmap favicon) {
                showAddress(u);
            }

            @Override
            public void onPageFinished(WebView view, String u) {
                showAddress(u);
                backBtn.setAlpha(view.canGoBack() ? 1f : 0.35f);
                fwdBtn.setAlpha(view.canGoForward() ? 1f : 0.35f);
            }
        });

        web.setDownloadListener(new android.webkit.DownloadListener() {
            @Override
            public void onDownloadStart(String u, String ua, String cd, String mime, long len) {
                try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u))); } catch (Exception ignored) { }
            }
        });

        backBtn.setAlpha(0.35f);
        fwdBtn.setAlpha(0.35f);
        if (url != null) web.loadUrl(url);
    }

    private void showAddress(String u) {
        if (u == null) return;
        Uri p = Uri.parse(u);
        String host = p.getHost() != null ? p.getHost() : u;
        address.setText((("https".equals(p.getScheme())) ? "\uD83D\uDD12 " : "") + host);
    }

    private TextView button(String label) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(Color.WHITE);
        t.setTextSize(22);
        t.setGravity(Gravity.CENTER);
        t.setLayoutParams(new LinearLayout.LayoutParams((int) (46 * dp), (int) (48 * dp)));
        return t;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.stopLoading();
            web.destroy();
        }
        super.onDestroy();
    }
}
