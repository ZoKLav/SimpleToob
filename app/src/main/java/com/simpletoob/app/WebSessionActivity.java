package com.simpletoob.app;

import android.app.AlertDialog;
import android.content.*;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;

/** First-party account window. No password fields, credential-form scraping or native JS bridge. */
public final class WebSessionActivity extends BaseActivity {
    private WebView web;
    private TextView site;
    private Button useAccount;
    private ProgressBar progress;
    private String profile;
    private boolean capturing,rendererLost;
    private AlertDialog message;
    private final LoginNavigation navigation=new LoginNavigation();
    public static void signIn(android.app.Activity a){open(a,AccountRoutes.SIGN_IN);}
    public static void open(android.app.Activity a,String url){a.startActivity(new Intent(a,WebSessionActivity.class).putExtra("url",AccountRoutes.entry(url)));}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);profile=app.sessions.profile();LinearLayout root=ui.column();ui.root(root);
        LinearLayout bar=ui.toolbar(tr(R.string.ui_youtube_account_cba2dc),this::finish);bar.addView(ui.icon("more",tr(R.string.ui_account_options_d67188),this::options));root.addView(bar);
        site=ui.text(tr(R.string.ui_connecting_fd3e79),12,ui.muted);site.setPadding(ui.dp(20),0,ui.dp(20),ui.dp(8));root.addView(site);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ui.accent));root.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(2)));
        web=new WebView(this);LoginWebSettings.apply(web,app.prefs.number("login_text_zoom",100),compatibility());Diagnostics.record("account renderer: "+(compatibility()?"software compatibility":"hardware"));
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                if(!r.isForMainFrame())return false;String url=r.getUrl().toString();
                if(!WebJson.loginHost(url)){notice("This account window only opens Google and YouTube pages.");return true;}
                if(navigation.stopped())return true;if(r.hasGesture())navigation.reset();
                String path=r.getUrl().getPath();if(path!=null&&(path.startsWith("/upload")||path.startsWith("/create"))||"studio.youtube.com".equals(r.getUrl().getHost())){notice("Uploads are not included.");return true;}return false;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
                String url=r.getUrl().toString(),host=r.getUrl().getHost();
                if(WebDownloader.mediaUrl(url)||host!=null&&(host.equals("doubleclick.net")||host.endsWith(".doubleclick.net")||host.equals("googlesyndication.com")||host.endsWith(".googlesyndication.com"))||url.contains("/pagead/")||url.contains("/api/stats/ads"))return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));return null;
            }
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){
                site.setText(host(url));if(!WebJson.loginHost(url)){v.stopLoading();progress.setVisibility(View.INVISIBLE);notice("Navigation stopped outside Google/YouTube.");return;}
                if(!navigation.start(url,android.os.SystemClock.elapsedRealtime())){v.stopLoading();progress.setVisibility(View.INVISIBLE);notice("Repeated account-page redirects were stopped. Reload from the menu to retry.");Diagnostics.record("account navigation loop stopped");return;}
                progress.setVisibility(View.VISIBLE);Diagnostics.record(url.contains("accounts.google.com")?"Google account navigation":"YouTube account navigation");
            }
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()){progress.setVisibility(View.INVISIBLE);notice("The account page couldn't load. Check your connection or reload from the menu.");}}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame()){progress.setVisibility(View.INVISIBLE);Diagnostics.record("account web page HTTP "+response.getStatusCode());notice("The account page returned HTTP "+response.getStatusCode()+".");}}
            @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();progress.setVisibility(View.INVISIBLE);notice("Secure connection failed. Loading was stopped.");}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){rendererLost=true;navigation.reset();progress.setVisibility(View.INVISIBLE);useAccount.setEnabled(false);if(v.getParent()!=null)((ViewGroup)v.getParent()).removeView(v);v.destroy();notice("The account-page renderer stopped. Close and reopen this window.");Diagnostics.record("account renderer stopped");return true;}
            @Override public void onPageFinished(WebView v,String url){if(rendererLost)return;if(!capturing)progress.setVisibility(View.INVISIBLE);site.setText(host(url));if(!navigation.stopped()&&WebJson.youtube(url))v.evaluateJavascript("document.querySelectorAll('video,audio').forEach(v=>{v.pause();v.autoplay=false;});",null);}
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));ui.divider(root);
        LinearLayout footer=ui.column();footer.setPadding(ui.dp(16),ui.dp(10),ui.dp(16),ui.dp(12));useAccount=ui.button(tr(R.string.ui_use_this_account_d5d543),true,this::capture);useAccount.setEnabled(false);footer.addView(useAccount,new LinearLayout.LayoutParams(-1,-2));root.addView(footer);
        String target=AccountRoutes.entry(getIntent().getStringExtra("url"));Bundle webState=saved==null?null:saved.getBundle("web_state");boolean sameProfile=profile.equals(app.prefs.text("web_session_profile",""));
        if(sameProfile){useAccount.setEnabled(true);if(webState==null||!profile.equals(saved.getString("web_profile"))||web.restoreState(webState)==null)web.loadUrl(target);}
        else work(app.account,app.sessions::read,cookies->CookieManager.getInstance().removeAllCookies(removed->{if(isFinishing()||isDestroyed())return;WebStorage.getInstance().deleteAllData();List<String[]> entries=new ArrayList<>();for(String pair:cookies.split(";")){if(pair.trim().isEmpty())continue;String value=pair.trim()+"; Path=/; Secure"+(pair.trim().matches("(?:SID|HSID|SSID|__Secure-[13]PSID)=.*")?"; HttpOnly":"");entries.add(new String[]{"https://www.youtube.com",value});entries.add(new String[]{"https://m.youtube.com",value});}restoreCookies(entries,0,()->{app.prefs.put("web_session_profile",profile);useAccount.setEnabled(true);web.loadUrl(target);});}),this::notice);
        if(android.os.Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,this::goBack);
    }
    private void restoreCookies(List<String[]> entries,int index,Runnable done){if(isFinishing()||isDestroyed()||rendererLost)return;if(index==entries.size()){CookieManager.getInstance().flush();done.run();return;}String[] item=entries.get(index);CookieManager.getInstance().setCookie(item[0],item[1],ok->restoreCookies(entries,index+1,done));}
    private String host(String url){try{return new java.net.URI(url).getHost();}catch(Exception e){return "Account page";}}
    private void notice(String text){if(isFinishing()||isDestroyed())return;if(message!=null&&message.isShowing())message.dismiss();message=new AlertDialog.Builder(this).setTitle(tr(R.string.ui_youtube_account_cba2dc)).setMessage(tx(text)).setPositiveButton(tr(R.string.ui_ok_9ce3bd),null).create();message.show();}
    private boolean sameProfile(){return profile.equals(app.sessions.profile());}
    private void capture(){
        if(capturing||rendererLost)return;if(!sameProfile()){notice("The active profile changed. Reopen this account window.");return;}
        String page=web.getUrl();if(!WebJson.youtube(page)){notice("Finish signing in and return to YouTube first.");return;}
        capturing=true;useAccount.setEnabled(false);useAccount.setText(tr(R.string.ui_checking_account_d9e59d));progress.setVisibility(View.VISIBLE);
        // Read only YouTube's selected-session metadata, never form values or Google sign-in pages.
        web.evaluateJavascript("(()=>{const g=window.ytcfg;if(!g||!g.get)return null;return JSON.stringify({account:g.get('SESSION_INDEX'),delegated:g.get('DELEGATED_SESSION_ID')||''});})()",result->{
            if(isFinishing()||isDestroyed()||rendererLost)return;
            try{
                if(!sameProfile()||!page.equals(web.getUrl()))throw new JSONException("Page changed");
                Object value=new JSONArray("["+result+"]").opt(0);if(!(value instanceof String))throw new JSONException("No selected session");
                JSONObject selection=new JSONObject((String)value);int account=YoutubeAccountProtocol.accountIndex(selection.opt("account"));String delegated=YoutubeAccountProtocol.delegated(selection.opt("delegated"));
                String cookies=CookieManager.getInstance().getCookie("https://"+host(page));if(WebJson.cookie(cookies,"SAPISID").isEmpty()&&WebJson.cookie(cookies,"__Secure-3PAPISID").isEmpty())throw new JSONException("No signed session");
                final String snapshot=cookies;
                work(app.account,()->{if(!sameProfile())throw new YoutubeRepository.ApiException("Profile changed; reopen this window.");app.sessions.save(profile,snapshot,account,delegated);app.web.invalidate();return app.web.verify();},name->{finishCheck();toast(tr(R.string.ui_signed_in_as_1_s_aff3e2, name));app.imports.foreground();finish();},failure->{finishCheck();notice(failure);});
            }catch(Exception e){finishCheck();notice("Couldn't read the selected YouTube account yet. Let the YouTube page finish loading, then try again.");}
        });
    }
    private void finishCheck(){capturing=false;if(!rendererLost){useAccount.setEnabled(true);useAccount.setText(tr(R.string.ui_use_this_account_d5d543));}progress.setVisibility(View.INVISIBLE);}
    private void reload(){if(rendererLost){notice("Close and reopen this window to restart the page renderer.");return;}navigation.reset();web.stopLoading();web.loadUrl(AccountRoutes.SIGN_IN);}
    private boolean compatibility(){return app.prefs.flag("login_software",true);}
    private void displayInfo(){android.content.pm.PackageInfo engine=WebView.getCurrentWebViewPackage();String info="Account renderer: "+(compatibility()?"Software compatibility":"Hardware")+"\nWebView: "+(engine==null?"unknown":engine.packageName+" "+engine.versionName)+"\nView: "+web.getWidth()+" × "+web.getHeight()+" px\nDensity: "+getResources().getDisplayMetrics().densityDpi+" dpi\nPage text: "+web.getSettings().getTextZoom()+"%\n\n"+Diagnostics.report();new AlertDialog.Builder(this).setTitle(tr(R.string.ui_display_information_7b9b00)).setMessage(info).setNeutralButton(tr(R.string.ui_copy_af74f7),(d,i)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("SimpleToob display",info));toast(tr(R.string.ui_display_information_copied_ea0d0a));}).setPositiveButton(tr(R.string.ui_close_bbfa77),null).show();}
    private void renderingMode(){new AlertDialog.Builder(this).setTitle(tr(R.string.ui_account_page_rendering_7420e8)).setSingleChoiceItems(new String[]{tr(R.string.ui_software_compatibility_9c3526),tr(R.string.ui_hardware_b76ab6)},compatibility()?0:1,(d,i)->{app.prefs.put("login_software",i==0);LoginWebSettings.renderer(web,i==0);Diagnostics.record("account renderer changed");d.dismiss();toast(i==0?tr(R.string.ui_software_compatibility_enabled_d97fa3):tr(R.string.ui_hardware_rendering_enabled_19f68b));}).setNegativeButton(tr(R.string.ui_cancel_77dfd2),null).show();}
    private void options(){new AlertDialog.Builder(this).setTitle(tr(R.string.ui_account_options_d67188)).setItems(new String[]{tr(R.string.ui_reload_sign_in_96d01c),tr(R.string.ui_rendering_mode_45b17a),tr(R.string.ui_page_text_size_9cbed6),tr(R.string.ui_display_information_7b9b00),tr(R.string.ui_choose_another_google_account_21b002),tr(R.string.ui_remove_app_session_79fd3b),tr(R.string.ui_close_bbfa77)},(d,i)->{if(i==6){finish();return;}if(rendererLost){notice("Close and reopen this window to restart the renderer.");return;}if(capturing){notice("Finish the current account check first.");return;}if(i==0)reload();else if(i==1)renderingMode();else if(i==2)new AlertDialog.Builder(this).setTitle(tr(R.string.ui_page_text_size_9cbed6)).setItems(new String[]{"100%","115%","130%","150%","175%"},(x,j)->{int zoom=new int[]{100,115,130,150,175}[j];app.prefs.put("login_text_zoom",zoom);web.getSettings().setTextZoom(zoom);}).show();else if(i==3)displayInfo();else if(i==4){navigation.reset();web.loadUrl("https://accounts.google.com/AccountChooser?service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin");}else if(i==5)new AlertDialog.Builder(this).setTitle(tr(R.string.ui_remove_this_app_s_saved_session_ea2a70)).setNegativeButton(tr(R.string.ui_cancel_77dfd2),null).setPositiveButton(tr(R.string.ui_remove_e96390),(x,y)->logout()).show();}).show();}
    private void logout(){work(app.account,()->{if(!sameProfile())throw new YoutubeRepository.ApiException("Profile changed; reopen this window.");app.sessions.clear();app.web.invalidate();return true;},r->CookieManager.getInstance().removeAllCookies(x->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();app.prefs.put("web_session_profile",profile);reload();toast(tr(R.string.ui_app_session_removed_1f25e0));}),this::notice);}
    private void goBack(){if(!rendererLost&&web!=null&&web.canGoBack())web.goBack();else finish();}
    @Override public void onBackPressed(){goBack();}
    @Override protected void onSaveInstanceState(Bundle b){if(web!=null&&!rendererLost){Bundle state=new Bundle();web.saveState(state);b.putBundle("web_state",state);b.putString("web_profile",profile);}super.onSaveInstanceState(b);}
    @Override protected void onPause(){if(web!=null&&!rendererLost){CookieManager.getInstance().flush();web.onPause();}super.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null&&!rendererLost)web.onResume();}
    @Override protected void onDestroy(){if(message!=null)message.dismiss();if(web!=null&&!rendererLost){web.stopLoading();web.destroy();}super.onDestroy();}
}
