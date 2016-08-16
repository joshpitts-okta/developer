/*
**  Copyright (c) 2010-2015, Panasonic Corporation.
**
**  Permission to use, copy, modify, and/or distribute this software for any
**  purpose with or without fee is hereby granted, provided that the above
**  copyright notice and this permission notice appear in all copies.
**
**  THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
**  WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
**  MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
**  ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
**  WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
**  ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
**  OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
*/
package com.psas.tools.repository.interfaces.test;

import org.slf4j.LoggerFactory;

@SuppressWarnings("javadoc")
public class LegacyIids 
{
    //@formatter:off
    private static String[] legacy1= new String[]
    {
        "[1:{01}]",
        "[1:{0A}]",
        "[1:{08}]",
        "[1:{0B}]",
        "[1:{09}]",
    };
    private static String[] legacy2= new String[]
    {
        "[1:{0201}]",
        "[1:{0210}]",
        "[1:{0225}]",
        "[1:{0228}]",
        "[1:{0226}]",
        "[1:{0202}]",
        "[1:{0203}]",
        "[1:{0204}]",
        "[1:{0205}]",
        "[1:{0206}]",
        "[1:{0207}]",
        "[1:{0208}]",
        "[1:{020A}]",
        "[1:{0209}]",
        "[1:{020F}]",
        "[1:{0235}]",
        "[1:{020B}]",
        "[1:{020C}]",
        "[1:{0222}]",
        "[1:{0211}]",
        "[1:{020E}]",
        "[1:{021F}]",
        "[1:{0220}]",
        "[1:{0238}]",
        "[1:{0236}]",
        "[1:{0237}]",
        "[1:{0212}]",
        "[1:{0227}]",
        "[1:{0213}]",
        "[1:{0233}]",
    };
    
    private static String[] legacy4= new String[]
    {
        "[1:{0100003E}]",
        "[1:{01000022}]",
        "[1:{01000027}]",
        "[1:{0100004D}]",
        "[1:{01000002}]",
        "[1:{01000047}]",
        "[1:{01000045}]",
        "[1:{0100004F}]",
        "[1:{01000040}]",
        "[1:{01000046}]",
        "[1:{0100003F}]",
        "[1:{0100003D}]",
        "[1:{0100003B}]",
        "[1:{01000050}]",
        "[1:{01000042}]",
        "[1:{01000034}]",
        "[1:{01000028}]",
        "[1:{0100000A}]",
        "[1:{01000001}]",
        "[1:{01000041}]",
        "[1:{01000000}]",
        "[1:{01000043}]",
        "[1:{01000044}]",
    };
    //@formatter:on
    
    
    public LegacyIids()
    {
    }
    
    public String hexStrToLongStr(String iid)
    {
        int b1 = iid.indexOf('{') + 1;
        int b2 = iid.indexOf('}');
        iid = iid.substring(b1, b2);
        return Long.toString(Long.valueOf(iid, 16));
    }
    
    public void run()
    {
        StringBuilder sb = new StringBuilder("\n//@formatter:off\nprivate static final int[] legacy1Byte = new int[]\n{\n");
        for(int i=0; i < legacy1.length; i++)
            sb.append("\n\t").append(hexStrToLongStr(legacy1[i])).append(",\t//").append(legacy1[i]);
        sb.append("\n};\n");
        sb.append("private static final int[] legacy2Byte = new int[]\n{\n");
        for(int i=0; i < legacy2.length; i++)
            sb.append("\n\t").append(hexStrToLongStr(legacy2[i])).append(",\t//").append(legacy2[i]);
        sb.append("\n};\n");
        sb.append("private static final int[] legacy4Byte = new int[]\n{\n");
        for(int i=0; i < legacy4.length; i++)
            sb.append("\n\t").append(hexStrToLongStr(legacy4[i])).append(",\t//").append(legacy4[i]);
        sb.append("\n};\n\n");
        LoggerFactory.getLogger(getClass()).info(sb.toString());
    }
    
/*
     working

[2:{10020011}]
[1:{01000067}]
[63:{01000006}]
[1:{03}]
[1:{01000009}]
[1:{0217}]
[1:{021E}]
[1:{01000039}]
[63:{AA000002}]
[63:{0234}]
[63:{0235}]
[1:{0100002B}]
[1:{0100003A}]
[1:{01000051}]
[63:{BAD00001}]
[2:{100280B5}]
[2:{100200C8}]
[1:{01000015}]
[1:{0219}]
[63:{01008356}]
[63:{EDE1}]
[63:{0232}]
[63:{0503}]
[63:{0504}]
[63:{0505}]
[63:{02280024}]
[63:{02280027}]
[63:{DBC01001}]
[63:{01006174}]
[1:{01000029}]
[63:{0400}]
[63:{0803}]
[63:{0802}]
[1:{0216}]
[63:{0800}]
[63:{0801}]
[1:{021B}]
[1:{01000037}]
[1:{01000038}]
[63:{0804}]
[1:{0214}]
[1:{0113}]
[63:{EDE5}]
[1:{01000064}]
[1:{01000063}]
[1:{01000062}]
[1:{01000065}]
[1:{01000066}]
[1:{01000068}]
[1:{01000060}]
[1:{01000061}]
[63:{02280036}]
[1:{0114}]
[63:{02281336}]
[1:{022B}]
[63:{AC0F}]
[1:{022D}]
[1:{022E}]
[63:{02280377}]
[63:{0233}]
[1:{01000035}]
[1:{0215}]
[1:{0239}]
[1:{0221}]
[1:{01000052}]
[63:{DC07}]
[63:{01000012}]
[63:{01000014}]
[63:{01000103}]
[63:{01000101}]
[63:{01000108}]
[63:{01000102}]
[63:{01000106}]
[63:{01000105}]
[63:{01000054}]
[63:{01000015}]
[63:{01000013}]
[63:{01000016}]
[63:{01000017}]
[63:{01000053}]
[63:{01000051}]
[63:{01000052}]
[63:{01000011}]
[63:{01000021}]
[63:{01000007}]
[63:{AC13}]
[63:{AC14}]
[63:{0506}]
[63:{01000010}]
[63:{01000031}]
[63:{01000018}]
[63:{01000040}]
[1:{022F}]
[63:{0123}]
[1:{01000011}]
[1:{0231}]
[63:{01001513}]
[1:{07}]
[1:{0115}]
[1:{01000049}]
[63:{02284208}]
[63:{0231}]
[63:{EDE2}]
[63:{AC10}]
[63:{AC12}]
[1:{0230}]
[63:{AA000001}]
[1:{04}]
[1:{0100004B}]
[63:{EDE3}]
[63:{0511}]
[1:{01000017}]
[1:{01000019}]
[63:{DC06}]
[63:{DC04}]
[63:{DBC00021}]
[1:{0100002A}]
[63:{3478}]
[63:{EDE4}]
[63:{DBC00001}]
[1:{01000008}]
[63:{01000032}]
[63:{01000030}]
[63:{AA000003}]
[63:{0211}]
[1:{05}]
[63:{E445}]
[63:{0230}]
[1:{021D}]
[1:{0100001A}]
[63:{22803749}]
[1:{01000012}]
[1:{0100001B}]
[1:{0100001C}]
[63:{01000001}]
[63:{01000002}]
[1:{0116}]
[1:{023A}]
[1:{0224}]
[1:{0223}]
[63:{7374}]
[63:{01000107}]
[63:{01000111}]
[63:{01000110}]
[63:{01000120}]
[1:{021C}]

 */
}
