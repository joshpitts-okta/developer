/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package org.opendof.tools.repository.interfaces.test.legacySax;

@SuppressWarnings("javadoc")
public class StrH
{
    public static boolean useSpaces = true;
    public static int spacesPerTab = 4;
    public static boolean pretty = true;

    public static String cdataFixup(String value)
    {
        value = value.trim();
        if(value.indexOf('<') != -1)
        {
            if(value.startsWith("<![CDATA["))
                return value;
            value = "<![CDATA[" + value + "]]>";
        }
        return value;
    }
    
    public static StringBuilder level(StringBuilder sb, int level)
    {
        if (pretty)
        {
            if (useSpaces)
            {
                for (int i = 0; i < level; i++)
                    for (int j = 0; j < spacesPerTab; j++)
                        sb.append(" ");
            } else
                for (int i = 0; i < level; i++)
                    sb.append("\t");
        }
        return sb;
    }

    public enum CType
    {
        Open, Single, Close
    }

    public static StringBuilder closer(StringBuilder sb, String tag, CType type, boolean newline)
    {
        switch (type)
        {
            case Close:
                sb.append("</").append(tag).append(">");
                break;
            case Open:
                sb.append(">");
                break;
            case Single:
                sb.append("/>");
                break;
            default:
                break;

        }
        if (newline)
        {
            if (!pretty)
                sb.append("");
            else
                sb.append("\n");
        }
        return sb;
    }

    public static StringBuilder exportNames(StringBuilder sb, String tag, NameDescriptionData names, boolean close, int level)
    {
        if (names == null)
            return sb;
        ++level;
        names.export(sb, level);
        --level;
        if (close && tag != null)
            StrH.element(sb, tag, CType.Close, level);
        return sb;
    }

    /*
        <element/>
        <element>
            <element attr="z"/>
            <element>cdata</element>
            <element attr="z" attr2="x">cdata</element>
        </element>
     */

    public static StringBuilder element(StringBuilder sb, String tag, CType type, int level)
    {
        // <element/> 
        // <element>
        // </element>
        level(sb, level);
        switch(type)
        {
            case Close:
                sb.append("</").append(tag).append(">");
                break;
            case Open:
                sb.append("<").append(tag).append(">");
                break;
            case Single:
                sb.append("<").append(tag).append("/>");
                break;
            default:
                break;
            
        }
        if(pretty)
            sb.append("\n");
        else
            sb.append("");
        return sb;
    }

    public static StringBuilder element(StringBuilder sb, String tag, String cdata, boolean close, int level)
    {
        //      <element>cdata</element>
        return element(sb, tag, (String[]) null, (String[]) null, cdata, close, level);
    }

    public static StringBuilder element(StringBuilder sb, String tag, String attr, String attrValue, String cdata, boolean close, int level)
    {
        //@formatter:off
        return 
            element(sb, tag, 
                 attr == null ? null : new String[] { attr }, 
                 attrValue == null ? null : new String[] { attrValue }, 
                 cdata, close, level);
        //@formatter:on
    }

    public static StringBuilder element(StringBuilder sb, String tag, String[] attrNames, String[] attrValues, String cdata, boolean close, int level)
    {
        //      <element attr="z" attr2="x">cdata</element>
        //      <element attr="z" attr2="x"/>

        level(sb, level);
        sb.append("<").append(tag);
        boolean hadAttrs = false;
        if (attrNames != null)
        {
            if (attrNames.length != attrValues.length)
                throw new RuntimeException("attr names length not equal to values length");
            for (int i = 0; i < attrNames.length; i++)
            {
                hadAttrs = true;
                sb.append(" ");
                sb.append(attrNames[i]).append("=").append("\"").append(attrValues[i]).append("\"");
            }
        }
        if (cdata == null)
        {
            if (!hadAttrs)
                return closer(sb, null, CType.Single, true);
            if(close)
                return closer(sb, null, CType.Single, true);
            return closer(sb, null, CType.Open, true);
        }
        closer(sb, null, CType.Open, false);
        sb.append(cdata);
        return closer(sb, tag, CType.Close, true);
    }

    //    public static String attr(String tag, String value, boolean more)
    //    {
    //        StringBuilder msb = new StringBuilder(tag).append("=\"").append(value).append("\"");
    //        if(more)
    //            msb.append(" ");
    //        return msb.toString();
    //    }

    //    public static StringBuilder ttl(StringBuilder sb, int level, Object ... values)
    //    {
    //        String[] array = new String[values.length];
    //        for(int i=0; i < array.length; i++)
    //            array[i] = (values[i] == null ? "null" : values[i].toString());
    //        return tabToLevel(sb, level, pretty, array);
    //    }
    //    
    //    public static StringBuilder tabToLevel(StringBuilder sb, int level, boolean eol, String ... values)
    //    {
    //        if(pretty)
    //        {
    //            for(int i=0; i < level; i++)
    //            {
    //                sb.append(spacePerTab);
    //            }
    //        }
    //        for(int j=0; j < values.length; j++)
    //            sb.append(values[j]);
    //        if(eol)
    //            sb.append("\n");
    //        else
    //            sb.append(" ");
    //        return sb;
    //    }
}
