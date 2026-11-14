package com.simpletoob.app;
/** Time policy accepts the latest run timestamp; completion is tracked separately for the UI. */
public final class ImportPolicy {
    private ImportPolicy(){}
    public static long intervalMillis(int minutes){if(minutes==0)return 0;if(minutes<15||minutes>10080)throw new IllegalArgumentException("Use 15–10,080 minutes, or 0 for manual only.");return minutes*60000L;}
    public static boolean due(boolean signedIn,int minutes,long lastSuccess,long now){return signedIn&&minutes>0&&(lastSuccess<=0||now<lastSuccess||now-lastSuccess>=intervalMillis(minutes));}
    public static int minimum(int subscriptions,int playlists){if(subscriptions==0)return playlists;if(playlists==0)return subscriptions;return Math.min(subscriptions,playlists);}
}
