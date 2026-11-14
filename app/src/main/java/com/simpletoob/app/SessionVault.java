package com.simpletoob.app;
import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import java.security.KeyStore;
import java.nio.charset.StandardCharsets;
/** Per-profile credentials; no Google password is collected by native code. All methods use workers. */
public final class SessionVault {
    private Runnable changed=()->{};public void onChanged(Runnable r){changed=r;}
    public String revision(){return storage.getString(profile()+".revision",present()?"legacy":"none");}
    public String owner(){return storage.getString(profile()+".owner","");}
    public String avatar(){return storage.getString(profile()+".avatar","");}
    private final Prefs prefs;private final android.content.SharedPreferences storage;
    SessionVault(Context c,Prefs p){prefs=p;storage=c.getSharedPreferences("web_sessions",Context.MODE_PRIVATE);}
    private javax.crypto.SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);String alias="simpletoob-web-session";if(ks.containsAlias(alias))return (javax.crypto.SecretKey)ks.getKey(alias,null);KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return g.generateKey();}
    public String profile(){return prefs.text("profile","default");}
    public boolean present(){return !storage.getString(profile(),"").isEmpty();}
    public String read()throws Exception{return readSession().cookies;}
    public YoutubeAccountProtocol.Session readSession()throws Exception{String data=storage.getString(profile(),"");if(data.isEmpty())return YoutubeAccountProtocol.stored("",prefs.number("auth_user",0));String[] p=data.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p[0],Base64.NO_WRAP)));String raw=new String(c.doFinal(Base64.decode(p[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);return YoutubeAccountProtocol.stored(raw,prefs.number("auth_user",0));}
    public void save(String cookies)throws Exception{save(profile(),cookies);}
    public void save(String savedProfile,String cookies)throws Exception{save(savedProfile,cookies,prefs.number("auth_user",0),"");}
    public synchronized void save(String savedProfile,String cookies,int account,String delegated)throws Exception{if(cookies==null||cookies.length()>65536||cookies.contains("\r")||cookies.contains("\n"))throw new IllegalArgumentException("Invalid session");if(WebJson.cookie(cookies,"SAPISID").isEmpty()&&WebJson.cookie(cookies,"__Secure-3PAPISID").isEmpty())throw new IllegalArgumentException("YouTube hasn't provided a signed-in session yet.");String payload=YoutubeAccountProtocol.session(cookies,account,delegated).toString();Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());String value=Base64.encodeToString(c.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(c.doFinal(payload.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!storage.edit().putString(savedProfile,value).putString(savedProfile+".revision",java.util.UUID.randomUUID().toString()).remove(savedProfile+".owner").remove(savedProfile+".avatar").remove(savedProfile+".name").remove(savedProfile+".verified").commit())throw new IllegalStateException("Couldn't save session");changed.run();}
    public synchronized void clear(){String id=profile();storage.edit().remove(id).remove(id+".owner").remove(id+".avatar").putString(id+".revision",java.util.UUID.randomUUID().toString()).remove(id+".name").remove(id+".verified").apply();changed.run();}
    public synchronized boolean setIdentity(String profile,String revision,AccountCollections.Identity i){if(!revision.equals(storage.getString(profile+".revision","legacy")))return false;storage.edit().putString(profile+".owner",i.owner).putString(profile+".avatar",i.avatar).putString(profile+".name",i.name).putLong(profile+".verified",System.currentTimeMillis()).apply();return true;}
    public void setName(String profile,String name){storage.edit().putString(profile+".name",name).putLong(profile+".verified",System.currentTimeMillis()).apply();}
    public boolean verified(){return present()&&System.currentTimeMillis()-storage.getLong(profile()+".verified",0)<6*60*60*1000L;}
    public String name(){return storage.getString(profile()+".name","Session saved; verification pending");}
}
