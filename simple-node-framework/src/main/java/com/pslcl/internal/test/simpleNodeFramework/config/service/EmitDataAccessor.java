/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.config.service;

@SuppressWarnings("javadoc")
public interface EmitDataAccessor<T>
{
    public void init(T config) throws Exception;
    public void stop() throws Exception;
}