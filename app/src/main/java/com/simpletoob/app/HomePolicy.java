package com.simpletoob.app;

/** Stable Home state and responsive layout. Display labels never become preference/account IDs. */
final class HomePolicy {
    static final String POPULAR="popular",FOR_YOU="for_you";
    static String mode(String value){return FOR_YOU.equals(value)?FOR_YOU:POPULAR;}
    static String chart(String value){return "gaming".equals(value)||"trailers".equals(value)?value:"music";}
    static String kiosk(String chart){switch(chart(chart)){case "gaming":return "trending_gaming";case "trailers":return "trending_movies_and_shows";default:return "trending_music";}}
    static String chartLabel(String chart){switch(chart(chart)){case "gaming":return "Gaming";case "trailers":return "Trailers";default:return "Music";}}
    static String preference(String profile,String field){return "home."+profile+"."+field;}
    static String defaultMode(boolean signedIn){return signedIn?FOR_YOU:POPULAR;}
    static String scope(String profile,String revision,int account,String language,String region){return profile+"\n"+revision+"\n"+account+"\n"+language+"\n"+region;}
    static int columns(int widthDp,boolean compact,boolean grid){if(compact||!grid||widthDp<700)return 1;return widthDp>=1050?3:2;}
}
