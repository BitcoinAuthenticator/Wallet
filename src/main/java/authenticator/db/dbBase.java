package authenticator.db;

import java.io.IOException;

public class dbBase {
	public String filePath;
	
	public dbBase(String appName) throws IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + appName + ".config";
	}
}
