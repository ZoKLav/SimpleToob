package com.simpletoob.app;

import android.content.*;
import android.content.res.Resources;
import android.view.View;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Android-only resource selection, first-launch persistence and RTL checks. Not a visual/font proof. */
@RunWith(AndroidJUnit4.class)
public final class LocaleTest {
    @Test public void firstLaunchDetectsOnceAndExplicitChoicePersists(){Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();SharedPreferences prefs=context.getSharedPreferences("preferences",Context.MODE_PRIVATE);String old=prefs.getString("ui_language",null);java.util.Map<String,?> before=new java.util.HashMap<>(prefs.getAll());try{
        prefs.edit().remove("ui_language").commit();android.os.LocaleList system=Resources.getSystem().getConfiguration().getLocales();String[] tags=new String[system.size()];for(int i=0;i<tags.length;i++)tags[i]=system.get(i).toLanguageTag();assertEquals(LanguagePolicy.device(tags),AppLanguage.selected(context));assertTrue(prefs.contains("ui_language"));
        AppLanguage.select(context,"es");assertEquals("es",AppLanguage.selected(context));assertEquals("Ajustes",I18n.text(context,"Settings"));assertEquals("es",AppLanguage.selected(context));
        for(String tag:LanguagePolicy.TAGS){AppLanguage.select(context,tag);Context wrapped=AppLanguage.wrap(context);assertEquals(tag,wrapped.getResources().getConfiguration().getLocales().get(0).toLanguageTag());assertEquals(tag.equals("ar")?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR,wrapped.getResources().getConfiguration().getLayoutDirection());assertFalse(I18n.text(context,"Use device language").isEmpty());}
        java.util.Map<String,?> after=new java.util.HashMap<>(prefs.getAll());before.remove("ui_language");after.remove("ui_language");assertEquals(before,after);
    }finally{if(old==null)prefs.edit().remove("ui_language").commit();else prefs.edit().putString("ui_language",old).commit();}}
}
