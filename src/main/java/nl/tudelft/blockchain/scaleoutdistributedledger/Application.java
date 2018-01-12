package nl.tudelft.blockchain.scaleoutdistributedledger;

import lombok.Getter;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Ed25519Key;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Node;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.Transaction;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketClient;
import nl.tudelft.blockchain.scaleoutdistributedledger.sockets.SocketServer;

import java.io.IOException;
import nl.tudelft.blockchain.scaleoutdistributedledger.mocks.TendermintChainMock;

import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.MainChain;
import nl.tudelft.blockchain.scaleoutdistributedledger.model.mainchain.tendermint.TendermintChain;

/**
 * Class to run a node.
 */
public class Application {
	public static final int TRACKER_SERVER_PORT = 3000;
	public static final String TRACKER_SERVER_ADDRESS = "localhost";
	public static final int NODE_PORT = 8007;
	private static MainChain mainChain;
	
	// Check whether we are in testing or production environment (default: production)
	public final boolean isProduction;
	
	@Getter
	private LocalStore localStore;

	@Getter
	private Thread serverThread;
	
	@Getter
	private SocketClient socketClient;

	/**
	 * Creates a new application.
	 * @param tendermintPort - the port on which the Tendermint server will run.
	 * @throws IOException - error while registering nodes
	 */
	public Application(int tendermintPort) throws IOException {
		this(tendermintPort, true);
	}

	/**
	 * Creates a new application.
	 * @param tendermintPort - the port on which the Tendermint server will run.
	 * @param isProduction - is production environment or not
	 * @throws IOException - error while registering nodes
	 */
	public Application(int tendermintPort, boolean isProduction) throws IOException {
		this.isProduction = isProduction;
		this.setupNode(tendermintPort);
	}
	
	/**
	 * Send a transaction to the receiver of the transaction.
	 * An abstract of the block containing the transaction (or a block after it) must already be
	 * committed to the main chain.
	 * @param transaction - the transaction to send
	 * @throws InterruptedException - when sending is interrupted
	 */
	public void sendTransaction(Transaction transaction) throws InterruptedException {
		CommunicationHelper.sendTransaction(transaction, socketClient);
	}

	/**
	 * Setup your own node.
	 * Register to the tracker and setup the local store.
	 * @param tmPort - the port on which to run the Tendermint server
	 * @throws java.io.IOException - error while registering node
	 */
	private void setupNode(int tmPort) throws IOException {
		// Create and register node
		Ed25519Key key = new Ed25519Key();
		Node ownNode = TrackerHelper.registerNode(key.getPublicKey());
		ownNode.setPrivateKey(key.getPrivateKey());

		// Setup local store
		this.localStore = new LocalStore(ownNode);

		this.serverThread = new Thread(new SocketServer(NODE_PORT, this.localStore));
		this.serverThread.start();
		this.socketClient = new SocketClient();
		
		// Setup Tendermint
		if (this.isProduction) {
			// Production environment
			mainChain = new TendermintChain(tmPort);
		} else {
			// Testing environment - mock external resources
			mainChain = new TendermintChainMock();
		}
	}

	/**
	 * Returns the singleton main chain.
	 * @return -
	 */
	public static MainChain getMainChain() {
		return mainChain;
	}
}
