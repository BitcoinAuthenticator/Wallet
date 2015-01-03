package org.wallet.utils;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseUI {
	
	public Logger LOG;
	public BaseUI(){ }
	public BaseUI(Class<?> t)
	{
		init(t);
	}
	
	public void initialize(Class<?> t){
		init(t);
	}
	
	private void init(Class<?> t){
		LOG = LoggerFactory.getLogger(t);
	}

	/**
	 * For passing params to the controller
	 * 
	 */
	public ArrayList<Object> arrParams;
	public void setParams(ArrayList<Object> param)
	{
		arrParams = param;
	}
	
	public void updateUIForParams(){
		
	}
	
	protected boolean hasParameters(){
		if(arrParams == null)
			return false;
		if(arrParams.size() == 0)
			return false;
		
		return true;
	}

    public void cancel() {
        
    }
}
