/*
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
package org.opendof.tools.repository.interfaces.servlet.auth.jira.locus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Timer;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.scribejava.core.model.Token;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;

@SuppressWarnings("javadoc")
public class JiraDataAccessor
{
    public void restGet(String url, String token)
    {
        
        String url = "http://www.google.com/search?q=httpClient";

        HttpClient client = HttpClientBuilder.create().build();
        HttpGet request = new HttpGet(url);
        request.addHeader("Content-Type", "application/json");
        request.addHeader("Accept", "application/json");
        request.addHeader("Authorization", "Bearer " + token);

        // add request header
        request.addHeader("User-Agent", USER_AGENT);
        HttpResponse response = client.execute(request);

        System.out.println("Response Code : " 
                    + response.getStatusLine().getStatusCode());

        BufferedReader rd = new BufferedReader(
            new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }        
        
        try {

            DefaultHttpClient httpClient = new DefaultHttpClient();
            HttpGet getRequest = new HttpGet(
                "http://localhost:8080/RESTfulExample/json/product/get");
            getRequest.addHeader("accept", "application/json");

            HttpResponse response = httpClient.execute(getRequest);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                   + response.getStatusLine().getStatusCode());
            }

            BufferedReader br = new BufferedReader(
                             new InputStreamReader((response.getEntity().getContent())));

            String output;
            System.out.println("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
            }

            httpClient.getConnectionManager().shutdown();

          } catch (ClientProtocolException e) {
        
            e.printStackTrace();

          } catch (IOException e) {
        
            e.printStackTrace();
          }

        }        
    }
    
	private final String GRAN = "&gran=";
	private final String MIN15 = GRAN + "15min";
//	private final String HOURLY = GRAN + "hourly";
//	private final String DAILY = GRAN + "daily";
//	private final String YEARLY = GRAN + "yearly";
	private final String WHSUM = "fields=Wh_sum";
	private static final String DateFormat = "yyyy-MM-dd'T'HH:mm:ss";
	
	private final long MINMS = 60 * 1000L;
	private final long HOURMS = MINMS * 60L;
	private final long DAYMS = 24L * HOURMS;
	private final long WEEKMS = 7L * DAYMS;
//	private final long MONTHMS = 30L * DAYMS;
//	private final long YEARMS = (long) (52.142857 * WEEKMS);
//	
//	private final HashMap<Integer,List<String>> compFieldMap = new HashMap<Integer,List<String>>();
    private final Logger log;
    private final String apiUrl;
    private final String compUrl;
    private final String authUrl;
    private final String availDataUrl = "components/%ID%/dataavailable";
    private final String user;
    private final String secret;
    private final List<String> partnerIds;
    private final Gson gson;
    private final JsonParser jsonParser;
    private final RequestThrottle throttle;
    private final AtomicBoolean gotToken = new AtomicBoolean(false);
    private final AtomicBoolean getTokenFailed = new AtomicBoolean(false);

//    private HashMap<Integer, InverterSum> inverterTotals = new HashMap<Integer, InverterSum>();

    private volatile Timer timer;
 // guarded by gotToken
    private String token;
	private long authRetryDelay;

    public JiraDataAccessor(Properties properties)
    {
        log = LoggerFactory.getLogger(getClass());
        String url = null;//properties.getProperty(LocusEnergyCollectorBridge.ApiUrlKey, LocusEnergyCollectorBridge.ApiUrlDefault);
        if (!url.endsWith("/"))
            url += "/";
        apiUrl = url;
        compUrl = apiUrl + "components/";
        authUrl = null; //properties.getProperty(LocusEnergyCollectorBridge.AuthUrlKey, LocusEnergyCollectorBridge.AuthUrlDefault);
        user = null; //properties.getProperty(LocusEnergyCollectorBridge.UserKey);
        secret = null; //properties.getProperty(LocusEnergyCollectorBridge.SecretKey);
        String value = null; //properties.getProperty(LocusEnergyCollectorBridge.AuthRetryDelayKey, LocusEnergyCollectorBridge.AuthRetryDelayDefault);
        authRetryDelay = Long.parseLong(value);
        partnerIds = new ArrayList<String>();
        List<Entry<String, String>> list = null; //getPropertiesForBaseKey(LocusEnergyCollectorBridge.PartnerIdKey, properties);
        for (Entry<String, String> entry : list)
            partnerIds.add(entry.getValue());
        gson = new GsonBuilder().setPrettyPrinting().create();
        jsonParser = new JsonParser();
        throttle = new RequestThrottle(2);
     // Locus Energy authentication/token timer
        timer = new Timer();
//        timer.schedule(new GetTokenTask(), 0);
    }
    
    public JiraDataAccessor(String authUrl, String apiUrl, String user, String secret, String partnerID, long authRetryDelay)
    {
        log = LoggerFactory.getLogger(getClass());
        String url = apiUrl;
        if (!url.endsWith("/"))
            url += "/";
        this.apiUrl = url;
        compUrl = apiUrl + "components/";
        this.authUrl = authUrl;
        this.user = user;
        this.secret = secret;
        this.authRetryDelay = authRetryDelay;
        partnerIds = new ArrayList<String>();
        partnerIds.add(partnerID);
        gson = new GsonBuilder().setPrettyPrinting().create();
        jsonParser = new JsonParser();
        throttle = new RequestThrottle(2);
     // Locus Energy authentication/token timer
        timer = new Timer();
//        timer.schedule(new GetTokenTask(), 0);
    }

    public Token getTokenObj() throws Exception
    {
    	CloseableHttpClient client = HttpClientBuilder.create().build();
        HttpPost request = new HttpPost(authUrl);
        String payload = "grant_type=client_credentials&client_id=" + user + "&client_secret=" + secret;
        request.setEntity(new StringEntity(payload));
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        HttpResponse response = client.execute(request);
        String body = EntityUtils.toString(response.getEntity());
        request.reset();
        return gson.fromJson(body, Token.class);
    }

    public List<String> getPartnerIds()
    {
        return new ArrayList<String>(partnerIds);
    }

    public Date getSiteInstallDate(int siteId, String tz) throws Exception
    {
//        String token = getToken();
//        String url = apiUrl + "sites/" + siteId + "/installinfo";
//        String body = sendGet(url, token);
//        SiteInstallInfo siteInstallInfo = gson.fromJson(body, SiteInstallInfo.class);
//        if(log.isDebugEnabled())
//            siteInstallInfo.setJson(gson.toJson(jsonParser.parse(body)));
//        return siteInstallInfo.getEarliestDate(tz);
        return null;
    }

    private String sendGet(String url, String token) throws Exception
    {
        int emptyBodyRetryCount = 0;
        LocusEnergyReply leReply =  null;
        String body = null;
        CloseableHttpClient client = HttpClientBuilder.create().build();
        do
        {     
            throttle.waitAsNeeded();       
            HttpGet request = new HttpGet(url);
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Accept", "application/json");
//            request.addHeader("Authorization", "Bearer " + token);
    
            HttpResponse response = null;
            long t2 = System.currentTimeMillis();
            try
            {
                response = client.execute(request);
            }catch(Exception e)
            {
                throw new Exception("Failed to obtain a response from url: " + url, e);
            }
            log.debug("Locus Energy url: " + url + " call took " + (System.currentTimeMillis() - t2));
            body = EntityUtils.toString(response.getEntity());
            try
            {
                leReply =  gson.fromJson(body, LocusEnergyReply.class);
            }catch(Exception e)
            {
                throw new Exception("URL did not return a Locus Energy JSON reply. url: " + url, e);
            }finally{
            	request.reset();
            }
            if(leReply == null)
            {
                if(++emptyBodyRetryCount == 3)
                    break;
            }else
                break;
        }while(true);
//        if(leReply == null)
//            throw new Exception("Request failed json Reply class null. url: " + url);
//        if(leReply.getStatusCode() != 200)
//            throw new Exception("Request failed with: " + leReply.getMessage() + " status: " + leReply.getStatusCode() + " url: " + url);
        return body;
    }
}
