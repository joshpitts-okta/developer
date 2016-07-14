package com.pslcl.chad.app.swing;

import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;

@SuppressWarnings("javadoc")
public class WrappingSpinnerNumberModel extends SpinnerNumberModel 
{
    private static final long serialVersionUID = 5176701528390057370L;
    private final int first;
    private final int last;
    private SpinnerModel linkedModel = null;

    public WrappingSpinnerNumberModel(int value, int min, int max, int step) 
    {
        super(value, min, max, step);
        first = min;
        last = max;
    }

    public void setLinkedModel(SpinnerModel linkedModel) 
    {
        this.linkedModel = linkedModel;
    }

    @Override
    public Object getNextValue() 
    {
        Object value = super.getNextValue();
        if (value == null)
        {
            value = new Integer(first);
            if (linkedModel != null) 
                linkedModel.setValue(linkedModel.getNextValue());
        }
        return value;
    }

    @Override
    public Object getPreviousValue() 
    {
        Object value = super.getPreviousValue();
        if (value == null) 
        {
            value = new Integer(last);
            if (linkedModel != null) 
                linkedModel.setValue(linkedModel.getPreviousValue());
        }
        return value;
    }
}
