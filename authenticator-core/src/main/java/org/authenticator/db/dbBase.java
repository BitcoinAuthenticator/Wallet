package org.authenticator.db;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.Serializable;

public class DbBase {
	public String filePath;

	public DbBase() { }
	
	public DbBase(String filePath) throws IOException{
		this.filePath = filePath;//new java.io.File( "." ).getCanonicalPath() + "/" + fileName + ".config";
	}

	public byte[] dumpToByteArray() { return null; }
	public String dumpKey() { return ""; }

	public void restoreFromBytes(byte[] data) throws IOException { }
}
