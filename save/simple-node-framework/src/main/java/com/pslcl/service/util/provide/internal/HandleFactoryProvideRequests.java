/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.opendof.core.oal.DOFSystem;
import org.slf4j.LoggerFactory;

import com.pslcl.service.util.provide.BindingRequest;
import com.pslcl.service.util.provide.ProvideRequest;
import com.pslcl.service.util.provide.internal.interest.InterestKey;
import com.pslcl.service.util.provide.internal.interest.InterestValue;
import com.pslcl.service.util.provide.internal.interest.ProvideRequests;
import com.pslcl.service.util.provide.internal.interest.ProvideSet;

public class HandleFactoryProvideRequests implements Callable<Void>
{
    private final ProvideMultiplexer provideMultiplexer;
    private final List<Future<ProvideRequests>> futures;
    private final BindingRequest key;
    
    public HandleFactoryProvideRequests(ProvideMultiplexer provideMultiplexer, BindingRequest key, List<Future<ProvideRequests>> futures, boolean referenceCountKey)
    {
        this.provideMultiplexer = provideMultiplexer;
        this.futures = futures;
        this.key = key;
    }
    
    @Override
    public Void call() throws Exception
    {
        try
        {
            // The ProvideFactories might be blocking on these futures, complete all of them before moving to locking collection access below.
            List<ProvideRequests> provideRequestsList = new ArrayList<ProvideRequests>();
            for(Future<ProvideRequests> future : futures)
                provideRequestsList.add(future.get());

            // trying to obtain a DOFSystem can block, do that here before moving to locking collection access below.
            for(ProvideRequests provideRequests : provideRequestsList)
            {
                if(provideRequests == null)
                    continue;
                List<DOFSystem> systems = new ArrayList<DOFSystem>();
                for(ProvideRequest provideRequest : provideRequests.provideRequests)
                    systems.add(provideMultiplexer.getSystem(provideRequest.getBindingRequest()));
                // note: that if an element of systems is null, it failed, outstandingInterestHandler has been notified and beginProvide below should not be called.
                provideRequests.systems = systems;
            }
            
            // ok, all factory call futures are complete and all futures to obtain remote domain systems are complete
            // start up the provides and calculate the interest reference counts.
            
            // There is a funky cross over here between "interest" and provides where a provide from a provide set is an implied interest.
            // The interest reference counting structures (only used internally) are different than the actual provide tracking structures
            // (which have api listing access). The ProvideSet (trigger, trigger refCount and set of interest values) provides a reference count
            // for the set of requested provides.
            
            synchronized (provideMultiplexer.triggerInterestToProvideSetMap)
            {
                int setCount = 0;
                boolean keyProvideFound = false;
                for(ProvideRequests provideRequests : provideRequestsList)  // there is a provideRequests per factory that was interested in the trigger iid.
                {
                    if(provideRequests == null) // however it could be null, if so, move on to next
                        continue;
                    ++setCount;  // if zero at end of looping, notify outstandingInterestHandler that key interest is outstanding
                    
                    // calculate interest reference counting for this factories provide set
                    InterestKey interestKey = new InterestKey(provideRequests.factory, key);
                    ProvideSet provideSet = new ProvideSet(key);
                    
                    int systemIndex = 0;
                    for(ProvideRequest provideRequest : provideRequests.provideRequests)
                    {
                        DOFSystem system = provideRequests.systems.get(systemIndex++); 
                        BindingRequest br = provideRequest.getBindingRequest();
                        if(!keyProvideFound && br.equals(key))  // see if the trigger interest is in this provide set.
                            keyProvideFound = true;
                        InterestValue interestValue = new InterestValue(br);
                        provideSet.provides.add(interestValue); // build the provide interest set.
                        // build the reverse mapping of interest to factory provide sets.
                        // it is an un-deterministic situation if two factory sets are trying to provide the same bindingRequest.  We will log the situation but not track the multiples.
                        InterestKey key = provideMultiplexer.bindingRequestToTriggerInterestMap.get(br);
                        if(key == null)
                            provideMultiplexer.bindingRequestToTriggerInterestMap.put(br, interestKey);
                        else
                            LoggerFactory.getLogger(getClass()).warn("multiple factories providing same bindingRequest, triggerInterest:  " + interestKey.toString() + " bindingRequest: " + br.toString());
                        // provide set interests have been captured, Fire up the provide.
                        if(system != null)
                            provideMultiplexer.beginProvide(provideRequests.factory, system, provideRequest, interestValue);
                    }
                    provideMultiplexer.triggerInterestToProvideSetMap.put(interestKey, provideSet);
                }
                if(setCount == 0 || !keyProvideFound)
                {
                    // notify the outstandingInterestHandler that there is no provide for this interest.
                    synchronized(provideMultiplexer)
                    {
                        if(provideMultiplexer.outstandingInterestHandler != null)
                            provideMultiplexer.outstandingInterestHandler.outstandingInterest(key);
                    }
                }
            }
        }catch(Exception e)
        {
            LoggerFactory.getLogger(getClass()).error("Unchecked exception thrown", e);
            throw e;
        }
        return null;
    }
}
