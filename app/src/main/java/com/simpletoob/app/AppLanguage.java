package com.simpletoob.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.LocaleList;
import java.util.Locale;

/** First launch saves the closest supported device locale. Explicit choices persist across launches. */
final class AppLanguage {
    private static final String KEY="ui_language";
    private AppLanguage(){}
    private static String[] deviceTags(){LocaleList locales=Resources.getSystem().getConfiguration().getLocales();String[] tags=new String[locales.size()];for(int i=0;i<tags.length;i++)tags[i]=locales.get(i).toLanguageTag();return tags;}
    static synchronized String selected(Context context){SharedPreferences prefs=context.getSharedPreferences("preferences",Context.MODE_PRIVATE);if(!prefs.contains(KEY)){String language=LanguagePolicy.device(deviceTags());prefs.edit().putString(KEY,language).apply();return language;}return prefs.getString(KEY,"en");}
    static String effective(Context context){return LanguagePolicy.resolve(selected(context),deviceTags());}
    static Locale locale(Context context){return Locale.forLanguageTag(effective(context));}
    static Context wrap(Context context){Locale locale=locale(context);Configuration original=context.getResources().getConfiguration();if(original.getLocales().get(0).toLanguageTag().equals(locale.toLanguageTag()))return context;Configuration configured=new Configuration(original);configured.setLocales(locale.getLanguage().equals("en")?new LocaleList(locale):new LocaleList(locale,Locale.ENGLISH));configured.setLayoutDirection(locale);return context.createConfigurationContext(configured);}
    static void select(Context context,String tag){if(!"system".equals(tag)&&!tag.equals(LanguagePolicy.supported(tag)))throw new IllegalArgumentException("Unsupported interface language");context.getSharedPreferences("preferences",Context.MODE_PRIVATE).edit().putString(KEY,tag).apply();}
    static String[] names(Context context){return new String[]{I18n.text(context,"Use device language"),"English","中文（简体）","हिन्दी","Español","العربية","Français","বাংলা"};}
    static String name(Context context){String selected=selected(context);if(selected.equals("system"))return I18n.text(context,"Use device language");for(int i=0;i<LanguagePolicy.TAGS.length;i++)if(LanguagePolicy.TAGS[i].equals(selected))return names(context)[i+1];return "English";}
}
