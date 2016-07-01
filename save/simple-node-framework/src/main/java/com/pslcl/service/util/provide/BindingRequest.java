/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide;

import org.opendof.core.oal.DOFInterfaceID;
import org.opendof.core.oal.DOFObjectID;
import org.opendof.core.oal.DOFSecurityScope;

/**
 *  The binding request data class.
 *  </p>
 *  A binding request originates in a <code>ProvideManager</code>'s 
 *  implementation of the <code>DOFSystem.ActivateInterestListener</code>.
 *  This class captures the domainID, objectID and interfaceID of the reactive 
 *  interest hitting the DOF's activate interest listener callback.  It is an 
 *  immutable data object. 
 *  </p>
 *  BindingRequest objects are passed into the <code>ProvideFactory.getProvideRequestSet</code> 
 *  method of all factories which have registered for the <code>DOFInterfaceID</code> 
 *  of the reactive interest.  
 *  <p>
 *  The application (the <code>ProvideFactory</code>s) then supplies the needed
 *  <code>ProvideRequest</code>s which the <code>ProvideManager</code> will then use
 *  to begin and manage the provides with.
 *   
 * @see ProvideFactory 
 * @see ProvideManager
 */
public class BindingRequest
{
    private final DOFSecurityScope scope;
    private final int remoteDomainID;
    private final DOFObjectID objectID;
    private final DOFInterfaceID interfaceID;

    /**
     * Binding Request copy constructor with different <code>DOFInterfaceID</code>.
     * 
     * @param bindingRequest the <code>BindingRequest</code> to be copied.  Must not be null.
     * @param interfaceID the <code>DOFInterfaceID</code> to be used.  Must not be null. 
     * @throws IllegalArgumentException if an illegal value is passed into the constructor.
     */
    public BindingRequest(BindingRequest bindingRequest, DOFInterfaceID interfaceID)
    {
        if (bindingRequest == null || interfaceID == null)
            throw new IllegalArgumentException("bindingRequest == null || interfaceID == null");
        scope = bindingRequest.scope;
        remoteDomainID = bindingRequest.remoteDomainID;
        objectID = bindingRequest.objectID;
        this.interfaceID = interfaceID;
    }

    /**
     * Binding Request constructor.
     * </p>
     * @param securityScope the remote domain securityScope associated with the provide.  May be null (if non-secure system).
     * @param remoteDomainID the remote domain ID associated with the provide. 
     * -1 if non-secure or domain is the same as the connection, >= 0 otherwise.
     * @param objectID the <code>DOFObjectID</code> to be associated with the provide.  Must not be null.
     * @param interfaceID the <code>DOFInterfaceID</code> to be associated with the provide.  Must not be null.
     * @throws IllegalArgumentException if an illegal value is passed into the constructor.
     */
    public BindingRequest(DOFSecurityScope securityScope, int remoteDomainID, DOFObjectID objectID, DOFInterfaceID interfaceID)
    {
        if (objectID == null || interfaceID == null)
            throw new IllegalArgumentException("objectID == null || interfaceID == null");
        this.remoteDomainID = remoteDomainID;
        this.objectID = objectID;
        this.interfaceID = interfaceID;
        scope = securityScope;
    }

    /**
     * Return the Security Scope.
     * @return The securityScope of the binding request.  May return null (if non-secure system).
     */
    public DOFSecurityScope getSecurityScope()
    {
        return scope;
    }
    
    /**
     * Return the remoteDomainID.
     * @return The remoteDomainID associated with the domain of the binding request.  
     * -1 if non-secure or same as connection domain, >= 0 if remote domain.
     */
    public int getRemoteDomainID()
    {
        return remoteDomainID;
    }
    
    /**
     * Return the objectID. 
     * @return The <code>DOFObjectID</code> to be associated with the provide. Will never return null.  
     */
    public DOFObjectID getObjectID()
    {
        return objectID;
    }
    
    /**
     * Return the interfaceID.
     * @return The <code>DOFInterfaceID</code> to be associated with the provide.  Will never return null. 
     */
    public DOFInterfaceID getInterfaceID()
    {
        return interfaceID;
    }
    
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((interfaceID == null) ? 0 : interfaceID.hashCode());
        result = prime * result + ((objectID == null) ? 0 : objectID.hashCode());
        result = prime * result + remoteDomainID;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof BindingRequest))
            return false;
        BindingRequest other = (BindingRequest) obj;
        
        if (interfaceID == null)
        {
            if (other.interfaceID != null)
                return false;
        } else if (!interfaceID.equals(other.interfaceID))
            return false;
        if (objectID == null)
        {
            if (other.objectID != null)
                return false;
        } else if (!objectID.equals(other.objectID))
            return false;
        if (remoteDomainID != other.remoteDomainID)
            return false;
        return true;
    }

    @Override
    public String toString()
    {
        //@formatter:off
        return "remoteDomainID: " + remoteDomainID +
               " objectID: " + objectID.toStandardString() + 
               " interfaceID: " + interfaceID.toStandardString();
        //@formatter:off
    }
}

