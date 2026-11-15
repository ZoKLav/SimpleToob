package com.simpletoob.app;

import java.util.*;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.kiosk.*;
import org.schabi.newpipe.extractor.localization.*;
import org.schabi.newpipe.extractor.stream.*;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

/** Actual category charts, with country/localization forced on this extractor rather than global state. */
final class ChartFeed {
    static final class Page {final List<Video> videos;final org.schabi.newpipe.extractor.Page next;Page(List<Video> videos,org.schabi.newpipe.extractor.Page next){this.videos=videos;this.next=next;}}
    static Page fetch(String chart,String language,String region,org.schabi.newpipe.extractor.Page continuation)throws Exception {
        KioskList kiosks=ServiceList.YouTube.getKioskList();kiosks.forceLocalization(new Localization(language,region));kiosks.forceContentCountry(new ContentCountry(region));KioskExtractor extractor=kiosks.getExtractorById(HomePolicy.kiosk(chart),continuation);
        // KioskInfo.getInfo(extractor) requires a fetched extractor; it does not fetch on our behalf.
        extractor.fetchPage();List<StreamInfoItem> items;org.schabi.newpipe.extractor.Page next;
        if(continuation==null){KioskInfo info=KioskInfo.getInfo(extractor);items=info.getRelatedItems();next=info.getNextPage();if(items.isEmpty()&&!info.getErrors().isEmpty())throw new ExtractionException("Could not parse the category chart",info.getErrors().get(0));}
        else {ListExtractor.InfoItemsPage<StreamInfoItem> page=extractor.getPage(continuation);items=page.getItems();next=page.getNextPage();}
        List<Video> rows=new ArrayList<>();Set<String> ids=new HashSet<>();for(StreamInfoItem item:items){String id=VideoIds.parse(item.getUrl());if(id==null||!ids.add(id))continue;rows.add(VideoMetadata.item(new Video(id,item.getName(),item.getUploaderName(),item.getUploaderUrl(),image(item.getThumbnails()),"",item.getTextualUploadDate()==null?"":item.getTextualUploadDate(),item.getDuration()).withAvatar(image(item.getUploaderAvatars())),item));}
        return new Page(Collections.unmodifiableList(rows),next);
    }
    private static String image(List<Image> images){return images==null||images.isEmpty()?"":images.get(images.size()-1).getUrl();}
}
