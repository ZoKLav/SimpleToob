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

/** First-party authorization, followed by a stopped native handoff. No form scraping or JS credential bridge. */
public final class WebSessionActivity extends BaseActivity {
    private WebView web;private TextView site;private Button useAccount;private ProgressBar progress;private LinearLayout ready;
    private String profile;private boolean capturing,rendererLost,probing,useWhenReady;private int document,probeAttempts;
    private volatile YoutubeAccountProtocol.Session pending;
    private AlertDialog message;private android.window.OnBackInvokedCallback systemBack;
    private final LoginNavigation navigation=new LoginNavigation();
    public static void signIn(android.app.Activity a){open(a,AccountRoutes.SIGN_IN);}
    public static void open(android.app.Activity a,String url){a.startActivity(new Intent(a,WebSessionActivity.class).putExtra("url",AccountRoutes.entry(url)));}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);profile=app.sessions.profile();LinearLayout root=ui.column();ui.root(root);
        LinearLayout bar=ui.toolbar(tr(R.string.ui_youtube_account_cba2dc),this::finish);bar.addView(ui.icon("more",tr(R.string.ui_account_options_d67188),this::options));root.addView(bar);
        site=ui.text(tr(R.string.ui_connecting_fd3e79),12,ui.muted);site.setPadding(ui.dp(20),0,ui.dp(20),ui.dp(8));root.addView(site);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setIndeterminate(true);progress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(ui.accent));root.addView(progress,new LinearLayout.LayoutParams(-1,ui.dp(2)));
        web=new WebView(this){@Override public boolean onTouchEvent(MotionEvent event){if(event.getActionMasked()==MotionEvent.ACTION_UP&&pending==null&&!capturing)navigation.reset();return super.onTouchEvent(event);}};
        LoginWebSettings.apply(web,app.prefs.number("login_text_zoom",100),compatibility());Diagnostics.record("account renderer: "+(compatibility()?"software compatibility":"hardware"));
        web.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
                if(!r.isForMainFrame())return false;if(pending!=null)return true;String url=r.getUrl().toString();
                if(!WebJson.loginHost(url)){notice("This account window only opens Google and YouTube pages.");return true;}
                // A redirect can inherit a gesture flag; that must not renew its redirect budget.
                if(r.hasGesture()&&!r.isRedirect())navigation.reset();if(navigation.stopped())return true;
                String path=r.getUrl().getPath();if(path!=null&&(path.startsWith("/upload")||path.startsWith("/create"))||"studio.youtube.com".equals(r.getUrl().getHost())){notice("Uploads are not included.");return true;}return false;
            }
            @Override public WebResourceResponse shouldInterceptRequest(WebView v,WebResourceRequest r){
                String url=r.getUrl().toString(),host=r.getUrl().getHost();
                if(pending!=null||WebDownloader.mediaUrl(url)||host!=null&&(host.equals("doubleclick.net")||host.endsWith(".doubleclick.net")||host.equals("googlesyndication.com")||host.endsWith(".googlesyndication.com"))||url.contains("/pagead/")||url.contains("/api/stats/ads"))return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));return null;
            }
            @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){
                if(pending!=null)return;document++;probing=false;probeAttempts=0;useWhenReady=false;site.setText(host(url));
                if(!sameProfile()||!WebJson.loginHost(url)){v.stopLoading();progress.setVisibility(View.INVISIBLE);notice("Navigation stopped outside Google/YouTube.");return;}
                if(!navigation.start(url,android.os.SystemClock.elapsedRealtime())){v.stopLoading();progress.setVisibility(View.INVISIBLE);probe(false);notice("Repeated account-page redirects were stopped. Reload from the menu to retry.");Diagnostics.record("account navigation loop stopped");return;}
                progress.setVisibility(View.VISIBLE);Diagnostics.record(url.contains("accounts.google.com")?"Google account navigation":"YouTube account navigation");
            }
            @Override public void onPageCommitVisible(WebView v,String url){if(url.equals(v.getUrl()))probe(false);}
            @Override public void onPageFinished(WebView v,String url){if(rendererLost||pending!=null)return;if(!capturing)progress.setVisibility(View.INVISIBLE);site.setText(host(url));if(url.equals(v.getUrl()))probe(false);}
            @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame()&&pending==null&&!rendererLost){progress.setVisibility(View.INVISIBLE);notice("The account page couldn't load. Check your connection or reload from the menu.");}}
            @Override public void onReceivedHttpError(WebView v,WebResourceRequest r,WebResourceResponse response){if(r.isForMainFrame()&&pending==null){progress.setVisibility(View.INVISIBLE);Diagnostics.record("account web page HTTP "+response.getStatusCode());notice("The account page returned HTTP "+response.getStatusCode()+".");}}
            @Override public void onReceivedSslError(WebView v,SslErrorHandler h,SslError e){h.cancel();progress.setVisibility(View.INVISIBLE);notice("Secure connection failed. Loading was stopped.");}
            @Override public boolean onRenderProcessGone(WebView v,RenderProcessGoneDetail d){rendererLost=true;document++;navigation.reset();progress.setVisibility(View.INVISIBLE);useAccount.setEnabled(pending!=null);if(v.getParent()!=null)((ViewGroup)v.getParent()).removeView(v);v.destroy();if(pending==null)notice("The account-page renderer stopped. Close and reopen this window.");Diagnostics.record("account renderer stopped");return true;}
        });
        root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
        ready=ui.column();ui.padded(ready);ready.setVisibility(View.GONE);ready.addView(ui.heading(tx("Account ready"),23));ready.addView(ui.note(tx("Authorization returned to YouTube. Use this account to finish, or choose another account.")));ready.addView(ui.quiet(tx("Choose another account"),()->beginBrowser(googleChooser())));root.addView(ready,new LinearLayout.LayoutParams(-1,0,1));ui.divider(root);
        LinearLayout footer=ui.column();footer.setPadding(ui.dp(16),ui.dp(10),ui.dp(16),ui.dp(12));useAccount=ui.button(tr(R.string.ui_use_this_account_d5d543),true,this::capture);useAccount.setEnabled(false);footer.addView(useAccount,new LinearLayout.LayoutParams(-1,-2));root.addView(footer);
        String target=AccountRoutes.entry(getIntent().getStringExtra("url"));Bundle webState=saved==null?null:saved.getBundle("web_state");boolean sameBrowserProfile=profile.equals(app.prefs.text("web_session_profile",""));
        if(sameBrowserProfile){useAccount.setEnabled(true);if(webState==null||!profile.equals(saved.getString("web_profile"))||web.restoreState(webState)==null)web.loadUrl(target);}
        else work(app.account,app.sessions::read,this::installCookies,failure->{installCookies("");notice(failure);});
        if(android.os.Build.VERSION.SDK_INT>=33){systemBack=this::goBack;getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,systemBack);}
    }
    private void installCookies(String cookies){if(!sameProfile())return;CookieManager.getInstance().removeAllCookies(removed->{if(isFinishing()||isDestroyed()||!sameProfile())return;WebStorage.getInstance().deleteAllData();List<String[]> entries=new ArrayList<>();for(String pair:cookies.split(";")){if(pair.trim().isEmpty())continue;String value=pair.trim()+"; Path=/; Secure"+(pair.trim().matches("(?:SID|HSID|SSID|__Secure-[13]PSID)=.*")?"; HttpOnly":"");entries.add(new String[]{"https://www.youtube.com",value});entries.add(new String[]{"https://m.youtube.com",value});}restoreCookies(entries,0,()->{app.prefs.put("web_session_profile",profile);useAccount.setEnabled(true);web.loadUrl(AccountRoutes.entry(getIntent().getStringExtra("url")));});});}
    private void restoreCookies(List<String[]> entries,int index,Runnable done){if(isFinishing()||isDestroyed()||rendererLost||!sameProfile())return;if(index==entries.size()){CookieManager.getInstance().flush();done.run();return;}String[] item=entries.get(index);CookieManager.getInstance().setCookie(item[0],item[1],ok->restoreCookies(entries,index+1,done));}
    private String host(String url){try{return new java.net.URI(url).getHost();}catch(Exception e){return "Account page";}}
    private void notice(String text){if(isFinishing()||isDestroyed())return;if(message!=null&&message.isShowing())message.dismiss();message=new AlertDialog.Builder(this).setTitle(tr(R.string.ui_youtube_account_cba2dc)).setMessage(tx(text)).setPositiveButton(tr(R.string.ui_ok_9ce3bd),null).create();message.show();}
    private boolean sameProfile(){return profile.equals(app.sessions.profile());}
    private void probe(boolean requested){
        if(isFinishing()||isDestroyed()||rendererLost||capturing||!sameProfile())return;if(pending!=null){if(requested)commitSelection();return;}useWhenReady|=requested;if(requested)probeAttempts=0;
        String page=web.getUrl();if(!LoginNavigation.landing(page)){if(requested)notice("Finish signing in and return to YouTube first.");return;}if(probing||probeAttempts>=6)return;probing=true;probeAttempts++;int expectedDocument=document;
        // Only page-provided account/context metadata is read. Never inspect password/forms or Google pages.
        String script="(()=>{const g=window.ytcfg;if(!g)return null;const get=k=>g.get?g.get(k):(g.data_||{})[k];const use=(a,b)=>(a===undefined||a===null)?b:a;const u=(get('INNERTUBE_CONTEXT')||{}).user||{};return JSON.stringify({account:use(get('SESSION_INDEX'),u.sessionIndex),delegated:use(get('DELEGATED_SESSION_ID'),use(u.onBehalfOfUser,'')),loggedIn:use(get('LOGGED_IN'),null)});})()";
        web.evaluateJavascript(script,result->{
            if(isFinishing()||isDestroyed()||rendererLost||expectedDocument!=document)return;probing=false;if(!sameProfile()||!page.equals(web.getUrl())||pending!=null)return;
            try{Object value=new JSONArray("["+result+"]").opt(0);if(!(value instanceof String))throw new JSONException("No selected session");String cookies=CookieManager.getInstance().getCookie("https://"+host(page));if(WebJson.cookie(cookies,"SAPISID").isEmpty()&&WebJson.cookie(cookies,"__Secure-3PAPISID").isEmpty())throw new JSONException("No signed session");YoutubeAccountProtocol.Session selected=YoutubeAccountProtocol.pageSelection(new JSONObject((String)value),page,cookies);freeze(selected);if(useWhenReady)commitSelection();}
            catch(Exception ignored){if(useWhenReady){useWhenReady=false;notice("Couldn't read the selected YouTube account yet. Let the YouTube page finish loading, then try again.");}else if(probeAttempts<6)web.postDelayed(()->{if(expectedDocument==document)probe(false);},500);}
        });
    }
    private void freeze(YoutubeAccountProtocol.Session selected){pending=selected;if(message!=null)message.dismiss();web.stopLoading();web.loadUrl("about:blank");web.onPause();web.setVisibility(View.GONE);ready.setVisibility(View.VISIBLE);progress.setVisibility(View.INVISIBLE);site.setText(tx("Account ready"));useAccount.setEnabled(true);CookieManager.getInstance().flush();Diagnostics.record("account authorization handed to native view");}
    private void capture(){if(capturing)return;if(!sameProfile()){notice("The active profile changed. Reopen this account window.");return;}if(pending!=null)commitSelection();else if(!rendererLost)probe(true);}
    private void commitSelection(){
        if(capturing||pending==null||!sameProfile())return;capturing=true;useAccount.setEnabled(false);useAccount.setText(tr(R.string.ui_checking_account_d9e59d));progress.setVisibility(View.VISIBLE);
        YoutubeAccountProtocol.Session selection=pending;String expectedRevision=app.sessions.revision(),oldOwner=app.sessions.owner();
        work(app.account,()->{YoutubeWebClient candidate=new YoutubeWebClient(app.sessions,app.prefs,selection).pin(profile,expectedRevision);AccountCollections.Identity identity=candidate.inspectAccount();if(!app.sessions.saveVerified(profile,expectedRevision,selection,identity))throw new YoutubeRepository.ApiException("Account changed during verification; retry.");app.web.invalidate();return identity;},identity->{finishCheck();toast(tr(R.string.ui_signed_in_as_1_s_aff3e2,identity.name));app.imports.accountSelected(!oldOwner.equals(identity.owner));finish();},failure->{finishCheck();notice(failure);});
    }
    private void finishCheck(){capturing=false;useWhenReady=false;useAccount.setEnabled(!rendererLost||pending!=null);useAccount.setText(tr(R.string.ui_use_this_account_d5d543));progress.setVisibility(View.INVISIBLE);}
    private static String googleChooser(){return "https://accounts.google.com/AccountChooser?service=youtube&continue=https%3A%2F%2Fwww.youtube.com%2Fsignin";}
    private void beginBrowser(String url){if(capturing)return;if(rendererLost){notice("Close and reopen this window to restart the page renderer.");return;}pending=null;useWhenReady=false;document++;probing=false;probeAttempts=0;navigation.reset();ready.setVisibility(View.GONE);web.setVisibility(View.VISIBLE);web.onResume();web.stopLoading();web.loadUrl(url);}
    private void reload(){beginBrowser(AccountRoutes.SIGN_IN);}
    private boolean compatibility(){return app.prefs.flag("login_software",true);}
    private void displayInfo(){android.content.pm.PackageInfo engine=WebView.getCurrentWebViewPackage();String info="Account renderer: "+(compatibility()?"Software compatibility":"Hardware")+"\nWebView: "+(engine==null?"unknown":engine.packageName+" "+engine.versionName)+"\nView: "+web.getWidth()+" × "+web.getHeight()+" px\nDensity: "+getResources().getDisplayMetrics().densityDpi+" dpi\nPage text: "+web.getSettings().getTextZoom()+"%\n\n"+Diagnostics.report();new AlertDialog.Builder(this).setTitle(tr(R.string.ui_display_information_7b9b00)).setMessage(info).setNeutralButton(tr(R.string.ui_copy_af74f7),(d,i)->{android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);clipboard.setPrimaryClip(ClipData.newPlainText("SimpleToob display",info));toast(tr(R.string.ui_display_information_copied_ea0d0a));}).setPositiveButton(tr(R.string.ui_close_bbfa77),null).show();}
    private void renderingMode(){new AlertDialog.Builder(this).setTitle(tr(R.string.ui_account_page_rendering_7420e8)).setSingleChoiceItems(new String[]{tr(R.string.ui_software_compatibility_9c3526),tr(R.string.ui_hardware_b76ab6)},compatibility()?0:1,(d,i)->{app.prefs.put("login_software",i==0);LoginWebSettings.renderer(web,i==0);Diagnostics.record("account renderer changed");d.dismiss();toast(i==0?tr(R.string.ui_software_compatibility_enabled_d97fa3):tr(R.string.ui_hardware_rendering_enabled_19f68b));}).setNegativeButton(tr(R.string.ui_cancel_77dfd2),null).show();}
    private void options(){String[] labels={tx("Reload sign-in"),tx("Rendering mode"),tx("Page text size"),tx("Display information"),tx("Choose another Google account"),tx("Choose a YouTube channel"),tx("Remove app session"),tx("Close")};new AlertDialog.Builder(this).setTitle(tr(R.string.ui_account_options_d67188)).setItems(labels,(d,i)->{if(i==7){finish();return;}if(capturing){notice("Finish the current account check first.");return;}if(rendererLost&&i!=6){notice("Close and reopen this window to restart the renderer.");return;}if(i==0)reload();else if(i==1)renderingMode();else if(i==2)new AlertDialog.Builder(this).setTitle(tr(R.string.ui_page_text_size_9cbed6)).setItems(new String[]{"100%","115%","130%","150%","175%"},(x,j)->{int zoom=new int[]{100,115,130,150,175}[j];app.prefs.put("login_text_zoom",zoom);web.getSettings().setTextZoom(zoom);}).show();else if(i==3)displayInfo();else if(i==4)beginBrowser(googleChooser());else if(i==5)beginBrowser("https://www.youtube.com/channel_switcher");else if(i==6)new AlertDialog.Builder(this).setTitle(tr(R.string.ui_remove_this_app_s_saved_session_ea2a70)).setNegativeButton(tr(R.string.ui_cancel_77dfd2),null).setPositiveButton(tr(R.string.ui_remove_e96390),(x,y)->logout()).show();}).show();}
    private void logout(){work(app.account,()->{if(!sameProfile())throw new YoutubeRepository.ApiException("Profile changed; reopen this window.");app.sessions.clear();app.web.invalidate();return true;},r->CookieManager.getInstance().removeAllCookies(x->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();app.prefs.put("web_session_profile",profile);reload();toast(tr(R.string.ui_app_session_removed_1f25e0));}),this::notice);}
    private void goBack(){if(pending==null&&!rendererLost&&web!=null&&web.canGoBack())web.goBack();else finish();}
    @Override public void onBackPressed(){goBack();}
    @Override protected void onSaveInstanceState(Bundle b){if(web!=null&&!rendererLost&&pending==null){Bundle state=new Bundle();web.saveState(state);b.putBundle("web_state",state);b.putString("web_profile",profile);}super.onSaveInstanceState(b);}
    @Override protected void onPause(){if(web!=null&&!rendererLost){CookieManager.getInstance().flush();web.onPause();}super.onPause();}
    @Override protected void onResume(){super.onResume();if(web!=null&&!rendererLost&&pending==null)web.onResume();}
    @Override protected void onDestroy(){document++;pending=null;if(android.os.Build.VERSION.SDK_INT>=33&&systemBack!=null)getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(systemBack);if(message!=null)message.dismiss();if(web!=null&&!rendererLost){web.stopLoading();web.destroy();}super.onDestroy();}
}
