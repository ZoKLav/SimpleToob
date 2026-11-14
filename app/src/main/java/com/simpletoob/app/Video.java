package com.simpletoob.app;

import org.json.JSONException;
import org.json.JSONObject;

public final class Video {
    public final String id, title, channel, channelId, thumbnail, description, published;
    public final long duration, metadataAt;
    public Video(String id, String title, String channel, String channelId, String thumbnail,
                 String description, String published, long duration) {
        this(id,title,channel,channelId,thumbnail,description,published,duration,System.currentTimeMillis());
    }
    private Video(String id,String title,String channel,String channelId,String thumbnail,
                  String description,String published,long duration,long metadataAt) {
        if (!VideoIds.valid(id)) throw new IllegalArgumentException("Invalid video ID");
        this.id=id; this.title=title; this.channel=channel; this.channelId=channelId;
        this.thumbnail=thumbnail; this.description=description; this.published=published; this.duration=duration; this.metadataAt=metadataAt;
    }
    public static Video link(String id) {
        return new Video(id, "YouTube video · " + id, "", "", "https://i.ytimg.com/vi/"+id+"/mqdefault.jpg", "", "", 0);
    }
    public JSONObject json() {
        JSONObject j=new JSONObject();
        try {
            j.put("id",id).put("title",title).put("channel",channel).put("channelId",channelId)
                .put("thumbnail",thumbnail).put("description",description).put("published",published).put("duration",duration).put("metadataAt",metadataAt);
        } catch (JSONException e) { throw new IllegalStateException(e); }
        return j;
    }
    public static Video fromJson(JSONObject j) {
        return new Video(j.optString("id"), j.optString("title"), j.optString("channel"), j.optString("channelId"),
            j.optString("thumbnail"), j.optString("description"), j.optString("published"), j.optLong("duration"),j.optLong("metadataAt",System.currentTimeMillis()));
    }
    public String durationLabel() {
        if (duration<=0) return "Live / duration unavailable";
        return duration>=3600 ? String.format(java.util.Locale.ROOT,"%d:%02d:%02d",duration/3600,(duration/60)%60,duration%60)
            : String.format(java.util.Locale.ROOT,"%d:%02d",duration/60,duration%60);
    }
}
