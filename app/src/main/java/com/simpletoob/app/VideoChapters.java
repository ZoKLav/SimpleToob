package com.simpletoob.app;
import java.util.*;
import org.schabi.newpipe.extractor.stream.StreamSegment;
final class VideoChapters {
    static final class Chapter {final String title;final long start;Chapter(String title,long start){this.title=title;this.start=start;}}
    static List<Chapter> from(List<StreamSegment> segments,long duration){
        TreeMap<Long,Chapter> rows=new TreeMap<>();if(segments!=null)for(StreamSegment s:segments){if(s==null)continue;long start=s.getStartTimeSeconds()*1000L;if(start<0||(duration>0&&start>=duration)||s.getTitle()==null||s.getTitle().trim().isEmpty())continue;rows.putIfAbsent(start,new Chapter(s.getTitle(),start));if(rows.size()>=500)break;}return new ArrayList<>(rows.values());
    }
}
