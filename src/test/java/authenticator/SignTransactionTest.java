package authenticator;

import static org.junit.Assert.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;

public class SignTransactionTest {

	private Address getP2SHAddress(){
		//network params
		NetworkParameters params = MainNetParams.get();
		ECKey k1 = new ECKey();
		ECKey k2 = new ECKey();
		//Create a 2-of-2 multisig output script.
		List<ECKey> keys = ImmutableList.of(k1,k2);//childPubKey, walletKey);
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		
		return multisigaddr;
	}
	
	private ArrayList<TransactionOutput> getInputs(Coin amount, int numOfInputs){
		Coin amountPerInput = amount.divide(numOfInputs);
		ArrayList<TransactionOutput> ret = new ArrayList<TransactionOutput>();
		for(int i=0;i<numOfInputs;i++){
			TransactionOutput out = new TransactionOutput(MainNetParams.get(), 
					null, 
					amountPerInput, 
					new ECKey().toAddress(MainNetParams.get()));
			ret.add(out);
		}
		return ret;
	}
	
	private Transaction preparePayToHashTransaction() throws NoSuchAlgorithmException, AddressFormatException, JSONException, IOException{
		/*Coin outAmpunt = Coin.valueOf(100000000 - 10000);
		TransactionOutput out = new TransactionOutput(MainNetParams.get(), 
				null, 
				outAmpunt, 
				getP2SHAddress());
		ArrayList<TransactionOutput> outs = new ArrayList<TransactionOutput>(); outs.add(out);
		ArrayList<TransactionOutput> inputs = getInputs(outAmpunt.add(Coin.valueOf(10000)), 2);
		
		Transaction tx = new WalletOperation().mkUnsignedTxWithSelectedInputs(inputs, 
				outs, 
				Coin.valueOf(10000),
				new ECKey().toAddress(MainNetParams.get()).toString(),
				MainNetParams.get());*/
		
		Transaction tx = new Transaction(MainNetParams.get());
		return tx;
		
	}
	
	@Test
	public void prepareTxTest() {
		Transaction tx;
		try {
			tx = preparePayToHashTransaction();
			String s = tx.toString();
		} catch (NoSuchAlgorithmException | AddressFormatException
				| JSONException | IOException e) { e.printStackTrace();
		}
		
	}

}
