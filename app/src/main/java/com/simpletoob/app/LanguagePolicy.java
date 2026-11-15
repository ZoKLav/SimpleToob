package com.simpletoob.app;

import java.util.Locale;

/** UI locale policy only. Never changes extraction language, region, stored setting IDs or user text. */
final class LanguagePolicy {
    static final String[] TAGS={"en","zh-Hans","hi","es","ar","fr","bn"};
    static String supported(String tag){if(tag==null)return "";String language=Locale.forLanguageTag(tag.replace('_','-')).getLanguage();if(language.equals("zh"))return "zh-Hans";for(String supported:TAGS)if(supported.equals(language))return supported;return "";}
    static String device(String... tags){for(String tag:tags){String supported=supported(tag);if(!supported.isEmpty())return supported;}return "en";}
    static String resolve(String selected,String... deviceTags){if("system".equals(selected))return device(deviceTags);String supported=supported(selected);return supported.isEmpty()?"en":supported;}
}
