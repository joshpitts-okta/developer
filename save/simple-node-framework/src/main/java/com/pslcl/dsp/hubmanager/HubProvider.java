/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.opendof.core.oal.DOF;
import org.opendof.core.oal.DOFApplicationErrorException;
import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFObjectID.Attribute;
import org.opendof.core.oal.DOFObjectID.Authentication;
import org.opendof.core.oal.DOFObjectID.Domain;
import org.opendof.core.oal.DOFOperation;
import org.opendof.core.oal.DOFOperation.Provide;
import org.opendof.core.oal.DOFRequest.Get;
import org.opendof.core.oal.DOFSystem;
import org.opendof.core.oal.DOFValue;
import org.opendof.core.oal.value.DOFUInt8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HubProvider extends DOFObject.DefaultProvider 
{
    private final static long Minute = 1000L * 60L;
    
    private final Logger log;
    private final DOFSystem coreSystem;
    private final DOFObjectID augmentedOid;
    private final int gracePeriodMinutes;
    private final Authentication groupId;
    private final Domain domainId;
    private final AtomicBoolean isRequestProvided = new AtomicBoolean(false);
    private final AtomicLong graceStartTime;
    private final AtomicLong broadcastGetRemaining;
    
    private DOFOperation provideOperation;
    private DOFObject provideObject;

    /**
     * Initialize this HubProvider with a known gracePeriod. The hubProvider will provide on the augmentedProviderID and initialize a hub connection.
     * 
     * The hubProvider will also maintain Activate interest on the {@link HubRequestInterface#IID} in the domain specified by the augmentedProviderID. 
     * If the resulting provide is cancelled for longer than the specified grace period, 
     * 
     * @param coreSystem A system in the requested domain.
     * @param augmentedProviderID An augmented {@link DOFObjectID}. The base OID is the GroupID and the group attribute is the DomainID.
     * @param gracePeriodMinutes The initial grace period. The activated provide in the specified domain will also provide a gracePeriod and is seen as the source of truth.
     */
    public HubProvider(DOFSystem coreSystem, DOFObjectID augmentedProviderID, int gracePeriodMinutes)
    {
        if (!augmentedProviderID.hasAttribute(Attribute.GROUP))
            throw new IllegalArgumentException("Missing required group attribute.");
        this.coreSystem = coreSystem;
        this.augmentedOid = augmentedProviderID;
        this.gracePeriodMinutes = gracePeriodMinutes;
        log = LoggerFactory.getLogger(getClass());
        graceStartTime = new AtomicLong();
        broadcastGetRemaining = new AtomicLong();
        
        groupId = Authentication.create(augmentedProviderID.getBase());
        Attribute attr = augmentedProviderID.getAttribute(Attribute.GROUP);
        domainId = Domain.create(attr.getValueObjectID());
    }

    public void init()
    {
        startProvide();
    }

    public void requestProvideStarted()
    {
        isRequestProvided.set(true);
    }

    public void requestProvideStopped(int broadcastGetRemainig)
    {
        graceStartTime.set(System.currentTimeMillis());
        broadcastGetRemaining.set(broadcastGetRemainig);
        isRequestProvided.set(false);
    }

    private short getRemainingGracePeriod()
    {
        long currentTime = System.currentTimeMillis();
        short minutesExpired = (short) ((currentTime - graceStartTime.get()) / Minute);
        short minutesRemaining = 0;
        if(broadcastGetRemaining.get() != -1)
            minutesRemaining = (short) (broadcastGetRemaining.get() - minutesExpired);
        else
            minutesRemaining = (short) (gracePeriodMinutes - minutesExpired);
        return (short) Math.max(minutesRemaining, 0);
    }

    private void startProvide()
    {
        if (provideOperation != null && !provideOperation.isComplete())
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("\nHubProvider activate for: " + augmentedOid.toStandardString() + "\n");

        sb.append("\tgroupId: " + groupId + "\n");
        sb.append("\tdomain: " + domainId + "\n");

        if (provideObject == null)
            provideObject = coreSystem.createObject(augmentedOid);
        provideOperation = provideObject.beginProvide(HubRequestInterface.DEF, DOF.TIMEOUT_NEVER, this, null);
    }

    //    @Override
    public void destroy()
    {
        if (provideOperation != null)
            provideOperation.cancel();
        if (provideObject != null)
            provideObject.destroy();

        provideOperation = null;
        provideObject = null;
    }

    /**************************************************************************
     * DOFObject.DefaultProvider implementation
     **************************************************************************/

    @Override
    public void get(Provide operation, Get request, Property property)
    {
        int itemId = property.getItemID();
        try
        {
            DOFValue value = null;
            switch (itemId)
            {
                case HubRequestInterface.GracePeriod_ItemId:
                    if (isRequestProvided.get())
                    {
                        value = new DOFUInt8((short) gracePeriodMinutes);
                    } else
                    {
                        value = new DOFUInt8(getRemainingGracePeriod());
                    }
                    break;
                default:
                    String msg = getClass().getName() + ".get ItemId: " + itemId + " not supported by this provider, see provider side logs for details.";
                    log.warn(msg);
                    request.respond(new DOFApplicationErrorException(msg + ", see provider side logs for details."));
                    return;
            }
            request.respond(value);
        } catch (Exception e)
        {
            String msg = getClass().getName() + ".get ItemId: " + itemId + "failed gathering data";
            log.warn(msg, e);
            request.respond(new DOFApplicationErrorException(msg));
        }
    }

    /**************************************************************************
     * GracePeriodProvider implementation TODO: In anticipation of service utils. 
     **************************************************************************/
    public int getGracePeriod()
    {
        return gracePeriodMinutes * 60 * 1000;
    }
}