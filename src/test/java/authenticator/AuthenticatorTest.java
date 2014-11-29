package authenticator;

import static org.junit.Assert.*;

import org.junit.Test;

import authenticator.listeners.BAGeneralEventsAdapter;
import authenticator.listeners.BAGeneralEventsListener;

public class AuthenticatorTest {

	@Test
	public void addAndFindGeneralListener() {
		new Authenticator(new BAApplicationParameters());
		BAGeneralEventsListener l = new BAGeneralEventsAdapter();
		Authenticator.addGeneralEventsListener(l);
		BAGeneralEventsListener returned = Authenticator.getGeneralListenerByIdentityHashCode(l);
		assertTrue(returned != null);
	}

	@Test
	public void deleteGeneralListener() {
		new Authenticator(new BAApplicationParameters());
		BAGeneralEventsListener l = new BAGeneralEventsAdapter();
		Authenticator.addGeneralEventsListener(l);
		
		Authenticator.removeGeneralListener(l);
		BAGeneralEventsListener returned = Authenticator.getGeneralListenerByIdentityHashCode(l);
		assertTrue(returned == null);
	}
}
