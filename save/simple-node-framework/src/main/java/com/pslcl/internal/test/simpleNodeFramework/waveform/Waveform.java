package com.pslcl.internal.test.simpleNodeFramework.waveform;

import java.util.concurrent.atomic.AtomicBoolean;

import org.opendof.core.oal.DOFInterface.Property;
import org.opendof.core.oal.DOFObject;
import org.slf4j.LoggerFactory;

@SuppressWarnings("javadoc")
public class Waveform extends Thread
{
    private static final long Period = 5000;

    private final Property valueProperty;
    private DOFObject object;
    private long startTime;
    private short value;
    private short minValue;
    private short maxValue;

    private final AtomicBoolean stop;
    private final AtomicBoolean waveformEnabled;
    private final Type type;
    private final int serviceIndex;

    public Waveform(Type type, DOFObject object, int serviceIndex)
    {
        valueProperty = WaveformInterface.getValueProperty();
        stop = new AtomicBoolean(false);
        waveformEnabled = new AtomicBoolean(true);
        maxValue = 255;
        this.type = type;
        startTime = System.currentTimeMillis();
        this.object = object;
        this.serviceIndex = serviceIndex;
    }

    public boolean isEnabled()
    {
        return waveformEnabled.get();
    }
    
    public void setEnabled(boolean value)
    {
        waveformEnabled.set(value);
    }
    
    public synchronized short getValue()
    {
        return value;
    }

    public synchronized short getMinValue()
    {
        return minValue;
    }

    public synchronized short getMaxValue()
    {
        return maxValue;
    }

    public synchronized void setValue(short value)
    {
        this.value = value;
        object.changed(valueProperty);
    }

    public synchronized void setMinValue(short value)
    {
        minValue = value;
    }

    public synchronized void setMaxValue(short value)
    {
        maxValue = value;
    }

    @Override
    public void run()
    {
        while (!stop.get())
        {
            try
            {
                Thread.sleep(100);

                synchronized (this)
                {
                    double phase = ((System.currentTimeMillis() - startTime) % Period) / (double) Period;
                    short lastValue = value;
                    short middle = (short) (((maxValue - minValue) / 2) + minValue);
                    short range = (short) ((maxValue - minValue) / 2);
                    if (!waveformEnabled.get())
                        value = middle;
                    else
                    {
                        switch (type)
                        {
                            case Square:
                                if (phase < 0.5)
                                    value = minValue;
                                else
                                    value = maxValue;
                                break;

                            case Triange:
                                if (phase < 0.25)
                                {
                                    short R = (short) (middle + ((range) * 4 * phase));
                                    if (R > maxValue)
                                        R = maxValue;
                                    value = R;
                                } else if (phase < 0.75)
                                {
                                    short R = (short) (maxValue - (range * 4 * (phase - 0.25)));
                                    if (R < minValue)
                                        R = minValue;
                                    value = R;
                                } else
                                {
                                    short R = (short) (minValue + (range * 4 * (phase - 0.75)));
                                    if (R > maxValue)
                                        R = maxValue;
                                    value = R;
                                }
                                break;

                            case Sine:
                                double nextValue = (maxValue * 2 * phase);
                                if(nextValue >= maxValue)
                                {
                                    double delta = nextValue - maxValue;
                                    nextValue = maxValue - delta;
                                }
                                value = (short)nextValue;
                                break;
                            default:
                                break;
                        }
                    }

                    if (value != lastValue)
                        object.changed(valueProperty);

                }

            } catch (Exception e)
            {
                LoggerFactory.getLogger(getClass()).error("Waveform failed: ", e);
            }
        }
    }

    public void close()
    {
        stop.set(true);
    }

    public enum Type
    {
        Sine, Square, Triange
    }
}
