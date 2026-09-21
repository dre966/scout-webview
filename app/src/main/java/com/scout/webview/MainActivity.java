package com.scout.webview;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;

public class MainActivity extends Activity {
    private WebView webView;
    private String token;
    private String targetUrl = "https://scoutandrunner.com/scout";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        Intent i = getIntent();
        if (i != null) {
            Uri data = i.getData();
            if (data != null) {
                String t = data.getQueryParameter("token");
                String u = data.getQueryParameter("url");
                if (t != null) token = t;
                if (u != null) targetUrl = u;
            }
            if (i.hasExtra("token")) token = i.getStringExtra("token");
            if (i.hasExtra("url")) targetUrl = i.getStringExtra("url");
            // also accept scout://go?token=...&url=...
        }
        // Allow dashboard to pass token via intent: scout://go?token=xxx
        // Fallback: if launched normally, try to load last token from intent data string
        if (token == null && i.getDataString() != null && i.getDataString().contains("token=")) {
            try { token = Uri.parse(i.getDataString()).getQueryParameter("token"); } catch (Exception ignored) {}
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (token != null && !token.isEmpty() && url.contains("scoutandrunner.com")) {
                    String js = "(function(){try{"
                        + "localStorage.setItem('cv-auth-storage', JSON.stringify({state:{token:'" + escapeJs(token) + "'}}));"
                        + "localStorage.setItem('auth','" + escapeJs(token) + "');"
                        + "sessionStorage.setItem('auth','" + escapeJs(token) + "');"
                        + "console.log('Scout token injected for "+targetUrl+"');"
                        + "}catch(e){console.log(e)}})();";
                    view.evaluateJavascript(js, null);
                    // only inject once
                    token = null;
                }
            }
        });
        webView.loadUrl(targetUrl);
    }

    private String escapeJs(String s) {
        return s.replace("\\","\\\\").replace("'","\\'").replace("\n","\\n");
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
