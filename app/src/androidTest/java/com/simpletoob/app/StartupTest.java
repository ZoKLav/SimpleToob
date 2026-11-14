package com.simpletoob.app;

import android.view.View;
import android.view.ViewGroup;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Device regression: 0.2 crashed in Ui.root before the first screen appeared. */
@RunWith(AndroidJUnit4.class)
public final class StartupTest {
    @Test public void coldLaunchAndRecreationAttachTheRootWithoutCrashing() {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertAttached(scenario);
            scenario.recreate();
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertAttached(scenario);
        }
    }
    private void assertAttached(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(activity->{
            ViewGroup content=activity.findViewById(android.R.id.content);
            assertNotNull(content);
            assertEquals(1,content.getChildCount());
            View root=content.getChildAt(0);
            assertTrue(root.isAttachedToWindow());
            assertTrue(root.getWidth()>0);
            assertTrue(root.getHeight()>0);
            if(android.os.Build.VERSION.SDK_INT>=30)assertNotNull(root.getWindowInsetsController());
        });
    }
}
