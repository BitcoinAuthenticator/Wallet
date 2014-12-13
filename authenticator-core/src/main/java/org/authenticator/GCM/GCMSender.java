package org.authenticator.GCM;

import java.io.IOException;
import java.util.ArrayList;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

import org.authenticator.GCM.dispacher.MessageBuilder;

public class GCMSender {
	final String GCM_API_KEY = "AIzaSyCKpWWEQk6UDW6ZSAeJfYVyuHcJg1g2V_o";
	
	public void sender(ArrayList<String> devicesList,MessageBuilder msg) throws Exception{
		
	    Sender sender = new Sender(GCM_API_KEY);
	    String data = msg.toString();
		Message message = new Message.Builder()
		                .collapseKey("1")
		                .timeToLive(3)
		                .delayWhileIdle(true)
		                .addData("data",data)
		                .build();
	    MulticastResult result = sender.send(message, devicesList, 1);
	    System.out.println(result.toString());
	    if(result.getFailure() > 0 ) {
	    	String errorString = "Failed to send some or all GCM notifications:\n";
	    	for(Result r: result.getResults()) {
	    		errorString += r.toString() + "\n";
	    	}
	    	throw new Exception(errorString);
	    }
        
        if (result.getResults() != null) {
            int canonicalRegId = result.getCanonicalIds();
            if (canonicalRegId != 0) {
            }
        } else {
            int error = result.getFailure();
            System.out.println(error);
        }
	}
}
