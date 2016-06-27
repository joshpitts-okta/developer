package org.opendof.tools.repository.interfaces.test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("javadoc")
public class GoogleSearch
{
    private Logger log;

    public GoogleSearch()
    {
        log = LoggerFactory.getLogger(getClass());
    }

    private String screwupQueryString(String target)
    {
        target = target.replace("[", "%5B");
        target = target.replace(":", "%3A");
        target = target.replace("{", "%7B");
        target = target.replace("}", "%7D");
        target = target.replace("]", "%5D");
        return target;
    }

//    private String iidToEscapedIid(String iidString)
//    {
//        // [1:{1234}]
//        StringBuilder sb = new StringBuilder("%5B"); // '['
//        int idx = iidString.indexOf(':');
//        sb.append(iidString.substring(1, idx)) // pick up registry
//                        .append("%3A") // ':'
//                        .append("%7B"); // '{'
//        idx = iidString.indexOf('{') + 1;
//        int idx2 = iidString.indexOf('}');
//        sb.append(iidString.substring(idx, idx2)).append("%7D").append("%5D");
//        log.info(sb.toString());
//        return sb.toString();
//    }

    //@formatter:off
    private String[] iids = new String[] 
    { 
        "?cmd=GetInterface&trans=raw&repo=opendof&user=pslcl&group=none&iid=dof.1.01000048", 
        "dof/1.1", 
        "dof/01.1", 
        "dof/01.1.E", 
        "dof/01.abcdef12.E", 
        "dof/01.abcdef12E", 
        "zzzdof/1.1zzz", 
        "dof/123.1", 
        "dof/.1", 
        "dof/.g", 
        "dof/a.1", 
        "dof/1.a", 
        "dof/1.", 
        "dof/1a",
        screwupQueryString("[1:{01020304}]"),
        screwupQueryString("[1:{010203BB}]"),
        screwupQueryString("[1:{010203aa}]"),
    };
    //@formatter:off

    public void testUriStandform()
    {
//        for (int i = 0; i < iids.length; i++)
//            log.info(HttpCommandParser.fixupIidStandardForm(iids[i]));
    }

    private static final String SolAwsTomcat = "http://52.7.126.107/ir-servlet-1.0.0-SNAPSHOT/interfaceRepository";
    private static final String LocalTomcat = "http://localhost:8080/ir-servlet/";
    private static final String ServletName = "interfaceRepository";
    private static final String HomePage = "repository.html";

    private static final String CmdGet = "?cmd=GetInterface&trans=raw&repo=opendof&user=pslcl&group=none&iid=dof.1.01000048";
    private static final String HashGet = "#!cmd=GetInterface&trans=raw&repo=opendof&user=pslcl&group=none&iid=dof.1.0232";
    private static final String LocalCmdGet = LocalTomcat + ServletName + CmdGet;
    private static final String SolAwsCmdGet = SolAwsTomcat + ServletName + CmdGet;
    private static final String LocalHashGet = LocalTomcat + ServletName + HashGet;
    private static final String LocalHome = LocalTomcat + HomePage;

    public void run()
    {
        //        host + "#!cmd=GetInterface&trans=raw&repo=opendof&user=pslcl&group=none&iid="+iidToEscapedIid("[1:{0232}]"));

        testUriStandform();

        CloseableHttpResponse response1 = null;
        try
        {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            //@Formatter:off
            HttpGet httpGet = new HttpGet(LocalHashGet);

            //@Formatter:on
            response1 = httpclient.execute(httpGet);
            System.out.println(response1.getStatusLine());
            HttpEntity entity1 = response1.getEntity();
            InputStream is = entity1.getContent();
            char[] buffer = new char[1024 * 16];
            Reader r = new InputStreamReader(is, "UTF8");
            int size = r.read(buffer);
            String page = new String(buffer);
            log.info(page);
        } catch (Exception e)
        {
            log.error("failed: ", e);
        } finally
        {
            try
            {
                if (response1 != null)
                    response1.close();
            } catch (Exception e1)
            {
            }
        }

    }
}