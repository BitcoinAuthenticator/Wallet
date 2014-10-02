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
import authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicHierarchy;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException.MnemonicLengthException;
import com.google.protobuf.ByteString;
 
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
 	 	
 	List<AccountTracker> accountTracker;
 	int nextAvailableAccount;
 	HierarchyCoinTypes typeBitcoin;
 	
 	@SuppressWarnings("static-access")
 	public BAHierarchy(){}
 	public BAHierarchy(HierarchyCoinTypes coinType){
 		typeBitcoin = coinType;
 	}
 	
 	public void buildWalletHierarchyForStartup(List<AccountTracker> tracker){
 		this.accountTracker = tracker;
 		calculateNextAvailableAccountIndex();
 	}
 	
 	//###############################
 	//
 	//		API
 	//
 	//###############################
 	
 	public ATAccountAddressHierarchy generateAccountAddressHierarchy(byte[] seed, int accountIdx, HierarchyAddressTypes type) {
 		DeterministicKey addressType = generatePathUntilAccountsAddress(seed, accountIdx, type);
 		
 		ATAccountAddressHierarchy.Builder ret = ATAccountAddressHierarchy.newBuilder();
 		byte[] pubkey = addressType.getPubKey();
		byte[] chaincode = addressType.getChainCode();
 		ret.setHierarchyKey(ByteString.copyFrom(pubkey));
 		ret.setHierarchyChaincode(ByteString.copyFrom(chaincode));
 		
 		return ret.build();
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
 	public DeterministicKey getNextPubKey(int accountIndex, HierarchyAddressTypes type, ATAccountAddressHierarchy H) throws NoUnusedKeyException, NoAccountCouldBeFoundException, KeyIndexOutOfRangeException{
  	   AccountTracker tracker = getAccountTracker(accountIndex); 
  	   int indx = type == HierarchyAddressTypes.External? tracker.getUnusedExternalKey().getI(): tracker.getUnusedInternalKey().getI();
  	   DeterministicKey ret = getPubKeyFromAccount(accountIndex, type, indx, H);	
 	   return ret;
 	}
 	
 	public DeterministicKey getPrivKeyFromAccount(byte[] seed, int accountIndex, HierarchyAddressTypes type, ChildNumber addressKey) throws KeyIndexOutOfRangeException{
 		return getPrivKeyFromAccount(seed, accountIndex, type, addressKey.num());
 	}
 	
 	@SuppressWarnings("static-access")
	public DeterministicKey getPrivKeyFromAccount(byte[] seed, int accountIdx, HierarchyAddressTypes type, int addressKey) throws KeyIndexOutOfRangeException{
 		if(addressKey > Math.pow(2, 31)) throw new KeyIndexOutOfRangeException("Key index out of range");
 		
 		HDKeyDerivation HDKey = null;
 		
 		//path
 		DeterministicKey addressType = this.generatePathUntilAccountsAddress(seed, accountIdx, type);
     	//address
     	ChildNumber addressIndex = new ChildNumber(addressKey, false); // is not harden
     	DeterministicKey address = HDKey.deriveChildKey(addressType, addressIndex);
 		 
 		return address;
 	}
 	
 	public DeterministicKey getPubKeyFromAccount(int accountIndex, 
 			HierarchyAddressTypes type, 
 			int addressKey,
 			ATAccountAddressHierarchy H) throws KeyIndexOutOfRangeException{
 		if(addressKey > Math.pow(2, 31)) throw new KeyIndexOutOfRangeException("Key index out of range");
 		
 		HDKeyDerivation HDKey = null;
 		
 		DeterministicKey addressTypeHDKey = HDKeyDerivation.createMasterPubKeyFromBytes(H.getHierarchyKey().toByteArray(), 
 				H.getHierarchyChaincode().toByteArray());
  	    ChildNumber ind = new ChildNumber(addressKey,false);
 	    
 	    DeterministicKey ret = HDKey.deriveChildKey(addressTypeHDKey, ind);	    
 		return ret;
 	}
 	
 	public void markAddressAsUsed(int accountIndex, int keyIndex, HierarchyAddressTypes type) throws NoAccountCouldBeFoundException{
 		getAccountTracker(accountIndex).setKeyAsUsed(keyIndex, type);
 	}
 	
 	public int whatIsTheNextAvailableAccountIndex() {
 		return nextAvailableAccount;
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
 	
 	public void setKeyLookAhead(int value){
		keyLookAhead = value;
		for(AccountTracker a:accountTracker)
		{
			a.setKeylookahead(value);
		}
	}
 	
 	//###############################
 	//
 	//		Private
 	//
 	//###############################
 	
 	private void calculateNextAvailableAccountIndex(){
 		int highestAccountIdx = 0;
 		for(AccountTracker acc: accountTracker){
 			if(acc.getAccountIndex() > highestAccountIdx)
 				highestAccountIdx = acc.getAccountIndex();
 		}
 		
 		nextAvailableAccount = highestAccountIdx + 1;
 	}
 	
 	private DeterministicKey generatePathUntilAccountsAddress(byte[] seed, int accountIdx, HierarchyAddressTypes type) {
 		HDKeyDerivation HDKey = null;

 		DeterministicKey masterkey = HDKey.createMasterPrivateKey(seed);
     	// purpose level
     	ChildNumber purposeIndex = new ChildNumber(HierarchyPurpose.Bip43_VALUE, true); // is harden
     	DeterministicKey purpose = HDKey.deriveChildKey(masterkey,purposeIndex);
     	// coin level
     	ChildNumber coinIndex = new ChildNumber(typeBitcoin.getNumber(), true); // is harden
     	DeterministicKey coin = HDKey.deriveChildKey(purpose,coinIndex);
 		//account
     	ChildNumber accountIndex = new ChildNumber(accountIdx, true); // is harden
     	DeterministicKey account = HDKey.deriveChildKey(coin, accountIndex);
     	//address type
     	ChildNumber addressTypeIndex = new ChildNumber(type.getNumber(), false); // is not harden
     	DeterministicKey addressType = HDKey.deriveChildKey(account, addressTypeIndex);
     	
     	return addressType;
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
