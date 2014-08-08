 package authenticator.hierarchy;
 
 import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import authenticator.Authenticator;
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
	static public int keyLookAhead = 10; 
 	
 	DeterministicHierarchy hierarchy;
 	DeterministicKey rootKey;
 	List<AccountTracker> accountTracker;
 	int nextAvailableAccount;
 	HierarchyCoinTypes typeBitcoin;
 	
 	@SuppressWarnings("static-access")
 	public BAHierarchy(){}
 	public BAHierarchy(DeterministicKey masterkey, HierarchyCoinTypes coinType){
 		typeBitcoin = coinType;
 		HDKeyDerivation HDKey = null;
     	// purpose level
     	ChildNumber purposeIndex = new ChildNumber(HierarchyPurpose.Bip43_VALUE,false); // is not harden
     	DeterministicKey purpose = HDKey.deriveChildKey(masterkey,purposeIndex);
     	// coin level
     	ChildNumber coinIndex = new ChildNumber(coinType.getNumber(),false); // is not harden
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
 	
 	public void buildWalletHierarchyForStartup(List<AccountTracker> tracker){
 		this.accountTracker = tracker;
 		calculateNextAvailableAccountIndex();
 	}
 	
 	private void calculateNextAvailableAccountIndex(){
 		int highestAccountIdx = 0;
 		for(AccountTracker acc: accountTracker){
 			if(acc.getAccountIndex() > highestAccountIdx)
 				highestAccountIdx = acc.getAccountIndex();
 		}
 		
 		nextAvailableAccount = highestAccountIdx + 1;
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
 	public DeterministicKey getNextPubKey(int accountIndex, HierarchyAddressTypes type) throws NoUnusedKeyException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException{
  	   AccountTracker tracker = getAccountTracker(accountIndex); //this.accountTracker.get(getAccountPlace(accountIndex));
  	   ChildNumber indx = type == HierarchyAddressTypes.External? tracker.getUnusedExternalKey(): tracker.getUnusedInternalKey();
  	   DeterministicKey ret = getPubKeyFromAccount(accountIndex, type, indx);// hierarchy.deriveChild(p, false, true, indx);	
 	    
 	   return ret;
 	}
 	
 	public DeterministicKey getPrivKeyFromAccount(byte[] seed, int accountIndex, HierarchyAddressTypes type, ChildNumber addressKey) throws KeyIndexOutOfRangeException{
 		return getPrivKeyFromAccount(seed, accountIndex, type, addressKey.num());
 	}
 	@SuppressWarnings("static-access")
	public DeterministicKey getPrivKeyFromAccount(byte[] seed, int accountIndex, HierarchyAddressTypes type, int addressKey) throws KeyIndexOutOfRangeException{
 		if(addressKey > Math.pow(2, 31)) throw new KeyIndexOutOfRangeException("Key index out of range");
 		HDKeyDerivation HDKey = null;

 		DeterministicKey masterkey = HDKey.createMasterPrivateKey(seed);//Authenticator.getWalletOperation().mWalletWrapper.trackedWallet.getKeyChainSeed().getSecretBytes());
     	// purpose level
     	ChildNumber purposeIndex = new ChildNumber(HierarchyPurpose.Bip43_VALUE,false); // is not harden
     	DeterministicKey purpose = HDKey.deriveChildKey(masterkey,purposeIndex);
     	// coin level
     	ChildNumber coinIndex = new ChildNumber(typeBitcoin.getNumber(),false); // is not harden
     	DeterministicKey coin = HDKey.deriveChildKey(purpose,coinIndex);
 		DeterministicHierarchy temp = new DeterministicHierarchy(coin);
 		// root
 		List<ChildNumber> p = new ArrayList<ChildNumber>(coin.getPath());
 		// account
 		ChildNumber account = new ChildNumber(accountIndex,false);
  	    p.add(account);
  	    // address type
  	    ChildNumber addressType = new ChildNumber(type.getNumber(),false); // TODO - also savings addresses
  	    p.add(addressType);
  	    // address
  	    ChildNumber ind = new ChildNumber(addressKey,false);
 	    
 	    DeterministicKey ret = temp.deriveChild(p, false, true, ind);	    
 		return ret;
 	}
 	
 	public DeterministicKey getPubKeyFromAccount(int accountIndex, HierarchyAddressTypes type, ChildNumber addressKey) throws KeyIndexOutOfRangeException{
 		return getPubKeyFromAccount(accountIndex, type, addressKey.num());
 	}
 	public DeterministicKey getPubKeyFromAccount(int accountIndex, HierarchyAddressTypes type, int addressKey) throws KeyIndexOutOfRangeException{
 		if(addressKey > Math.pow(2, 31)) throw new KeyIndexOutOfRangeException("Key index out of range");
 		//root
 		List<ChildNumber> p = new ArrayList<ChildNumber>(rootKey.getPath());
 		// account
 		ChildNumber account = new ChildNumber(accountIndex,false);
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
		
		AccountTracker at = addAccountToTracker(index,this.keyLookAhead);
		this.nextAvailableAccount ++;
 		return at;
 	}
 	
 	public AccountTracker addAccountToTracker(int index, int lookAhead){
 		AccountTracker at = new AccountTracker(index, lookAhead);
		this.accountTracker.add(at);
		calculateNextAvailableAccountIndex();
		return at;
 	}
	
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
	
	public void setKeyLookAhead(int value){
		keyLookAhead = value;
		for(AccountTracker a:accountTracker)
		{
			a.setKeylookahead(value);
		}
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
		 * Those trackers are to return up to #keyLookAhead fresh keys
		 */
		private List<ChildNumber> returnedExternalKeys;
		private List<ChildNumber> returnedInternalKeys;
		
		private int keyLookAhead;
		
		public AccountTracker(int accountIndex, int keyLookAhead){
			this.accountIndex = accountIndex;
			setKeylookahead(keyLookAhead);
			this.usedExternalKeys = new ArrayList<Integer>();
			this.usedInternalKeys = new ArrayList<Integer>();
			init();
		}
		
		public AccountTracker(int accountIndex, int keyLookAhead, List<Integer> usedExternalKeys, List<Integer> usedInternalKeys){
			this.accountIndex = accountIndex;
			setKeylookahead(keyLookAhead);
			this.usedExternalKeys = new ArrayList<Integer>(usedExternalKeys);
			this.usedInternalKeys = new ArrayList<Integer>(usedInternalKeys);
			init();
		}
		
		private void init(){
			this.returnedExternalKeys = new ArrayList<ChildNumber>();
			this.returnedInternalKeys = new ArrayList<ChildNumber>();
		}
		
		public void setKeylookahead(int value){
			this.keyLookAhead = value;
		}
		
		public ChildNumber getUnusedExternalKey() throws NoUnusedKeyException{
			return handleGetKey(usedExternalKeys, returnedExternalKeys);
		}
		
		public ChildNumber getUnusedInternalKey() throws NoUnusedKeyException{
			return handleGetKey(usedInternalKeys, returnedInternalKeys);
		}
		
		private ChildNumber handleGetKey(List<Integer> usedKeyList, List<ChildNumber> returnedKeyList) throws NoUnusedKeyException{
			ChildNumber ret;
			/**
			 * In case we reached our lookahead limit, clean the returned keys
			 * trackers and start again. We empty them because some keys could be
			 * marked as used and make room for new keys 
			 */
			if(returnedKeyList.size() > keyLookAhead)
				returnedKeyList.clear();
			
			ret = getUnusedKeyIndex(usedKeyList, returnedKeyList);
			returnedKeyList.add(ret);
			
			return ret;
		}
		
		private ChildNumber getUnusedKeyIndex(List<Integer> usedKeys, List<ChildNumber> alreadyReturnedKey) throws NoUnusedKeyException{
			// TODO - i have no idea what was i thinking doing this loop !
			// 		  we should find a better way for doing it
			
			for(int i=0; i< Math.pow(2, 31); i++){ 
				if(!usedKeys.contains(i) && !alreadyReturnedKey.contains( new ChildNumber(i, false))){
					return new ChildNumber(i, false);
				}
			}
			
			throw new NoUnusedKeyException("No unused key could be found or derived");
		}
		
		public void setKeyAsUsed(int keyIndex, HierarchyAddressTypes type){
			if(type == HierarchyAddressTypes.External){
				usedExternalKeys.add( new Integer(keyIndex));
			}
			else{
				usedInternalKeys.add( new Integer(keyIndex));
			}
		}
		
		public int getAccountIndex(){ return accountIndex; }
	}
	
 }
