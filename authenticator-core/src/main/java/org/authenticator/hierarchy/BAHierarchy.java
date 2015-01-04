 package org.authenticator.hierarchy;
 
 import java.util.List;

import org.authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import org.authenticator.hierarchy.exceptions.NoUnusedKeyException;
 import org.authenticator.protobuf.AuthWalletHierarchy;
 import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyCoinTypes;
 import org.authenticator.protobuf.ProtoConfig.ATAccount.ATAccountAddressHierarchy;

import org.bitcoinj.crypto.ChildNumber;
 import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
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
	 /**
	  * Fresh unused keys will be given up to the set keyLookAhead, if the user will ask 
	  * more it will get the same keyLookAhead keys to maintain a strict handling over keys for later hierarchy restoring.
	  */
	static public int keyLookAhead = 10; 
 	 	
 	List<SingleAccountManagerImpl> accounts;
 	int nextAvailableAccount;

 	@SuppressWarnings("static-access")
 	public BAHierarchy() {  }

 	public void buildWalletHierarchyForStartup(List<SingleAccountManagerImpl> trackers){
 		accounts = trackers;
 		calculateNextAvailableAccountIndex();
 	}
 	
 	//###############################
 	//
 	//		API
 	//
 	//###############################


	 /**
	  * This method will return an unused key.<br>
	  * <b>The key could have been returned by this method previously</b><br>
	  * The reason for not returning a truly fresh unseen key is to minimize unused key derivation for future restoring from seed
	  *
	  * @param accountIndex
	  * @param type
	  * @param H
	  * @return
	  * @throws NoUnusedKeyException
	  * @throws NoAccountCouldBeFoundException
	  */
 	public DeterministicKey getNextPubKey(int accountIndex, HierarchyAddressTypes type, ATAccountAddressHierarchy H) throws NoUnusedKeyException, NoAccountCouldBeFoundException {
  	   SingleAccountManagerImpl tracker = getAccountTracker(accountIndex);
  	   int indx = tracker.getUnusedKey(HierarchyAddressTypes.External).getI();
  	   DeterministicKey ret = HierarchyUtils.getPubKeyFromAccount(accountIndex, type, indx, H);
 	   return ret;
 	}
 	
 	public void markAddressAsUsed(int accountIndex, int keyIndex, HierarchyAddressTypes type) throws NoAccountCouldBeFoundException{
 		getAccountTracker(accountIndex).setKeyAsUsed(keyIndex, type);
 	}
 	
 	public int whatIsTheNextAvailableAccountIndex() {
 		return nextAvailableAccount;
 	}
 	
 	public SingleAccountManagerImpl generateNewAccount(){
		int index = this.nextAvailableAccount;
		
		SingleAccountManagerImpl at = addAccountToTracker(index,this.keyLookAhead);
		this.nextAvailableAccount ++;
 		return at;
 	}
 	
 	public SingleAccountManagerImpl addAccountToTracker(int index, int lookAhead){
 		SingleAccountManagerImpl at = new SingleAccountManagerImpl(index, lookAhead);
		accounts.add(at);
		calculateNextAvailableAccountIndex();
		return at;
 	}
 	
 	public void setKeyLookAhead(int value){
		keyLookAhead = value;
		for(SingleAccountManagerImpl a:accounts)
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
 		for(SingleAccountManagerImpl acc: accounts){
 			if(acc.getAccountIndex() > highestAccountIdx)
 				highestAccountIdx = acc.getAccountIndex();
 		}
 		
 		nextAvailableAccount = highestAccountIdx + 1;
 	}
 	

 		
	private SingleAccountManagerImpl getAccountTracker(int accountIndex) throws NoAccountCouldBeFoundException{
		for(int i=0;i<accounts.size();i++){
			SingleAccountManagerImpl t = accounts.get(i);
			if(t.getAccountIndex() == accountIndex){
				return t;
			}
 		}
		throw new NoAccountCouldBeFoundException("Could not find account");
 	}
	
 }
