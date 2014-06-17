package authenticator;

import java.util.List;

import wallettemplate.Main;

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
	DeterministicKey mPubKey;
	
	public PairedKey (NetworkParameters netparams, byte[] key, byte[] chaincode){
		params = netparams;
		HDKeyDerivation HDKey = null;
  		mPubKey = HDKey.createMasterPubKeyFromBytes(key, chaincode);
	}
	
	public Address getCurrentPairedAddress(KeyChain.KeyPurpose purpose){
		DeterministicKey walletIndex = Main.bitcoin.wallet().currentReceiveKey();
		ChildNumber index = walletIndex.getChildNumber();
		HDKeyDerivation HDKey = null;
		DeterministicKey account = HDKey.deriveChildKey(mPubKey, 0);
		DeterministicKey type = null;
		if (purpose == KeyChain.KeyPurpose.RECEIVE_FUNDS){type = HDKey.deriveChildKey(account, 0);}
		if (purpose == KeyChain.KeyPurpose.CHANGE){type = HDKey.deriveChildKey(account, 1);}
		DeterministicKey authIndex = HDKey.deriveChildKey(type, index);
		byte[] walletPubKey = walletIndex.getPubKey();
		byte[] authPubKey = authIndex.getPubKey();
		ECKey walletKey = new ECKey(null, walletPubKey);
		ECKey authKey = new ECKey(null, authPubKey);
		List<ECKey> keys = ImmutableList.of(authKey, walletKey);
		//Create a 2-of-2 multisig output script.
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		Main.bitcoin.wallet().addWatchedAddress(multisigaddr);
		return multisigaddr;
	}
	
	public Address getFreshPairedAddress(KeyChain.KeyPurpose purpose){
		DeterministicKey walletIndex = Main.bitcoin.wallet().freshReceiveKey();
		ChildNumber index = walletIndex.getChildNumber();
		HDKeyDerivation HDKey = null;
		DeterministicKey account = HDKey.deriveChildKey(mPubKey, 0);
		DeterministicKey type = null;
		if (purpose == KeyChain.KeyPurpose.RECEIVE_FUNDS){type = HDKey.deriveChildKey(account, 0);}
		if (purpose == KeyChain.KeyPurpose.CHANGE){type = HDKey.deriveChildKey(account, 1);}
		DeterministicKey authIndex = HDKey.deriveChildKey(type, index);
		byte[] walletPubKey = walletIndex.getPubKey();
		byte[] authPubKey = authIndex.getPubKey();
		ECKey walletKey = new ECKey(null, walletPubKey);
		ECKey authKey = new ECKey(null, authPubKey);
		List<ECKey> keys = ImmutableList.of(authKey, walletKey);
		//Create a 2-of-2 multisig output script.
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		Main.bitcoin.wallet().addWatchedAddress(multisigaddr);
		return multisigaddr;
	}
}
