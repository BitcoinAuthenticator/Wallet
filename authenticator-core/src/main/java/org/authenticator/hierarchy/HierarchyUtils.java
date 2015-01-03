package org.authenticator.hierarchy;

import java.util.ArrayList;
import java.util.List;

import org.authenticator.hierarchy.exceptions.IncorrectPathException;
import org.authenticator.protobuf.AuthWalletHierarchy;
import org.authenticator.protobuf.AuthWalletHierarchy.HierarchyPurpose;

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
}
