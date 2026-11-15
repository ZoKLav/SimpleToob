package com.simpletoob.app;
import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
/** Real Android SQLite transactions: idempotency, mirror ownership and edited local copies. */
@RunWith(AndroidJUnit4.class)
public final class LibraryImportTest {
    @Test public void importsProtectManualAndEditedData(){Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();String profile="test-"+UUID.randomUUID();LibraryStore db=new LibraryStore(c,profile);try{
        String manual="UCabcdefghijklmnopqrstuv",remote="UCzyxwvutsrqponmlkjihgfe";db.subscribe(manual,"Manual",true);db.importSubscriptions("owner",Arrays.asList(new AccountCollections.Channel(ChannelUrls.parse(manual),"Remote rename"),new AccountCollections.Channel(ChannelUrls.parse(remote),"Imported")),false);assertEquals(2,db.subscriptions().size());db.importSubscriptions("owner",Collections.emptyList(),true);assertTrue(db.isSubscribed(manual));assertFalse(db.isSubscribed(remote));
        String local=db.createPlaylist("Local");Video first=Video.link("AbCde_12-34"),second=Video.link("ZbCde_12-34");AccountCollections.Playlist entry=new AccountCollections.Playlist("PL_test","Imported");db.importPlaylist("owner",entry,Collections.singletonList(first));db.importPlaylist("owner",entry,Collections.singletonList(first));assertEquals(2,db.playlists().size());LibraryStore.LocalPlaylist imported=null;for(LibraryStore.LocalPlaylist p:db.playlists())if(!p.remote.isEmpty())imported=p;assertNotNull(imported);db.addToPlaylist(imported.id,second);db.importPlaylist("owner",entry,Collections.singletonList(first));assertEquals(2,db.playlistVideos(imported.id).size());db.allowRemoteRefresh(imported.id);db.importPlaylist("owner",entry,Collections.singletonList(first));assertEquals(1,db.playlistVideos(imported.id).size());db.finishPlaylistImport("owner",Collections.emptySet(),true);assertEquals(1,db.playlists().size());assertEquals(local,db.playlists().get(0).id);
    }finally{db.close();c.deleteDatabase("profile_"+profile+".db");}}
    @Test public void selectedAccountViewsKeepLocalRowsAndOtherImports(){Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();String profile="scope-"+UUID.randomUUID();LibraryStore db=new LibraryStore(context,profile);try{
        String manual="UCabcdefghijklmnopqrstuv",a="UCzyxwvutsrqponmlkjihgfe",b="UCabcdefghijklmnopqrstuX";
        db.subscribe(manual,"Manual",true);db.importSubscriptions("A",Collections.singletonList(new AccountCollections.Channel(ChannelUrls.parse(a),"Account A")),false);db.importSubscriptions("B",Collections.singletonList(new AccountCollections.Channel(ChannelUrls.parse(b),"Account B")),false);
        assertEquals(3,db.subscriptions().size());assertEquals(2,db.subscriptions("A").size());assertEquals(2,db.subscriptions("B").size());assertEquals(1,db.subscriptions("").size());assertTrue(db.isSubscribed(a,"A"));assertFalse(db.isSubscribed(a,"B"));assertTrue(db.isSubscribed(manual,"B"));
        db.createPlaylist("Local");AccountCollections.Playlist entry=new AccountCollections.Playlist("PL_same","Imported");db.importPlaylist("A",entry,Collections.singletonList(Video.link("AbCde_12-34")));db.importPlaylist("B",entry,Collections.singletonList(Video.link("ZbCde_12-34")));
        assertEquals(3,db.playlists().size());assertEquals(2,db.playlists("A").size());assertEquals(2,db.playlists("B").size());assertEquals(1,db.playlists("").size());
    }finally{db.close();context.deleteDatabase("profile_"+profile+".db");}}

}
