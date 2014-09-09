package authenticator.hierarchy;

import java.util.ArrayList;
import java.util.List;

import authenticator.hierarchy.exceptions.IncorrectPathException;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyPurpose;

import com.google.bitcoin.crypto.ChildNumber;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.common.collect.ImmutableList;

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

}
