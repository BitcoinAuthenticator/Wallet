package authenticator.hierarchy;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import authenticator.Authenticator;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPurpose;

import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicHierarchy;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.MnemonicCode;
import com.google.bitcoin.crypto.MnemonicException.MnemonicLengthException;

/**
 * This class handles the key hierarchy for the Authenticator wallet.<br>
 * The hierarchy is built uppon BIP44, https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki<br>
 * <b>The Hierarchy:</b><br
 * m / purpose' / coin_type' / account' / change / address_index<br>
 * 
 * @author alon
 *
 */
public class BAHierarchy{
	
	DeterministicHierarchy hierarchy;
	DeterministicKey rootKey;
	/**
	 * 0 - account id
	 * 1 - external chain
	 * 2 - internal chain
	 */
	List<ChildNumber[]> accountsHeightsUsed;
	int nextAvailableAccount;
	
	@SuppressWarnings("static-access")
	public BAHierarchy(byte[] seed, HierarchyCoinTypes coinType){
		HDKeyDerivation HDKey = null;
    	DeterministicKey masterkey = HDKey.createMasterPrivateKey(seed);
    	// purpose level
    	ChildNumber purposeIndex = new ChildNumber(HierarchyPurpose.Bip43_VALUE,true); // is harden
    	DeterministicKey purpose = HDKey.deriveChildKey(masterkey,purposeIndex);
    	// coin level
    	ChildNumber coinIndex = new ChildNumber(coinType.getNumber(),true); // is harden
    	DeterministicKey coin = HDKey.deriveChildKey(purpose,coinIndex);
    	
    	//put root
    	setRoot(coin);
	}
	public BAHierarchy setRoot(DeterministicKey rootKey) {
		this.rootKey = rootKey;
		hierarchy = new DeterministicHierarchy(rootKey);
		return this;
	}
		
	/**
	 * 
	 * 
	 * @param accountByNumberOfKeys:<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;<b>Integer[0]</b> - account id<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;<b>Integer[1]</b> - external chain<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;<b>Integer[2]</b> - internal chain
	 */
	public void buildWalletHierarchyForStartup(List<Integer[]> accountByNumberOfKeys, int nextAvailableAccount){
		this.nextAvailableAccount = nextAvailableAccount;
		accountsHeightsUsed = new ArrayList<ChildNumber[]>();
		for(int i=0;i < accountByNumberOfKeys.size(); i++){
			Integer[] cnt = accountByNumberOfKeys.get(i);
			ChildNumber acc = new ChildNumber(cnt[0],false);
			ChildNumber Ext = new ChildNumber(cnt[1],false); 
			ChildNumber Int = new ChildNumber(cnt[2],false);
			accountsHeightsUsed.add(new ChildNumber[]{acc,Ext,Int});
		}
	}
	
	/**
	 * Will return the next key based on the current last used height for the chain.<br>
	 * Bumps last used height.
	 * 
	 * @param accountIndex
	 * @param type
	 * @return
	 */
	public DeterministicKey getNextKey(int accountIndex, HierarchyAddressTypes type){
		// root
		List<ChildNumber> p = new ArrayList<ChildNumber>(rootKey.getPath());
		// account
		ChildNumber account = new ChildNumber(accountIndex,true);
 	    p.add(account);
 	    // address type
 	    ChildNumber addressType = new ChildNumber(type.getNumber(),false); // TODO - also savings addresses
 	    p.add(addressType);	    
	    
 	   ChildNumber indx;
 	   
 	    if(getAccountPlace(accountIndex) != -1){
 	    	indx = accountsHeightsUsed.get(getAccountPlace(accountIndex))[type == HierarchyAddressTypes.External? 1:2];
 	    }
 	    else
 	    	/**
 	 	    * in case accountsHeightsUsed doesn't have a register of the account 
 	 	    */
 	    	indx = new ChildNumber(0,false);
 	    
 	    DeterministicKey ret = hierarchy.deriveChild(p, false, true, indx);	
	    
	    bumpHeight(accountIndex,type);
	    
		return ret;
	}
	
	public DeterministicKey getKeyFromAcoount(int accountIndex, HierarchyAddressTypes type, int addressKey){
		// root
		List<ChildNumber> p = new ArrayList<ChildNumber>(rootKey.getPath());
		// account
		ChildNumber account = new ChildNumber(accountIndex,true);
 	    p.add(account);
 	    // address type
 	    ChildNumber addressType = new ChildNumber(type.getNumber(),false); // TODO - also savings addresses
 	    p.add(addressType);
 	    // address
 	    ChildNumber ind = new ChildNumber(addressKey,false);
	    
	    DeterministicKey ret = hierarchy.deriveChild(p, false, true, ind);	    
		return ret;
	}
	
	public int generateNewAccount(){
		int ret = this.nextAvailableAccount;
		accountsHeightsUsed.add(new ChildNumber[]{  new ChildNumber(this.nextAvailableAccount,false), 
													new ChildNumber(0,false),
													new ChildNumber(0,false) });
		this.nextAvailableAccount ++;
		return ret;
	}
	
	private void bumpHeight(int accountIndex, HierarchyAddressTypes type){
		ChildNumber[] updr;
		int accPlace = getAccountPlace(accountIndex);
		ChildNumber[] x = accountsHeightsUsed.get(accPlace);
		ChildNumber indx = x[type == HierarchyAddressTypes.External? 1:2];
		
	    if(type == HierarchyAddressTypes.External)
	    	updr = new ChildNumber[]{x[0], new ChildNumber(indx.getI() +1, false), x[2]};
	    else
	    	updr = new ChildNumber[]{x[0], x[1], new ChildNumber(indx.getI() +1, false)};
	    accountsHeightsUsed.set(accPlace, updr);
	}
	
	private int getAccountPlace(int accountIndex){
		for(int i=0;i<accountsHeightsUsed.size();i++){
			ChildNumber[] x = accountsHeightsUsed.get(i);
			ChildNumber accID = x[0];
			if(accID.num() == accountIndex){
				return i;
			}
		}
		
		return -1;
	}
	
	/**
	 * This method implements BIP39 to generate a 512 bit seed from 128 bit checksummed entropy. The seed and the
	 * mnemonic encoded entropy are saved to internal storage.
	 */
	public static byte[] generateMnemonicSeed(){
		//Generate 128 bits entropy.
        SecureRandom secureRandom = null;
		try {
			secureRandom = SecureRandom.getInstance("SHA1PRNG");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		byte[] bytes = new byte[16];
		secureRandom.nextBytes(bytes);
		MnemonicCode ms = null;
		try {
			ms = new MnemonicCode();
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<String> mnemonic = null;
		try {
		mnemonic = ms.toMnemonic(bytes);
		} catch (MnemonicLengthException e) {
			e.printStackTrace();
		}
		byte[] seed = MnemonicCode.toSeed(mnemonic, "");	
		return seed;		
	}

}
