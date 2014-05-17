package authenticator;

import java.io.IOException;

public class BAUtils {

	static public String getAbsolutePathForFile(String fileName) throws IOException
	{
		return new java.io.File( "." ).getCanonicalPath() + "/" + fileName;
	}
}
