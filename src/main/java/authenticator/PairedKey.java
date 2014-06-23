package authenticator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import wallettemplate.Main;
import wallettemplate.utils.KeyUtils;
import wallettemplate.utils.ProtoConfig.Authenticator;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.bitcoin.wallet.DeterministicKeyChain;
import com.google.bitcoin.wallet.KeyChain;
import com.google.common.collect.ImmutableList;

public class PairedKey {
	
	NetworkParameters params;
	ECKey walletKey;
	ECKey authKey;
	String filePath;
	
	public PairedKey (NetworkParameters netparams, KeyChain.KeyPurpose purpose) throws FileNotFoundException, IOException{
		filePath = new java.io.File( "." ).getCanonicalPath() + "/" + Main.APP_NAME + ".config";
		params = netparams;
		HDKeyDerivation HDKey = null;
		Authenticator auth = Authenticator.parseFrom(new FileInputStream(filePath));
		byte[] key = KeyUtils.hexStringToByteArray(auth.getMpubkey());
		byte[] chaincode = KeyUtils.hexStringToByteArray(auth.getChaincode());
  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chaincode);
  		DeterministicKey walletIndex = Main.bitcoin.wallet().freshKey(purpose);
		ChildNumber index = walletIndex.getChildNumber();
		DeterministicKey type = null;
		if (purpose == KeyChain.KeyPurpose.RECEIVE_FUNDS){type = HDKey.deriveChildKey(mPubKey, 0);}
		if (purpose == KeyChain.KeyPurpose.CHANGE){type = HDKey.deriveChildKey(mPubKey, 1);}
		DeterministicKey authIndex = HDKey.deriveChildKey(type, index);
		byte[] walletPubKey = walletIndex.getPubKey();
		byte[] authPubKey = authIndex.getPubKey();
		walletKey = new ECKey(null, walletPubKey);
		authKey = new ECKey(null, authPubKey);
	}
	
	public PairedKey (NetworkParameters netparams, ECKey wal, ECKey auth){
		walletKey = wal;
		authKey = auth;
		params = netparams;
	}
	
	public Address getAddress(){
		List<ECKey> keys = ImmutableList.of(authKey, walletKey);
		//Create a 2-of-2 multisig output script.
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		Main.bitcoin.wallet().addWatchedAddress(multisigaddr);
		return multisigaddr;
	}
	
	public ArrayList<ECKey> getECKeys(){
		ArrayList<ECKey> keys = new ArrayList<ECKey>();
		keys.add(walletKey);
		keys.add(authKey);
		return keys;
	}
	
	public void save(){
		Main.bitcoin.wallet().addWatchedAddress(getAddress());
	}
}
