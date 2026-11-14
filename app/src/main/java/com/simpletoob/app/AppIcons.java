package com.simpletoob.app;

import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Small density-independent outline icons, drawn locally without an icon font. */
final class AppIcons extends Drawable {
    private final String name;private final Paint p=new Paint(3);private final int color;
    AppIcons(String name,int color){this.name=name;this.color=color;}
    @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);
        switch(name){
        case "back":path(c,15,5,8,12,15,19);break;
        case "search":c.drawCircle(10.5f,10.5f,6.5f,p);c.drawLine(16,16,21,21,p);break;
        case "home":path(c,3,10,12,3,21,10);path(c,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
        case "subs":c.drawRoundRect(3,7,21,21,3,3,p);c.drawLine(6,3,18,3,p);triangle(c,10,11,15,14,10,17);break;
        case "library":c.drawLine(3,4,3,20,p);c.drawRoundRect(7,4,21,20,2,2,p);triangle(c,12,9,17,12,12,15);break;
        case "settings":c.drawCircle(12,12,4,p);for(int i=0;i<8;i++){double a=i*Math.PI/4;c.drawLine((float)(12+8*Math.cos(a)),(float)(12+8*Math.sin(a)),(float)(12+10*Math.cos(a)),(float)(12+10*Math.sin(a)),p);}break;
        case "account":c.drawCircle(12,12,9,p);c.drawCircle(12,9,3,p);c.drawArc(6,13,18,23,200,140,false,p);break;
        case "play":triangle(c,8,5,20,12,8,19);break;
        case "pause":p.setStyle(Paint.Style.FILL);c.drawRoundRect(7,5,10,19,1,1,p);c.drawRoundRect(14,5,17,19,1,1,p);break;
        case "full":path(c,8,3,3,3,3,8);path(c,16,3,21,3,21,8);path(c,3,16,3,21,8,21);path(c,21,16,21,21,16,21);break;
        case "more":p.setStyle(Paint.Style.FILL);for(int y=5;y<=19;y+=7)c.drawCircle(12,y,1.6f,p);break;
        case "share":path(c,8,7,12,3,16,7);path(c,12,3,12,15);path(c,5,11,5,21,19,21,19,11);break;
        case "save":path(c,6,3,18,3,18,21,12,17,6,21,6,3);break;
        case "download":path(c,12,3,12,16);path(c,7,11,12,16,17,11);path(c,4,17,4,21,20,21,20,17);break;
        case "like":path(c,8,10,12,3,14,3,14,9,20,9,21,12,18,21,8,21,8,10);path(c,3,10,6,10,6,21,3,21,3,10);break;
        case "dislike":c.rotate(180,12,12);path(c,8,10,12,3,14,3,14,9,20,9,21,12,18,21,8,21,8,10);path(c,3,10,6,10,6,21,3,21,3,10);break;
        case "check":path(c,5,12,10,17,20,6);break;
        case "refresh":c.drawArc(4,4,20,20,40,285,false,p);path(c,17,3,20,6,16,7);break;
        case "comment":path(c,4,4,20,4,20,17,9,17,4,21,4,4);c.drawLine(8,9,16,9,p);c.drawLine(8,13,14,13,p);break;
        case "link":c.save();c.rotate(-40,12,12);c.drawRoundRect(2,8,13,16,4,4,p);c.drawRoundRect(11,8,22,16,4,4,p);c.restore();break;
        case "chevron":path(c,9,6,15,12,9,18);break;
        default:c.drawCircle(12,12,8,p);break;
        }c.restore();
    }
    private void path(Canvas c,float... xy){Path path=new Path();path.moveTo(xy[0],xy[1]);for(int i=2;i<xy.length;i+=2)path.lineTo(xy[i],xy[i+1]);c.drawPath(path,p);}
    private void triangle(Canvas c,float... xy){Path path=new Path();path.moveTo(xy[0],xy[1]);path.lineTo(xy[2],xy[3]);path.lineTo(xy[4],xy[5]);path.close();c.drawPath(path,p);}
    @Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
