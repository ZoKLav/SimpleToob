package com.simpletoob.app;

import android.content.Intent;
import android.view.*;
import android.widget.TextView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Device regression for system Back and navigation restoration; requires Android hardware/emulator. */
@RunWith(AndroidJUnit4.class)
public final class NavigationTest {
    private String originalLanguage;
    @org.junit.Before public void englishLabels(){android.content.Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();originalLanguage=context.getSharedPreferences("preferences",0).getString("ui_language",null);AppLanguage.select(context,"en");}
    @org.junit.After public void restoreLanguage(){android.content.SharedPreferences.Editor editor=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("preferences",0).edit();if(originalLanguage==null)editor.remove("ui_language");else editor.putString("ui_language",originalLanguage);editor.commit();}

    @Test public void systemBackReturnsThroughSectionsAndSettingsAfterRecreation(){
        Intent intent=new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),MainActivity.class).putExtra("open_screen","Library");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(intent)){
            scenario.onActivity(a->clickLabel(a.findViewById(android.R.id.content),"Saved videos"));
            back();scenario.onActivity(a->{assertFalse(a.isFinishing());assertNotNull(label(a.findViewById(android.R.id.content),"Saved videos"));});
            scenario.onActivity(a->{View root=a.findViewById(android.R.id.content);View settings=description(root,"Settings");assertNotNull(settings);settings.performClick();clickLabel(root,"Appearance");});
            scenario.recreate();back();
            scenario.onActivity(a->{assertFalse(a.isFinishing());assertNotNull(label(a.findViewById(android.R.id.content),"Make SimpleToob yours"));});
            back();scenario.onActivity(a->{assertFalse(a.isFinishing());assertNotNull(label(a.findViewById(android.R.id.content),"Saved videos"));});
        }
    }
    private static void back(){InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);InstrumentationRegistry.getInstrumentation().waitForIdleSync();}
    private static View description(View v,String value){if(value.contentEquals(v.getContentDescription()==null?"":v.getContentDescription()))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View found=description(group.getChildAt(i),value);if(found!=null)return found;}}return null;}
    private static View label(View v,String value){if(v instanceof TextView&&value.contentEquals(((TextView)v).getText()))return v;if(v instanceof ViewGroup){ViewGroup group=(ViewGroup)v;for(int i=0;i<group.getChildCount();i++){View found=label(group.getChildAt(i),value);if(found!=null)return found;}}return null;}
    private static void clickLabel(View root,String value){View target=label(root,value);assertNotNull(target);while(target!=null&&!target.isClickable()){android.view.ViewParent parent=target.getParent();target=parent instanceof View?(View)parent:null;}assertNotNull(target);target.performClick();}
}
