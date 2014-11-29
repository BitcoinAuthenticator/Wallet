package authenticator.GCM;

import java.io.IOException;
import java.util.ArrayList;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Sender;

import authenticator.GCM.dispacher.MessageBuilder;

public class GCMSender {

		public void sender(ArrayList<String> devicesList,MessageBuilder msg)
		{
			final String GCM_API_KEY = "AIzaSyCKpWWEQk6UDW6ZSAeJfYVyuHcJg1g2V_o";
		    Sender sender = new Sender(GCM_API_KEY);
		    //ArrayList<String> devicesList = new ArrayList<String>();
		    //devicesList.add("APA91bGr1kYu7L6oKUfyCEhg0ofuGoFYdRbqj1QHBFAMVI_eFkYSp2NU3u01MfQ92jhBUVY4qhCYKO-xERCq3t52yKih671fEkNPHS_YIVfrvuj9PcD8_ETAoKdhHAnWpNZkofbFjOzdD0uMamTOQ0_xIoRymcm8DjeZ5zi6sfXryJ-bykS4nd0");
		    String data = msg.toString();//"{Data:Hello World}";
			Message message = new Message.Builder()
			                .collapseKey("1")
			                .timeToLive(3)
			                .delayWhileIdle(true)
			                .addData("data",data)
			                .build();
		    MulticastResult result;
			try {
				result = sender.send(message, devicesList, 1);
//				sender.send(message, devicesList, 1);

                System.out.println(result.toString());
                if (result.getResults() != null) {
                    int canonicalRegId = result.getCanonicalIds();
                    if (canonicalRegId != 0) {
                    }
                } else {
                    int error = result.getFailure();
                    System.out.println(error);
                }
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
}
