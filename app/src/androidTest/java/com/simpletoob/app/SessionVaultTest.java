package com.simpletoob.app;
import android.content.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
/** Real Android Keystore/preferences commit boundaries, isolated from the user's saved sessions. */
@RunWith(AndroidJUnit4.class)
public final class SessionVaultTest {
    @Test public void verifiedReplacementIsAtomicAndRejectsStaleSelection()throws Exception{
        Context base=InstrumentationRegistry.getInstrumentation().getTargetContext();String prefix="vault-test-"+UUID.randomUUID()+"-";
        Context context=new ContextWrapper(base){@Override public SharedPreferences getSharedPreferences(String name,int mode){return base.getSharedPreferences(prefix+name,mode);}};
        Prefs prefs=new Prefs(context);SessionVault vault=new SessionVault(context,prefs);AtomicInteger changes=new AtomicInteger();
        try{vault.save("default","SAPISID=account-A",0,"");String original=vault.revision();AccountCollections.Identity a=new AccountCollections.Identity("Account A","","owner-A"),b=new AccountCollections.Identity("Account B","","owner-B");assertTrue(vault.setIdentity("default",original,a));vault.onChanged(changes::incrementAndGet);
            YoutubeAccountProtocol.Session next=YoutubeAccountProtocol.stored(YoutubeAccountProtocol.session("SAPISID=account-B",2,"12345").toString(),0);
            assertFalse(vault.saveVerified("default","stale-revision",next,b));assertEquals("SAPISID=account-A",vault.read());assertEquals("owner-A",vault.owner());assertEquals(0,changes.get());
            assertTrue(vault.saveVerified("default",original,next,b));assertEquals("Account B",vault.name());assertEquals("owner-B",vault.owner());assertEquals(2,vault.readSession().account);assertEquals("12345",vault.readSession().delegated);assertNotEquals(original,vault.revision());assertTrue(vault.verified());assertEquals(1,changes.get());
            assertFalse(vault.saveVerified("default",original,next,a));assertEquals("owner-B",vault.owner());assertEquals(1,changes.get());String revision=vault.revision();prefs.put("profile","other-profile");assertFalse(vault.saveVerified("default",revision,next,a));prefs.put("profile","default");assertEquals("owner-B",vault.owner());
        }finally{base.deleteSharedPreferences(prefix+"preferences");base.deleteSharedPreferences(prefix+"web_sessions");}
    }
}
