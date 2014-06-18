package authenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BASE {
	public Logger LOG;
	public BASE(Class<?> t)
	{
		LOG = LoggerFactory.getLogger(t);
	}

}
