package com.pslcl.chad.app;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.Map.Entry;

import com.pslcl.chad.app.StrH.StringPair;

public class PropertiesHelper
{
    private PropertiesHelper()
    {
    }
    
    public static List<Entry<String, String>> getPropertiesForBaseKey(String baseKey, Properties properties)
    {
        ArrayList<Entry<String, String>> entries = new ArrayList<Entry<String, String>>();
        Hashtable<Integer, StringPair> orderingMap = new Hashtable<Integer, StringPair>();
        
        int found = 0;
        for (Entry<Object, Object> entry : properties.entrySet())
        {
            String key = (String) entry.getKey();
            int index = 0;
            if (key.startsWith(baseKey))
            {
                ++found;
                char[] chars = key.toCharArray();
                if(Character.isDigit(chars[chars.length-1]))
                {
                    int strIndex = 0;
                    for(int i=chars.length-1; i >=0; i--)
                    {
                        if(!Character.isDigit(chars[i]))
                        {
                            strIndex = i + 1;
                            break;
                        }
                    }
                    index = Integer.parseInt(key.substring(strIndex));
                }
                orderingMap.put(index, new StringPair(entry));
            }
        }
        int i=0;
        int hit = 0;
        do
        {
            StringPair pair = orderingMap.get(i);
            if(pair != null)
            {
                entries.add(pair);
                ++hit;
            }
            ++i;
        }while(hit < found);
        return entries;
    }
}
