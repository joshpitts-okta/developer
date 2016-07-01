package com.pslcl.chad.sourceSink;

public interface DataTransferImpl extends Runnable{
	public DataTransferType getDataTransferType();
	public void close();
}
