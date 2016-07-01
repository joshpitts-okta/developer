/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.dsp.hubmanager;

import org.opendof.core.oal.DOFException;
import org.opendof.core.oal.DOFInterface;
import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObject;
import org.opendof.core.oal.DOFType;
import org.opendof.core.oal.value.DOFUInt8;

public class HubRequestInterface
{
    public static final DOFInterfaceID IID = DOFInterfaceID.create("[01:{01000040}]");

    /**
     * GracePeriod_ItemId = 1
     */
    public static final int GracePeriod_ItemId = 1;
    
    /**
     * GracePeriod: uint8 GracePeriod - The time in minutes which the Hub
     * Manage should maintain the hub if it sees the originating service drop
     * this interfaces provide before terminating the Hub. Must be between 0 - 15 inclusive.
     */
    public static final DOFType GracePeriod_Type = DOFUInt8.TYPE;

    /**
     * // * domainId: DomainId // * domainId - The EMIT Domain to associate the
     * Hub with. // * // * groupId: GroupId // * groupId - The UDP Group to
     * associate the Hub with.
     * 
     * gracePeriod: GracePeriod gracePeriod - The number of milliseconds to
     * maintain the Hub on a dropped provide of this interface.
     */
    // @formatter:off
    public static final DOFInterface DEF = new DOFInterface.Builder(IID)
            .addProperty(GracePeriod_ItemId, false, true, GracePeriod_Type)
            .build();
    // @formatter:on

    public static short getGracePeriod(DOFObject dobject, int timeout) throws DOFException
    {
        DOFUInt8 value = (DOFUInt8) dobject.get(DEF.getProperty(GracePeriod_ItemId), timeout).get();
        return value.get();
    }
}
