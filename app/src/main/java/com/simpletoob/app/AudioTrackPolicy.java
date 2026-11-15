package com.simpletoob.app;

import java.util.*;
import org.schabi.newpipe.extractor.stream.*;

/** Original role outranks bitrate. No UI language, content locale or device region enters this policy. */
final class AudioTrackPolicy {
    private AudioTrackPolicy(){}
    static int role(AudioStream stream){AudioTrackType type=stream.getAudioTrackType();return type==AudioTrackType.ORIGINAL?4:type==null?3:type==AudioTrackType.SECONDARY?2:type==AudioTrackType.DUBBED?1:0;}
    static String key(AudioStream stream){String id=stream.getAudioTrackId();if(id!=null&&!id.isEmpty())return id;return (stream.getAudioLocale()==null?"und":stream.getAudioLocale().toLanguageTag())+":"+(stream.getAudioTrackType()==null?"unknown":stream.getAudioTrackType().name());}
    static List<AudioStream> selected(List<AudioStream> streams,String explicit){
        if(streams.isEmpty())return Collections.emptyList();String chosen=explicit;
        if(chosen==null||chosen.isEmpty()||streams.stream().noneMatch(s->key(s).equals(explicit))){AudioStream best=Collections.max(streams,Comparator.comparingInt(AudioTrackPolicy::role).thenComparingInt(AudioStream::getAverageBitrate).thenComparing(AudioTrackPolicy::key));chosen=key(best);}
        List<AudioStream> result=new ArrayList<>();for(AudioStream stream:streams)if(key(stream).equals(chosen))result.add(stream);return result;
    }
    static List<AudioStream> tracks(List<AudioStream> streams){LinkedHashMap<String,AudioStream> tracks=new LinkedHashMap<>();for(AudioStream stream:streams){String key=key(stream);AudioStream previous=tracks.get(key);if(previous==null||previous.getAverageBitrate()<stream.getAverageBitrate())tracks.put(key,stream);}List<AudioStream> result=new ArrayList<>(tracks.values());result.sort(Comparator.comparingInt(AudioTrackPolicy::role).reversed().thenComparing(AudioTrackPolicy::key));return result;}
}
