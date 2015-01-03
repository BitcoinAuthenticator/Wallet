
package org.authenticator.helpers;

import java.util.List;
import java.util.Map;

import org.authenticator.BAApplicationParameters;
import org.authenticator.BAApplicationParameters.WrongOperatingSystemException;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * The basic BitcoinAuthenticator Javafx application. Covers functionalities like command line params and general app instantiations
 * @author alon
 *
 */
public class BAApplication extends Application{

	public static BAApplicationParameters ApplicationParams;
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
	
	@Override
	public String toString() {
		return ApplicationParams.toString();
	}
}
