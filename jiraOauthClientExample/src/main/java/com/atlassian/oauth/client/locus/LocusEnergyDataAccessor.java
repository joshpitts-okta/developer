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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.panasonic.pslcl.service.solardatasource.collectorbridge.locusenergy.dataset.ComponentDataField;
import com.panasonic.pslcl.service.solardatasource.collectorbridge.locusenergy.dataset.InverterDatum;
import com.panasonic.pslcl.service.solardatasource.collectorbridge.locusenergy.dataset.InverterDatums;
import com.panasonic.pslcl.service.solardatasource.collectorbridge.locusenergy.dataset.MeterDatums;
import com.panasonic.pslcl.service.solardatasource.collectorbridge.locusenergy.dataset.WeatherStationDatums;
import com.panasonic.pslcl.service.solardatasource.common.DataSet;
import com.panasonic.pslcl.service.solardatasource.common.RequestThrottle;
import com.panasonic.pslcl.service.solardatasource.storage.PersistedStorage;

@SuppressWarnings("javadoc")
public class LocusEnergyDataAccessor
{
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
    private final PersistedStorage storage;

    private HashMap<Integer, InverterSum> inverterTotals = new HashMap<Integer, InverterSum>();

    private volatile Timer timer;
 // guarded by gotToken
    private String token;
	private long authRetryDelay;

    public LocusEnergyDataAccessor(Properties properties, PersistedStorage storage)
    {
    	this.storage = storage;
        log = LoggerFactory.getLogger(getClass());
        String url = properties.getProperty(LocusEnergyCollectorBridge.ApiUrlKey, LocusEnergyCollectorBridge.ApiUrlDefault);
        if (!url.endsWith("/"))
            url += "/";
        apiUrl = url;
        compUrl = apiUrl + "components/";
        authUrl = properties.getProperty(LocusEnergyCollectorBridge.AuthUrlKey, LocusEnergyCollectorBridge.AuthUrlDefault);
        user = properties.getProperty(LocusEnergyCollectorBridge.UserKey);
        secret = properties.getProperty(LocusEnergyCollectorBridge.SecretKey);
        String value = properties.getProperty(LocusEnergyCollectorBridge.AuthRetryDelayKey, LocusEnergyCollectorBridge.AuthRetryDelayDefault);
        authRetryDelay = Long.parseLong(value);
        partnerIds = new ArrayList<String>();
        List<Entry<String, String>> list = getPropertiesForBaseKey(LocusEnergyCollectorBridge.PartnerIdKey, properties);
        for (Entry<String, String> entry : list)
            partnerIds.add(entry.getValue());
        gson = new GsonBuilder().setPrettyPrinting().create();
        jsonParser = new JsonParser();
        throttle = new RequestThrottle(2);
     // Locus Energy authentication/token timer
        timer = new Timer();
        timer.schedule(new GetTokenTask(), 0);
    }
    
    public LocusEnergyDataAccessor(String authUrl, String apiUrl, String user, String secret, String partnerID, long authRetryDelay)
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
        timer.schedule(new GetTokenTask(), 0);
        storage = null;
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

    public Clients getClients(String partnerId) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "partners/" + partnerId + "/clients";
        String body = sendGet(url, token);
        Clients clients = gson.fromJson(body, Clients.class);
        if(log.isDebugEnabled())
            clients.setJson(gson.toJson(jsonParser.parse(body)));
        return clients;
    }

    public Sites getSites(int clientId) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "clients/" + clientId + "/sites";
        String body = sendGet(url, token);
        Sites sites = gson.fromJson(body, Sites.class);
        if(log.isDebugEnabled())
            sites.setJson(gson.toJson(jsonParser.parse(body)));
        return sites;
    }

    public Components getComponents(int siteId) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "sites/" + siteId + "/components";
        String body = sendGet(url, token);
        Components components = gson.fromJson(body, Components.class);
        if(log.isDebugEnabled())
            components.setJson(gson.toJson(jsonParser.parse(body)));
        return components;
    }
    
    public ComponentDataField getComponentDataField(int compID) throws Exception{
    	String token = getToken();
        String url = apiUrl + availDataUrl.replace("%ID%", "" + compID);
        String body = sendGet(url, token);
        ComponentDataField compDataFields = gson.fromJson(body, ComponentDataField.class);
        return compDataFields;
    }

    public Date getSiteInstallDate(int siteId, String tz) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "sites/" + siteId + "/installinfo";
        String body = sendGet(url, token);
        SiteInstallInfo siteInstallInfo = gson.fromJson(body, SiteInstallInfo.class);
        if(log.isDebugEnabled())
            siteInstallInfo.setJson(gson.toJson(jsonParser.parse(body)));
        return siteInstallInfo.getEarliestDate(tz);
    }

    public TreeSet<DataSet> getInverterData(int componentId, String fields, String period, Date start, Date end, Date siteStart) throws Exception
    {
    	String token = getToken();
        String granularity = GRAN + period;
//        granularity = DAILY;
        String url = apiUrl + "components/" + componentId + "/data?" + fields + granularity +  getRangeStr(start, end);
//        url = "https://api.locusenergy.com/v3/components/2115473/data?fields=A_max,BusV_max,W_max,Wh_sum,DCA_max,DCV_max&gran=yearly&start=2014-12-04T07:00:00&end=2015-12-20T08:59:59&tz=UTC";
        String body = sendGet(url, token);
        if(body.length() < 20) return null;
        InverterDatums datums = gson.fromJson(body, InverterDatums.class);
        datums.checkJson(new JsonParser().parse(body).getAsJsonObject().getAsJsonArray("data"));
        if(datums.getData().isEmpty()){
        	return new TreeSet<DataSet>();
        }
        //get all wh_sum data from siteStartup to present
        InverterSum storedTotal = getStoredTotal(componentId);
        
        float whSum = 0f;
        boolean updateStored = false;
        if(storedTotal == null){
        	storedTotal = new InverterSum(siteStart, whSum);
        	updateStored = true;
        }else if(storedTotal.getLastDate().before(start) || storedTotal.getLastDate().equals(start)){
        	updateStored = true;
        }
        Date nextRow = new Date(storedTotal.getLastDate().getTime() + (15 * MINMS));
    	if(!nextRow.before(start)){
        	whSum += storedTotal.getLastTotal();
        	siteStart = storedTotal.getLastDate();
        	updateStored = true;
        	if(start.equals(storedTotal.getLastDate())){
        		start = new Date(start.getTime() + 60000);
        	}
        } else {
        	whSum += getInverterDataWhTotal(componentId, siteStart, start);
        }

        // add the whSum into all the 15min datums wsSum
    	float prevRow = 0;
    	Date lastDate = null;
    	List<InverterDatum> dataList = datums.getData();
    	removeDuplicateData(dataList, siteStart);
        for(InverterDatum datum : dataList){
        	prevRow = datum.getWhSum();
            datum.addWhSum(whSum);
            whSum += prevRow;
            if(lastDate == null || lastDate.before(datum.getTimestamp())){
            	lastDate = datum.getTimestamp();
            }
        }
        if(lastDate != null && updateStored){
        	storeTotal(componentId, new InverterSum(lastDate, whSum));
        }
        return datums.getSortedData();
    }
    
    private void removeDuplicateData(List<InverterDatum> dataList, Date siteStart) {
	    ArrayList<InverterDatum> remove = new ArrayList<InverterDatum>();
	    for(InverterDatum datum : dataList){
	    	Date ts = datum.getTimestamp();
	    	if(ts == null || ts.before(siteStart) || ts.equals(siteStart)){
	    		remove.add(datum);
	    	}
	    }
	    for(InverterDatum datum : remove){
	    	dataList.remove(datum);
	    }
    }

	private InverterSum getStoredTotal(int componentId) {
	    InverterSum sum = inverterTotals.get(componentId);
	    if(sum == null)
	        try {
	            sum = storage.getLastPersistedObject(getStorageName(componentId), InverterSum.class);
            } catch (Exception e) {
	            log.debug("Failed to get InverterSum for {}. Exception: {}", getStorageName(componentId), e.getMessage());
            }
	    return sum;
    }
    
    private void storeTotal(int id, InverterSum sum) {
	    inverterTotals.put(id, sum);
	    try {
	        storage.persistObject(getStorageName(id), sum);
        } catch (IOException e) {
        	log.debug("Failed to store InverterSum for {}. Exception: {}", getStorageName(id), e.getMessage());
        }
    }

	public Float getInverterDataWhTotal(int componentId, Date start, Date end) throws Exception
    {
        String token = getToken();
        float whSum = 0.0F;
        Date reqEnd = new Date(end.getTime() - (MINMS * 15));//Don't include last row of this period.
        long delta = end.getTime() - start.getTime();
        String gran = MIN15;
        
        if(delta > WEEKMS){
        	Date nextStart = new Date(start.getTime() + WEEKMS);
        	whSum += getInverterDataWhTotal(componentId, nextStart, end);
        	reqEnd = new Date(nextStart.getTime() - (MINMS * 15));
        } else if(delta >= HOURMS){
        	reqEnd = new Date(end.getTime() - (MINMS * 15));
        }else{
        	return whSum;
        }
        
        String url = compUrl + componentId + "/data?" + WHSUM + gran + getRangeStr(start, reqEnd);
        String body = sendGet(url, token);
        if(body.length() < 20) return 0f;
        InverterDatums datums = gson.fromJson(body, InverterDatums.class);
        datums.checkJson(new JsonParser().parse(body).getAsJsonObject().getAsJsonArray("data"));
        if(datums.getData().isEmpty()){
        	return whSum;
        }
        
        // total up all the wh_sums
        for(InverterDatum datum : datums.getData()){
            whSum += datum.getWhSum();
        }
        return whSum;
    }
    
    private String getStorageName(Integer id){
    	String storageName = LocusEnergyCollectorBridge.NameDefault;
    	storageName += "/invertersums/" + id.toString() + ".sum";
    	return storageName;
    }
    
    public static String getRangeStr(Date start, Date end) {

        SimpleDateFormat sdf = new SimpleDateFormat(DateFormat);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String startStr = sdf.format(start);
        String endStr = sdf.format(end);
	    return "&start=" + startStr + "&end=" + endStr + "&tz=UTC";
    }

	public TreeSet<DataSet> getMeterData(int componentId, String fields, String period, Date start, Date end) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "components/" + componentId + "/data?" + fields + GRAN + period + getRangeStr(start, end);
        String body = sendGet(url, token);
        MeterDatums datums = gson.fromJson(body, MeterDatums.class);
        datums.checkJson(new JsonParser().parse(body).getAsJsonObject().getAsJsonArray("data"));
        return datums.getSortedData();
    }

	public ComponentDataField getComponentDataFields(int componentId) throws Exception
    {
		Gson gson = new Gson();
        String token = getToken();
        String url = apiUrl + "components/" + componentId + "/dataavailable";
        String body = sendGet(url, token);
        String modBod = StringEscapeUtils.unescapeHtml(body);
        ComponentDataField compFields = gson.fromJson(modBod, ComponentDataField.class);
        return compFields;
    }

    public TreeSet<DataSet> getWeatherStationData(int componentId, String fields, String period, Date start, Date end) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "components/" + componentId + "/data?" + fields + GRAN + period + getRangeStr(start, end);
        String body = sendGet(url, token);
        WeatherStationDatums datums = gson.fromJson(body, WeatherStationDatums.class);
        datums.checkJson(new JsonParser().parse(body).getAsJsonObject().getAsJsonArray("data"));
        return datums.getSortedData();
    }
    
	public TreeSet<DataSet> getCombinerData(int componentId, String fields, String period, Date start, Date end) throws Exception
    {
        String token = getToken();
        String url = apiUrl + "components/" + componentId + "/data?" + fields + GRAN + period + getRangeStr(start, end);
        String body = sendGet(url, token);
        MeterDatums datums = gson.fromJson(body, MeterDatums.class);
        datums.checkJson(new JsonParser().parse(body).getAsJsonObject().getAsJsonArray("data"));
        return datums.getSortedData();
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
            request.addHeader("Authorization", "Bearer " + token);
    
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

    /**
     * Make sure we have a current token before proceeding.
     * 
     * @return true if the token was obtained, false otherwise.
     */
    public boolean waitForToken()
    {
        synchronized (gotToken)
        {
            while (!gotToken.get())
            {
                try
                {
                    gotToken.wait();
                } catch (InterruptedException e)
                {
                }
            }
            if (getTokenFailed.get())
            {
                gotToken.set(false);
                log.warn("Locus Energy: Could not authenticate.  Abandoning attemps until next collection period.");
                return false;
            }
            return true;
        }
    }

    public String getToken()
    {
        synchronized (gotToken)
        {
            return token;
        }
    }

    private class GetTokenTask extends TimerTask
    {
        @Override
        public void run()
        {
            gotToken.set(false);
            synchronized (gotToken)
            {
                gotToken.set(true);
                try
                {
                    Token key = getTokenObj();
                    token = key.getAccessToken();
                    // give it 10 seconds for next request time to complete before key expires
                    timer.schedule(new GetTokenTask(), (key.getExpiresIn() - 10) * 1000);
                    getTokenFailed.set(false);
                } catch (Exception e)
                {
                    token = null;
                    //TODO: make this delay configurable
                    timer.schedule(new GetTokenTask(), authRetryDelay);
                    log.warn("Locus Energy: Could not obtain the api security token: " + e.getClass().getSimpleName() + " " + e.getMessage());
                    getTokenFailed.set(true);
                    return;
                } finally
                {
                    gotToken.notifyAll();
                }
            }
        }
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
                if (Character.isDigit(chars[chars.length - 1]))
                {
                    int strIndex = 0;
                    for (int i = chars.length - 1; i >= 0; i--)
                    {
                        if (!Character.isDigit(chars[i]))
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
        int i = 0;
        int hit = 0;
        do
        {
            StringPair pair = orderingMap.get(i);
            if (pair != null)
            {
                entries.add(pair);
                ++hit;
            }
            ++i;
        } while (hit < found);
        return entries;
    }

    public static class StringPair implements Entry<String, String>
    {
        private final String key;
        private String value;

        public StringPair(Entry<Object, Object> entry)
        {
            key = (String) entry.getKey();
            value = (String) entry.getValue();
        }

        @Override
        public String setValue(String value)
        {
            String old = this.value;
            this.value = value;
            return old;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public String getValue()
        {
            return value;
        }

        @Override
        public String toString()
        {
            return key + "=" + value;
        }
    }
}
