package com.simpletoob.app;

import android.content.Context;
import android.text.Html;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.stream.*;
import org.schabi.newpipe.extractor.search.SearchInfo;
import org.schabi.newpipe.extractor.linkhandler.*;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabInfo;
import org.schabi.newpipe.extractor.comments.*;
import org.schabi.newpipe.extractor.localization.Localization;
import java.util.*;

/** Anonymous public website extraction. No developer API keys, embedded players or ad sources. */
public final class YoutubeRepository {
    public static final class Page {
        public final List<Video> videos;public final String next;
        Page(List<Video> videos,String next){this.videos=videos;this.next=next;}
    }
    public static final class Comment {
        public final String author,body,text,replies,avatar;public final long likes,replyCount;
        Comment(String author,String body,long likes,long replyCount,String replies,String avatar){this.author=author;this.body=body;this.likes=likes;this.replyCount=replyCount;this.text=author+"\n"+body;this.replies=replies;this.avatar=avatar;}
    }
    public static final class Comments {
        public final List<Comment> rows;public final String next;
        Comments(List<Comment> rows,String next){this.rows=rows;this.next=next;}
    }
    public static final class ApiException extends Exception { ApiException(String message){super(message);} }
    public static final class Media {
        public final Video video;public final StreamInfo info;public final List<Choice> choices;public final List<AudioStream> audioTracks;public final String audioTrackId;
        Media(Video video,StreamInfo info,List<Choice> choices,List<AudioStream> audioTracks,String audioTrackId){this.video=video;this.info=info;this.choices=choices;this.audioTracks=audioTracks;this.audioTrackId=audioTrackId;}
    }
    public static final class Choice {
        public final String label,videoUrl,audioUrl,codec,mime;
        public final boolean hls;
        Choice(String label,String video,String audio){this(label,video,audio,"","video/mp4",false);}
        Choice(String label,String video,String audio,String codec,String mime,boolean hls){this.label=label;videoUrl=video;audioUrl=audio;this.codec=codec==null?"":codec;this.mime=mime;this.hls=hls;}
        public boolean downloadable(){return !hls&&mime.equals("video/mp4");}
        public boolean separate(){return !audioUrl.isEmpty();}
    }
    private static final class Continuation {
        org.schabi.newpipe.extractor.Page page;SearchQueryHandler search;ListLinkHandler channel;String comments;
        Continuation(org.schabi.newpipe.extractor.Page p){page=p;}
    }
    private final LinkedHashMap<String,Continuation> pages=new LinkedHashMap<>();
    private final LinkedHashMap<String,String> chosenAudio=new LinkedHashMap<>();
    private final LinkedHashMap<String,Media> mediaCache=new LinkedHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String,java.util.concurrent.FutureTask<Media>> inFlight=new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String,Long> fetched=new HashMap<>();
    private final Prefs prefs;private volatile boolean initialized;private volatile String localeKey="";
    YoutubeRepository(Context context,Prefs prefs){this.prefs=prefs;}
    private void initialize(){if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Extraction must run off the UI thread");String key=prefs.text("language","en")+":"+prefs.text("region","US");if(!initialized||!localeKey.equals(key))synchronized(this){Localization locale=new Localization(prefs.text("language","en"),prefs.text("region","US"));if(!initialized){NewPipe.init(new WebDownloader(),locale);initialized=true;}else NewPipe.setupLocalization(locale);localeKey=key;}}
    public synchronized void invalidateMedia(String id){mediaCache.remove(id);fetched.remove(id);}
    public synchronized void clearCache(){pages.clear();mediaCache.clear();fetched.clear();}
    private synchronized String remember(Continuation c) {
        if(!org.schabi.newpipe.extractor.Page.isValid(c.page))return "";
        if(pages.size()>=64)pages.remove(pages.keySet().iterator().next());String key=UUID.randomUUID().toString();pages.put(key,c);return key;
    }
    private synchronized Continuation continuation(String key) throws ApiException {
        Continuation c=pages.get(key);if(c==null)throw new ApiException("This page expired. Refresh the list.");return c;
    }
    public Page popular(String token)throws Exception{return search(prefs.text("home_query","new videos"),"relevance","any",token,"");}
    public Page search(String query,String order,String duration,String token,String channelUrl)throws Exception {
        initialize();try {
            List<? extends InfoItem> items;org.schabi.newpipe.extractor.Page next;Continuation c;
            if(!token.isEmpty()) {
                c=continuation(token);ListExtractor.InfoItemsPage<InfoItem> p;
                if(c.channel!=null)p=ChannelTabInfo.getMoreItems(ServiceList.YouTube,c.channel,c.page);
                else p=SearchInfo.getMoreItems(ServiceList.YouTube,c.search,c.page);
                items=p.getItems();next=p.getNextPage();
            } else if(!channelUrl.isEmpty()) {
                ChannelInfo channel=ChannelInfo.getInfo(ServiceList.YouTube,channelUrl);
                ListLinkHandler tab=channel.getTabs().stream().filter(t->t.getContentFilters().contains("videos")).findFirst().orElse(null);
                if(tab==null)throw new ApiException("This channel doesn't expose a videos tab.");
                ChannelTabInfo p=ChannelTabInfo.getInfo(ServiceList.YouTube,tab);items=p.getRelatedItems();next=p.getNextPage();c=new Continuation(next);c.channel=tab;
            } else {
                SearchQueryHandler handler=ServiceList.YouTube.getSearchQHFactory().fromQuery(query,Collections.singletonList("videos"),"");
                SearchInfo p=SearchInfo.getInfo(ServiceList.YouTube,handler);items=p.getRelatedItems();next=p.getNextPage();c=new Continuation(next);c.search=handler;
            }
            List<Video> rows=new ArrayList<>();for(InfoItem item:items)if(item instanceof StreamInfoItem){Video v=video((StreamInfoItem)item);if(v!=null)rows.add(v);}
            // These are explicit local filters over the loaded page, not fake server-side ordering.
            if(duration.equals("short"))rows.removeIf(v->v.duration>=240);
            if(duration.equals("medium"))rows.removeIf(v->v.duration<240||v.duration>1200);
            if(duration.equals("long"))rows.removeIf(v->v.duration<=1200);
            c.page=next;return new Page(rows,remember(c));
        }catch(ApiException e){throw e;}catch(Exception e){throw extractionError(e);}
    }
    public String channelAvatar(String channel)throws Exception{initialize();String url=ChannelUrls.parse(channel);if(url==null)return "";return thumbnail(ChannelInfo.getInfo(ServiceList.YouTube,url).getAvatars());}
    /** Called by the media loader, never by the UI thread. No video bytes are fetched for audio playback. */
    private List<AudioStream> validAudio(StreamInfo info){List<AudioStream> result=new ArrayList<>();for(AudioStream stream:info.getAudioStreams())if(stream.isUrl()&&WebDownloader.mediaUrl(stream.getContent()))result.add(stream);return result;}
    public String audio(String id)throws Exception{Media m=media(id);List<AudioStream> pool=AudioTrackPolicy.selected(validAudio(m.info),m.audioTrackId);AudioStream stream=pool.stream().max(Comparator.comparingInt((AudioStream a)->a.getFormat()==MediaFormat.M4A?1:0).thenComparingInt(AudioStream::getAverageBitrate)).orElse(null);if(stream==null)throw new ApiException("No public audio-only stream is available for this video.");return stream.getContent();}
    public Media selectAudioTrack(String id,String key)throws Exception{Media current=media(id);synchronized(this){if(current.audioTracks.stream().noneMatch(a->AudioTrackPolicy.key(a).equals(key)))throw new ApiException("This audio track is no longer available. Reload the video.");if(chosenAudio.size()>=64)chosenAudio.remove(chosenAudio.keySet().iterator().next());chosenAudio.put(id,key);Media updated=compose(id,current.info);mediaCache.put(id,updated);return updated;}}
    public Video details(String id)throws Exception{return media(id).video;}
    public Media media(String id)throws Exception {
        if(android.os.Looper.myLooper()==android.os.Looper.getMainLooper())throw new IllegalStateException("Media extraction must run off the UI thread");
        java.util.concurrent.FutureTask<Media> task=new java.util.concurrent.FutureTask<>(()->extractMedia(id));java.util.concurrent.FutureTask<Media> active=inFlight.putIfAbsent(id,task);
        if(active!=null){try{return active.get();}catch(java.util.concurrent.ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw new ApiException("Media extraction failed");}}
        try{task.run();return task.get();}catch(java.util.concurrent.ExecutionException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw new ApiException("Media extraction failed");}finally{inFlight.remove(id,task);}
    }
    private Media extractMedia(String id)throws Exception {
        initialize();if(!VideoIds.valid(id))throw new ApiException("Invalid video ID");
        synchronized(this){Media m=mediaCache.get(id);if(m!=null && System.currentTimeMillis()-fetched.getOrDefault(id,0L)<3*60*1000)return m;}
        try {
            StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,"https://www.youtube.com/watch?v="+id);
            Media m=compose(id,info);synchronized(this){if(mediaCache.size()>=8){String first=mediaCache.keySet().iterator().next();mediaCache.remove(first);fetched.remove(first);}mediaCache.put(id,m);fetched.put(id,System.currentTimeMillis());}return m;
        }catch(Exception e){throw extractionError(e);}
    }
    private Media compose(String id,StreamInfo info){
        Video v=new Video(id,info.getName(),info.getUploaderName(),info.getUploaderUrl(),thumbnail(info.getThumbnails()),plain(info.getDescription()),info.getTextualUploadDate(),info.getDuration()).withAvatar(thumbnail(info.getUploaderAvatars()));
            List<Choice> choices=new ArrayList<>();List<AudioStream> valid=validAudio(info);String explicit;synchronized(this){explicit=chosenAudio.getOrDefault(id,"");}List<AudioStream> pool=AudioTrackPolicy.selected(valid,explicit);List<AudioStream> tracks=AudioTrackPolicy.tracks(valid);
            AudioStream mp4=pool.stream().filter(a->a.isUrl()&&a.getFormat()==MediaFormat.M4A&&WebDownloader.mediaUrl(a.getContent())).max(Comparator.comparingInt(AudioStream::getAverageBitrate)).orElse(null);
            AudioStream webm=pool.stream().filter(a->a.isUrl()&&a.getFormat()!=MediaFormat.M4A&&WebDownloader.mediaUrl(a.getContent())).max(Comparator.comparingInt(AudioStream::getAverageBitrate)).orElse(null);
            for(VideoStream s:info.getVideoStreams())if(s.isUrl()&&!s.isVideoOnly()&&s.getFormat()!=null&&WebDownloader.mediaUrl(s.getContent()))choices.add(new Choice(s.getResolution()+" · "+s.getFormat().getName()+" · "+s.getCodec(),s.getContent(),"",s.getCodec(),s.getFormat().getMimeType(),false));
            for(VideoStream s:info.getVideoOnlyStreams())if(s.isUrl()&&s.getFormat()!=null&&WebDownloader.mediaUrl(s.getContent())){
                AudioStream audio=s.getFormat()==MediaFormat.MPEG_4?mp4:webm;
                if(audio!=null)choices.add(new Choice(s.getResolution()+" · "+s.getFormat().getName()+" · "+s.getCodec(),s.getContent(),audio.getContent(),s.getCodec(),s.getFormat().getMimeType(),false));
            }
            if(WebDownloader.mediaUrl(info.getHlsUrl()))choices.add(new Choice("Live · HLS",info.getHlsUrl(),"","","application/x-mpegURL",true));
            if(tracks.size()>1&&choices.stream().anyMatch(Choice::separate))choices.removeIf(c->!c.separate());
            choices.sort((a,b)->Integer.compare(resolution(b.label),resolution(a.label)));
                    return new Media(v,info,choices,tracks,pool.isEmpty()?"":AudioTrackPolicy.key(pool.get(0)));
    }
    public Comments comments(String id,String token,String ignored)throws Exception {
        initialize();String url="https://www.youtube.com/watch?v="+id;
        try {
            List<CommentsInfoItem> items;org.schabi.newpipe.extractor.Page next;
            if(token.isEmpty()){CommentsInfo i=CommentsInfo.getInfo(ServiceList.YouTube,url);if(i.isCommentsDisabled())throw new ApiException("Comments are disabled for this video.");items=i.getRelatedItems();next=i.getNextPage();}
            else {Continuation c=continuation(token);url=c.comments;ListExtractor.InfoItemsPage<CommentsInfoItem> p=CommentsInfo.getMoreItems(ServiceList.YouTube,url,c.page);items=p.getItems();next=p.getNextPage();}
            List<Comment> rows=new ArrayList<>();for(CommentsInfoItem i:items){Continuation r=new Continuation(i.getReplies());r.comments=url;rows.add(new Comment(i.getUploaderName(),plain(i.getCommentText()),i.getLikeCount(),i.getReplyCount(),remember(r),thumbnail(i.getUploaderAvatars())));}
            Continuation n=new Continuation(next);n.comments=url;return new Comments(rows,remember(n));
        }catch(ApiException e){throw e;}catch(Exception e){throw extractionError(e);}
    }
    public static final class CollectionEntry{public final String title,url,kind;CollectionEntry(String title,String url,String kind){this.title=title;this.url=url;this.kind=kind;}}
    public static final class CollectionPage{public final List<CollectionEntry> entries;public final String next;CollectionPage(List<CollectionEntry> entries,String next){this.entries=entries;this.next=next;}}
    public CollectionPage collections(String query,String type,String token)throws Exception {
        initialize();try{List<InfoItem> items;org.schabi.newpipe.extractor.Page next;SearchQueryHandler handler;
            if(token.isEmpty()){handler=ServiceList.YouTube.getSearchQHFactory().fromQuery(query,Collections.singletonList(type),"");SearchInfo info=SearchInfo.getInfo(ServiceList.YouTube,handler);items=info.getRelatedItems();next=info.getNextPage();}
            else{Continuation c=continuation(token);handler=c.search;ListExtractor.InfoItemsPage<InfoItem> page=SearchInfo.getMoreItems(ServiceList.YouTube,handler,c.page);items=page.getItems();next=page.getNextPage();}
            List<CollectionEntry> rows=new ArrayList<>();for(InfoItem item:items){String kind=item instanceof org.schabi.newpipe.extractor.playlist.PlaylistInfoItem?"playlist":item instanceof org.schabi.newpipe.extractor.channel.ChannelInfoItem?"channel":"video";if(WebJson.youtube(item.getUrl())||VideoIds.parse(item.getUrl())!=null)rows.add(new CollectionEntry(item.getName(),item.getUrl(),kind));}Continuation c=new Continuation(next);c.search=handler;return new CollectionPage(rows,remember(c));
        }catch(Exception e){throw extractionError(e);}
    }
    public static final class PlaylistResult {public final String title;public final Page page;PlaylistResult(String title,Page page){this.title=title;this.page=page;}}
    public PlaylistResult playlist(String url,String token)throws Exception {
        initialize();try {
            java.util.List<org.schabi.newpipe.extractor.stream.StreamInfoItem> items;org.schabi.newpipe.extractor.Page next;String title="Playlist";
            if(token.isEmpty()){org.schabi.newpipe.extractor.playlist.PlaylistInfo info=org.schabi.newpipe.extractor.playlist.PlaylistInfo.getInfo(ServiceList.YouTube,url);items=info.getRelatedItems();next=info.getNextPage();title=info.getName();}
            else{Continuation c=continuation(token);org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage<org.schabi.newpipe.extractor.stream.StreamInfoItem> page=org.schabi.newpipe.extractor.playlist.PlaylistInfo.getMoreItems(ServiceList.YouTube,url,c.page);items=page.getItems();next=page.getNextPage();}
            java.util.List<Video> rows=new java.util.ArrayList<>();for(org.schabi.newpipe.extractor.stream.StreamInfoItem i:items){Video v=video(i);if(v!=null)rows.add(v);}return new PlaylistResult(title,new Page(rows,remember(new Continuation(next))));
        }catch(Exception e){throw extractionError(e);}
    }
    private static Video video(StreamInfoItem i){String id=VideoIds.parse(i.getUrl());if(id==null)return null;return new Video(id,i.getName(),i.getUploaderName(),i.getUploaderUrl(),thumbnail(i.getThumbnails()),i.getShortDescription()==null?"":i.getShortDescription(),i.getTextualUploadDate()==null?"":i.getTextualUploadDate(),i.getDuration()).withAvatar(thumbnail(i.getUploaderAvatars()));}
    private static String thumbnail(List<Image> images){return images==null||images.isEmpty()?"":images.get(images.size()-1).getUrl();}
    private static String plain(Description d){if(d==null)return "";return d.type()==Description.Type.HTML?Html.fromHtml(d.content(),Html.FROM_HTML_MODE_LEGACY).toString():d.content();}
    public static int resolution(String label){try{String n=label.split("p")[0].replaceAll("[^0-9]","");return Integer.parseInt(n);}catch(Exception e){return 0;}}
    private ApiException extractionError(Exception e){
        if(e instanceof ApiException)return (ApiException)e;
        String type=e.getClass().getSimpleName();
        if(type.contains("ReCaptcha")||type.contains("ContentNotAvailable")||type.contains("AgeRestricted"))return new ApiException("YouTube requires verification or restricts this video. SimpleToob won't bypass that or switch to ad-supported playback.");
        return new ApiException("Couldn't read YouTube's current pages ("+type+"). Check your connection and retry. Extraction may need an update; no ads will be loaded as a fallback.");
    }
}
