package com.simpletoob.app;
import org.json.*;
import java.util.*;
final class Profiles {
    static final class Profile {final String id,name;Profile(String id,String name){this.id=id;this.name=name;}}
    static List<Profile> list(Prefs prefs){
        List<Profile> rows=new ArrayList<>();rows.add(new Profile("default","Default"));
        try{JSONArray a=new JSONArray(prefs.text("profiles","[]"));for(int i=0;i<a.length();i++){JSONObject p=a.getJSONObject(i);String id=p.optString("id");if(id.matches("[a-f0-9-]{36}"))rows.add(new Profile(id,p.optString("name","Profile")));}}catch(Exception ignored){}return rows;
    }
    static String name(Prefs p){String id=p.text("profile","default");for(Profile row:list(p))if(row.id.equals(id))return row.name;return "Default";}
    static Profile create(Prefs p,String name)throws Exception{
        String id=UUID.randomUUID().toString();JSONArray a=new JSONArray(p.text("profiles","[]"));a.put(new JSONObject().put("id",id).put("name",name));p.put("profiles",a.toString());return new Profile(id,name);
    }
}
