package ui_helpers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javafx.scene.control.ComboBox;
import authenticator.Authenticator;
import authenticator.db.PairingObject;

public class ComboBoxHelper {

	public static ArrayList<PairingObject> getUpdatedPairingObjectArray(){
		return Authenticator.getWalletOperation().getAllPairingObjectArray();
	}
	
	public static Map<String,String> populateComboWithPairingNames(ComboBox cmb)
	{
		ArrayList<PairingObject> arr = getUpdatedPairingObjectArray();
		Map<String,String> pairNameToId = new HashMap<String,String>();
		cmb.getItems().clear();
		for(PairingObject po:arr)
		{
			if(!pairNameToId.containsKey(po.pairingName))
				pairNameToId.put(po.pairingName, po.pairingID);
		}
		cmb.getItems().addAll(pairNameToId.keySet());
		return pairNameToId;
	}
}
