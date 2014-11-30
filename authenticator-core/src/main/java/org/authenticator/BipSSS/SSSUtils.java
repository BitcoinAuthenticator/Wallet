package org.authenticator.BipSSS;

import java.util.ArrayList;
import java.util.List;

import org.authenticator.BipSSS.BipSSS.Share;

public class SSSUtils {
	public List<Share[]> getAllPossibleCombinations(List<Share> shares, int len){
		List<Share[]> ret = new ArrayList<Share[]>();
		
		getCombination(shares, len, 0, new Share[len], ret);
		return ret;
	}
	
	private void getCombination(List<Share> arr, int len, int startPosition, Share[] result, List<Share[]> finalRsult){
		if (len == 0){
			Share[] temp = new Share[result.length];
			System.arraycopy(result, 0, temp, 0, result.length);
			finalRsult.add(temp);			
            return;
        }       
        for (int i = startPosition; i <= arr.size() - len; i++){
            //result[result.length - len] = arr[i];
            //combinations2(arr, len-1, i+1, result);
        	
        	result[result.length - len] = arr.get(i);
        	getCombination(arr, len-1, i+1, result, finalRsult);
        }
	}
}
