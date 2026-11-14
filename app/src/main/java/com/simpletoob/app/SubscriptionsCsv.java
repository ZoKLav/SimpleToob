package com.simpletoob.app;
import java.util.*;
/** Google Takeout subscriptions.csv, including BOM, quoted commas and escaped quotes. */
public final class SubscriptionsCsv {
    public static List<String[]> parse(String input){
        List<List<String>> records=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false;
        for(int i=0;i<input.length();i++){char c=input.charAt(i);if(c=='"'){if(quoted&&i+1<input.length()&&input.charAt(i+1)=='"'){field.append('"');i++;}else quoted=!quoted;}
            else if(c==','&&!quoted){row.add(field.toString());field.setLength(0);}
            else if((c=='\n'||c=='\r')&&!quoted){if(c=='\r'&&i+1<input.length()&&input.charAt(i+1)=='\n')i++;row.add(field.toString());field.setLength(0);records.add(row);row=new ArrayList<>();}
            else field.append(c);
        }
        if(quoted)throw new IllegalArgumentException("Unclosed CSV quote");
        if(field.length()>0||!row.isEmpty()){row.add(field.toString());records.add(row);}
        List<String[]> out=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(List<String> r:records){String url=null;for(String v:r){url=ChannelUrls.parse(v.replace("\uFEFF",""));if(url!=null)break;}if(url!=null&&seen.add(url))out.add(new String[]{url,r.size()>2?r.get(2):url});}
        return out;
    }
}
