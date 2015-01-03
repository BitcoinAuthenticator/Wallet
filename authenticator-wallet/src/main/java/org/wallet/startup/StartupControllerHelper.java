package org.wallet.startup;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.TextField;

public class StartupControllerHelper {
	
	public static void SSSValidator(TextField txf, ObservableValue<? extends Number> observable, Number oldValue, Number newValue){
		if(newValue.intValue() > oldValue.intValue()){
            char ch = txf.getText().charAt(oldValue.intValue());  
            //Check if the new character is the number or other's
            if(!(ch >= '0' && ch <= '9')){       
                 //if it's not number then just setText to previous one
            	txf.setText(txf.getText().substring(0,txf.getText().length()-1)); 
            }
       }
	}	
}
