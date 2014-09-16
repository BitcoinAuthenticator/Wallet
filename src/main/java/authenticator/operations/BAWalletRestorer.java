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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Platform;

import org.json.JSONException;

import authenticator.Authenticator;
import authenticator.BAApplicationParameters;
import authenticator.BAApplicationParameters.NetworkType;
import authenticator.BASE;
import authenticator.walletCore.exceptions.AddressNotWatchedByWalletException;
import authenticator.hierarchy.BAHierarchy;
import authenticator.hierarchy.exceptions.KeyIndexOutOfRangeException;
import authenticator.hierarchy.exceptions.NoAccountCouldBeFoundException;
import authenticator.network.TrustedPeerNodes;
import authenticator.protobuf.AuthWalletHierarchy.HierarchyAddressTypes;
import authenticator.protobuf.ProtoConfig.ATAccount;
import authenticator.protobuf.ProtoConfig.ATAddress;

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
	WalletListener vWalletListener = new WalletListener();
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
    
    DownloadListener downloadListener = new DownloadListener(){
		@Override
		 protected void progress(double pct, int blocksSoFar, Date date) {
			if(pct < 100){
			 listener.onStatusChange("Downloading Block Chain ... ");
			 listener.onProgress(pct, blocksSoFar, date);
			}
			else
				listener.onStatusChange("Finishing ... ");
		 }
		
		 @Override
         protected void doneDownload() {
			 endLoop();
		 }
	};
    
    WalletRestoreListener listener;
        
    /**
     * Discovery vars
     */
    ConcurrentHashMap<ATAccount,List<ATAddress>> mapAccountAddresses;
    
    final int MINIMUM_BLOOM_DATA_LENGTH = 8;
    final long AUTHENTICATOR_CREATION_TIME = 1388534402; // Represents 1.1.2014 00:00:01
    
    long earliestKeyTime = AUTHENTICATOR_CREATION_TIME;
    String startTsmp;
    String endTsmp;
    
    /**
     *	Flags
     */
    boolean useTor = false;
    boolean usePreselectedAddresses = true;
    
	public BAWalletRestorer(Authenticator auth,WalletRestoreListener l) {      
		super (BAWalletRestorer.class);
		listener = l;
		vAuthenticator = auth;
		vWallet = vAuthenticator.getWalletOperation().getTrackedWallet();
		vBAApplicationParameters = vAuthenticator.getApplicationParams();
		mainThread = new Thread(){
			@Override
			public void run() {
				directory = new File(auth.getApplicationParams().getApplicationDataFolderAbsolutePath());

				netParams = vAuthenticator.getWalletOperation().getNetworkParams();
		        
		        if(usePreselectedAddresses)
		        {
		        	if(auth.getApplicationParams().getBitcoinNetworkType() != NetworkType.TEST_NET) {
						peerAddresses = TrustedPeerNodes.MAIN_NET();
					}
		        }
		        try {
		        	startTsmp = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(Calendar.getInstance().getTime());    
		        	listener.onStatusChange("Initializing ... ");
	        		// a number that should cover a lookahead of a getdata request, should be about 500 blocks
	        		initWatchedAddresses(BAHierarchy.keyLookAhead * 15); 
	        		init();
	        		notifyStarted();
	        		initBlockChainDownload(downloadListener);
		        	endTsmp = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss").format(Calendar.getInstance().getTime());
		        	
		        	//this.toString());
		        				
		        	System.out.println(toString());
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
	
	private boolean didGetNewTx = false;
	/**
	 * A fix because of some weird behavior, the download listener indicates the download is
	 * complete but we still get Tx handling after that. This loop verifies there are no handled Tx for 
	 * at least 30 second and then fires the on done event
	 */
	private void endLoop(){
		new Thread(){
			@Override
            public void run() {
				try {
					while (true){
						 didGetNewTx = false;
						 Thread.sleep(60000);
						 						 
						 if(!didGetNewTx){
							listener.onStatusChange("Finished !");
							disposeOfRestorer(true);	
							listener.onDiscoveryDone();
							break;
						 }				 
					 }
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
	
	private void init() throws IOException{
		if (!directory.exists()) {
            if (!directory.mkdir()) {
                throw new IOException("Could not create named directory.");
            }
        }
		try {
			/**
			 * Delete files so we start from zero every round
			 */
			File chainFile = new File(directory, vAuthenticator.getApplicationParams().getAppName() + ".spvchain");
			chainFile.delete();
			vStore = new SPVBlockStore(netParams, new File(directory, vAuthenticator.getApplicationParams().getAppName() + ".spvchain"));
			
			if(netParams == MainNetParams.get()) 
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
	        
	        vWalletFile = new File(directory + vBAApplicationParameters.getAppName() + ".wallet");
	        if(!vWalletFile.exists())
	        	vWallet.saveToFile(vWalletFile);
	    	        
	        vChain.addWallet(vWallet);
            vPeerGroup.addWallet(vWallet);
            vPeerGroup.setMaxConnections(11);
            vWallet.addEventListener(vWalletListener);
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
		vAuthenticator.getWalletOperation().setHierarchyKeyLookAhead(lookaheadForEachAccount);
		mapAccountAddresses = new ConcurrentHashMap<ATAccount,List<ATAddress>>();//new HashMap<ATAccount,List<ATAddress>>();
		List<ATAccount> all = vAuthenticator.getWalletOperation().getAllAccounts();
		//List<String> addressesToAdd = new ArrayList<String>();
		for(ATAccount acc:all){
			List<ATAddress> arr = new ArrayList<ATAddress>();
			for(int i=0; i< lookaheadForEachAccount; i++){
				ATAddress add = vAuthenticator.getWalletOperation().getNextExternalAddress(acc.getIndex());
				arr.add(add);
				//vAuthenticator.getWalletOperation().addAddressToWatch(add.getAddressStr()); 
				//addressesToAdd.add(add.getAddressStr());
			}
			mapAccountAddresses.put(acc, arr);
		}
		
		//vAuthenticator.getWalletOperation().addAddressesToWatch(addressesToAdd);
	}
	
	@SuppressWarnings("unused")
	private void initBlockChainDownload(DownloadListener listener) throws IOException, InterruptedException, TimeoutException{
		this.listener.onStatusChange("Discovering ... ");
        vPeerGroup.startBlockChainDownload(listener);
        listener.await();
	}
	
	public void setCheckpoints(InputStream checkpoints) {
        this.checkpoints = checkNotNull(checkpoints);
    }
	
	PeerGroup createPeerGroup() throws TimeoutException {
        if (useTor) {
            return PeerGroup.newWithTor(netParams, vChain, new TorClient());
        }
        else
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
		private synchronized void handler(Wallet wallet, Transaction tx) throws Exception{
        	didGetNewTx = true;
        	LOG.info("Received Tx: " + tx.toString());
        	for(TransactionOutput out:tx.getOutputs()){
        		Script scr = out.getScriptPubKey();
    			Address addr = scr.getToAddress(netParams);
    			if(wallet.isAddressWatched(addr)){
    				Set<ATAccount> keys = new HashSet<ATAccount>(mapAccountAddresses.keySet());
    				for(ATAccount acc:keys){
    					List<ATAddress> addresses = new ArrayList<ATAddress>(mapAccountAddresses.get(acc));
    					for(ATAddress add:addresses){
    						if(addr.toString().equals(add.getAddressStr())){
    							vAuthenticator.getWalletOperation().markAddressAsUsed(acc.getIndex(), add.getKeyIndex(), HierarchyAddressTypes.External);
    							ATAddress newAdd = vAuthenticator.getWalletOperation().getNextExternalAddress(acc.getIndex());
    							mapAccountAddresses.get(acc).add(newAdd);
    							vAuthenticator.getWalletOperation().addAddressToWatch(newAdd.getAddressStr()); 
    							LOG.info("Address " + add.getAddressStr() + " found used. " + 
    									"Added a new address " + newAdd.getAddressStr());
    						}
    					}
    				}
    			}
        	}
        
        	listener.onTxFound(tx, 
        			tx.getValueSentToMe(vWallet), 
        			tx.getValueSentFromMe(vWallet));
        }
	}
	
	public interface WalletRestoreListener{
		public void onProgress(double pct, int blocksSoFar, Date date);
		public void onTxFound(Transaction Tx, Coin received, Coin sent);
		public void onStatusChange(String newStatus);
		public void onDiscoveryDone();
	}
	
	@Override
    protected void doStart() {    	
    	mainThread.start();
    }
    
    @Override
	protected void doStop() {
    	listener.onStatusChange("Aborting ... ");
    	disposeOfRestorer(false);
    }
    
    private void disposeOfRestorer(boolean blocking){
    	
    	try {
    		vPeerGroup.stopAsync();
	    	vWallet.saveToFile(vWalletFile);
	        vStore.close();

	        vPeerGroup.removeWallet(vWallet);
	        vPeerGroup.removeEventListener(downloadListener);
	        vChain.removeWallet(vWallet);
	        vWallet.removeEventListener(vWalletListener);
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
}
