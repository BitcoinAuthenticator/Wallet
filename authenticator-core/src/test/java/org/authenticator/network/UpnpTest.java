package org.authenticator.network;

import org.bitlet.weupnp.GatewayDevice;
import org.bitlet.weupnp.GatewayDiscover;
import org.bitlet.weupnp.PortMappingEntry;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class UpnpTest {

	@Test
	public void validMappingTest() {
		UpNp upnpMock = Mockito.spy(new UpNp());
		// mock GatewayDiscover
		GatewayDiscover GWDMokced = Mockito.mock(GatewayDiscover.class);
		Mockito.doReturn(GWDMokced).when(upnpMock).generateGWDiscoverer();
		{ // discover method
			try {
				Map<InetAddress, GatewayDevice> ret = new HashMap<>();
				Mockito.when(GWDMokced.discover()).thenReturn(ret);

				InetAddress mockedAdd = Mockito.mock(InetAddress.class);
				Mockito.when(mockedAdd.getHostAddress()).thenReturn("host address");
				Mockito.when(mockedAdd.toString()).thenReturn("local address");

				GatewayDevice mockedGD = Mockito.mock(GatewayDevice.class);
				Mockito.when(mockedGD.getFriendlyName()).thenReturn("GD friendly name");
				Mockito.when(mockedGD.getPresentationURL()).thenReturn("GD presentation URL");
				Mockito.when(mockedGD.getModelName()).thenReturn("GD mode name");
				Mockito.when(mockedGD.getModelNumber()).thenReturn("GD model number");
				Mockito.when(mockedGD.getLocalAddress()).thenReturn(mockedAdd);
				Mockito.when(mockedGD.getExternalIPAddress()).thenReturn("GD external IP address");
				Mockito.when(mockedGD.getSpecificPortMappingEntry(Mockito.eq(1111),
						Mockito.eq("TCP"),
						Mockito.any(PortMappingEntry.class)))
						.thenReturn(false);
				Mockito.when(mockedGD.addPortMapping(1111,
						1111,
						"host address",
						"TCP",
						"BTCAuthenticator_Mapping"))
						.thenReturn(true);

				Mockito.when(GWDMokced.getValidGateway()).thenReturn(mockedGD);

				ret.put(mockedAdd, mockedGD);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}

		// test
		try {
			upnpMock.run(new String[]{ new Integer(1111).toString() });

			ArgumentCaptor<String> argMsg = ArgumentCaptor.forClass(String.class);
			Mockito.verify(upnpMock, Mockito.atLeastOnce()).addLogLine(argMsg.capture());

			List<String> logged = argMsg.getAllValues();
			assertTrue(logged.size() == 10);
			assertTrue(logged.get(0).equals("Starting weupnp"));
			assertTrue(logged.get(1).equals("Looking for Gateway Devices..."));
			assertTrue(logged.get(2).equals("1 gateway(s) found\n"));
			assertTrue(logged.get(3).equals("Listing gateway details of device #" + 1 +
					"\n\tFriendly name: GD friendly name" +
					"\n\tPresentation URL: GD presentation URL" +
					"\n\tModel name: GD mode name" +
					"\n\tModel number: GD model number" +
					"\n\tLocal interface address: host address\n"));
			assertTrue(logged.get(4).equals("Using gateway: GD friendly name"));
			assertTrue(logged.get(5).equals("Using local address: host address"));
			assertTrue(logged.get(6).equals("External address: GD external IP address"));
			assertTrue(logged.get(7).equals("Attempting to map port 1111"));
			assertTrue(logged.get(8).equals("Mapping free. Sending port mapping request for port 1111"));
			assertTrue(logged.get(9).equals("Mapping SUCCESSFUL"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}

	@Test
	public void noGateWayTest() {
		UpNp upnpMock = Mockito.spy(new UpNp());
		// mock GatewayDiscover
		GatewayDiscover GWDMokced = Mockito.mock(GatewayDiscover.class);
		Mockito.doReturn(GWDMokced).when(upnpMock).generateGWDiscoverer();
		{ // discover method

			try {
				Map<InetAddress, GatewayDevice> ret = new HashMap<>();
				Mockito.when(GWDMokced.discover()).thenReturn(ret);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}

		// test
		try {
			upnpMock.run(new String[]{ new Integer(1111).toString() });

			ArgumentCaptor<String> argMsg = ArgumentCaptor.forClass(String.class);
			Mockito.verify(upnpMock, Mockito.atLeastOnce()).addLogLine(argMsg.capture());

			List<String> logged = argMsg.getAllValues();
			assertTrue(logged.size() == 4);
			assertTrue(logged.get(0).equals("Starting weupnp"));
			assertTrue(logged.get(1).equals("Looking for Gateway Devices..."));
			assertTrue(logged.get(2).equals("No gateways found"));
			assertTrue(logged.get(3).equals("Stopping weupnp"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

	@Test
	public void portMappedTest() {
		UpNp upnpMock = Mockito.spy(new UpNp());
		// mock GatewayDiscover
		GatewayDiscover GWDMokced = Mockito.mock(GatewayDiscover.class);
		Mockito.doReturn(GWDMokced).when(upnpMock).generateGWDiscoverer();
		{ // discover method
			try {
				Map<InetAddress, GatewayDevice> ret = new HashMap<>();
				Mockito.when(GWDMokced.discover()).thenReturn(ret);

				InetAddress mockedAdd = Mockito.mock(InetAddress.class);
				Mockito.when(mockedAdd.getHostAddress()).thenReturn("host address");
				Mockito.when(mockedAdd.toString()).thenReturn("local address");

				GatewayDevice mockedGD = Mockito.mock(GatewayDevice.class);
				Mockito.when(mockedGD.getFriendlyName()).thenReturn("GD friendly name");
				Mockito.when(mockedGD.getPresentationURL()).thenReturn("GD presentation URL");
				Mockito.when(mockedGD.getModelName()).thenReturn("GD mode name");
				Mockito.when(mockedGD.getModelNumber()).thenReturn("GD model number");
				Mockito.when(mockedGD.getLocalAddress()).thenReturn(mockedAdd);
				Mockito.when(mockedGD.getExternalIPAddress()).thenReturn("GD external IP address");
				Mockito.when(mockedGD.getSpecificPortMappingEntry(Mockito.eq(1111),
						Mockito.eq("TCP"),
						Mockito.any(PortMappingEntry.class)))
						.thenReturn(true);

				Mockito.when(GWDMokced.getValidGateway()).thenReturn(mockedGD);

				ret.put(mockedAdd, mockedGD);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}

		// test
		try {
			upnpMock.run(new String[]{ new Integer(1111).toString() });

			ArgumentCaptor<String> argMsg = ArgumentCaptor.forClass(String.class);
			Mockito.verify(upnpMock, Mockito.atLeastOnce()).addLogLine(argMsg.capture());

			List<String> logged = argMsg.getAllValues();
			assertTrue(logged.size() == 9);
			assertTrue(logged.get(0).equals("Starting weupnp"));
			assertTrue(logged.get(1).equals("Looking for Gateway Devices..."));
			assertTrue(logged.get(2).equals("1 gateway(s) found\n"));
			assertTrue(logged.get(3).equals("Listing gateway details of device #" + 1 +
					"\n\tFriendly name: GD friendly name" +
					"\n\tPresentation URL: GD presentation URL" +
					"\n\tModel name: GD mode name" +
					"\n\tModel number: GD model number" +
					"\n\tLocal interface address: host address\n"));
			assertTrue(logged.get(4).equals("Using gateway: GD friendly name"));
			assertTrue(logged.get(5).equals("Using local address: host address"));
			assertTrue(logged.get(6).equals("External address: GD external IP address"));
			assertTrue(logged.get(7).equals("Attempting to map port 1111"));
			assertTrue(logged.get(8).equals("Port 1111 is already mapped. Aborting test."));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}

	}

	@Test
	public void portMappingFailedTest() {
		UpNp upnpMock = Mockito.spy(new UpNp());
		// mock GatewayDiscover
		GatewayDiscover GWDMokced = Mockito.mock(GatewayDiscover.class);
		Mockito.doReturn(GWDMokced).when(upnpMock).generateGWDiscoverer();
		{ // discover method
			try {
				Map<InetAddress, GatewayDevice> ret = new HashMap<>();
				Mockito.when(GWDMokced.discover()).thenReturn(ret);

				InetAddress mockedAdd = Mockito.mock(InetAddress.class);
				Mockito.when(mockedAdd.getHostAddress()).thenReturn("host address");
				Mockito.when(mockedAdd.toString()).thenReturn("local address");

				GatewayDevice mockedGD = Mockito.mock(GatewayDevice.class);
				Mockito.when(mockedGD.getFriendlyName()).thenReturn("GD friendly name");
				Mockito.when(mockedGD.getPresentationURL()).thenReturn("GD presentation URL");
				Mockito.when(mockedGD.getModelName()).thenReturn("GD mode name");
				Mockito.when(mockedGD.getModelNumber()).thenReturn("GD model number");
				Mockito.when(mockedGD.getLocalAddress()).thenReturn(mockedAdd);
				Mockito.when(mockedGD.getExternalIPAddress()).thenReturn("GD external IP address");
				Mockito.when(mockedGD.getSpecificPortMappingEntry(Mockito.eq(1111),
						Mockito.eq("TCP"),
						Mockito.any(PortMappingEntry.class)))
						.thenReturn(false);
				Mockito.when(mockedGD.addPortMapping(1111,
						1111,
						"host address",
						"TCP",
						"BTCAuthenticator_Mapping"))
						.thenReturn(false);

				Mockito.when(GWDMokced.getValidGateway()).thenReturn(mockedGD);

				ret.put(mockedAdd, mockedGD);
			} catch (Exception e) {
				e.printStackTrace();
				assertTrue(false);
			}
		}

		// test
		try {
			upnpMock.run(new String[]{new Integer(1111).toString()});

			ArgumentCaptor<String> argMsg = ArgumentCaptor.forClass(String.class);
			Mockito.verify(upnpMock, Mockito.atLeastOnce()).addLogLine(argMsg.capture());

			List<String> logged = argMsg.getAllValues();
			assertTrue(logged.size() == 10);
			assertTrue(logged.get(0).equals("Starting weupnp"));
			assertTrue(logged.get(1).equals("Looking for Gateway Devices..."));
			assertTrue(logged.get(2).equals("1 gateway(s) found\n"));
			assertTrue(logged.get(3).equals("Listing gateway details of device #" + 1 +
					"\n\tFriendly name: GD friendly name" +
					"\n\tPresentation URL: GD presentation URL" +
					"\n\tModel name: GD mode name" +
					"\n\tModel number: GD model number" +
					"\n\tLocal interface address: host address\n"));
			assertTrue(logged.get(4).equals("Using gateway: GD friendly name"));
			assertTrue(logged.get(5).equals("Using local address: host address"));
			assertTrue(logged.get(6).equals("External address: GD external IP address"));
			assertTrue(logged.get(7).equals("Attempting to map port 1111"));
			assertTrue(logged.get(8).equals("Mapping free. Sending port mapping request for port 1111"));
			assertTrue(logged.get(9).equals("Port mapping attempt failed"));
		} catch (Exception e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}
}
