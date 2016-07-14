package com.atlassian.oauth.client.jiraOauthClientExample;

import com.google.common.collect.Lists;

import java.net.URLEncoder;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Encoder;

import org.opendof.tools.repository.interfaces.servlet.auth.RsaPrivateKeyFactory;
import org.opendof.tools.repository.interfaces.servlet.auth.jira.JiraClientInfo;
import org.opendof.tools.repository.interfaces.servlet.auth.jira.rest.JiraRestClient;

/**
 * @since v1.0
 */
@SuppressWarnings("javadoc")
public class JIRAOAuthClient
{
    private static final String CALLBACK_URI = "http://cadams:8080//oauth/jira";
    protected static final String CONSUMER_KEY = "devInterfaceRepository";
//    protected static final String CONSUMER_PRIVATE_KEY = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDFkPMZQaTqsSXI+bSI65rSVaDzic6WFA3WCZMVMi7lYXJAUdkXo4DgdfvEBO21Bno3bXIoxqS411G8S53I39yhSp7z2vcB76uQQifi0LEaklZfbTnFUXcKCyfwgKPp0tQVA+JZei6hnscbSw8qEItdc69ReZ6SK+3LHhvFUUP1nLhJDsgdPHRXSllgZzqvWAXQupGYZVANpBJuK+KAfiaVXCgA71N9xx/5XTSFi5K+e1T4HVnKAzDasAUt7Mmad+1PE+56Gpa73FLk1Ww+xaAEvss6LehjyWHM5iNswoNYzrNS2k6ZYkDnZxUlbrPDELETbz/n3YgBHGUlyrXi2PBjAgMBAAECggEAAtMctqq6meRofuQbEa4Uq5cv0uuQeZLV086VPMNX6k2nXYYODYl36T2mmNndMC5khvBYpn6Ykk/5yjBmlB2nQOMZPLFPwMZVdJ2Nhm+naJLZC0o7fje49PrN2mFsdoZeI+LHVLIrgoILpLdBAz/zTiW+RvLvMnXQU4wdp4eO6i8J/Jwh0AY8rWsAGkk1mdZDwklPZZiwR3z+DDsDwPxFs8z6cE5rWJd2c/fhAQrHwOXyrQPsGyLHTOqS3BkjtEZrKRUlfdgV76VlThwrE5pAWuO0GPyfK/XCklwcNS1a5XxCOq3uUogWRhCsqUX6pYfAVS6xzX56MGDndQVlp7U5uQKBgQDyTDwhsNTWlmr++FyYrc6liSF9NEMBNDubrfLJH1kaOp590bE8fu3BG0UlkVcueUr05e33Kx1DMSFW72lR4dht1jruWsbFp6LlT3SUtyW2kcSet3fC8gySs2r6NncsZ2XFPoxTkalKpQ1atGoBe3XIKeT8RDZtgoLztQy7/7yANQKBgQDQvSHEKS5SttoFFf4YkUh2QmNX5m7XaDlTLB/3xjnlz8NWOweK1aVysb4t2Tct/SR4ZZ/qZDBlaaj4X9h9nlxxIMoXEyX6Ilc4tyCWBXxn6HFMSa/Rrq662Vzz228cPvW2XGOQWdj7IqwKO9cXgJkI5W84YtMtYrTPLDSjhfpxNwKBgGVCoPq/iSOpN0wZhbE1KiCaP8mwlrQhHSxBtS6CkF1a1DPm97g9n6VNfUdnB1Vf0YipsxrSBOe416MaaRyUUzwMBRLqExo1pelJnIIuTG+RWeeu6zkoqUKCAxpQuttu1uRo8IJYZLTSZ9NZhNfbveyKPa2D4G9B1PJ+3rSO+ztlAoGAZNRHQEMILkpHLBfAgsuC7iUJacdUmVauAiAZXQ1yoDDo0Xl4HjcvUSTMkccQIXXbLREh2w4EVqhgR4G8yIk7bCYDmHvWZ2o5KZtD8VO7EVI1kD0z4Zx4qKcggGbp2AINnMYqDetopX7NDbB0KNUklyiEvf72tUCtyDk5QBgSrqcCgYEAnlg3ByRd/qTFz/darZi9ehT68Cq0CS7/B9YvfnF7YKTAv6J2Hd/i9jGKcc27x6IMi0vf7zrqCyTMq56omiLdu941oWfsOnwffWRBInvrUWTj6yGHOYUtg2z4xESUoFYDeWwe/vX6TugL3oXSX3Sy3KWGlJhn/OmsN2fgajHRip0=";
    public static final String CONSUMER_PRIVATE_KEY = "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBANS+E+4pqBUUeDMoCRkgUieIb25vIIO90rCq0PrtJfgVS+mZCptLNNJHSxCuY3DNoBJRmUnLVndrrBzwM0GoY6hJqMibsnL43r1knmvlWE1uGG24hEZVw9V+mkPoVNwZl8UtWsrKQ4Wz9fI9ElYOj8B/rkpZi/IyI2iBVqqZiex3AgMBAAECgYAcTm44cjJ7G44iwGD1hudney/YcdDRtqAiAZhsysESkULYghR3NTqCABpszcdNaw6xYUoUF4oZuanc79jiE5WuwHQi2qGAmWnbACrZ9JuLkx/WJBXMxGYTAlxl9KoCx8SORxdtfjztcakVGR3E/qxut8RfE17zUxgQv9Y+V+FMAQJBAPP78C7Z87/lNmmVnXgNCb6di3gmZYDag4jZukbcue9c2dFBu0Yu2b86Ybm49hS5XSXmch6E2f6upvWASddS1yECQQDfOEGuAnFHsvBf2nnkcU6HN+kuoucbwaVSxmZexPMj6IpBJ/ULhnjMJQ3dc7wlet5LmSQZQ/ZSH59+etXGdAiXAkEAxUI6DAcW9VziQzc9myQfbsd9TWTtx+HjWq991XD7uzS8vlyAhtu2HM+c10VdX6AGWXAZb+dFQI4AWpigGsDVQQJATZ2+GMd4pDmwI2RQZgKQD6x9RF4Yhio7ViDuj51j/eRpzmYaaruDXVi1DX+kuFOU4CyLxM1A5SVhzBTiKHNIWQJBALoEU+Q25Z8EaN6eyis3EcayD6dULZ3JZKy69zzofjdBciYpUi0nGf7SvtNHgDHzmOD8WlgPRIWM/H3/7GY5/zQ=";
    
    
    
    
    public static void getRestUserInfo(JiraClientInfo clientInfo)
    {
        if (false)
        {
            JIRAOAuthClient client = new JIRAOAuthClient();
            String[] args = new String[3];
            args[0] = "request";
            args[1] = clientInfo.getAccessToken().getParameter("oauth_token");
            args[2] = "https://issue.pslcl.com/rest/auth/1/session";
            client.issueRestCommand(args);
            return;
//            myClient(args);
        }
        if (false)
        {
            JIRAOAuthClient client = new JIRAOAuthClient();
            String[] args = new String[3];
            args[0] = "requestToken";
            args[1] = "https://issue.pslcl.com";
            args[2] = "http://cadams:8080/oauth/jira";
            client.issueRestCommand(args);
            return;
        }

//        Token is 3T6Sd7hEIeQRepTYGZCWQ19Gtc2plWPT
//        Token secret is ZSeDVnHoZw9Ak6eav2jUaLULd41HXPTF
//        Retrieved request token. go to https://issue.pslcl.com/plugins/servlet/oauth/authorize?oauth_token=3T6Sd7hEIeQRepTYGZCWQ19Gtc2plWPT
//        queryString: oauth_token=3T6Sd7hEIeQRepTYGZCWQ19Gtc2plWPT&oauth_verifier=9FSgAa
        if (false)
        {
            JIRAOAuthClient client = new JIRAOAuthClient();
            String[] args = new String[5];
            args[0] = "accessToken";
            args[1] = "https://issue.pslcl.com";
            args[2] = "3T6Sd7hEIeQRepTYGZCWQ19Gtc2plWPT";
            args[3] = "3T6Sd7hEIeQRepTYGZCWQ19Gtc2plWPT";
            args[4] = "9FSgAa";
            client.issueRestCommand(args);
            return;
        }

        //        accessToken = VnbHM5EhMGweYRBaQAaEmtpzD6wGwItZ
        //        tokenSecret = 
        // [oauth_verifier=9FSgAa, oauth_token=3T6Sd7hEIeQRepTYGZCWQ19Gtc2plWPT, oauth_consumer_key=devInterfaceRepository, oauth_signature_method=RSA-SHA1, oauth_timestamp=1461094873, oauth_nonce=555607107809177, oauth_version=1.0, oauth_signature=SbG%2F3Qjsr5FlkPZig%2BT5XrPFRZEYaYrTyVZQoBqVT9ayhGAe5PdqA9eWHNuuFcqSRY72lW%2BTg1UG1VFJ8SedYkQxMS0RztV3R7G2JJG1upli50yEHY0uPr3g8TnyvAr8WbyTH830JrRDtxXR4vI5JGxHE9OfgIKHy6nRIiTJKnI%3D]         
        if (true)
        {
            JIRAOAuthClient client = new JIRAOAuthClient();
            String[] args = new String[3];
            args[0] = "request";
            args[1] = "VnbHM5EhMGweYRBaQAaEmtpzD6wGwItZ";
            args[2] = "https://issue.pslcl.com/rest/auth/1/session";
            try{
            client.issueRestCommand(args);
//String baseString =  "GET&https%3A%2F%2Fissue.pslcl.com%2Frest%2Fauth%2F1%2Fsession&oauth_consumer_key%3DdevInterfaceRepository%26oauth_nonce%3D555734084896481%26oauth_signature_method%3DRSA-SHA1%26oauth_timestamp%3D1461095000%26oauth_token%3DCIJTqyUKbmnfB4AfpWihdatsNsoBs8jQ%26oauth_version%3D1.0";
//                      GET&https%3A%2F%2Fissue.pslcl.com%2Frest%2Fauth%2F1%2Fsession%26oauth_consumer_key%3DdevInterfaceRepository%26oauth_nonce%3D555734084896481%26oauth_signature_method%3DRSA-SHA1%26oauth_timestamp%3D1461095000%26oauth_token%3DCIJTqyUKbmnfB4AfpWihdatsNsoBs8jQ%26oauth_version%3D1.0
            
            String baseStringa = "GET&";
            String baseStringb = "https://issue.pslcl.com/rest/auth/1/session";
            String baseStringc = "oauth_consumer_key=devInterfaceRepository&oauth_nonce=555734084896481&oauth_signature_method=RSA-SHA1&oauth_timestamp=1461095000&oauth_token=CIJTqyUKbmnfB4AfpWihdatsNsoBs8jQ&oauth_version=1.0";
            baseStringb  = URLEncoder.encode(baseStringb, JiraRestClient.EncodingDefault);
            baseStringc = URLEncoder.encode(baseStringc, JiraRestClient.EncodingDefault);
            baseStringa = baseStringa+baseStringb+"&"+baseStringc;
            baseStringb = baseStringa;
            String secret = getSignature(baseStringa);
            secret = URLEncoder.encode(secret, JiraRestClient.EncodingDefault);
            String lastParam = "%26"+JiraRestClient.OauthSignatureKey+"%3D"+secret;
            
            System.err.println(secret);
            }catch(Exception e)
            {
                e.printStackTrace();
            }
        }
//jiraRestclient uri    https://issue.pslcl.com/rest/auth/1/session?oauth_consumer_key=devInterfaceRepository&oauth_nonce=563148495487524&oauth_signature_method=RSA-SHA1&oauth_timestamp=1461102415&oauth_token=IdAixp9XcyZtiV8fAuS1L0pfn8uujSIE&oauth_version=1.0&oauth_signature=TSu-j3FmFNmmwcquGJt2JOfrogNiwHv1sFggyadVI_DKz74XgMIDlm7cyl_ttNTLqIg-wjipO_ZtLqExBUFG58JDKP5i8aPbOEnlZbmvtiGhReuhEeN6Y0zh-f2zusbMKgUIHyc8n-4FS6O9bHP3Z-U3IeiCWyq3YH3qZaoWwPU%3D        
    }
    private static final Encoder encoder = Base64.getUrlEncoder();
    public static String getSignature(String baseString) throws Exception
    {
        byte[] baseBytes = baseString.getBytes(JiraRestClient.EncodingDefault);
        PrivateKey pkey = RsaPrivateKeyFactory.getPrivateKey(JIRAOAuthClient.CONSUMER_PRIVATE_KEY);
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(pkey);
        signer.update(baseBytes);
        byte[] signature = signer.sign(); 
        byte[] encoded = encoder.encode(signature);
        return new String(encoded, JiraRestClient.Base64Encoding);
    }
    
    
    
    
    
    
    public enum Command
    {
        REQUEST_TOKEN("requestToken"),
        ACCESS_TOKEN("accessToken"), REQUEST("request");

        private String name;

        Command(final String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }

    public void issueRestCommand(String[] args)
    {
        ArrayList<String> arguments = Lists.newArrayList(args);
        if (arguments.isEmpty())
        {
            throw new IllegalArgumentException("No command specified. Use one of " + getCommandNames() );
        }
        String action = arguments.get(0);
        if (Command.REQUEST_TOKEN.getName().equals(action))
        {
            String baseUrl = arguments.get(1);
            String callBack = "oob";
            if (arguments.size() == 3)
            {
                callBack = arguments.get(2);
            }
            AtlassianOAuthClient jiraoAuthClient = new AtlassianOAuthClient(CONSUMER_KEY, CONSUMER_PRIVATE_KEY, baseUrl, callBack);
            //STEP 1: Get request token
            TokenSecretVerifierHolder requestToken = jiraoAuthClient.getRequestToken();
            String authorizeUrl = jiraoAuthClient.getAuthorizeUrlForToken(requestToken.token);
            System.out.println("Token is " + requestToken.token);
            System.out.println("Token secret is " + requestToken.secret);
            System.out.println("Retrieved request token. go to " + authorizeUrl);
        }
        else if (Command.ACCESS_TOKEN.getName().equals(action))
        {
            String baseUrl = arguments.get(1);
            AtlassianOAuthClient jiraoAuthClient = new AtlassianOAuthClient(CONSUMER_KEY, CONSUMER_PRIVATE_KEY, baseUrl, CALLBACK_URI);
            String requestToken = arguments.get(2);
            String tokenSecret = arguments.get(3);
            String verifier = arguments.get(4);
            String accessToken = jiraoAuthClient.swapRequestTokenForAccessToken(requestToken, tokenSecret, verifier);
            System.out.println("Access token is : " + accessToken);
        }
        else if (Command.REQUEST.getName().equals(action))
        {
            AtlassianOAuthClient jiraoAuthClient = new AtlassianOAuthClient(CONSUMER_KEY, CONSUMER_PRIVATE_KEY, null, CALLBACK_URI);
            String accessToken = arguments.get(1);
            String url = arguments.get(2);
            String responseAsString = jiraoAuthClient.makeAuthenticatedRequest(url, accessToken);
            System.out.println("RESPONSE IS" + responseAsString);
        }
        else
        {
            System.out.println("Command " + action + " not supported. Only " + getCommandNames() + " are supported.");
        }
    }

    private static String getCommandNames()
    {
        String names = "";
        for (Command value : Command.values())
        {
            names += value.getName() + " ";
        }
        return names;
    }
}
