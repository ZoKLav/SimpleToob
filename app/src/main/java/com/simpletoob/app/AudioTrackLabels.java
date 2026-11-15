package com.simpletoob.app;
import android.content.Context;
import org.schabi.newpipe.extractor.stream.*;
final class AudioTrackLabels {
    private AudioTrackLabels(){}
    static String label(Context context,AudioStream stream){AudioTrackType type=stream.getAudioTrackType();String kind=type==AudioTrackType.ORIGINAL?"Original audio":type==AudioTrackType.DUBBED?"Dubbed audio":type==AudioTrackType.DESCRIPTIVE?"Audio description":type==AudioTrackType.SECONDARY?"Alternate audio":"Unlabelled audio";String language=stream.getAudioLocale()==null?(stream.getAudioTrackName()==null||stream.getAudioTrackName().isEmpty()?I18n.text(context,"Unknown language"):stream.getAudioTrackName()):stream.getAudioLocale().getDisplayName(AppLanguage.locale(context));return I18n.text(context,kind)+" · "+language;}
    static String[] labels(Context context,java.util.List<AudioStream> tracks){String[] result=new String[tracks.size()];for(int i=0;i<result.length;i++)result[i]=label(context,tracks.get(i));return result;}
}
