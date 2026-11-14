package com.simpletoob.app;
public final class PlaybackPolicy {
    private PlaybackPolicy(){}
    public static boolean avc(String codec){return codec!=null&&(codec.toLowerCase(java.util.Locale.ROOT).startsWith("avc")||codec.toLowerCase(java.util.Locale.ROOT).contains("h264"));}
    public static long score(int height,String codec,boolean combined,String preference,int max){
        long under=height<=max?1_000_000L:0;long size=height<=max?height:10000-height;
        long preferred=preference.equals("AVC first")&&avc(codec)?100_000_000L:preference.equals("VP9 first")&&codec!=null&&codec.startsWith("vp9")?100_000_000L:0;
        return preferred+under+size*10+(combined?1:0);
    }
}
