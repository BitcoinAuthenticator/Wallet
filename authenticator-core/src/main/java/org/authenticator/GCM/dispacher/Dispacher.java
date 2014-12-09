package org.authenticator.GCM.dispacher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.json.JSONException;
import org.xml.sax.SAXException;
import org.authenticator.Authenticator;
import org.authenticator.GCM.GCMSender;
import org.authenticator.GCM.exceptions.GCMSendFailedException;
import org.authenticator.network.UpNp;
import org.authenticator.protobuf.ProtoConfig.ATGCMMessageType;

/**
 * A wrapper class for dispaching GCM notifications to devices.
 * 
 * @author alon
 *
 */
public class Dispacher {
	DataOutputStream outStream;
	DataInputStream inStream;
	
	public Dispacher(){}
	public Dispacher(DataOutputStream out,DataInputStream in)
	{
		outStream = out;
		inStream = in;
	}
	
	public String dispachMessage(ATGCMMessageType msgType, Device device, String ... args) throws GCMSendFailedException
	{
		switch (msgType){
		/**
		 * arg:
		 * [0] - Custom msg
		 * [1] - external IP
		 * [2] - Internal IP
		 */
		case SignTX:
			if(device.gcmRegId != null)
			{				
				MessageBuilder msgGCM = null;
				try {
					
					msgGCM = new MessageBuilder(ATGCMMessageType.SignTX,
							new String[]{new String(device.pairingID),
										args[1],
										args[2], 
										args[0]});
					ArrayList<String> devicesList = new ArrayList<String>();
					devicesList.add(new String(device.gcmRegId));
					GCMSender sender = new GCMSender();
					sender.sender(devicesList,msgGCM);
					return msgGCM.getString("RequestID");
				} catch (Exception e) {
					e.printStackTrace();
					throw new GCMSendFailedException("Could not send GCM notification");
				}					
			}
			else
				;//TODO
			break;
		/**
		 * arg:
		 * [0] - external IP
		 * [1] - Internal IP
		 */
		case UpdatePendingRequestIPs:
			if(device.gcmRegId != null)
			{
				MessageBuilder msgGCM = null;
				try {
					msgGCM = new MessageBuilder(ATGCMMessageType.UpdatePendingRequestIPs,
							new String[]{new String(device.pairingID),
											args[0],
											args[1], 
										    ""});
					ArrayList<String> devicesList = new ArrayList<String>();
					devicesList.add(new String(device.gcmRegId));
					GCMSender sender = new GCMSender();
					sender.sender(devicesList,msgGCM);
					return null;
				} catch (Exception e) { 
					e.printStackTrace(); 
					throw new GCMSendFailedException("Could not send GCM notification");
				}
			}
			else
				;//TODO
			break;
			/**
			 * arg:
			 * [0] - Custom msg
			 */
			case CoinsReceived:
				if(device.gcmRegId != null)
				{				
					MessageBuilder msgGCM = null;
					try {
						
						msgGCM = new MessageBuilder(ATGCMMessageType.CoinsReceived,
								new String[]{new String(device.pairingID), args[0]});
						ArrayList<String> devicesList = new ArrayList<String>();
						devicesList.add(new String(device.gcmRegId));
						GCMSender sender = new GCMSender();
						sender.sender(devicesList,msgGCM);
						return null;
					} catch (Exception e) {
						e.printStackTrace();
						throw new GCMSendFailedException("Could not send GCM notification");
					}
					
				}
				else
					;
				break;
		}
		return null;
	}

}
