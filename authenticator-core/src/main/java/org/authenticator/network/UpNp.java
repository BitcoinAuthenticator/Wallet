package org.authenticator.network;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

import org.authenticator.BASE;

/**
 * Class using Universal Plug and Play to create a port mapping on a gateway device. 
 */
public class UpNp extends BASE{

	private int PORT;
	static String externalIPAddress;
	static String localIPAddress;
	static GatewayDevice activeGW;

	public UpNp() { super(UpNp.class); }
	/**
	 * args:<br>
	 * [0] - port number
	 * @param args
	 * @throws Exception
	 */
	public void run(String[] args) throws Exception{
		PORT = Integer.parseInt(args[0]);
		boolean LIST_ALL_MAPPINGS = false;
		addLogLine("Starting weupnp");
		GatewayDiscover gatewayDiscover = new GatewayDiscover();
		addLogLine("Looking for Gateway Devices...");
		Map<InetAddress, GatewayDevice> gateways = gatewayDiscover.discover();
		if (gateways.isEmpty()) {
			addLogLine("No gateways found");
			addLogLine("Stopping weupnp");
			return;
		}
		addLogLine(gateways.size()+" gateway(s) found\n");
		int counter=0;
		for (GatewayDevice gw: gateways.values()) {
			counter++;
			addLogLine("Listing gateway details of device #" + counter+
					"\n\tFriendly name: " + gw.getFriendlyName()+
					"\n\tPresentation URL: " + gw.getPresentationURL()+
					"\n\tModel name: " + gw.getModelName()+
					"\n\tModel number: " + gw.getModelNumber()+
					"\n\tLocal interface address: " + gw.getLocalAddress().getHostAddress()+"\n");
		}
		// Choose the first active gateway for the tests
		activeGW = gatewayDiscover.getValidGateway();
		if (null != activeGW) {
			addLogLine("Using gateway: " + activeGW.getFriendlyName());
		} else {
			addLogLine("No active gateway device found");
			addLogLine("Stopping weupnp");
			return;
		}
		
		/* get local and external IPs*/
		InetAddress localAddress = activeGW.getLocalAddress();
		localIPAddress = activeGW.getLocalAddress().toString();
		addLogLine("Using local address: "+ localAddress.getHostAddress());
		externalIPAddress = activeGW.getExternalIPAddress();
		addLogLine("External address: "+ externalIPAddress);
		
		/* Check if port is mapped */
		addLogLine("Attempting to map port " + PORT);
		PortMappingEntry portMapping = new PortMappingEntry();
		if (activeGW.getSpecificPortMappingEntry(PORT,"TCP",portMapping)) {
			addLogLine("Port " + PORT + " is already mapped. Aborting test.");
			return;
		} else {
			addLogLine("Mapping free. Sending port mapping request for port " + PORT);
			if (activeGW.addPortMapping(PORT, PORT,localAddress.getHostAddress(), "TCP", "BTCAuthenticator_Mapping")) {
				addLogLine("Mapping SUCCESSFUL"); 
			}
			else
				addLogLine("Port mapping attempt failed"); 
		} 
	}
	
	public boolean isPortMapped(int port) throws IOException, SAXException
	{
		if (activeGW != null)
		{
			PortMappingEntry portMapping = new PortMappingEntry();
			return activeGW.getSpecificPortMappingEntry(port,"TCP",portMapping);
		}
		return false;
	}
	
	/**This method removes the mapping*/
	public void removeMapping() throws IOException, SAXException{
		if(activeGW != null)
			if (activeGW.deletePortMapping(PORT,"TCP")) {
				addLogLine("Removed port mapping to port: " + PORT);
	        } else {
				addLogLine("Port mapping removal FAILED");
	        }
	}
	
	/**These two methods return the external and local IP address*/
	public String getExternalIP(){
		return externalIPAddress;
	}
	public String getLocalIP(){
		/*
		 * For some reason weupnp returns a '/' prefix with the localIPAddress string, 
		 * this is a hack to fix it
		 */
		if (localIPAddress.substring(0).equals("/")){
				return localIPAddress.substring(1, localIPAddress.length());
		}
		
		return localIPAddress;
	}

	/**For logging purposes*/
	private static void addLogLine(String line) {
		String timeStamp = DateFormat.getTimeInstance().format(new Date());
		String logline = timeStamp+": "+line+"\n";
		System.out.print(logline);
	}

}