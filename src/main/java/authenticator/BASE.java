package authenticator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.AbstractService;

public class BASE extends AbstractService{
	public Logger LOG;
	public BASE(Class<?> t)
	{
		LOG = LoggerFactory.getLogger(t);
	}
	
	@Override
	protected void doStart() {
		// TODO Auto-generated method stub
		
	}
	@Override
	protected void doStop() {
		// TODO Auto-generated method stub
		
	}

}
