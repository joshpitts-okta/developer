package com.pslcl.internal.test.simpleNodeFramework;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.opendof.core.oal.DOFObjectID;

import com.pslcl.dsp.UniqueAuthenticatorIDResolver;

@SuppressWarnings("javadoc")
public class RandomIDResolver implements UniqueAuthenticatorIDResolver
{

    private final Random random = new Random();

    private final Map<DOFObjectID, Byte> idMap = new HashMap<DOFObjectID, Byte>();

    @Override
    public byte getAuthenticatorID(DOFObjectID.Domain domainID, DOFObjectID nodeID) throws Exception
    {
        // Assign the same random ID for every domain on a single node.
        byte id;
        if (!idMap.containsKey(nodeID))
        {
            do
            {
                int ran = random.nextInt();
                if(ran < 0)
                    continue;
                id = (byte) (ran % 64);
                break;
            }while(true);
            idMap.put(nodeID, id);
            return id;
        }
        return idMap.get(nodeID);
    }

    @Override
    public void releaseAuthenticatorID(DOFObjectID.Domain domainID, DOFObjectID nodeID, byte authenticatorID)
    {
        // Nothing to do.
    }

}
