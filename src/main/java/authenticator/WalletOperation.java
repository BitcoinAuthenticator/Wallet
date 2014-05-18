package authenticator;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.util.encoders.Hex;

import authenticator.db.PairingObject;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.crypto.DeterministicKey;
import com.google.bitcoin.crypto.HDKeyDerivation;
import com.google.bitcoin.crypto.TransactionSignature;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.script.ScriptBuilder;
import com.google.common.collect.ImmutableList;

/*import dispacher.Device;
import dispacher.Dispacher;
import dispacher.MessageType;*/


/**
 * This class is a collection of methods for creating and sending a transaction over to the Authenticator
 */
public class WalletOperation extends BASE{
	
	static String unsignedTx;
	static Transaction spendtx;
	static Map<String,Integer>  mpNumInputs;
	static Map<String,ArrayList<byte[]>> mpPublickeys;
	static Map<String,ArrayList<Integer>> mpChildkeyindex;
	static Map<String,Boolean> mpTestnet;
	
	public WalletOperation() throws IOException{
		super(WalletOperation.class);
		if(mpNumInputs == null)
			mpNumInputs = new HashMap<String,Integer>();
		if(mpPublickeys == null)
			mpPublickeys = new HashMap<String,ArrayList<byte[]>>();
		if(mpChildkeyindex == null)
			mpChildkeyindex = new HashMap<String,ArrayList<Integer>>();
		if(mpChildkeyindex == null)
			mpTestnet = new HashMap<String,Boolean>();
		
		String filePath = BAUtils.getAbsolutePathForFile("wallet.json");//new java.io.File( "." ).getCanonicalPath() + "/wallet.json";
		File f = new File(filePath);
		if(f.exists() && !f.isDirectory()) {
			WalletFile file = new WalletFile();
			mpTestnet = file.getTestnets();
		}
	}
	
	
	/**Pushes the raw transaction the the Eligius mining pool*/
	void pushTx(String tx) throws IOException{
		this.LOG.info("Broadcasting to network...");
		String urlParameters = "transaction="+ tx + "&send=Push";
		String request = "http://eligius.st/~wizkid057/newstats/pushtxn.php";
		URL url = new URL(request); 
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false); 
		connection.setRequestMethod("POST"); 
		connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded"); 
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
		connection.setUseCaches (false);
		DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
		wr.writeBytes(urlParameters);
		wr.flush();
		wr.close();
		int responseCode = connection.getResponseCode();
		this.LOG.info("\nSending 'POST' request to URL : " + url);
		//Get reponse 
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();
		connection.disconnect();
		//Print txid
		this.LOG.info("Success!");
		this.LOG.info("txid: " + response.substring(response.indexOf("string(64) ")+12, response.indexOf("string(64) ")+76));
	}
	
	/**
	 * Derives a child public key from the master public key. Generates a new local key pair.
	 * Uses the two public keys to create a 2of2 multisig address. Saves key and address to json file.
	 * @throws AddressFormatException 
	 */
	@SuppressWarnings("static-access")
	public String genAddress(String pairingID) throws NoSuchAlgorithmException, JSONException, AddressFormatException{
		//Derive the child public key from the master public key.
		WalletFile file = new WalletFile();
		ArrayList<String> keyandchain = file.getPubAndChain(pairingID);
		byte[] key = hexStringToByteArray(keyandchain.get(0));
		byte[] chain = hexStringToByteArray(keyandchain.get(1));
		int index = (int) file.getKeyNum(pairingID)+1;
		HDKeyDerivation HDKey = null;
  		DeterministicKey mPubKey = HDKey.createMasterPubKeyFromBytes(key, chain);
  		DeterministicKey childKey = HDKey.deriveChildKey(mPubKey, index);
  		byte[] childpublickey = childKey.getPubKey();
  		//Select network parameters
  		NetworkParameters params = null;
        if (mpTestnet.get(pairingID)==false){
        	params = MainNetParams.get();
        } 
        else {
        	params = TestNet3Params.get();
        }
		ECKey childPubKey = new ECKey(null, childpublickey);
		//Create a new key pair which will kept in the wallet.
		ECKey walletKey = new ECKey();
		byte[] privkey = walletKey.getPrivKeyBytes();
		List<ECKey> keys = ImmutableList.of(childPubKey, walletKey);
		//Create a 2-of-2 multisig output script.
		byte[] scriptpubkey = Script.createMultiSigOutputScript(2,keys);
		Script script = ScriptBuilder.createP2SHOutputScript(Utils.sha256hash160(scriptpubkey));
		//Create the address
		Address multisigaddr = Address.fromP2SHScript(params, script);
		//Save keys to file
		file.writeToFile(pairingID,bytesToHex(privkey),multisigaddr.toString());
		String ret = multisigaddr.toString();
		Authenticator.getWallet().addP2ShAddressToWathc(ret);
		return ret;
	}
	
	/**
	 * Gets the unspent outputs JSON for the wallet from blockr.io. Returns enough unspent outputs to 
	 * cover the total transaction output. There is a problem here with the way blockr handles unspent outputs. 
	 * It only shows unspent outputs for confirmed txs. For unconfirmed you can only get all transactions. 
	 * So if all unconfirmed outputs are unspent, it will work correctly, but if you spent an unconfirmed output, 
	 * and try to make another transaction before it confirms, you could get an error. 
	 */
	ArrayList<UnspentOutput> getUnspentOutputs(String pairingID, long outAmount) throws JSONException, IOException{
		mpNumInputs.put(pairingID, 0);
		mpChildkeyindex.put(pairingID, new ArrayList<Integer>());
		ArrayList<UnspentOutput> outList = new ArrayList<UnspentOutput>();
		mpPublickeys.put(pairingID, new ArrayList<byte[]>());
		WalletFile file = new WalletFile();
		ArrayList<String> addrs = file.getAddresses(pairingID);
		long inAmount = 0;
		//First add the confirmed unspent outputs
		for (int i=0; i<addrs.size(); i++){
			if (inAmount < (outAmount + 10000)){
				JSONObject json;
				UnspentOutput out = null;
				if (mpTestnet.get(pairingID)){json = readJsonFromUrl("http://tbtc.blockr.io/api/v1/address/unspent/" + addrs.get(i));}
				else {json = readJsonFromUrl("http://btc.blockr.io/api/v1/address/unspent/" + addrs.get(i));}
				JSONObject data = json.getJSONObject("data");
				JSONArray unspent = data.getJSONArray("unspent");
				if (unspent.length()!=0){
					for (int x=0; x<unspent.length(); x++){
						if (inAmount < outAmount + 10000){
							JSONObject txinfo = unspent.getJSONObject(x);
							double amount = Double.parseDouble((String) txinfo.get("amount"));
							out = new UnspentOutput(txinfo.get("tx").toString(), txinfo.get("n").toString(), (long)(amount*100000000));
							outList.add(out);
							inAmount = (long) (inAmount + (amount*100000000));
							//Add the public key and index for this address to the respective ArrayLists
							BigInteger privatekey = new BigInteger(1, hexStringToByteArray(file.getPrivKey(pairingID,addrs.get(i))));
							byte[] publickey = ECKey.publicKeyFromPrivate(privatekey, true);
							mpPublickeys.get(pairingID).add(publickey);
							mpChildkeyindex.get(pairingID).add((int) file.getAddrIndex(pairingID,addrs.get(i)));
							mpNumInputs.put(pairingID, mpNumInputs.get(pairingID) + 1);
						}
					}
				}
			}
		}		
		//If we still don't have enough outputs move on to adding the unconfirmed
		for (int j=0; j<addrs.size(); j++){
			if (inAmount < (outAmount + 10000)){
				JSONObject json;
				UnspentOutput out = null;
				if (mpTestnet.get(pairingID)){json = readJsonFromUrl("http://tbtc.blockr.io/api/v1/address/unconfirmed/" + addrs.get(j));}
				else {json = readJsonFromUrl("http://btc.blockr.io/api/v1/address/unconfirmed/" + addrs.get(j));}
				JSONObject data1 = json.getJSONObject("data");
				JSONArray unconfirmed = data1.getJSONArray("unconfirmed");
				if (unconfirmed.length()!=0){
					for (int x=0; x<unconfirmed.length(); x++){
						if (inAmount < outAmount + 10000){
							JSONObject tx = unconfirmed.getJSONObject(x);
							double amount = (double) tx.get("amount");
							out = new UnspentOutput(tx.get("tx").toString(), tx.get("n").toString(), (long)(amount*100000000));
							outList.add(out);
							inAmount = (long) (inAmount + amount*100000000);
							//Add the public key and index for this address to the respective ArrayLists
							BigInteger privatekey = new BigInteger(1, hexStringToByteArray(file.getPrivKey(pairingID,addrs.get(j))));
							byte[] publickey = ECKey.publicKeyFromPrivate(privatekey, true);
							mpPublickeys.get(pairingID).add(publickey);
							mpNumInputs.put(pairingID, mpNumInputs.get(pairingID) + 1);
							mpChildkeyindex.get(pairingID).add((int) file.getAddrIndex(pairingID,addrs.get(j)));
						}
					}
				}
			}
		}
		if (inAmount < outAmount + 10000) this.LOG.info("Insufficient funds");
		System.out.println(mpNumInputs.get(pairingID));
		System.out.println(inAmount);
		return outList;
	}
	
	/**Builds a raw unsigned transaction*/
	void mktx(String pairingID, ArrayList<String> MILLI, ArrayList<String> to) throws AddressFormatException, JSONException, IOException, NoSuchAlgorithmException {
		//Gather the data needed to construct the inputs and outputs
		long totalouts=0; 
		for (int i=0; i<MILLI.size(); i++){
			totalouts = totalouts + Long.parseLong(MILLI.get(i));
		}
		ArrayList<UnspentOutput> out = getUnspentOutputs(pairingID,totalouts); 
		//Set the network parameters
		NetworkParameters params = null;
        if (mpTestnet.get(pairingID)==false){
        	params = MainNetParams.get();
        } 
        else {
        	params = TestNet3Params.get();
        }
  		spendtx = new Transaction(params);
  		byte[] script = hexStringToByteArray("");
  		//Creates the inputs which reference a previous unspent output
  		long totalins = 0;
  		for (int x=0; x<out.size(); x++){
  			totalins = totalins + out.get(x).getAmount();
  			int index = Integer.parseInt(out.get(x).getIndex());
  	  		Sha256Hash txhash = new Sha256Hash(out.get(x).getTxid());
  			TransactionOutPoint outpoint = new TransactionOutPoint(params, index, txhash);
  			TransactionInput input = new TransactionInput(params, null, script, outpoint);
  			//Add the inputs
  			spendtx.addInput(input);
		}
		//Add the outputs
		for (int i=0; i<MILLI.size(); i++){
			Address outaddr = new Address(params, to.get(i));
			spendtx.addOutput(BigInteger.valueOf(Long.parseLong(MILLI.get(i))), outaddr);
		}
		//Add the change
		if (totalins > (totalouts + 10000)){
			Long changetotal = (totalins - (totalouts+10000));
			String changeaddr = genAddress(pairingID);
			Address change = new Address(params, changeaddr);
			spendtx.addOutput(BigInteger.valueOf(changetotal), change);
		}
		//Convert tx to byte array for sending.
		final StringBuilder sb = new StringBuilder();
		Formatter formatter = new Formatter(sb);
		try {
		    ByteArrayOutputStream os = new ByteArrayOutputStream();
		    spendtx.bitcoinSerialize(os);
		    byte[] bytes = os.toByteArray();
		    for (byte b : bytes) {
		        formatter.format("%02x", b);  
		    }
		    this.LOG.info("Raw Unsigned Transaction: " + sb.toString());
		    unsignedTx = sb.toString();
		}catch (IOException e) {
			this.LOG.info("Couldn't serialize to hex string.");
		} finally {
		    formatter.close();
		}
	}
	
	/**Returns the balance of the addresses in the wallet using blockr api*/
	public long getBalance(String pairingID, ArrayList<String> addresses) throws JSONException, IOException{
		WalletFile file = new WalletFile();
		JSONObject json;
		JSONArray data;
		double addrbalance;
		long unconfirmedbalance = 0;
		String addr = "";
		if (file.getKeyNum(pairingID)!=0){
			//Get confirmed Balance
			long balance = 0;
			int num = 0;
			int count = addresses.size();
			for (int a=0; a<(addresses.size()/19)+1; a++){	
				addr = "";
				if (((count/19)+1)>1){
					for (int i=num; i<num+19; i++){
						addr = addr + addresses.get(i) + ",";
					}
				num=num+19;
				count=count-19;
				}
				else {
					for (int i=num; i<num+count; i++){
						addr = addr + addresses.get(i) + ",";
					}
				}
				if (mpTestnet.get(pairingID))
				{
					json = readJsonFromUrl("http://tbtc.blockr.io/api/v1/address/balance/" + addr);
				}
				else 
				{
					json = readJsonFromUrl("http://btc.blockr.io/api/v1/address/balance/" + addr);
				}
				data = json.getJSONArray("data");
				addrbalance=0;
				for (int i=0; i<data.length(); i++){
					JSONObject info = data.getJSONObject(i);
					addrbalance = (double) info.getDouble("balance");
					balance = (long) (balance + (addrbalance)*100000000);
				}
			}
			return balance;
			}
		else {
			return 0;
		}
	}
    
	//#####################################
	//
	//		Simple db DAL
	//
	//#####################################
	
	public ArrayList<PairingObject> getAllPairingObjectArray()
	{
		WalletFile f = new WalletFile();
		return f.getPairingObjectsArray();
	}
	
	//#####################################
	//
	//		Helper functions
	//
	//#####################################
	
	/**For reading the JSON*/
	private static String readAll(Reader rd) throws IOException {
	    StringBuilder sb = new StringBuilder();
	    int cp;
	    while ((cp = rd.read()) != -1) {
	      sb.append((char) cp);
	    }
	    return sb.toString();
	  }

	/**Reads JSON object from a URL*/
	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException { 
	    URL urladdr = new URL(url);
        URLConnection conn = urladdr.openConnection();
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10.4; en-US; rv:1.9.2.2) Gecko/20100316 Firefox/3.6.2");
        BufferedReader rd = null;
	    try {
	      rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	      String jsonText = readAll(rd);
	      JSONObject json = new JSONObject(jsonText);
	      return json;
	    } finally {
	      rd.close();
	    }
	  }

	/**Hex encodes a DeterministicKey object*/
	private static String hexEncodePub(DeterministicKey pubKey) {
        return hexEncode(pubKey.getPubKey());
    }
    private static String hexEncode(byte[] bytes) {
        return new String(Hex.encode(bytes));
    }
    
    /**Converts a byte array to a hex string*/
    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	/**Converts a hex string to a byte array*/
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}
    
	/**Defines and object of data that makes up an unspent output*/
    class UnspentOutput {
    	String txid;
    	String index;
    	long amount;
    	
    	public UnspentOutput(String id, String in, long amt){
    		this.txid = id;
    		this.index = in;
    		this.amount = amt;
    	}
    	
    	public String getTxid(){
    		return txid;
    	}
    	
    	public String getIndex(){
    		return index;
    	}
    	
    	public long getAmount(){
    		return amount;
    	}
    }
    
}


