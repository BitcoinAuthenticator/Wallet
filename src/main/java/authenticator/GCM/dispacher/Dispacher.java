package authenticator.GCM.dispacher;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import org.json.JSONException;
import org.xml.sax.SAXException;

import authenticator.UpNp;
import authenticator.GCM.GCMSender;

public class Dispacher {
	DataOutputStream outStream;
	DataInputStream inStream;
	UpNp plugnplay;
	
	public Dispacher(){}
	public Dispacher(DataOutputStream out,DataInputStream in)
	{
		outStream = out;
		inStream = in;
	}
	
	public void dispachMessage(MessageType msgType, Device device, String ... args) throws JSONException, IOException
	{
		switch (msgType){
		case signTx:
			if(device.gcmRegId != null)
			{
				final int port = 1234;
				
				plugnplay = new UpNp();
				
				try {
					if (!plugnplay.isPortMapped(port)) // TODO - move port to singelton
						plugnplay.run(null);
					//assert(plugnplay.isPortMapped(port));
					MessageBuilder msgGCM = new MessageBuilder(MessageType.signTx,
							new String[]{new String(device.pairingID),plugnplay.getExternalIP(),
							   plugnplay.getLocalIP().substring(1), args[0]});
					ArrayList<String> devicesList = new ArrayList<String>();
					devicesList.add(new String(device.gcmRegId));
					GCMSender sender = new GCMSender();
					sender.sender(devicesList,msgGCM);
					
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else
				;//TODO
			break;
		}
	}

}
