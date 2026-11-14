package com.simpletoob.app;
import android.view.View;
import android.webkit.*;
/** Scoped login rendering; this never changes the native video player's acceleration. */
final class LoginWebSettings {
    private LoginWebSettings(){}
    static void apply(WebView web,int textZoom){apply(web,textZoom,false);}
    static void apply(WebView web,int textZoom,boolean compatibility){WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMediaPlaybackRequiresUserGesture(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setUseWideViewPort(true);s.setLoadWithOverviewMode(false);s.setTextZoom(Math.max(75,Math.min(200,textZoom)));s.setSupportZoom(true);s.setBuiltInZoomControls(true);s.setDisplayZoomControls(false);web.setInitialScale(0);renderer(web,compatibility);CookieManager cm=CookieManager.getInstance();cm.setAcceptCookie(true);cm.setAcceptThirdPartyCookies(web,true);}
    static void renderer(WebView web,boolean compatibility){web.setLayerType(compatibility?View.LAYER_TYPE_SOFTWARE:View.LAYER_TYPE_NONE,null);web.invalidate();}
}
