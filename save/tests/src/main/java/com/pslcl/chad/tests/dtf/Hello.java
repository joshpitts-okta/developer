package com.pslcl.chad.tests.dtf;

import com.pslcl.dtf.core.util.cli.CliBase;

@SuppressWarnings("javadoc")
public class Hello extends CliBase
{
    public Hello(String[] args, String configPathKey)
    {
        super(args, configPathKey);
    }

    public static void main(String args[])
    {
        System.out.println("hello");
    }
}
