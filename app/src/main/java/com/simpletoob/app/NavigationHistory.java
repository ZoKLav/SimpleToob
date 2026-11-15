package com.simpletoob.app;

import java.util.*;
import org.json.*;

/** Section history, separate from redraws and refreshes. Contains no video lists or account data. */
final class NavigationHistory {
    private static final int LIMIT=32;
    static final class Route {
        final String screen,query,channel,category;
        Route(String screen,String query,String channel,String category){
            this.screen=Arrays.asList("Home","Search","Subs","Library","Settings","Saved","History").contains(screen)?screen:"Home";
            this.query=this.screen.equals("Search")?bounded(query,2048):"";
            this.channel=this.screen.equals("Search")?bounded(channel,512):"";
            this.category=this.screen.equals("Settings")?bounded(category,128):"";
        }
        private static String bounded(String text,int max){return text==null?"":text.substring(0,Math.min(text.length(),max));}
        @Override public boolean equals(Object value){if(!(value instanceof Route))return false;Route r=(Route)value;return screen.equals(r.screen)&&query.equals(r.query)&&channel.equals(r.channel)&&category.equals(r.category);}
        @Override public int hashCode(){return Objects.hash(screen,query,channel,category);}
        JSONObject json()throws JSONException{return new JSONObject().put("screen",screen).put("query",query).put("channel",channel).put("category",category);}
        static Route read(JSONObject j){return new Route(j.optString("screen","Home"),j.optString("query"),j.optString("channel"),j.optString("category"));}
        Route parent(){if(screen.equals("Settings")&&!category.isEmpty())return new Route("Settings","","","");if(screen.equals("Saved")||screen.equals("History"))return new Route("Library","","","");return screen.equals("Home")?null:new Route("Home","","","");}
    }
    private final ArrayDeque<Route> previous=new ArrayDeque<>();
    private Route current;
    NavigationHistory(Route initial){current=initial;}
    void visit(Route route){if(current.equals(route))return;if(previous.size()==LIMIT)previous.removeFirst();previous.addLast(current);current=route;}
    Route back(boolean useParents){Route route=previous.pollLast();if(route==null&&useParents)route=current.parent();if(route!=null)current=route;return route;}
    boolean hasHistory(){return !previous.isEmpty();}
    String save(){try{JSONArray rows=new JSONArray();for(Route route:previous)rows.put(route.json());return new JSONObject().put("current",current.json()).put("previous",rows).toString();}catch(JSONException e){return "";}}
    void restore(String json){try{JSONObject value=new JSONObject(json);if(!current.equals(Route.read(value.getJSONObject("current"))))return;JSONArray rows=value.getJSONArray("previous");ArrayDeque<Route> restored=new ArrayDeque<>();for(int n=Math.max(0,rows.length()-LIMIT);n<rows.length();n++)restored.addLast(Route.read(rows.getJSONObject(n)));previous.clear();previous.addAll(restored);}catch(Exception ignored){/* Old or invalid saved state starts a fresh section history. */}}
}
