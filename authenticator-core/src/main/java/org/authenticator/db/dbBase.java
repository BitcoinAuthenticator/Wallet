package org.authenticator.db;

import java.io.IOException;

public class dbBase {
	public String filePath;

	public dbBase() { }
	
	public dbBase(String filePath) throws IOException{
		this.filePath = filePath;//new java.io.File( "." ).getCanonicalPath() + "/" + fileName + ".config";
	}
}
