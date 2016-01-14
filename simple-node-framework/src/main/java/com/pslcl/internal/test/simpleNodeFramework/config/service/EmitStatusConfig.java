/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.config.service;

import org.pslcl.service.status.StatusTracker;


@SuppressWarnings("javadoc")
public interface EmitStatusConfig<T>
{
    public StatusTracker getStatusTracker();
}