package com.simpletoob.app;
import android.content.Context;
import android.content.SharedPreferences;
public final class Prefs {
    final SharedPreferences data;
    Prefs(Context c){data=c.getSharedPreferences("preferences",Context.MODE_PRIVATE);data.edit().remove("api_key").apply();}
    public String text(String key,String fallback){return data.getString(key,fallback);}
    public int number(String key,int fallback){return data.getInt(key,fallback);}
    public boolean flag(String key,boolean fallback){return data.getBoolean(key,fallback);}
    public void put(String key,String value){data.edit().putString(key,value).apply();}
    public void put(String key,int value){data.edit().putInt(key,value).apply();}
    public void put(String key,boolean value){data.edit().putBoolean(key,value).apply();}
}
