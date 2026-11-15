package com.simpletoob.app;
import java.util.*;
/** No request URLs, cookies, user text or headers are recorded. */
public final class Diagnostics {
    private static final ArrayDeque<String> events=new ArrayDeque<>();
    public static synchronized void record(String event){if(events.size()>=120)events.removeFirst();events.addLast(new Date()+" ["+Thread.currentThread().getName()+"] "+event);}
    public static synchronized void clear(){events.clear();}
    public static synchronized String report(){return "SimpleToob 0.8.1 diagnostics\nAndroid "+android.os.Build.VERSION.RELEASE+" / "+android.os.Build.MODEL+"\n"+String.join("\n",events);}
}
