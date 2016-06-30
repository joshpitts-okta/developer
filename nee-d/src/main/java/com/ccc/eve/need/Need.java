package com.ccc.eve.need;

import com.beimin.eveapi.EveApi;
import com.beimin.eveapi.connectors.ApiConnector;
import com.beimin.eveapi.exception.ApiException;
import com.beimin.eveapi.parser.ApiAuthorization;
import com.beimin.eveapi.parser.pilot.CharacterSheetParser;
import com.beimin.eveapi.response.pilot.CharacterSheetResponse;

public class Need
{
    private EveApi eveApi;
    private final ApiAuthorization auth;
    
    public Need()
    {
        auth = new ApiAuthorization(4430397, "FadaSJXHTxKesOLn6T1d29YgWOZkHSJiftIMfNymmq3HN4WFUeet3968x57tQn01");
        eveApi = new EveApi(auth);
        EveApi.setConnector(new ApiConnector(ApiConnector.EVE_API_URL));
    }
    
    private void run()
    {
        CharacterSheetParser parser = new CharacterSheetParser();
        try
        {
            CharacterSheetResponse response = parser.getResponse(auth);
            System.out.println("look here");
        } catch (ApiException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }
    
    
    public static void main(String[] args)
    {
        Need need = new Need();
        need.run();
    }
}
