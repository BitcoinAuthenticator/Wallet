
package authenticator.helpers;

import java.util.List;
import java.util.Map;

import wallettemplate.Main;
import authenticator.BAApplicationParameters;
import authenticator.BAApplicationParameters.WrongOperatingSystemException;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * The basic BitcoinAuthenticator Javafx application. Covers functionalities like command line params and general app instantiations
 * @author alon
 *
 */
public class BAApplication extends Application{
	private String BASE_APP_NAME = "WalletTemplate";

	public static BAApplicationParameters ApplicationParams;
    public NetworkParameters networkParams = MainNetParams.get();
    public WalletAppKit bitcoin;
    
	
    public BAApplication(){ }
    
    /**
     * Must be called on start()
     * @param appName
     * @throws WrongOperatingSystemException 
     */
    public boolean BAInit() throws WrongOperatingSystemException{
    	ApplicationParams = new BAApplicationParameters(getParameters().getNamed(),getParameters().getRaw());
    	return ApplicationParams.getShouldLaunchProgram();
    }
    
    //######################
    //
    //	JavaFX Functions
    //
    //######################
    
	@Override
	public void start(Stage primaryStage) throws Exception {
		
	}
}
