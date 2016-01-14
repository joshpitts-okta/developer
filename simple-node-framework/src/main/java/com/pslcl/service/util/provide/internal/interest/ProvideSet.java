/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.service.util.provide.internal.interest;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.pslcl.service.util.provide.BindingRequest;

public class ProvideSet
{
    public final BindingRequest trigger;
    public final Set<InterestValue> provides;
    public final AtomicInteger triggerCount;
    
    public ProvideSet(BindingRequest trigger)
    {
        this.trigger = trigger;
        provides = new HashSet<InterestValue>();
        triggerCount = new AtomicInteger(1);
    }
}
