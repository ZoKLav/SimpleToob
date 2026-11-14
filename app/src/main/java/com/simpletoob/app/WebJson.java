package com.simpletoob.app;
import org.json.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
public final class WebJson {
    private WebJson(){}
    public static boolean youtube(String url){try{URI u=URI.create(url);String h=u.getHost();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&h!=null&&(h.equals("youtube.com")||h.endsWith(".youtube.com"));}catch(Exception e){return false;}}
    public static boolean loginHost(String url){if(youtube(url))return true;try{URI u=URI.create(url);String host=u.getHost(),path=u.getPath();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&(u.getPort()==-1||u.getPort()==443)&&("accounts.google.com".equals(host)||"myaccount.google.com".equals(host)||"consent.google.com".equals(host)||"www.google.com".equals(host)&&path!=null&&(path.equals("/accounts/SetSID")||path.equals("/accounts/CheckCookie")));}catch(Exception e){return false;}}
    public static String cookie(String cookies,String name){if(cookies==null)return "";for(String pair:cookies.split(";")){int eq=pair.indexOf('=');if(eq>0&&pair.substring(0,eq).trim().equals(name))return pair.substring(eq+1).trim();}return "";}
    public static String auth(String cookies,long seconds)throws Exception{String sid=cookie(cookies,"SAPISID");if(sid.isEmpty())sid=cookie(cookies,"__Secure-3PAPISID");if(sid.isEmpty())throw new IllegalStateException("Sign in to YouTube inside the app first.");return "SAPISIDHASH "+seconds+"_"+hex(MessageDigest.getInstance("SHA-1").digest((seconds+" "+sid+" https://www.youtube.com").getBytes(StandardCharsets.UTF_8)));}
    public static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte n:b)s.append(String.format(Locale.ROOT,"%02x",n&255));return s.toString();}
    public static JSONObject objectAfter(String html,String marker)throws JSONException{int at=html.indexOf(marker);if(at<0)throw new JSONException("Missing page data");int start=html.indexOf('{',at+marker.length());if(start<0)throw new JSONException("Missing JSON");boolean quote=false,escape=false;int depth=0;for(int i=start;i<html.length();i++){char c=html.charAt(i);if(quote){if(escape)escape=false;else if(c=='\\')escape=true;else if(c=='"')quote=false;}else if(c=='"')quote=true;else if(c=='{')depth++;else if(c=='}'&&--depth==0)return new JSONObject(html.substring(start,i+1));}throw new JSONException("Incomplete JSON");}
    public static JSONObject initialData(String html)throws JSONException{
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:\\bytInitialData|window\\s*\\[\\s*['\"]ytInitialData['\"]\\s*\\])\\s*=").matcher(html);
        if(!m.find())throw new JSONException("Missing initial page data");
        return objectAfter(html.substring(m.end()),"");
    }
    public static List<JSONObject> objects(Object root,String key){List<JSONObject> out=new ArrayList<>();walk(root,key,out,0);return out;}
    private static void walk(Object node,String key,List<JSONObject> out,int depth){if(depth>70||out.size()>=500)return;if(node instanceof JSONObject){JSONObject j=(JSONObject)node;Object found=j.opt(key);if(found instanceof JSONObject)out.add((JSONObject)found);Iterator<String> keys=j.keys();while(keys.hasNext())walk(j.opt(keys.next()),key,out,depth+1);}else if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++)walk(a.opt(i),key,out,depth+1);}}
    public static String first(Object node,String key){return first(node,key,0);}
    private static String first(Object node,String key,int depth){if(depth>70)return "";if(node instanceof JSONObject){JSONObject j=(JSONObject)node;Object value=j.opt(key);if(value instanceof String)return (String)value;Iterator<String> names=j.keys();while(names.hasNext()){String s=first(j.opt(names.next()),key,depth+1);if(!s.isEmpty())return s;}}else if(node instanceof JSONArray){JSONArray a=(JSONArray)node;for(int i=0;i<a.length();i++){String s=first(a.opt(i),key,depth+1);if(!s.isEmpty())return s;}}return "";}
    public static String text(Object node){if(node instanceof String)return (String)node;if(!(node instanceof JSONObject))return "";JSONObject j=(JSONObject)node;if(j.has("simpleText"))return j.optString("simpleText");JSONArray runs=j.optJSONArray("runs");StringBuilder s=new StringBuilder();if(runs!=null)for(int i=0;i<runs.length();i++){JSONObject run=runs.optJSONObject(i);if(run!=null)s.append(run.optString("text"));}if(s.length()==0)s.append(j.optString("content"));return s.toString();}
}
