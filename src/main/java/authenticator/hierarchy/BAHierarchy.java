 package authenticator.hierarchy;
 
 import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import authenticator.hierarchy.exceptions.NoUnusedKeyException;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPurpose;

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
 	List<AccountTracker> accountTracker;
 	int nextAvailableAccount;
 	
 	@SuppressWarnings("static-access")
 	public BAHierarchy(){}
 	public BAHierarchy(DeterministicKey masterkey, HierarchyCoinTypes coinType){
 		HDKeyDerivation HDKey = null;
     	// purpose level
     	ChildNumber purposeIndex = new ChildNumber(HierarchyPurpose.Bip43_VALUE,false); // is harden
     	DeterministicKey purpose = HDKey.deriveChildKey(masterkey,purposeIndex);
     	// coin level
     	ChildNumber coinIndex = new ChildNumber(coinType.getNumber(),false); // is harden
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
 	 * DEPICATED
 	 * 
 	 * @param accountByNumberOfKeys:<br>

	 * &nbsp;&nbsp;&nbsp;&nbsp;<b>Integer[0]</b> - account id<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;<b>Integer[1]</b> - external chain<br>
	 * &nbsp;&nbsp;&nbsp;&nbsp;<b>Integer[2]</b> - internal chain
 	 */
	/*public void buildWalletHierarchyForStartup(List<Integer[]> accountByNumberOfKeys, int nextAvailableAccount){
		this.nextAvailableAccount = nextAvailableAccount;
 		accountsHeightsUsed = new ArrayList<ChildNumber[]>();
 		for(int i=0;i < accountByNumberOfKeys.size(); i++){
 			Integer[] cnt = accountByNumberOfKeys.get(i);
			ChildNumber acc = new ChildNumber(cnt[0],false);
			ChildNumber Ext = new ChildNumber(cnt[1],false); 
			ChildNumber Int = new ChildNumber(cnt[2],false);
			accountsHeightsUsed.add(new ChildNumber[]{acc,Ext,Int});
 		}
 	}*/
 	
 	public void buildWalletHierarchyForStartup(List<AccountTracker> tracker, int nextAvailableAccount){
 		this.nextAvailableAccount = nextAvailableAccount;
 		this.accountTracker = tracker;
 	}
 	
 	/**
 	 * This method will return an unused key.<br>
 	 * <b>The key could have been returned by this method previously</b><br>
 	 * The reason for not returning a truly fresh unseen key is to minimize unused key derivation for future restoring from seed
 	 * 
 	 * 
 	 * @param accountIndex
 	 * @param type
 	 * @return
 	 * @throws NoUnusedKeyException 
 	 * @throws NoAccountCouldBeFoundException 
 	 * @throws KeyIndexOutOfRangeException 
 	 */
 	public DeterministicKey getNextKey(int accountIndex, HierarchyAddressTypes type) throws NoUnusedKeyException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException{
  	   AccountTracker tracker = getAccountTracker(accountIndex); //this.accountTracker.get(getAccountPlace(accountIndex));
  	   ChildNumber indx = type == HierarchyAddressTypes.External? tracker.getUnusedExternalKey(): tracker.getUnusedInternalKey();
  	   DeterministicKey ret = getKeyFromAcoount(accountIndex, type, indx);// hierarchy.deriveChild(p, false, true, indx);	
 	    
 	   return ret;
 	}
 	
 	public DeterministicKey getKeyFromAcoount(int accountIndex, HierarchyAddressTypes type, ChildNumber addressKey) throws KeyIndexOutOfRangeException{
 		return getKeyFromAcoount(accountIndex, type, addressKey.num());
 	}
 	public DeterministicKey getKeyFromAcoount(int accountIndex, HierarchyAddressTypes type, int addressKey) throws KeyIndexOutOfRangeException{
 		if(addressKey > Math.pow(2, 31)) throw new KeyIndexOutOfRangeException("Key index out of range");
 		
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
 	
 	public void markAddressAsUsed(int accountIndex, int keyIndex, HierarchyAddressTypes type) throws NoAccountCouldBeFoundException{
 		getAccountTracker(accountIndex).setKeyAsUsed(keyIndex, type);
 	}
 	
 	public AccountTracker generateNewAccount(){
		int index = this.nextAvailableAccount;
		/*accountsHeightsUsed.add(new ChildNumber[]{  new ChildNumber(this.nextAvailableAccount,false), 
													new ChildNumber(0,false),
													new ChildNumber(0,false) });*/
		AccountTracker at = new AccountTracker(index);
		this.accountTracker.add(at);
		this.nextAvailableAccount ++;
 		return at;
 	}
 	
 	/*private void bumpHeight(int accountIndex, HierarchyAddressTypes type){
 		ChildNumber[] updr;
		int accPlace = getAccountPlace(accountIndex);
		ChildNumber[] x = accountsHeightsUsed.get(accPlace);
		ChildNumber indx = x[type == HierarchyAddressTypes.External? 1:2];
		
	    if(type == HierarchyAddressTypes.External)
	    	updr = new ChildNumber[]{x[0], new ChildNumber(indx.getI() +1, false), x[2]};
	    else
	    	updr = new ChildNumber[]{x[0], x[1], new ChildNumber(indx.getI() +1, false)};
	    accountsHeightsUsed.set(accPlace, updr);
	}*/
	
	private AccountTracker getAccountTracker(int accountIndex) throws NoAccountCouldBeFoundException{
		for(int i=0;i<this.accountTracker.size();i++){
			/*ChildNumber[] x = accountsHeightsUsed.get(i);
			ChildNumber accID = x[0];
			if(accID.num() == accountIndex){
				return i;
			}*/
			AccountTracker t = this.accountTracker.get(i);
			if(t.getAccountIndex() == accountIndex){
				return t;
			}
 		}
		throw new NoAccountCouldBeFoundException("Could not find account");
 	}
 	
 	/**
 	 * This method implements BIP39 to generate a 512 bit seed from 128 bit checksummed entropy. The seed and the
 	 * mnemonic encoded entropy are saved to internal storage.
 	 */
 	/*public static byte[] generateMnemonicSeed(){
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
 	}*/
 
	public class AccountTracker{
		private int accountIndex;
		private List<Integer> usedExternalKeys;
		private List<Integer> usedInternalKeys;
		
		/**
		 * Those trakcers are to return up to 10 fresh keys
		 */
		private List<ChildNumber> returnedExternalKeys;
		private List<ChildNumber> returnedInternalKeys;
		
		public AccountTracker(int accountIndex){
			this.accountIndex = accountIndex;
			this.usedExternalKeys = new ArrayList<Integer>();
			this.usedInternalKeys = new ArrayList<Integer>();
			init();
		}
		
		public AccountTracker(int accountIndex, List<Integer> usedExternalKeys, List<Integer> usedInternalKeys){
			this.accountIndex = accountIndex;
			this.usedExternalKeys = new ArrayList<Integer>(usedExternalKeys);
			this.usedInternalKeys = new ArrayList<Integer>(usedInternalKeys);
			init();
		}
		
		private void init(){
			this.returnedExternalKeys = new ArrayList<ChildNumber>();
			this.returnedInternalKeys = new ArrayList<ChildNumber>();
		}
		
		public ChildNumber getUnusedExternalKey() throws NoUnusedKeyException{
			ChildNumber ret;
			if(returnedExternalKeys.size() < 10){
				ret = getUnusedKeyIndex(usedExternalKeys, returnedExternalKeys);
				returnedExternalKeys.add(ret);
			}
			else{
				ret = returnedExternalKeys.get(0);
				returnedExternalKeys.clear();
				returnedExternalKeys.add(ret);
			}
			
			return ret;
		}
		
		public ChildNumber getUnusedInternalKey() throws NoUnusedKeyException{
			ChildNumber ret;
			if(returnedInternalKeys.size() < 10){
				ret = getUnusedKeyIndex(usedInternalKeys, returnedInternalKeys);
				returnedInternalKeys.add(ret);
			}
			else{
				ret = returnedInternalKeys.get(0);
				returnedInternalKeys.clear();
				returnedInternalKeys.add(ret);
			}
			
			return ret;
		}
		
		private ChildNumber getUnusedKeyIndex(List<Integer> arr, List<ChildNumber> alreadyReturnedKey) throws NoUnusedKeyException{
			for(int i=0; i< Math.pow(2, 31); i++){ // seems a bit excessive no ?
				if(!arr.contains(i) && !alreadyReturnedKey.contains( new ChildNumber(i, false))){
					return new ChildNumber(i, false);
				}
			}
			
			throw new NoUnusedKeyException("No unused key could be found or derived");
		}
		
		public void setKeyAsUsed(int keyIndex, HierarchyAddressTypes type){
			if(type == HierarchyAddressTypes.External)
				usedExternalKeys.add( new Integer(keyIndex));
			else
				usedInternalKeys.add( new Integer(keyIndex));
		}
		
		public int getAccountIndex(){ return accountIndex; }
	}
	
 }
