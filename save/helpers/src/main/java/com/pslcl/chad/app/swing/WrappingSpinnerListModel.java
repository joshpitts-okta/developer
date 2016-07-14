package com.pslcl.chad.app.swing;

import javax.swing.SpinnerListModel;
import javax.swing.SpinnerModel;

@SuppressWarnings("javadoc")
public class WrappingSpinnerListModel extends SpinnerListModel 
{
    private static final long serialVersionUID = -2910740320570355786L;
    private final Object first;
    private final Object last;
    private SpinnerModel linkedModel = null;

    public WrappingSpinnerListModel(Object[] values) 
    {
        super(values);
        first = values[0];
        last = values[values.length - 1];
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
            value = first;
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
            value = last;
            if (linkedModel != null) 
                linkedModel.setValue(linkedModel.getPreviousValue());
        }
        return value;
    }
}
