package authenticator.operations;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import authenticator.BASE;

import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.Block;
import com.google.bitcoin.core.BlockChain;
import com.google.bitcoin.core.BlockChainListener;
import com.google.bitcoin.core.BloomFilter;
import com.google.bitcoin.core.CheckpointManager;
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
import com.google.bitcoin.net.discovery.DnsDiscovery;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.store.BlockStoreException;
import com.google.bitcoin.store.SPVBlockStore;
import com.google.bitcoin.utils.Threading;
import com.subgraph.orchid.TorClient;

public class BAWalletRestorer extends BASE{
	Thread mainThread;
	//
	String filePrefix;
	NetworkParameters params;
    BlockChain vChain;
    SPVBlockStore vStore;
    PeerGroup vPeerGroup;
    InputStream checkpoints;
    PeerAddress[] peerAddresses;
    String userAgent, version;
    File directory;
    FilterProvider vFilterProvider;
        
    /**
     * Discovery vars
     */
    List<ECKey> watchedKeys;
    List<ECKey> lastBatchOfWatchedKeys;
    
    final int MINIMUM_BLOOM_DATA_LENGTH = 8;
    final long AUTHENTICATOR_CREATION_TIME = 1388534402; // Represents 1.1.2014 00:00:01
    
    int numberOfRounds = 1;
    int addedKeysInEveryRound = 100;
    long earliestKeyTime = AUTHENTICATOR_CREATION_TIME;
    String startTsmp;
    String endTsmp;
    
    
    
    /**
     *	Flags
     */
    boolean useTor = false;
    boolean usePreselectedAddresses = true;
    
    public void runRestorer(){
    	mainThread.start();
    }
    
	public BAWalletRestorer(DownloadListener listener) {      
		super (BAWalletRestorer.class);
		mainThread = new Thread(){
			@Override
			public void run() {
				directory = new File(".");
		        watchedKeys = new ArrayList<ECKey>();
		        
		        //params = TestNet3Params.get();
		        params = MainNetParams.get();
		        
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
		        
		        if(peerAddresses != null)
		        try {
		        	startTsmp = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(Calendar.getInstance().getTime());        	
		        	//initWatchedScripts();
		        	for(int i=0;i<1;i++){
		        		System.out.println("\nStarting round " + i + "\n");
		        		addMoreWatchedAddresses(addedKeysInEveryRound);
		        		init();
		        		initBlockChainDownload(listener);
		        		
		        		//if(didFindPositiveBalanceInLastBatch == false)
		        		//	break;
		        	}
		        	
		        	endTsmp = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(Calendar.getInstance().getTime());
		        	
		        	System.out.println(this.toString());
		        				
				} catch (IOException e) { e.printStackTrace();
				} catch (InterruptedException e) { e.printStackTrace();
				} catch (TimeoutException e) { e.printStackTrace();
				} catch (AddressFormatException e) { e.printStackTrace();
				}
			}
		};
	}
	
	@Override
	public String toString(){
		return "Started at: "		   		 + startTsmp 				+ "\n" +
			   "Ended at: " 		   		 + endTsmp   				+ "\n" +
     		   "Did " 				    	 + numberOfRounds 			+ " Rounds \n" +
     		   "Total elements in filter " 	 + watchedKeys.size()*2 	+ "\n" +
     		   "Fast catchup up until "		 + new java.util.Date((long)earliestKeyTime*1000).toLocaleString() + "\n";
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
			File chainFile = new File(directory, "RestoreWallet" + ".spvchain");
			chainFile.delete();
			vStore = new SPVBlockStore(params, new File(directory, "RestoreWallet" + ".spvchain"));
			
			setCheckpoints(getClass().getResourceAsStream("/wallettemplate/checkpoints"));
			if (checkpoints != null) {
	            // Ugly hack! We have to create the wallet once here to learn the earliest key time, and then throw it
	            // away. The reason is that wallet extensions might need access to peergroups/chains/etc so we have to
	            // create the wallet later, but we need to know the time early here before we create the BlockChain
	            // object.
	            CheckpointManager.checkpoint(params, checkpoints, vStore, earliestKeyTime);
	        }
			
	        vChain = new BlockChain(params, vStore);
	        vChain.addListener(new ChainListener(watchedKeys));
	        
	        vPeerGroup = createPeerGroup();
	        if (userAgent != null)
	            vPeerGroup.setUserAgent(userAgent, version);
	        

	        if (peerAddresses != null) {
                for (PeerAddress addr : peerAddresses) vPeerGroup.addAddress(addr);
                vPeerGroup.setMaxConnections(peerAddresses.length);
                peerAddresses = null;
            } else {
                vPeerGroup.addPeerDiscovery(new DnsDiscovery(params));
            }
	    	        
	        vFilterProvider = new FilterProvider(watchedKeys);
	        vPeerGroup.addPeerFilterProvider(vFilterProvider);
	        if (true){//blockingStartup) {
                vPeerGroup.startAsync();
                vPeerGroup.awaitRunning();
            }
		} catch (BlockStoreException e) {
            throw new IOException(e);
        } catch (TimeoutException e) { e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings("unused")
	private void initBlockChainDownload(DownloadListener listener) throws IOException, InterruptedException, TimeoutException{
		System.out.println("Connected peers: " + vPeerGroup.getConnectedPeers().size());
        vPeerGroup.startBlockChainDownload(listener);
        listener.await();
        
        //timer.cancel();
	}
	
	public void setCheckpoints(InputStream checkpoints) {
        this.checkpoints = checkNotNull(checkpoints);
    }
	
	PeerGroup createPeerGroup() throws TimeoutException {
        if (useTor) {
            return PeerGroup.newWithTor(params, vChain, new TorClient());
        }
        else
            return new PeerGroup(params, vChain);
    }
	
	private void initWatchedAddresses() throws AddressFormatException{
		//Address address = new Address(params,"1FJ9Tywe9btDTcvGoj7ccHDEkyZE4uVuWf");
    	//Script script = ScriptBuilder.createOutputScript(address);
        //script.setCreationTimeSeconds(AUTHENTICATOR_CREATION_TIME);
        //watchedScripts.add(script);
		//watchedAddresses.add(address);
		//System.out.println("Added init addresses");
	}
	
	private void addMoreWatchedAddresses(int howManyToAdd) throws AddressFormatException{
    	//List<Address> addresses = new ArrayList<Address>();
    	lastBatchOfWatchedKeys = new ArrayList<ECKey>();
    	for(int i=0; i< howManyToAdd;i++){
    		ECKey k = new ECKey();
			//Address add = k.toAddress(params);
			lastBatchOfWatchedKeys.add(k);
			watchedKeys.add(k);
    	}
    	
    	System.out.println("Added " + howManyToAdd +" random address");
	}

	
	public class FilterProvider implements PeerFilterProvider{
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
		
	}
	
	private class ChainListener implements BlockChainListener{
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
		
	}
}
