package authenticator;

import java.io.IOException;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.xml.sax.SAXException;

/**
 * Class using Universal Plug and Play to create a port mapping on a gateway device. 
 */
public class UpNp extends BASE{

	private static int SAMPLE_PORT = 1234;
	private static boolean LIST_ALL_MAPPINGS = false;
	static String externalIPAddress;
	static String localIPAddress;
	static GatewayDevice activeGW;

	public UpNp() { super(UpNp.class); }
	public static void run(String[] args) throws Exception{
		SAMPLE_PORT = Integer.parseInt(args[0]);
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
		// Testing PortMappingNumberOfEntries
		Integer portMapCount = activeGW.getPortMappingNumberOfEntries();
		addLogLine("GetPortMappingNumberOfEntries: " + (portMapCount!=null?portMapCount.toString():"(unsupported)"));
		// Testing getGenericPortMappingEntry
		PortMappingEntry portMapping = new PortMappingEntry();
		if (LIST_ALL_MAPPINGS) {
			int pmCount = 0;
			do {
				if (activeGW.getGenericPortMappingEntry(pmCount,portMapping))
					addLogLine("Portmapping #"+pmCount+" successfully retrieved ("+portMapping.getPortMappingDescription()+":"+portMapping.getExternalPort()+")");
				else{
					addLogLine("Portmapping #"+pmCount+" retrieval failed"); 
					break;
				}
				pmCount++;
			} while (portMapping!=null);
		} else {
			if (activeGW.getGenericPortMappingEntry(0,portMapping))
				addLogLine("Portmapping #0 successfully retrieved ("+portMapping.getPortMappingDescription()+":"+portMapping.getExternalPort()+")");
			else
				addLogLine("Portmapping #0 retrival failed");        	
		}
		InetAddress localAddress = activeGW.getLocalAddress();
		localIPAddress = activeGW.getLocalAddress().toString();
		addLogLine("Using local address: "+ localAddress.getHostAddress());
		externalIPAddress = activeGW.getExternalIPAddress();
		addLogLine("External address: "+ externalIPAddress);
		addLogLine("Querying device to see if a port mapping already exists for port "+ SAMPLE_PORT);
		if (activeGW.getSpecificPortMappingEntry(SAMPLE_PORT,"TCP",portMapping)) {
			addLogLine("Port "+SAMPLE_PORT+" is already mapped. Aborting test.");
			return;
		} else {
			addLogLine("Mapping free. Sending port mapping request for port "+SAMPLE_PORT);
			// Test static lease duration mapping
			if (activeGW.addPortMapping(SAMPLE_PORT,SAMPLE_PORT,localAddress.getHostAddress(),"TCP","test")) {
				addLogLine("Mapping SUCCESSFUL"); // Waiting "+WAIT_TIME+" seconds before removing mapping...");
				//Thread.sleep(1000*WAIT_TIME);
			}
		} 
		addLogLine("Stopping weupnp");
	}
	
	/**This method removes the mapping*/
	public void removeMapping() throws IOException, SAXException{
		if (activeGW.deletePortMapping(SAMPLE_PORT,"TCP")) {
			addLogLine("Port mapping removed, test SUCCESSFUL");
        } else {
			addLogLine("Port mapping removal FAILED");
        }
	}
	
	/**These two methods return the external and local IP address*/
	public String getExternalIP(){
		return externalIPAddress;
	}
	public String getLocalIP(){
		return localIPAddress;
	}

	/**For logging purposes*/
	private static void addLogLine(String line) {
		String timeStamp = DateFormat.getTimeInstance().format(new Date());
		String logline = timeStamp+": "+line+"\n";
		System.out.print(logline);
	}

}