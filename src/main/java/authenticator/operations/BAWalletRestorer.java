package authenticator.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONException;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BASE;
import authenticator.helpers.exceptions.AddressNotWatchedByWalletException;
import authenticator.hierarchy.BAHierarchy;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig.ATAddress;
import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ATAccount;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.BlockChainListener;
import com.google.bitcoin.core.BloomFilter;
import com.google.bitcoin.core.CheckpointManager;
import com.google.bitcoin.core.Coin;
import com.google.bitcoin.core.DownloadListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Peer;
import com.google.bitcoin.core.PeerAddress;
import com.google.bitcoin.core.PeerFilterProvider;
import com.google.bitcoin.core.PeerGroup;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.StoredBlock;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.TransactionInput;
import com.google.bitcoin.core.TransactionOutPoint;
import com.google.bitcoin.core.TransactionOutput;
import com.google.bitcoin.core.VerificationException;
import com.google.bitcoin.core.AbstractBlockChain.NewBlockType;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.script.Script;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.utils.Threading;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.Service.State;
import com.subgraph.orchid.TorClient;

public class BAWalletRestorer extends BASE{
	Thread mainThread;
	//
	Authenticator vAuthenticator;
	Wallet vWallet;
	File vWalletFile;
	NetworkParameters netParams;
	BAApplicationParameters vBAApplicationParameters;
    BlockChain vChain;
    SPVBlockStore vStore;
    PeerGroup vPeerGroup;
    InputStream checkpoints;
    PeerAddress[] peerAddresses;
    String userAgent, version;
    File directory;
    //FilterProvider vFilterProvider;
        
    /**
     * Discovery vars
     */
    //List<ECKey> watchedKeys;
    //List<ECKey> lastBatchOfWatchedKeys;
    Map<ATAccount,List<ATAddress>> mapAccountAddresses;
    
    final int MINIMUM_BLOOM_DATA_LENGTH = 8;
    final long AUTHENTICATOR_CREATION_TIME = 1388534402; // Represents 1.1.2014 00:00:01
    
    long earliestKeyTime = AUTHENTICATOR_CREATION_TIME;
    String startTsmp;
    String endTsmp;
    
    
    /**
     *	Flags
     */
    boolean useTor = false;
    boolean usePreselectedAddresses = false;
    
    @Override
    protected void doStart() {    	
    	mainThread.start();
    }
    
    @Override
	protected void doStop() {
    	vPeerGroup.stopAsync();
        vPeerGroup.addListener(new Service.Listener(){
        	@Override public void terminated(State from) {
        		try {
                    vWallet.saveToFile(vWalletFile);
                    vStore.close();

                    vPeerGroup = null;
                    vWallet = null;
                    vStore = null;
                    vChain = null;
                    
                    notifyStopped();
                }catch (IOException | BlockStoreException e) {
        			e.printStackTrace();
        			notifyStopped();
        		}
        	}
        }, MoreExecutors.sameThreadExecutor());
    }
    
	public BAWalletRestorer(Authenticator auth,DownloadListener listener) {      
		super (BAWalletRestorer.class);
		vAuthenticator = auth;
		vWallet = vAuthenticator.getWalletOperation().getTrackedWallet();
		vBAApplicationParameters = vAuthenticator.getApplicationParams();
		mainThread = new Thread(){
			@Override
			public void run() {
				directory = new File(".");
		        //watchedKeys = new ArrayList<ECKey>();
		        
		        //params = TestNet3Params.get();
				netParams = vAuthenticator.getWalletOperation().getNetworkParams();//MainNetParams.get();
		        
		        if(usePreselectedAddresses)
		        {
		        	try {
		        		peerAddresses = new PeerAddress[]{
		                		new PeerAddress(InetAddress.getByName("riker.plan99.net")),
		                		// IPV6
		                		new PeerAddress(InetAddress.getByName("InductiveSoul.US")),
		                		new PeerAddress(InetAddress.getByName("caffeinator.net")),
		                		new PeerAddress(InetAddress.getByName("messier.bzfx.net")),
		                		// IPV4
		                		new PeerAddress(InetAddress.getByName("bitcoin.coinprism.com")),
		                		new PeerAddress(InetAddress.getByName("btcnode1.evolyn.net")),
		                		new PeerAddress(InetAddress.getByName("InductiveSoul.US")),
		                	};
					} catch (UnknownHostException e) {
						e.printStackTrace();
					}
		        }
		        try {
		        	startTsmp = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(Calendar.getInstance().getTime());        	
	        		init();
	        		initWatchedAddresses(BAHierarchy.keyLookAhead);
	        		notifyStarted();
	        		initBlockChainDownload(listener);
		        	endTsmp = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(Calendar.getInstance().getTime());
		        	
		        	System.out.println(this.toString());
		        				
				} catch (Exception e) { e.printStackTrace(); }
			}
		};
	}
	
	@Override
	public String toString(){
		return "Started at: "		   		 + startTsmp 														+ "\n" +
			   "Ended at: " 		   		 + endTsmp   														+ "\n" +
     		   "Total elements in filter " 	 + vWallet.getWatchedAddresses().size()*2 							+ "\n" +
     		   "Fast catchup up until "		 + new java.util.Date((long)earliestKeyTime*1000).toLocaleString() 	+ "\n";
	}
	
	private void init() throws IOException{
		if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Could not create named directory.");
            }
        }
		try {
			/**
			 * Delete chain file so we start from zero every round
			 */
			File chainFile = new File(directory, vAuthenticator.getApplicationParams().getAppName() + ".spvchain");
			chainFile.delete();
			vStore = new SPVBlockStore(netParams, new File(directory, vAuthenticator.getApplicationParams().getAppName() + ".spvchain"));
			
			setCheckpoints(getClass().getResourceAsStream("/wallettemplate/checkpoints"));
			if (checkpoints != null) {
	            // Ugly hack! We have to create the wallet once here to learn the earliest key time, and then throw it
	            // away. The reason is that wallet extensions might need access to peergroups/chains/etc so we have to
	            // create the wallet later, but we need to know the time early here before we create the BlockChain
	            // object.
	            CheckpointManager.checkpoint(netParams, checkpoints, vStore, earliestKeyTime);
	        }
			
	        vChain = new BlockChain(netParams, vStore);
	        //vChain.addListener(new ChainListener(watchedKeys));
	        vPeerGroup = createPeerGroup();
	        if (userAgent != null)
	            vPeerGroup.setUserAgent(userAgent, version);
	        else
	        	vPeerGroup.setUserAgent(vBAApplicationParameters.getAppName(), "1.0");
	        	        
	        if (peerAddresses != null) {
                for (PeerAddress addr : peerAddresses) vPeerGroup.addAddress(addr);
                vPeerGroup.setMaxConnections(peerAddresses.length);
                peerAddresses = null;
            } else {
                vPeerGroup.addPeerDiscovery(new DnsDiscovery(netParams));
            }
	        
	        vWalletFile = new File(directory, vBAApplicationParameters.getAppName() + ".wallet");
	        if(!vWalletFile.exists())
	        	vWallet.saveToFile(vWalletFile);
	    	        
	        vChain.addWallet(vWallet);
            vPeerGroup.addWallet(vWallet);
            vPeerGroup.setMaxConnections(11);
            vWallet.addEventListener(new WalletListener());
	        if (true){//blockingStartup) {
                vPeerGroup.startAsync();
                vPeerGroup.awaitRunning();
            }
		} catch (BlockStoreException e) {
            throw new IOException(e);
        } catch (TimeoutException e) { e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("static-access")
	private void initWatchedAddresses(int lookaheadForEachAccount) throws Exception{
		mapAccountAddresses = new HashMap<ATAccount,List<ATAddress>>();
		List<ATAccount> all = vAuthenticator.getWalletOperation().getAllAccounts();
		for(ATAccount acc:all){
			List<ATAddress> arr = new ArrayList<ATAddress>();
			for(int i=0; i< lookaheadForEachAccount; i++){
				ATAddress add = vAuthenticator.getWalletOperation().getNextExternalAddress(acc.getIndex());
				arr.add(add);
				vAuthenticator.getWalletOperation().addAddressToWatch(add.getAddressStr()); 
			}
			mapAccountAddresses.put(acc, arr);
		}
	}
	
	@SuppressWarnings("unused")
	private void initBlockChainDownload(DownloadListener listener) throws IOException, InterruptedException, TimeoutException{
		System.out.println("Connected peers: " + vPeerGroup.getConnectedPeers().size());
        vPeerGroup.startBlockChainDownload(listener);
        listener.await();
	}
	
	public void setCheckpoints(InputStream checkpoints) {
        this.checkpoints = checkNotNull(checkpoints);
    }
	
	PeerGroup createPeerGroup() throws TimeoutException {
        /*if (useTor) {
            return PeerGroup.newWithTor(netParams, vChain, new TorClient());
        }
        else*/
            return new PeerGroup(netParams, vChain);
    }

	private class WalletListener extends AbstractWalletEventListener {
		@Override
        public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
			try {
				handler(wallet, tx);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
        	try {
				handler(wallet, tx);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        
        @SuppressWarnings("static-access")
		private void handler(Wallet wallet, Transaction tx) throws Exception{
        	LOG.debug("Some watched address appeared in a Tx");
        	for(TransactionOutput out:tx.getOutputs()){
        		Script scr = out.getScriptPubKey();
    			Address addr = scr.getToAddress(netParams);
    			if(wallet.isAddressWatched(addr))
    				for(ATAccount acc:mapAccountAddresses.keySet()){
    					for(ATAddress add:mapAccountAddresses.get(acc)){
    						if(addr.toString().equals(add.getAddressStr())){
    							vAuthenticator.getWalletOperation().markAddressAsUsed(acc.getIndex(), add.getKeyIndex(), HierarchyAddressTypes.External);
    							ATAddress newAdd = vAuthenticator.getWalletOperation().getNextExternalAddress(acc.getIndex());
    							mapAccountAddresses.get(acc).add(newAdd);
    							vAuthenticator.getWalletOperation().addAddressToWatch(newAdd.getAddressStr()); 
    							LOG.debug("Address " + add.getAddressStr() + " found used.\n" + 
    									"Added a new address " + newAdd.getAddressStr());
    						}
    					}
    				}
        	}
        }
	}
	
	/*public class FilterProvider implements PeerFilterProvider{
		ReentrantLock lock ;
		List<ECKey> keys;
		
		public FilterProvider(List<ECKey> keys){
			lock = Threading.lock("wallet");
			updateKeys(keys);
		}
		
		public void updateKeys(List<ECKey> keys){
			this.keys = new ArrayList<ECKey>(keys);
		}

		public long getEarliestKeyCreationTime() {
			return earliestKeyTime; //just a week ago   
		}

		public int getBloomFilterElementCount() {
			return keys.size() * 2; // for every key we enter its pubKey and pubKeyHash bytes
		}

		public BloomFilter getBloomFilter(int size, double falsePositiveRate, long nTweak) {
			BloomFilter filter = null;
			lock.lock();
	        try {	        	
	        	filter = new BloomFilter(size, falsePositiveRate, nTweak);

	        	
	        	for(ECKey k:keys){
	        		filter.insert(k.getPubKey());
	        		filter.insert(k.getPubKeyHash());
	        	}
	        	
	        }  finally {
	            lock.unlock();
	        }
			return filter;
		}

		public boolean isRequiringUpdateAllBloomFilter() {
			return false;
		}
		
		public Lock getLock() {
			return lock;
		}
		
	}*/
	
	/*private class ChainListener implements BlockChainListener{
		private List<Sha256Hash> relevantTx = new ArrayList<Sha256Hash>();
		List<ECKey> watchedKeys;
		List<byte[]> watchedPubKeys;
		List<byte[]> watchedPubKeyHashes;
		
		public ChainListener(List<ECKey> watchedKeys){
			this.watchedKeys = new ArrayList<ECKey>( watchedKeys );
			
			watchedPubKeys = new ArrayList<byte[]>();
			watchedPubKeyHashes = new ArrayList<byte[]>();
			for(ECKey k:watchedKeys){
				watchedPubKeys.add(k.getPubKey());
				watchedPubKeyHashes.add(k.getPubKeyHash());
			}
		}
		
		public void notifyNewBestBlock(StoredBlock block)
				throws VerificationException {
			// TODO Auto-generated method stub
			
		}

		public void reorganize(StoredBlock splitPoint,
				List<StoredBlock> oldBlocks, List<StoredBlock> newBlocks)
				throws VerificationException {
			// TODO Auto-generated method stub
			
		}

		@SuppressWarnings("unused")
		public boolean isTransactionRelevant(Transaction tx) {
			
			
			for(TransactionOutput out:tx.getOutputs())
			{
				try{
					byte[] hashOut = out.getScriptPubKey().getPubKeyHash();
					if(watchedPubKeyHashes.contains(hashOut)){
						relevantTx.add(tx.getHash());
						return true;
					}
				}
				catch(Exception e){
					e.printStackTrace();
					System.out.println("\n\n" + out.toString() + "\n\n");
					return false;
				}
			}
				
			
			
			for(TransactionInput in: tx.getInputs()){
				TransactionOutPoint putPoint = in.getOutpoint();
				Sha256Hash id  = putPoint.getHash();
				if(relevantTx.contains(id))
					return true;
				try{
					byte[] pubKeyIn = in.getScriptSig().getPubKey();
					if(watchedPubKeys.contains(pubKeyIn)){
						relevantTx.add(tx.getHash());
						return true;
					}
				}
				catch(Exception e){
					e.printStackTrace();
					System.out.println("\n\n" + in.toString() + "\n\n");
					return false;
				}
			}
			
			return false;
		}

		public void receiveFromBlock(Transaction tx, StoredBlock block,
				NewBlockType blockType, int relativityOffset)
				throws VerificationException {
			// TODO Auto-generated method stub
			
		}

		public boolean notifyTransactionIsInBlock(Sha256Hash txHash, StoredBlock block, NewBlockType blockType, int relativityOffset)
				throws VerificationException {
			if(relevantTx.contains(txHash))
				return true;
			return false;
		}
		
	}*/
}
