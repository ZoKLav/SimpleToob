package com.simpletoob.app;
import java.util.*;
import org.json.*;

/** Bounded first-page history; the first successful check seeds without notifying. */
final class SubscriptionUpdates {
    static final class Update {final String snapshot;final List<Video> fresh;Update(String snapshot,List<Video> fresh){this.snapshot=snapshot;this.fresh=fresh;}}
    static Update compare(String previous,List<Video> videos)throws JSONException{
        LinkedHashSet<String> seen=new LinkedHashSet<>();if(previous!=null){JSONArray old=new JSONArray(previous);for(int i=0;i<old.length();i++)if(VideoIds.valid(old.optString(i)))seen.add(old.getString(i));}
        List<Video> fresh=new ArrayList<>();LinkedHashSet<String> merged=new LinkedHashSet<>();
        // Only the prefix before a known upload is new; reordered older entries stay silent.
        boolean prefix=true;for(Video v:videos){if(seen.contains(v.id))prefix=false;if(previous!=null&&prefix&&merged.add(v.id))fresh.add(v);else merged.add(v.id);}
        merged.addAll(seen);JSONArray out=new JSONArray();for(String id:merged){if(out.length()==256)break;out.put(id);}return new Update(out.toString(),fresh);
    }
}
