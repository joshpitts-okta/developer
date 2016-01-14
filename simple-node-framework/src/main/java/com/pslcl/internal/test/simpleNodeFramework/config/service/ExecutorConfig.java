/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.config.service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("javadoc")
public interface ExecutorConfig<T>
{
    public ExecutorService getExecutor();
    public ScheduledExecutorService getScheduledExecutor();
}