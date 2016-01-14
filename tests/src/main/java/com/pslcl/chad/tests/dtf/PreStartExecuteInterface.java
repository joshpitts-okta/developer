package com.pslcl.chad.tests.dtf;

import java.util.Properties;

import org.apache.commons.cli.CommandLine;

import com.pslcl.dtf.core.runner.config.RunnerConfig;

@SuppressWarnings("javadoc")
public interface PreStartExecuteInterface
{
    public void execute(RunnerConfig config, Properties appProperties, CommandLine bindCmd) throws Exception;
}