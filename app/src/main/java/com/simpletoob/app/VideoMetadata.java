package com.simpletoob.app;

import java.time.*;
import java.time.format.*;
import java.text.NumberFormat;
import java.util.*;
import org.json.*;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.*;

/** Reuse metadata already fetched with a feed/player response. No per-card network requests. */
final class VideoMetadata {
    private VideoMetadata(){}
    static String isoDate(String value){try{return LocalDate.parse(value).toString();}catch(Exception ignored){return "";}}
    private static String exact(DateWrapper date){return date==null||date.isApproximation()?"":date.getInstant().atOffset(ZoneOffset.UTC).toLocalDate().toString();}
    static Video item(Video video,StreamInfoItem item){return video.withStats(item.getStreamType()==StreamType.LIVE_STREAM?-1:item.getViewCount(),"",exact(item.getUploadDate()));}
    static Video info(Video video,StreamInfo info){return video.withStats(info.getStreamType()==StreamType.LIVE_STREAM?-1:info.getViewCount(),"",exact(info.getUploadDate()));}
    static Video classic(Video video,JSONObject card){String views=WebJson.text(card.opt("viewCountText"));if(views.isEmpty())views=WebJson.text(card.opt("shortViewCountText"));return video.withPublished(WebJson.text(card.opt("publishedTimeText"))).withStats(-1,views,"");}
    static Video lockup(Video video,JSONObject metadata){JSONObject wrapper=metadata==null?null:metadata.optJSONObject("metadata");if(wrapper!=null&&wrapper.optJSONObject("contentMetadataViewModel")!=null)wrapper=wrapper.optJSONObject("contentMetadataViewModel");JSONArray rows=wrapper==null?null:wrapper.optJSONArray("metadataRows");String views="",date="";
        // Home lockups use an author row followed by views/date. Do not label the author as a view count.
        if(rows!=null&&rows.length()>1){for(int n=rows.length()-1;n>0;n--){JSONObject row=rows.optJSONObject(n);JSONArray parts=row==null?null:row.optJSONArray("metadataParts");if(parts==null||parts.length()==0)continue;JSONObject last=parts.optJSONObject(parts.length()-1);date=last==null?"":WebJson.text(last.opt("text"));if(parts.length()>1){JSONObject first=parts.optJSONObject(0);views=first==null?"":WebJson.text(first.opt("text"));}break;}}
        return video.withPublished(date).withStats(-1,views,"");
    }
    static String summary(Video video,Locale locale,boolean views,boolean date,String viewsPattern){List<String> parts=new ArrayList<>();if(views){if(video.viewCount>=0)parts.add(String.format(locale,viewsPattern,NumberFormat.getIntegerInstance(locale).format(video.viewCount)));else if(!video.viewText.isEmpty())parts.add(video.viewText);}if(date){String published=video.published;if(!video.uploadDate.isEmpty())published=LocalDate.parse(video.uploadDate).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale));if(published!=null&&!published.isEmpty())parts.add(published);}return String.join(" · ",parts);}
}
