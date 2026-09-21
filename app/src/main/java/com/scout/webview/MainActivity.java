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
        String debug = "";
        if (i != null) {
            // Primary: S.token / S.url extras (intent://...#Intent;S.token=...;S.url=...;end) — avoids URL length limits
            if (i.hasExtra("token")) token = i.getStringExtra("token");
            if (i.hasExtra("url")) targetUrl = i.getStringExtra("url");
            Uri data = i.getData();
            if (data != null) {
                String t = data.getQueryParameter("token");
                String u = data.getQueryParameter("url");
                if (t != null && (token==null || token.isEmpty())) token = t;
                if (u != null) targetUrl = u;
            }
            // dataString fallback: scout://go?token=... or intent://go?token=...
            if ((token==null || token.isEmpty()) && i.getDataString() != null && i.getDataString().contains("token=")) {
                try { token = Uri.parse(i.getDataString()).getQueryParameter("token"); } catch (Exception ignored) {}
            }
            debug = "intent=" + i.toString() + " data=" + i.getDataString();
        }
        android.widget.Toast.makeText(this, token!=null ? "Token ok, loading Scout..." : "No token, loading Scout login", android.widget.Toast.LENGTH_SHORT).show();
        android.util.Log.d("ScoutWebView", debug + " token=" + (token!=null ? token.substring(0,8)+"..." : "null"));

        final boolean[] injected = {false};
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!injected[0] && token != null && !token.isEmpty() && url.contains("scoutandrunner.com")) {
                    injected[0] = true;
                    // Fetch fresh user/roleData so cv-auth-storage matches real shape (from your dump) — then store and reload
                    String js = "(function(){try{"
                        + "var t='" + escapeJs(token) + "';"
                        + "function storeAndReload(user, roleData, session){"
                        + " var state={user:user||null, roleData:roleData||null, session:session||null, config:{maxTestsPerCycle:8}, accessFlags:null, token:t, refreshToken:'', refreshTokenExpiresAt:null, isAuthenticated:true, accountMergeCountryConflict:false};"
                        + " localStorage.setItem('cv-auth-storage', JSON.stringify({state:state,version:0}));"
                        + " localStorage.setItem('auth', t); sessionStorage.setItem('auth', t);"
                        + " console.log('Scout full state stored, reloading'); setTimeout(function(){location.href='https://scoutandrunner.com/scout';}, 300);"
                        + "}"
                        + "fetch('https://scoutandrunner.com/api/auth/me', {headers:{'Authorization':'Bearer '+t,'apikey':t,'Accept':'application/json'}})"
                        + ".then(function(r){return r.ok?r.json():null;})"
                        + ".then(function(j){ var u=(j&&j.user)||(j&&j.data&&j.data.user)||null; var rd=(j&&j.roleData)||null; var sess=(j&&j.session)||null; storeAndReload(u,rd,sess); })"
                        + ".catch(function(e){ console.log('auth/me fail',e); storeAndReload(null,null,null); });"
                        + "}catch(e){console.log(e)}})();";
                    view.evaluateJavascript(js, null);
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
