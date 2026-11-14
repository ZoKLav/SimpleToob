package com.simpletoob.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

public final class ImageLoader {
    private final ExecutorService executor;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(12*1024*1024) {
        @Override protected int sizeOf(String key,Bitmap value) { return value.getByteCount(); }
    };
    ImageLoader(ExecutorService executor) { this.executor=executor; }
    public void load(String url,ImageView target) {load(url,target,false);}
    public void load(String url,ImageView target,boolean fallback) {
        target.setTag(url); if(!fallback)target.setImageDrawable(null);
        Bitmap hit=cache.get(url); if(hit!=null) { target.setImageBitmap(hit); return; }
        try { executor.execute(() -> {
            Bitmap result=fetch(url); if(result==null) return; cache.put(url,result);
            main.post(() -> { if(url.equals(target.getTag())) target.setImageBitmap(result); });
        }); } catch(RejectedExecutionException ignored) { /* Scrolling never blocks on thumbnails. */ }
    }
    private Bitmap fetch(String source) {
        HttpURLConnection c=null;
        try {
            URI uri=URI.create(source); String host=uri.getHost();
            if(!AccountCollections.imageUrl(source))return null;
            c=(HttpURLConnection)new URL(source).openConnection(); c.setInstanceFollowRedirects(false);
            c.setConnectTimeout(6000); c.setReadTimeout(8000); if(c.getResponseCode()!=200) return null;
            byte[] bytes;
            try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                byte[] b=new byte[4096]; int n;
                while((n=in.read(b))!=-1) { if(out.size()+n>2*1024*1024) return null; out.write(b,0,n); }
                bytes=out.toByteArray();
            }
            BitmapFactory.Options bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);
            if(bounds.outWidth<=0 || bounds.outHeight<=0) return null;
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inSampleSize=1;
            while(bounds.outWidth/options.inSampleSize>640 || bounds.outHeight/options.inSampleSize>640) options.inSampleSize*=2;
            return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        } catch(Exception ignored) { return null; } finally { if(c!=null) c.disconnect(); }
    }
}
