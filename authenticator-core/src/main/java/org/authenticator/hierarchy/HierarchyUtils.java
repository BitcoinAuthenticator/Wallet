package org.authenticator.hierarchy;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.ByteString;
import org.authenticator.hierarchy.exceptions.IncorrectPathException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyPurpose;

import org.authenticator.protobuf.ProtoConfig;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import com.google.common.collect.ImmutableList;
import org.bitcoinj.crypto.HDKeyDerivation;

public class HierarchyUtils {
	
	/**
	 * If the path is full (following BIP 44) than will check for path correctness
	 * 
	 * @param path
	 * @param isFullPath
	 * @return
	 * @throws IncorrectPathException
	 */
	public static ChildNumber getKeyIndexFromPath(ImmutableList<ChildNumber> path, boolean isFullPath) throws IncorrectPathException{
     	if(isFullPath && path.size() < 5)
     		throw new IncorrectPathException("Cannot parse key path, incorrect path.");
		
		return path.get(path.size() - 1);
	}

	/**
	 * Given a seed, the method will return {@link org.bitcoinj.crypto.DeterministicKey DeterministicKey} hierarchy
	 * following BIP44 (https://github.com/bitcoin/bips/blob/master/bip-0044.mediawiki)
	 *
	 * @param seed
	 * @param accountIdx
	 * @param type
	 * @param coinType
	 * @return
	 */
	public static DeterministicKey generatePathUntilAccountsAddress(byte[] seed,
																	int accountIdx,
																	AuthWalletHierarchy.HierarchyAddressTypes type,
																	AuthWalletHierarchy.HierarchyCoinTypes coinType) {

		if(accountIdx > Math.pow(2, 32) | accountIdx < 0) throw new IllegalArgumentException("Account index out of range");

		HDKeyDerivation HDKey = null;

		DeterministicKey masterkey = HDKey.createMasterPrivateKey(seed);
		// purpose level
		ChildNumber purposeIndex = new ChildNumber(HierarchyPurpose.Bip43_VALUE, true); // is harden
		DeterministicKey purpose = HDKey.deriveChildKey(masterkey,purposeIndex);
		// coin level
		ChildNumber coinIndex = new ChildNumber(coinType.getNumber(), true); // is harden
		DeterministicKey coin = HDKey.deriveChildKey(purpose,coinIndex);
		//account
		ChildNumber accountIndex = new ChildNumber(accountIdx, true); // is harden
		DeterministicKey account = HDKey.deriveChildKey(coin, accountIndex);
		//address type
		ChildNumber addressTypeIndex = new ChildNumber(type.getNumber(), false); // is not harden
		DeterministicKey addressType = HDKey.deriveChildKey(account, addressTypeIndex);

		return addressType;
	}

	/**
	 *
	 * @param seed
	 * @param accountIdx
	 * @param type
	 * @param coinType
	 * @return
	 */
	public static ProtoConfig.ATAccount.ATAccountAddressHierarchy generateAccountAddressHierarchy(byte[] seed,
																						   int accountIdx,
																						   AuthWalletHierarchy.HierarchyAddressTypes type,
																						   AuthWalletHierarchy.HierarchyCoinTypes coinType) {

		if(accountIdx > Math.pow(2, 32) | accountIdx < 0) throw new IllegalArgumentException("Account index out of range");

		DeterministicKey addressType = HierarchyUtils.generatePathUntilAccountsAddress(seed, accountIdx, type, coinType);

		ProtoConfig.ATAccount.ATAccountAddressHierarchy.Builder ret = ProtoConfig.ATAccount.ATAccountAddressHierarchy.newBuilder();
		byte[] pubkey = addressType.getPubKey();
		byte[] chaincode = addressType.getChainCode();
		ret.setHierarchyKey(ByteString.copyFrom(pubkey));
		ret.setHierarchyChaincode(ByteString.copyFrom(chaincode));

		return ret.build();
	}

	/**
	 * Will return a {@link org.bitcoinj.crypto.DeterministicKey DeterministicKey} following BIP44
	 *
	 * @param seed
	 * @param accountIdx
	 * @param type
	 * @param addressKey
	 * @param coinType
	 * @return
	 * @throws java.lang.IllegalArgumentException
	 */
	public static DeterministicKey getPrivKeyFromAccount(byte[] seed,
												  int accountIdx,
												  AuthWalletHierarchy.HierarchyAddressTypes type,
												  int addressKey,
												  AuthWalletHierarchy.HierarchyCoinTypes coinType) throws IllegalArgumentException {
		if(addressKey > Math.pow(2, 31) | addressKey < 0) throw new IllegalArgumentException("Key index out of range");
		if(accountIdx > Math.pow(2, 32) | accountIdx < 0) throw new IllegalArgumentException("Account index out of range");

		//path
		DeterministicKey addressType = HierarchyUtils.generatePathUntilAccountsAddress(seed, accountIdx, type, coinType);
		//address
		ChildNumber addressIndex = new ChildNumber(addressKey, false); // is not harden
		return HDKeyDerivation.deriveChildKey(addressType, addressIndex);
	}

	/**
	 * Will return a {@link org.bitcoinj.crypto.DeterministicKey DeterministicKey} following BIP44
	 *
	 * @param accountIndex
	 * @param type
	 * @param addressKey
	 * @param H
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static DeterministicKey getPubKeyFromAccount(int accountIndex,
												 AuthWalletHierarchy.HierarchyAddressTypes type,
												 int addressKey,
												 ProtoConfig.ATAccount.ATAccountAddressHierarchy H) throws IllegalArgumentException{
		if(addressKey > Math.pow(2, 31)) throw new IllegalArgumentException("Key index out of range");
		if(accountIndex > Math.pow(2, 32) | accountIndex < 0) throw new IllegalArgumentException("Account index out of range");

		DeterministicKey addressTypeHDKey = HDKeyDerivation.createMasterPubKeyFromBytes(H.getHierarchyKey().toByteArray(),
				H.getHierarchyChaincode().toByteArray());
		ChildNumber ind = new ChildNumber(addressKey,false);

		return HDKeyDerivation.deriveChildKey(addressTypeHDKey, ind);
	}
}
