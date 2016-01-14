/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("javadoc")
public class TabToLevel
{
    public static final StringBuilder sb = new StringBuilder();
    public static final AtomicInteger level = new AtomicInteger(0);
    
    public static void ttl(Object ... values)
    {
        String[] array = new String[values.length];
        for(int i=0; i < array.length; i++)
            array[i] = (values[i] == null ? "null" : values[i].toString());
        tabToLevel(true, array);
    }

    public static void tabToLevel(boolean eol, String ... values)
    {
        for(int i=0; i < level.get(); i++)
            sb.append("\t");
        for(int j=0; j < values.length; j++)
            sb.append(values[j]);
        if(eol)
            sb.append("\n");
    }
}
