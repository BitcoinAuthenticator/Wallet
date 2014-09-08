package authenticator.db;

import java.io.IOException;

public class dbBase {
	public String filePath;
	
	public dbBase(String fileName) throws IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + fileName + ".config";
	}
}
