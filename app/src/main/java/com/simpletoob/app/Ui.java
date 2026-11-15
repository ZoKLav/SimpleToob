package com.simpletoob.app;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.graphics.drawable.*;
import android.content.res.ColorStateList;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    final Activity context;
    final Prefs prefs;final float fontScale;
    final boolean dark;
    final int bg,surface,text,muted,accent,onAccent,line;
    final boolean square;
    Ui(Activity activity,Prefs p) {
        context=activity;this.prefs=p;fontScale=Math.max(.8f,Math.min(1.6f,p.number("font_scale",100)/100f));
        square=p.flag("square_buttons",false);
        String mode=p.text("theme","System");
        dark=mode.equals("Dark") || (mode.equals("System") &&
            (activity.getResources().getConfiguration().uiMode&Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
        int gray=Math.max(0,Math.min(48,p.number("gray",16)));
        String tone=p.text("tone","Neutral"); int r=gray,g=gray,b=gray;
        if(gray>0 && tone.equals("Cool")) b=Math.min(255,gray+8);
        if(gray>0 && tone.equals("Warm")) r=Math.min(255,gray+8);
        bg=dark?Color.rgb(r,g,b):Color.rgb(248,248,250);
        surface=dark?Color.rgb(r+12,g+12,b+12):Color.WHITE;
        text=dark?Color.rgb(244,244,247):Color.rgb(25,25,30);
        muted=dark?Color.rgb(180,180,190):Color.rgb(85,85,99);
        line=dark?Color.rgb(r+32,g+32,b+32):Color.rgb(221,221,228);
        accent=p.number("accent",Color.rgb(255,118,102));
        double lum=(Color.red(accent)*.299+Color.green(accent)*.587+Color.blue(accent)*.114);
        onAccent=Color.luminance(accent)>.179?Color.BLACK:Color.WHITE;
    }
    String tr(int id,Object...args){return I18n.get(context,id,args);}
    int dp(float n) { return (int)(n*context.getResources().getDisplayMetrics().density+.5f); }
    GradientDrawable rounded(int color,int radius) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    LinearLayout column() { LinearLayout l=new LinearLayout(context); l.setOrientation(LinearLayout.VERTICAL); return l; }
    LinearLayout row() { LinearLayout l=new LinearLayout(context); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l; }
    TextView text(String value,int sp,int color) {
        TextView t=new TextView(context); t.setText(value); t.setTextSize(sp*fontScale);t.setTypeface(Typeface.create(prefs.text("font_family","sans-serif"),Typeface.NORMAL)); t.setTextColor(color); t.setLineSpacing(dp(3),1f); return t;
    }
    TextView heading(String value,int sp) { TextView t=text(value,sp,text); t.setTypeface(Typeface.create(prefs.text("font_family","sans-serif").equals("sans-serif")?"sans-serif-medium":prefs.text("font_family","sans-serif"),Typeface.NORMAL)); return t; }
    TextView note(String value) { TextView t=text(value,14,muted); t.setPadding(0,dp(8),0,dp(12)); return t; }
    private int radius(){return square?0:Math.max(0,Math.min(28,prefs.number("button_radius",16)));}
    android.graphics.drawable.Drawable touch(int color,int radius){return new RippleDrawable(ColorStateList.valueOf((text&0xffffff)|0x20000000),rounded(color,radius),rounded(Color.WHITE,radius));}
    Button button(String label,boolean filled,Runnable action) {
        Button b=new Button(context);b.setText(label);b.setAllCaps(false);b.setTextSize(14*fontScale);b.setTypeface(Typeface.create(prefs.text("font_family","sans-serif"),Typeface.NORMAL));
        b.setTextColor(filled?onAccent:text);b.setBackgroundTintList(null);b.setBackground(touch(filled?accent:surface,radius()));
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(44));b.setMinimumHeight(dp(44));b.setPadding(dp(16),dp(8),dp(16),dp(8));
        b.setOnClickListener(v->action.run());return b;
    }
    Button quiet(String label,Runnable action){Button b=button(label,false,action);b.setBackground(touch(Color.TRANSPARENT,radius()));return b;}
    Button chip(String icon,String label,Runnable action){Button b=button(label,false,action);b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);if(icon!=null){AppIcons d=new AppIcons(icon,text);d.setBounds(0,0,dp(18),dp(18));b.setCompoundDrawablesRelative(d,null,null,null);b.setCompoundDrawablePadding(dp(7));}return b;}
    ImageButton icon(String icon,String label,Runnable action){ImageButton b=new ImageButton(context);b.setImageDrawable(new AppIcons(icon,text));b.setScaleType(ImageView.ScaleType.CENTER_INSIDE);b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setBackground(touch(Color.TRANSPARENT,24));b.setContentDescription(label);b.setOnClickListener(v->action.run());b.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));return b;}
    LinearLayout toolbar(String title,Runnable back){LinearLayout bar=row();bar.setPadding(dp(8),dp(2),dp(8),dp(2));bar.addView(icon("back","Back",back));TextView name=heading(title,18);name.setPadding(dp(8),0,0,0);bar.addView(name,new LinearLayout.LayoutParams(0,-2,1));return bar;}
    void divider(LinearLayout parent){View d=new View(context);d.setBackgroundColor(line);parent.addView(d,new LinearLayout.LayoutParams(-1,dp(1)));}
    void addChip(LinearLayout row,View child){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMarginEnd(dp(8));row.addView(child,lp);}
    LinearLayout item(String icon,String title,String subtitle,Runnable action){LinearLayout row=row();row.setPadding(dp(12),dp(12),dp(8),dp(12));row.setMinimumHeight(dp(64));row.setBackground(touch(Color.TRANSPARENT,12));ImageView symbol=new ImageView(context);symbol.setImageDrawable(new AppIcons(icon,text));row.addView(symbol,new LinearLayout.LayoutParams(dp(24),dp(24)));LinearLayout labels=column();labels.setPadding(dp(16),0,dp(8),0);labels.addView(heading(title,16));if(subtitle!=null&&!subtitle.isEmpty()){TextView note=text(subtitle,13,muted);note.setPadding(0,dp(3),0,0);labels.addView(note);}row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));ImageView chevron=new ImageView(context);chevron.setImageDrawable(new AppIcons("chevron",muted));row.addView(chevron,new LinearLayout.LayoutParams(dp(18),dp(18)));row.setOnClickListener(v->action.run());row.setFocusable(true);return row;}
    EditText input(String hint) {
        EditText e=new EditText(context); e.setHint(hint); e.setTextSize(16*fontScale); e.setTextColor(text); e.setHintTextColor(muted);
        e.setHighlightColor((accent&0xffffff)|0x55000000);if(android.os.Build.VERSION.SDK_INT>=29&&e.getTextCursorDrawable()!=null){android.graphics.drawable.Drawable cursor=e.getTextCursorDrawable().mutate();cursor.setTint(accent);e.setTextCursorDrawable(cursor);}
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4096)}); e.setSingleLine(true); e.setPadding(dp(16),dp(12),dp(16),dp(12)); e.setBackground(rounded(surface,16)); return e;
    }
    void space(LinearLayout parent,int height) { View v=new View(context); parent.addView(v,new LinearLayout.LayoutParams(1,dp(height))); }
    void padded(LinearLayout l) { int n=Math.max(8,Math.min(32,prefs.number("panel_padding",20)));l.setPadding(dp(n),dp(16),dp(n),dp(16)); }
    void root(LinearLayout root) {
        root.setBackgroundColor(bg);
        root.setOnApplyWindowInsetsListener((v,insets)-> {
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                int keyboard=insets.getInsets(WindowInsets.Type.ime()).bottom;
                v.setPadding(safe.left,safe.top,safe.right,Math.max(safe.bottom,keyboard));
                return WindowInsets.CONSUMED;
            }
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        // Install content before touching decor-dependent window APIs. The Razr's
        // PhoneWindow.getInsetsController() throws when its decor is still null.
        context.setContentView(root,new android.view.ViewGroup.LayoutParams(-1,-1));
        if(android.os.Build.VERSION.SDK_INT>=30)context.getWindow().setDecorFitsSystemWindows(false);
        else context.getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                |(dark?0:View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR));
        context.getWindow().setStatusBarColor(bg); context.getWindow().setNavigationBarColor(bg);
        // View.post waits for attachment when needed. Use the attached View's
        // controller so there is no early PhoneWindow controller lookup.
        root.post(() -> {
            if(!root.isAttachedToWindow())return;
            if(android.os.Build.VERSION.SDK_INT>=30) {
                android.view.WindowInsetsController controller=root.getWindowInsetsController();
                if(controller!=null)controller.setSystemBarsAppearance(
                    dark?0:android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                    android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
            root.requestApplyInsets();
        });
    }
}
