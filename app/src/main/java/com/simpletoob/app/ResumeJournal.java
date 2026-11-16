package com.simpletoob.app;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Completed chunks only: media is synced before an atomic checkpoint is published. */
final class ResumeJournal {
    private final File journal;
    private final Properties metadata=new Properties();
    private final BitSet chunks=new BitSet();
    private final long size;
    ResumeJournal(File media,String identity,String validator,long size)throws IOException{
        this.size=size;journal=new File(media+".resume");
        if(journal.isFile()&&journal.length()<1024*1024)try(InputStream in=new FileInputStream(journal)){
            Properties saved=new Properties();saved.load(in);
            if(media.isFile()&&identity.equals(saved.getProperty("identity"))&&!validator.isEmpty()&&validator.equals(saved.getProperty("validator"))&&Long.toString(size).equals(saved.getProperty("size"))){
                BitSet restored=BitSet.valueOf(Base64.getDecoder().decode(saved.getProperty("chunks","")));
                if(restored.length()<=(size+1048575)/1048576)for(int i=restored.nextSetBit(0);i>=0;i=restored.nextSetBit(i+1))if(Math.min(size,(i+1L)*1048576)<=media.length())chunks.set(i);
            }
        }catch(IllegalArgumentException ignored){}catch(IOException ignored){}
        metadata.setProperty("identity",identity);metadata.setProperty("validator",validator);metadata.setProperty("size",Long.toString(size));
        if(chunks.isEmpty())try(RandomAccessFile out=new RandomAccessFile(media,"rw")){out.setLength(0);}
        // Never trust a stale journal if resetting the media was interrupted.
        save();
    }
    static String validator(String url,String etag,long size){
        if(!etag.isEmpty()&&!etag.startsWith("W/"))return "etag:"+etag;
        try{java.net.URI uri=java.net.URI.create(url);String host=uri.getHost();if(!"https".equals(uri.getScheme())||host==null||!(host.equals("googlevideo.com")||host.endsWith(".googlevideo.com")))return "";
            Map<String,String> params=new HashMap<>();String query=uri.getRawQuery();if(query==null)return "";for(String part:query.split("&")){String[] p=part.split("=",2);if(p.length==2&&Arrays.asList("id","itag","lmt","clen").contains(p[0])){if(params.put(p[0],p[1])!=null)return "";}}
            String id=params.get("id"),itag=params.get("itag"),lmt=params.get("lmt"),length=params.get("clen");
            if(id==null||id.length()>256||itag==null||!itag.matches("[0-9]{1,6}")||lmt==null||!lmt.matches("[0-9]{1,20}")||!Long.toString(size).equals(length))return "";
            return "cdn:"+id+":"+itag+":"+lmt+":"+length;
        }catch(Exception e){return "";}
    }
    synchronized boolean has(long start){return chunks.get((int)(start/1048576));}
    synchronized long bytes(){long bytes=0;for(int i=chunks.nextSetBit(0);i>=0;i=chunks.nextSetBit(i+1))bytes+=Math.min(1048576,size-i*1048576L);return bytes;}
    synchronized void commit(long start)throws IOException{chunks.set((int)(start/1048576));save();}
    private void save()throws IOException{
        metadata.setProperty("chunks",Base64.getEncoder().encodeToString(chunks.toByteArray()));File temp=new File(journal+".tmp");
        try(FileOutputStream out=new FileOutputStream(temp)){metadata.store(out,null);out.getFD().sync();}
        try{Files.move(temp.toPath(),journal.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(temp.toPath(),journal.toPath(),StandardCopyOption.REPLACE_EXISTING);}
    }
}
