/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */

package com.pslcl.internal.test.simpleNodeFramework.waveform.service.system;

import com.pslcl.internal.test.simpleNodeFramework.config.service.EmitConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.EmitDataAccessorConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.EmitStatusConfig;
import com.pslcl.internal.test.simpleNodeFramework.config.service.ExecutorConfig;

/**
 * Example of a custom service configuration with all options.
 * @param <T> The custom configuration type
 */
public interface WaveformServiceConfig<T> extends EmitConfig<T>, ExecutorConfig<T>, EmitStatusConfig<T>, EmitDataAccessorConfig<T>
{
}
