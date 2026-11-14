package com.simpletoob.app;
import org.json.*;
import java.util.*;
/** Pure protocol parsing/building. Never infer identity from page titles or unselected account rows. */
public final class YoutubeAccountProtocol {
    public static final String ACCOUNTS_ENDPOINT="account/accounts_list";
    private YoutubeAccountProtocol(){}
    public static final class Session {public final String cookies,delegated;public final int account;public final boolean selection;Session(String c,int a,String d,boolean selected){cookies=c;account=a;delegated=d;selection=selected;}}
    public static int accountIndex(Object value)throws JSONException{if(value==null||value==JSONObject.NULL)throw new JSONException("Missing selected account");String s=value.toString();if(!s.matches("[0-9]{1,2}"))throw new JSONException("Invalid account index");return Integer.parseInt(s);}
    public static String delegated(Object value)throws JSONException{String s=value==null||value==JSONObject.NULL?"":value.toString();if(!s.isEmpty()&&!s.matches("[0-9]{1,40}"))throw new JSONException("Invalid selected channel");return s;}
    public static JSONObject session(String cookies,int account,String delegated)throws JSONException{return new JSONObject().put("cookies",cookies).put("account",accountIndex(account)).put("delegated",delegated(delegated)).put("selection",true);}
    public static Session stored(String raw,int fallback)throws JSONException{if(!raw.startsWith("{"))return new Session(raw,accountIndex(fallback),"",false);JSONObject j=new JSONObject(raw);return new Session(j.getString("cookies"),accountIndex(j.opt("account")),delegated(j.opt("delegated")),j.optBoolean("selection",false));}
    public static JSONObject accountsRequest()throws JSONException{return new JSONObject().put("requestType","ACCOUNTS_LIST_REQUEST_TYPE_CHANNEL_SWITCHER").put("callCircumstance","SWITCHING_USERS_FULL");}
    public static Map<String,String> clientHeaders(JSONObject context)throws JSONException{JSONObject client=context.getJSONObject("client");if(!"WEB".equals(client.optString("clientName")))throw new JSONException("Expected YouTube WEB context");String version=client.getString("clientVersion");if(!version.matches("[A-Za-z0-9._-]{1,100}"))throw new JSONException("Invalid client version");Map<String,String> headers=new LinkedHashMap<>();headers.put("X-Youtube-Client-Name","1");headers.put("X-Youtube-Client-Version",version);return headers;}
    public static String selectedName(JSONObject response){Set<String> selected=new LinkedHashSet<>();for(String key:new String[]{"accountItem","accountItemRenderer"})for(JSONObject item:WebJson.objects(response,key))if(item.optBoolean("isSelected")&&!item.optBoolean("isDisabled")){String name=WebJson.text(item.opt("accountName"));if(!name.isEmpty())selected.add(name);}if(selected.size()==1)return selected.iterator().next();if(selected.size()>1)return "";Set<String> active=new LinkedHashSet<>();for(JSONObject header:WebJson.objects(response,"activeAccountHeaderRenderer")){String name=WebJson.text(header.opt("accountName"));if(!name.isEmpty())active.add(name);}return active.size()==1?active.iterator().next():"";}
}
