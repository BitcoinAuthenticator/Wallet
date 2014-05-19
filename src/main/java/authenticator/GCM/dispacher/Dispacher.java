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
	ServerSocket ss;
	
	public Dispacher(){}
	public Dispacher(DataOutputStream out,DataInputStream in)
	{
		outStream = out;
		inStream = in;
	}
	
	public void dispose()
	{
		try {
			outStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			inStream.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			plugnplay.removeMapping();
		} catch (IOException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			ss.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void dispachMessage(MessageType msgType, byte[] payload, Device device, boolean keepAlive) throws JSONException, IOException
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
					ss = new ServerSocket (port);
					MessageBuilder msgGCM = new MessageBuilder(MessageType.signTx,
							new String[]{new String(device.pairingID),plugnplay.getExternalIP(),
							   plugnplay.getLocalIP().substring(1)});
					ArrayList<String> devicesList = new ArrayList<String>();
					devicesList.add(new String(device.gcmRegId));
					GCMSender sender = new GCMSender();
					sender.sender(devicesList,msgGCM);
					
					//wait for user response
					System.out.println("Listening for Alice on port "+port+"...");
					Socket socket = ss.accept();
					System.out.println("Connected to Alice");
					//send tx for signing 
					inStream = new DataInputStream(socket.getInputStream());
					outStream = new DataOutputStream(socket.getOutputStream());
					
					write(payload.length,payload);
					
					// dispose
					if(!keepAlive){
						outStream.close();
						inStream.close();
						plugnplay.removeMapping();
						ss.close();
					}
					
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
	
	private void write(int length,byte[] payload) throws IOException  
	{
		outStream.writeInt(length);
		outStream.write(payload);
		
	}
	
	public int readInt() throws IOException
	{
		return inStream.readInt();
	}
	
	public void read(byte[] b) throws IOException
	{
		inStream.read(b);
	}
}
