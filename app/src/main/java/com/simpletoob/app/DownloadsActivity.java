package com.simpletoob.app;

import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.widget.*;
import java.util.*;

/** Recycled, bounded queue view. No service retains this screen. */
public final class DownloadsActivity extends BaseActivity {
    private final Handler timer=new Handler(Looper.getMainLooper());private LinearLayout body;private boolean visible,loading;private int generation;
    private final Runnable refresh=new Runnable(){public void run(){load();if(visible)timer.postDelayed(this,1500);}};
    @Override public void onCreate(Bundle b){super.onCreate(b);LinearLayout root=ui.column();ui.root(root);root.addView(ui.toolbar(tx("Downloads"),this::finish));body=ui.column();root.addView(body,new LinearLayout.LayoutParams(-1,0,1));}
    @Override protected void onStart(){super.onStart();visible=true;timer.post(refresh);}
    @Override protected void onStop(){visible=false;generation++;loading=false;timer.removeCallbacksAndMessages(null);super.onStop();}
    private void load(){if(loading)return;loading=true;String profile=app.sessions.profile();int token=generation;work(app.disk,()->{try(DownloadStore store=new DownloadStore(this)){return store.list(profile);}},jobs->{if(token!=generation)return;loading=false;body.removeAllViews();body.addView(ui.note(tx("Paused downloads keep verified parts. Cancel removes temporary files; completed files stay in your chosen folder.")));if(!DownloadService.state.isEmpty())body.addView(ui.note(DownloadService.state));ListView list=new ListView(this);String[] labels=new String[jobs.size()];for(int i=0;i<jobs.size();i++)labels[i]=jobs.get(i).title+"\n"+tx(jobs.get(i).state)+(jobs.get(i).message.isEmpty()?"":"\n"+tx(jobs.get(i).message));list.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,labels));list.setOnItemClickListener((p,v,i,id)->options(jobs.get(i)));body.addView(list,new LinearLayout.LayoutParams(-1,0,1));},m->{loading=false;if(visible)toast(m);});}
    private void options(DownloadStore.Job job){boolean done="complete".equals(job.state),cancelled="cancelled".equals(job.state);String[] options=cancelled?new String[]{tx("Remove from list")}:done?new String[]{tx("Open file"),tx("Remove from list")}:new String[]{tx("Resume"),tx("Pause"),tx("Cancel download"),tx("Remove from list")};new AlertDialog.Builder(this).setTitle(job.title).setItems(options,(d,i)->{
        if(done&&i==0){try{startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(job.output),"video/mp4").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));}catch(ActivityNotFoundException e){toast(tx("No app can open this file."));}return;}
        String action=done||cancelled?"remove":new String[]{"resume","pause","cancel","remove"}[i];startForegroundService(new Intent(this,DownloadService.class).setAction(action).putExtra("job",job.id).putExtra("profile",job.profile));timer.postDelayed(this::load,300);
    }).show();}
}
