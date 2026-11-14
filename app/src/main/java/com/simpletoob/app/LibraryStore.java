package com.simpletoob.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** All calls go through the application's single disk executor. Local collections and transactional account-import snapshots. */
public final class LibraryStore extends SQLiteOpenHelper {
    private static final long MAX_METADATA_AGE=29L*24*60*60*1000;
    public LibraryStore(Context context) { this(context,"default"); }
    public LibraryStore(Context context,String profile) { super(context,profile.equals("default")?"simpletoob.db":"profile_"+profile+".db",null,4); }
    public static final class Subscription {
        public final String url,name;
        Subscription(String url,String name){this.url=url;this.name=name;}
    }
    @Override public void onCreate(SQLiteDatabase db) {
        createSubscriptions(db);createPlaylists(db);createLinks(db);
        db.execSQL("CREATE TABLE videos (id TEXT PRIMARY KEY, metadata TEXT NOT NULL, fetched INTEGER NOT NULL, saved INTEGER NOT NULL DEFAULT 0, saved_at INTEGER NOT NULL DEFAULT 0, watched INTEGER NOT NULL DEFAULT 0, position REAL NOT NULL DEFAULT 0)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { if(oldVersion<2)createSubscriptions(db);if(oldVersion<3)createPlaylists(db);if(oldVersion<4){if(oldVersion>=2)db.execSQL("ALTER TABLE subscriptions ADD COLUMN local_owned INTEGER NOT NULL DEFAULT 1");if(oldVersion>=3){db.execSQL("ALTER TABLE playlists ADD COLUMN owner TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE playlists ADD COLUMN remote_id TEXT NOT NULL DEFAULT ''");db.execSQL("ALTER TABLE playlists ADD COLUMN local_changes INTEGER NOT NULL DEFAULT 0");}createLinks(db);} }
    private void createSubscriptions(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS subscriptions (url TEXT PRIMARY KEY,name TEXT NOT NULL,added INTEGER NOT NULL,local_owned INTEGER NOT NULL DEFAULT 1)");}
    public void subscribe(String url,String name,boolean subscribed){
        String normalized=ChannelUrls.parse(url);if(normalized==null)throw new IllegalArgumentException("Invalid channel URL");
        if(!subscribed){getWritableDatabase().delete("subscriptions","url=?",new String[]{normalized});return;}
        ContentValues c=new ContentValues();c.put("url",normalized);c.put("name",name.isEmpty()?normalized:name);c.put("added",System.currentTimeMillis());c.put("local_owned",1);
        getWritableDatabase().insertWithOnConflict("subscriptions",null,c,SQLiteDatabase.CONFLICT_REPLACE);
    }
    public boolean isSubscribed(String url){String normalized=ChannelUrls.parse(url);if(normalized==null)return false;try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM subscriptions WHERE url=?",new String[]{normalized})){return c.moveToFirst();}}
    public List<Subscription> subscriptions(){List<Subscription> rows=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT url,name FROM subscriptions ORDER BY name COLLATE NOCASE",null)){while(c.moveToNext())rows.add(new Subscription(c.getString(0),c.getString(1)));}return rows;}

    public static final class LocalPlaylist{public final String id,name,remote,owner;public final int size;public final boolean edited;LocalPlaylist(String id,String name,int size,String remote,boolean edited,String owner){this.owner=owner;this.id=id;this.name=name;this.size=size;this.remote=remote;this.edited=edited;}}
    private void createPlaylists(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS playlists (id TEXT PRIMARY KEY,name TEXT NOT NULL,created INTEGER NOT NULL,owner TEXT NOT NULL DEFAULT '',remote_id TEXT NOT NULL DEFAULT '',local_changes INTEGER NOT NULL DEFAULT 0)");db.execSQL("CREATE TABLE IF NOT EXISTS playlist_items (playlist TEXT NOT NULL,video TEXT NOT NULL,metadata TEXT NOT NULL,position INTEGER NOT NULL,PRIMARY KEY(playlist,video))");}
    public String createPlaylist(String name){String id=java.util.UUID.randomUUID().toString();ContentValues c=new ContentValues();c.put("id",id);c.put("name",name);c.put("created",System.currentTimeMillis());getWritableDatabase().insertOrThrow("playlists",null,c);return id;}
    public List<LocalPlaylist> playlists(){List<LocalPlaylist> out=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT p.id,p.name,(SELECT COUNT(*) FROM playlist_items i WHERE i.playlist=p.id),p.remote_id,p.local_changes,p.owner FROM playlists p ORDER BY p.created DESC",null)){while(c.moveToNext())out.add(new LocalPlaylist(c.getString(0),c.getString(1),c.getInt(2),c.getString(3),c.getInt(4)!=0,c.getString(5)));}return out;}
    public void addToPlaylist(String id,Video v){edited(id);int position=0;try(Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM playlist_items WHERE playlist=?",new String[]{id})){if(c.moveToFirst())position=c.getInt(0);}ContentValues c=new ContentValues();c.put("playlist",id);c.put("video",v.id);c.put("metadata",v.json().toString());c.put("position",position);getWritableDatabase().insertWithOnConflict("playlist_items",null,c,SQLiteDatabase.CONFLICT_IGNORE);}
    public List<Video> playlistVideos(String id){List<Video> rows=new ArrayList<>();try(Cursor c=getReadableDatabase().rawQuery("SELECT metadata FROM playlist_items WHERE playlist=? ORDER BY position",new String[]{id})){while(c.moveToNext())try{rows.add(Video.fromJson(new JSONObject(c.getString(0))));}catch(Exception ignored){}}return rows;}
    public void removeFromPlaylist(String id,String video){edited(id);getWritableDatabase().delete("playlist_items","playlist=? AND video=?",new String[]{id,video});}
    public void deletePlaylist(String id){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("playlist_items","playlist=?",new String[]{id});db.delete("playlists","id=?",new String[]{id});db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public void renamePlaylist(String id,String name){edited(id);ContentValues c=new ContentValues();c.put("name",name);getWritableDatabase().update("playlists",c,"id=?",new String[]{id});}
    public void movePlaylistItem(String id,String video,int offset){edited(id);List<Video> rows=playlistVideos(id);int index=-1;for(int i=0;i<rows.size();i++)if(rows.get(i).id.equals(video))index=i;if(index<0||index+offset<0||index+offset>=rows.size())return;java.util.Collections.swap(rows,index,index+offset);SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{for(int i=0;i<rows.size();i++){ContentValues c=new ContentValues();c.put("position",i);db.update("playlist_items",c,"playlist=? AND video=?",new String[]{id,rows.get(i).id});}db.setTransactionSuccessful();}finally{db.endTransaction();}}

    private void createLinks(SQLiteDatabase db){db.execSQL("CREATE TABLE IF NOT EXISTS import_links (owner TEXT NOT NULL,kind TEXT NOT NULL,remote TEXT NOT NULL,local TEXT NOT NULL,PRIMARY KEY(owner,kind,remote))");}
    private void edited(String id){ContentValues c=new ContentValues();c.put("local_changes",1);getWritableDatabase().update("playlists",c,"id=? AND remote_id<>''",new String[]{id});}
    public boolean importedEdited(String id){try(Cursor c=getReadableDatabase().rawQuery("SELECT local_changes FROM playlists WHERE id=? AND remote_id<>''",new String[]{id})){return c.moveToFirst()&&c.getInt(0)!=0;}}
    public void allowRemoteRefresh(String id){ContentValues c=new ContentValues();c.put("local_changes",0);getWritableDatabase().update("playlists",c,"id=?",new String[]{id});}
    private void link(SQLiteDatabase db,String owner,String kind,String remote,String local){ContentValues c=new ContentValues();c.put("owner",owner);c.put("kind",kind);c.put("remote",remote);c.put("local",local);db.insertWithOnConflict("import_links",null,c,SQLiteDatabase.CONFLICT_REPLACE);}
    public void importSubscriptions(String owner,List<AccountCollections.Channel> rows,boolean mirror){SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{java.util.Set<String> current=new java.util.HashSet<>();for(AccountCollections.Channel row:rows){String url=ChannelUrls.parse(row.url);if(url==null)continue;current.add(url);ContentValues c=new ContentValues();c.put("url",url);c.put("name",row.name.isEmpty()?url:row.name);c.put("added",System.currentTimeMillis());c.put("local_owned",0);db.insertWithOnConflict("subscriptions",null,c,SQLiteDatabase.CONFLICT_IGNORE);ContentValues name=new ContentValues();name.put("name",row.name.isEmpty()?url:row.name);db.update("subscriptions",name,"url=? AND local_owned=0",new String[]{url});link(db,owner,"subscription",url,url);}if(mirror){List<String> removed=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT remote FROM import_links WHERE owner=? AND kind='subscription'",new String[]{owner})){while(c.moveToNext())if(!current.contains(c.getString(0)))removed.add(c.getString(0));}for(String url:removed)db.delete("import_links","owner=? AND kind='subscription' AND remote=?",new String[]{owner,url});db.execSQL("DELETE FROM subscriptions WHERE local_owned=0 AND url NOT IN (SELECT local FROM import_links WHERE kind='subscription')");}db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public void importPlaylist(String owner,AccountCollections.Playlist entry,List<Video> rows){String id="yt-"+java.util.UUID.nameUUIDFromBytes((owner+":"+entry.id).getBytes(java.nio.charset.StandardCharsets.UTF_8));SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{if(importedEdited(id)){link(db,owner,"playlist",entry.id,id);db.setTransactionSuccessful();return;}ContentValues p=new ContentValues();p.put("id",id);p.put("name",entry.title);long created=System.currentTimeMillis();try(Cursor existing=db.rawQuery("SELECT created FROM playlists WHERE id=?",new String[]{id})){if(existing.moveToFirst())created=existing.getLong(0);}p.put("created",created);p.put("owner",owner);p.put("remote_id",entry.id);p.put("local_changes",0);db.insertWithOnConflict("playlists",null,p,SQLiteDatabase.CONFLICT_REPLACE);db.delete("playlist_items","playlist=?",new String[]{id});int position=0;for(Video v:rows){ContentValues c=new ContentValues();c.put("playlist",id);c.put("video",v.id);c.put("metadata",v.json().toString());c.put("position",position++);db.insertWithOnConflict("playlist_items",null,c,SQLiteDatabase.CONFLICT_IGNORE);}link(db,owner,"playlist",entry.id,id);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public void finishPlaylistImport(String owner,java.util.Set<String> ids,boolean mirror){if(!mirror)return;SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{List<String[]> remove=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT remote,local FROM import_links WHERE owner=? AND kind='playlist'",new String[]{owner})){while(c.moveToNext())if(!ids.contains(c.getString(0)))remove.add(new String[]{c.getString(0),c.getString(1)});}for(String[] pair:remove){if(!importedEdited(pair[1])){db.delete("playlist_items","playlist=?",new String[]{pair[1]});db.delete("playlists","id=? AND owner=?",new String[]{pair[1],owner});db.delete("import_links","owner=? AND kind='playlist' AND remote=?",new String[]{owner,pair[0]});}}db.setTransactionSuccessful();}finally{db.endTransaction();}}

    private void ensure(Video v) {
        SQLiteDatabase db=getWritableDatabase();
        ContentValues insert=new ContentValues(); insert.put("id",v.id); insert.put("metadata",v.json().toString()); insert.put("fetched",v.metadataAt);
        db.insertWithOnConflict("videos",null,insert,SQLiteDatabase.CONFLICT_IGNORE);
        // Do not overwrite richer fetched metadata with a pasted bare link.
        if(!v.channel.isEmpty()) {
            ContentValues update=new ContentValues(); update.put("metadata",v.json().toString()); update.put("fetched",v.metadataAt);
            db.update("videos",update,"id=?",new String[]{v.id});
        }
    }
    public void remember(Video v,double position) {
        ensure(v); ContentValues c=new ContentValues(); c.put("watched",System.currentTimeMillis());
        c.put("position",Math.max(0,position)); getWritableDatabase().update("videos",c,"id=?",new String[]{v.id});
    }
    public void save(Video v,boolean saved) {
        ensure(v); ContentValues c=new ContentValues(); c.put("saved",saved?1:0);
        c.put("saved_at",saved?System.currentTimeMillis():0); getWritableDatabase().update("videos",c,"id=?",new String[]{v.id});
    }
    public boolean isSaved(String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT saved FROM videos WHERE id=?",new String[]{id})) { return c.moveToFirst() && c.getInt(0)==1; }
    }
    public double position(String id) {
        try(Cursor c=getReadableDatabase().rawQuery("SELECT position FROM videos WHERE id=?",new String[]{id})) { return c.moveToFirst()?c.getDouble(0):0; }
    }
    public List<Video> list(boolean saved,String sort) {
        expireMetadata();
        String order="saved_at DESC";
        if(!saved) order="watched DESC";
        else if(sort.equals("Oldest first")) order="saved_at ASC";
        List<Video> list=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("videos",new String[]{"id","metadata","fetched"},saved?"saved=1":"watched>0",null,null,null,order,"500")) {
            while(c.moveToNext()) {
                try { list.add(System.currentTimeMillis()-c.getLong(2)>MAX_METADATA_AGE?Video.link(c.getString(0)):Video.fromJson(new JSONObject(c.getString(1)))); }
                catch(Exception ignored) { /* One corrupt row must not break the library. */ }
            }
        }
        if(saved && sort.equals("Title")) list.sort((a,b)->a.title.compareToIgnoreCase(b.title));
        return list;
    }
    public void clearHistory() {
        SQLiteDatabase db=getWritableDatabase(); ContentValues c=new ContentValues(); c.put("watched",0); c.put("position",0);
        db.update("videos",c,null,null); db.delete("videos","saved=0",null);
    }
    public void clearAll() { getWritableDatabase().delete("videos",null,null); }
    public void expireMetadata() {
        SQLiteDatabase db=getWritableDatabase();
        List<String> expired=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT id FROM videos WHERE fetched<?",new String[]{Long.toString(System.currentTimeMillis()-MAX_METADATA_AGE)})) {
            while(c.moveToNext()) expired.add(c.getString(0));
        }
        db.beginTransaction();
        try {
            for(String id:expired) {
                ContentValues v=new ContentValues(); v.put("metadata",Video.link(id).json().toString()); v.put("fetched",System.currentTimeMillis());
                db.update("videos",v,"id=?",new String[]{id});
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
