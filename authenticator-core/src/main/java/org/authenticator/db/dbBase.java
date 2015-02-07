package org.authenticator.db;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.Serializable;

public class DBBase {
	public String filePath;

	public DBBase() { }
	
	public DBBase(String filePath) throws IOException{
		this.filePath = filePath;//new java.io.File( "." ).getCanonicalPath() + "/" + fileName + ".config";
	}

	public byte[] dumpToByteArray() { return null; }

	/**
	 * A unique key that will identify the dump.<br>
	 * Backwards package is suggested.
	 * @return
	 */
	public String dumpKey() { return ""; }

	public void restoreFromBytes(byte[] data) throws IOException { }
}
