package com.simpletoob.app;
import java.net.URI;
/** Authentication entry points must never be confused with YouTube's legacy channel routes. */
public final class AccountRoutes {
    public static final String SIGN_IN="https://www.youtube.com/signin";
    private AccountRoutes(){}
    public static String entry(String requested){
        if(!WebJson.loginHost(requested)||channel(requested))return SIGN_IN;
        try{URI u=URI.create(requested);String path=u.getPath();if(WebJson.youtube(requested)&&(path==null||path.isEmpty()||path.equals("/")||path.equals("/account")||path.equals("/account/")))return SIGN_IN;return requested;}catch(Exception e){return SIGN_IN;}
    }
    public static boolean channel(String url){if(!WebJson.youtube(url))return false;try{String p=URI.create(url).getPath();return p!=null&&(p.equals("/account")||p.equals("/account/")||p.startsWith("/@")||p.startsWith("/channel/")||p.startsWith("/user/")||p.startsWith("/c/"));}catch(Exception e){return false;}}
}
