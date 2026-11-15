package com.simpletoob.app;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
/** Matched WEB-page context + authenticated first-party requests. No personal API key. */
public final class YoutubeWebClient {
    private final SessionVault vault;private final Prefs prefs;private final YoutubeAccountProtocol.Session candidate;private String pinnedProfile="",pinnedRevision="";
    public YoutubeWebClient pin(String profile,String revision){pinnedProfile=profile;pinnedRevision=revision;return this;}
    private void pinCheck()throws Exception{if(!pinnedProfile.isEmpty()&&(!pinnedProfile.equals(vault.profile())||!pinnedRevision.equals(vault.revision())||candidate==null&&!vault.present()))throw problem("Account changed; import stopped.");}
    private JSONObject context;private String cookie="",visitor="",identity="";private String activeProfile="";
    private long bootstrapAt;private int accountIndex;private String delegated="";private boolean selectedContext;
    private static final String WEB_AGENT="Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    public YoutubeWebClient(SessionVault vault,Prefs prefs){this(vault,prefs,null);}
    YoutubeWebClient(SessionVault vault,Prefs prefs,YoutubeAccountProtocol.Session candidate){this.vault=vault;this.prefs=prefs;this.candidate=candidate;}
    public static final class Entry{public final String id,title;public Entry(String id,String title){this.id=id;this.title=title;}}
    public synchronized void invalidate(){bootstrapAt=0;context=null;cookie="";identity="";visitor="";activeProfile="";delegated="";accountIndex=0;selectedContext=false;}
    private void worker(){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Web requests must run on a worker");}
    private JSONObject bootstrap(String video,boolean required)throws Exception{
        worker();pinCheck();String requestedProfile=vault.profile();YoutubeAccountProtocol.Session session=candidate==null?vault.readSession():candidate;cookie=session.cookies;accountIndex=session.account;delegated=session.delegated;selectedContext=session.selection;if(!requestedProfile.equals(vault.profile()))throw problem("Profile changed; retry in the selected profile.");activeProfile=requestedProfile;if(required&&cookie.isEmpty())throw problem("Sign in to YouTube inside SimpleToob first.");
        String url="https://www.youtube.com/"+(video.isEmpty()?"?authuser="+accountIndex:("watch?v="+video+"&authuser="+accountIndex));String html=request(url,null,required);
        JSONObject config=new JSONObject();int at=0;
        for(int i=0;i<60;i++){int index=html.indexOf("ytcfg.set(",at);if(index<0)break;try{JSONObject part=WebJson.objectAfter(html.substring(index),"ytcfg.set(");Iterator<String> keys=part.keys();while(keys.hasNext()){String k=keys.next();config.put(k,part.get(k));}}catch(JSONException ignored){}at=index+10;}
        context=config.optJSONObject("INNERTUBE_CONTEXT");if(context==null)throw problem("YouTube did not provide an account request context. The web page has been kept open.");YoutubeAccountProtocol.clientHeaders(context);if(config.has("SESSION_INDEX")&&!config.isNull("SESSION_INDEX")&&YoutubeAccountProtocol.accountIndex(config.opt("SESSION_INDEX"))!=accountIndex)throw problem("YouTube returned a different account context. Choose the account again.");JSONObject user=context.optJSONObject("user");if(user==null){user=new JSONObject();context.put("user",user);}if(selectedContext){if(delegated.isEmpty())user.remove("onBehalfOfUser");else user.put("onBehalfOfUser",delegated);}else{delegated=YoutubeAccountProtocol.delegated(config.opt("DELEGATED_SESSION_ID"));if(!delegated.isEmpty())user.put("onBehalfOfUser",delegated);}
        visitor=config.optString("VISITOR_DATA");identity=config.optString("ID_TOKEN");bootstrapAt=System.currentTimeMillis();activeProfile=requestedProfile;
        JSONObject data=null;try{data=WebJson.initialData(html);}catch(JSONException ignored){}
        return data==null?new JSONObject():data;
    }
    private void ready()throws Exception{if(context==null||!activeProfile.equals(vault.profile())||System.currentTimeMillis()-bootstrapAt>120000)bootstrap("",true);}
    private JSONObject action(String endpoint,JSONObject body)throws Exception{
        ready();if(!Arrays.asList("account/accounts_list","next","browse","like/like","like/dislike","like/removelike","subscription/subscribe","subscription/unsubscribe","comment/create_comment","comment/create_comment_reply","playlist/create","playlist/edit").contains(endpoint))throw problem("Unsupported account operation");
        JSONObject requestContext=new JSONObject(context.toString());JSONObject client=requestContext.optJSONObject("client");if(client!=null){client.put("hl",prefs.text("language","en"));client.put("gl",prefs.text("region","US"));}body.put("context",requestContext);String raw=request("https://www.youtube.com/youtubei/v1/"+endpoint+"?prettyPrint=false",body,true);JSONObject response=new JSONObject(raw);
        if(response.has("error"))throw problem("YouTube rejected this action. Sign in again or check the account/channel selected in the app.");
        pinCheck();return response;
    }
    public AccountCollections.Identity inspectAccount()throws Exception{String requestedProfile=vault.profile();invalidate();bootstrap("",true);JSONObject response=action(YoutubeAccountProtocol.ACCOUNTS_ENDPOINT,YoutubeAccountProtocol.accountsRequest());String name=YoutubeAccountProtocol.selectedName(response);if(selectedContext&&!YoutubeAccountProtocol.selectedChannelMatches(response,delegated))throw problem("YouTube returned a different account context. Choose the account again.");if(name.isEmpty())throw problem("YouTube didn't confirm one selected account/channel. The previous app account was kept.");if(!requestedProfile.equals(vault.profile()))throw problem("Profile changed while verifying; retry.");pinCheck();return AccountCollections.identity(response,delegated);}
    public String verify()throws Exception{String selectedRevision=vault.revision(),requestedProfile=vault.profile();AccountCollections.Identity identity=inspectAccount();if(!selectedRevision.equals(vault.revision())||!vault.setIdentity(requestedProfile,selectedRevision,identity))throw problem("Account changed during verification; retry.");return identity.name;}
    public synchronized JSONObject homeData(String continuation)throws Exception{pinCheck();JSONObject data=action("browse",continuation.isEmpty()?new JSONObject().put("browseId","FEwhat_to_watch"):new JSONObject().put("continuation",continuation));pinCheck();return data;}
    public List<Video> home()throws Exception{return HomeFeedParser.parse(homeData("")).videos;}
    public List<Video> accountFeed(String browseId)throws Exception{if(!Arrays.asList("FEsubscriptions","FEhistory","FElibrary","FEliked_videos").contains(browseId))throw problem("Unknown feed");return videos(action("browse",new JSONObject().put("browseId",browseId)));}
    public static final class CollectionResult {public final List<AccountCollections.Channel> channels;public final List<AccountCollections.Playlist> playlists;public final String next;CollectionResult(JSONObject data,String kind)throws Exception{JSONObject context=data.optJSONObject("responseContext"),web=context==null?null:context.optJSONObject("mainAppWebResponseContext");if(web!=null&&web.optBoolean("loggedOut"))throw new YoutubeRepository.ApiException("Your YouTube session needs verification. Existing local items were kept.");channels=AccountCollections.channels(data);playlists=AccountCollections.playlists(data);next=AccountCollections.continuation(data);boolean empty=kind.equals("subscriptions")?channels.isEmpty():playlists.isEmpty();if(empty&&next.isEmpty()&&!AccountCollections.explicitEmpty(data))throw new YoutubeRepository.ApiException("YouTube returned an unrecognized collection layout. Existing local items were kept.");}}
    public CollectionResult collection(String kind,String continuation)throws Exception{JSONObject request=YoutubeAccountProtocol.collectionRequest(kind,continuation);pinCheck();JSONObject data=action("browse",request);pinCheck();return new CollectionResult(data,kind);}
    public List<Entry> accountPlaylists()throws Exception{List<Entry> out=new ArrayList<>();Set<String> tokens=new HashSet<>(),ids=new HashSet<>();String token="";for(int page=0;page<500;page++){CollectionResult result=collection("playlists",token);for(AccountCollections.Playlist p:result.playlists)if(ids.add(p.id))out.add(new Entry(p.id,p.title));token=result.next;if(token.isEmpty())return out;if(!tokens.add(token))throw problem("YouTube repeated a playlist page; stopped without duplicating entries.");}throw problem("Playlist index is too large to finish in one import.");}
    public YoutubeRepository.PlaylistResult playlist(String id)throws Exception{return playlist(id,"");}
    public YoutubeRepository.PlaylistResult playlist(String id,String token)throws Exception{String normalized=AccountCollections.playlistId(id);if(normalized==null)throw problem("Invalid playlist");JSONObject data=action("browse",token.isEmpty()?new JSONObject().put("browseId","VL"+normalized):new JSONObject().put("continuation",token));return new YoutubeRepository.PlaylistResult(AccountCollections.playlistTitle(data,"YouTube playlist"),new YoutubeRepository.Page(AccountCollections.videos(data),AccountCollections.continuation(data)));}
    public void rate(String id,String rating)throws Exception{requireVideo(id);String endpoint=rating.equals("like")?"like/like":rating.equals("dislike")?"like/dislike":"like/removelike";confirmed(action(endpoint,new JSONObject().put("target",new JSONObject().put("videoId",id))),false);}
    public void subscribe(String channelId,boolean on)throws Exception{String url=ChannelUrls.parse(channelId);if(url==null||!url.contains("/channel/"))throw problem("This action needs the channel's UC identifier; open a video from that channel first.");String id=url.substring(url.lastIndexOf('/')+1);confirmed(action(on?"subscription/subscribe":"subscription/unsubscribe",new JSONObject().put("channelIds",new JSONArray().put(id))),false);}
    public void comment(String id,String text)throws Exception{
        requireVideo(id);if(text.trim().isEmpty()||text.length()>10000)throw problem("Comments must contain 1–10,000 characters.");
        JSONObject data=bootstrap(id,true);String params=WebJson.first(data,"createCommentParams");
        if(params.isEmpty()){
            JSONObject next=action("next",new JSONObject().put("videoId",id));params=WebJson.first(next,"createCommentParams");
            List<JSONObject> continuations=WebJson.objects(next,"continuationItemRenderer");
            for(int i=0;params.isEmpty()&&i<Math.min(4,continuations.size());i++){
                String continuation=WebJson.first(continuations.get(i),"token");if(continuation.isEmpty())continue;
                JSONObject comments=action("next",new JSONObject().put("continuation",continuation));params=WebJson.first(comments,"createCommentParams");
            }
        }
        if(params.isEmpty())throw problem("YouTube didn't expose a comment submission endpoint for this account/video. No comment was sent.");
        JSONObject result=action("comment/create_comment",new JSONObject().put("createCommentParams",params).put("commentText",text));confirmed(result,true);
    }
    public Entry createPlaylist(String title)throws Exception{if(title.trim().isEmpty()||title.length()>150)throw problem("Use a playlist name up to 150 characters.");JSONObject r=action("playlist/create",new JSONObject().put("title",title).put("privacyStatus","PRIVATE"));String id=r.optString("playlistId");if(!validPlaylist(id))throw problem("YouTube didn't confirm the new playlist. Check your account before retrying.");return new Entry(id,title);}
    public void addToPlaylist(String playlist,String video)throws Exception{requireVideo(video);if(!validPlaylist(playlist))throw problem("Invalid playlist");confirmed(action("playlist/edit",new JSONObject().put("playlistId",playlist).put("actions",new JSONArray().put(new JSONObject().put("action","ACTION_ADD_VIDEO").put("addedVideoId",video)))),false);}
    private void confirmed(JSONObject r,boolean comment)throws Exception{
        String status=r.optString("status");if(status.contains("FAIL")||status.contains("ERROR"))throw problem("YouTube rejected the action; your local state was not changed.");
        if(comment&&WebJson.first(r,"commentId").isEmpty()&&WebJson.objects(r,"createCommentAction").isEmpty()&&!WebJson.first(r,"status").equals("STATUS_SUCCEEDED")&&!WebJson.first(r,"actionResult").equals("STATUS_SUCCEEDED"))throw problem("YouTube's response did not confirm submission. Check comments before retrying; the draft was kept.");
        if(!comment&&r.length()==0)throw problem("YouTube returned no confirmation.");
    }
    private String request(String url,JSONObject body,boolean signed)throws Exception{
        pinCheck();if(!WebJson.youtube(url))throw problem("Account requests are limited to HTTPS YouTube pages.");
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("User-Agent",WEB_AGENT);
        c.setRequestProperty("Accept-Language",prefs.text("language","en")+",en;q=0.7");
        if(signed){if(!activeProfile.equals(vault.profile()))throw problem("Profile changed. Reopen this account action.");c.setRequestProperty("Cookie",cookie);}
        try{if(body!=null){if(signed){c.setRequestProperty("Authorization",WebJson.auth(cookie,System.currentTimeMillis()/1000));c.setRequestProperty("Origin","https://www.youtube.com");c.setRequestProperty("X-Origin","https://www.youtube.com");c.setRequestProperty("X-Goog-AuthUser",Integer.toString(accountIndex));c.setRequestProperty("X-Youtube-Bootstrap-Logged-In","true");if(!delegated.isEmpty())c.setRequestProperty("X-Goog-PageId",delegated);if(!visitor.isEmpty())c.setRequestProperty("X-Goog-Visitor-Id",visitor);if(!identity.isEmpty())c.setRequestProperty("X-Youtube-Identity-Token",identity);}for(Map.Entry<String,String> h:YoutubeAccountProtocol.clientHeaders(body.getJSONObject("context")).entrySet())c.setRequestProperty(h.getKey(),h.getValue());c.setRequestProperty("Accept","application/json");c.setRequestProperty("Referer","https://www.youtube.com/");c.setRequestMethod("POST");c.setRequestProperty("Content-Type","application/json; charset=UTF-8");c.setDoOutput(true);try(OutputStream out=c.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
            int code=c.getResponseCode();if(code<200||code>=300){String operation=url.contains("/account/accounts_list")?"checking the selected account":body==null?"loading the account context":"sending an account request";Diagnostics.record("YouTube HTTP "+code+" during "+operation);throw problem("YouTube returned HTTP "+code+" while "+operation+". The web page has been kept open.");}
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException();if(out.size()+n>16*1024*1024)throw new IOException("Response too large");out.write(b,0,n);}return out.toString(StandardCharsets.UTF_8.name());}
        }catch(IOException e){throw problem("Couldn't confirm YouTube's response. Check your connection; drafts are kept and account writes are not automatically retried.");}finally{c.disconnect();}
    }
    public static List<Video> videos(Object data){return data instanceof JSONObject?AccountCollections.videos((JSONObject)data):Collections.emptyList();}
    public static boolean validPlaylist(String id){return id!=null&&id.matches("[A-Za-z0-9_-]{2,150}");}
    private void requireVideo(String id)throws Exception{if(!VideoIds.valid(id))throw problem("Invalid video ID");}
    private YoutubeRepository.ApiException problem(String message){return new YoutubeRepository.ApiException(message);}
}
