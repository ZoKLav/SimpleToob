package com.simpletoob.app;
import android.app.job.*;
import android.os.*;
public final class ImportJobService extends JobService {
    private JobParameters active;
    @Override public boolean onStartJob(JobParameters p){SimpleToobApp app=(SimpleToobApp)getApplication();if(!app.sessions.present()||!app.sessions.profile().equals(p.getExtras().getString("profile"))||!app.sessions.revision().equals(p.getExtras().getString("revision")))return false;active=p;app.imports.due(()->new Handler(Looper.getMainLooper()).post(()->{if(active==p){active=null;jobFinished(p,false);}}));return true;}
    @Override public boolean onStopJob(JobParameters p){SimpleToobApp app=(SimpleToobApp)getApplication();if(active==p){active=null;if(app.sessions.profile().equals(p.getExtras().getString("profile"))&&app.sessions.revision().equals(p.getExtras().getString("revision")))app.imports.cancel();}return true;}
}
