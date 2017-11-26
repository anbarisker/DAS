
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;
import java.util.*;

@SuppressWarnings("serial")
public class Node extends UnicastRemoteObject implements ElectionNode {

	// Range for the period to check if heard from leader, [1,5]
	private static final int min = 1;
	private static final int max = 2;



	private static final int delay = 5000;
	private static final int leader_delay = 5000;
	private static final int send_msg_delay = 10000;
	//private final int silencePeriod = (min + (int)(Math.random() * ((max - min) + 1))) * 1000;
	private final int silencePeriod = 30000;
	private int messagePeriod = 0;

	private String host;

	private String name;
	private String leaderName = "";

	private int ignoreElection = 0;
	private boolean heardFromLeader = false;
	private boolean noLeaderFound = true;
	private boolean leaderexits = false;
	private String new_leadername ="";

	//hydro
	private static final float min_W_lvl = 15.0f;
	private static final float max_W_lvl = 30.0f;
	Random rand = new Random();
	private float water_temperature = rand.nextFloat()*(max_W_lvl - min_W_lvl + 1) + min;
	LinkedHashMap<String,Float> All_Water_Temperature = new LinkedHashMap<String,Float>();


	@SuppressWarnings("unused")
	private Node() throws RemoteException {super();}

	public Node(String nameIn, String hostIn) throws RemoteException {
		super();

		this.name = nameIn;
		this.host = hostIn;

		// Make sure that messages are getting sent more frequently then
		// the node checks for silence
		while (messagePeriod < silencePeriod) {
			messagePeriod = (min + (int) (Math.random() * ((max - min) + 1))) * 30000;
			System.out.println("MP : "+messagePeriod);
			System.out.println("SP : "+silencePeriod);

		}

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {

				//temp

				if (!name.equals(leaderName) &&
						!heardFromLeader && !getLeaderExits()) {

					try {
						System.out.println("Calling election...");
						Registry reg = LocateRegistry.getRegistry(host);
						//Registry reg = LocateRegistry.getRegistry();
						for (String nodeName : reg.list()) {
							try {
								if (!nodeName.equals(name) && nodeName.compareTo(name) > 0) {
									ElectionNode otherNode = (ElectionNode) reg.lookup(nodeName);
									String response = otherNode.startElection(name);

									if (response.length() > 0) {
										noLeaderFound = false;
										System.out.println("res "+response);
										break;
									}
								}
							} catch (DeadNodeException e) {
								System.out.println(e.toString());
							} catch (NotBoundException e) {
								try {
									System.out.println("Node Error: fml8" + nodeName + " unbound.");
									reg.unbind(nodeName);
								} catch (NotBoundException er) {
									// Shouldn't happen
								}
							} catch (ConnectException e) {
								try {
									System.out.println("Node Error: fml9" + nodeName + " unbound.");
									reg.unbind(nodeName);
								} catch (NotBoundException er) {
									// Shouldn't happen
								}
							}
						}

						if (noLeaderFound) {
							try {
								System.out.println("No leader found, electing myself.");
								startElection(name);
								noLeaderFound = false;
							} catch (DeadNodeException e) {
								System.out.println("Node Error: FML 1" + e.toString());
							}

						}
					} catch (RemoteException e) {
						System.out.println("Node Error: FML 2" + e.toString());
						e.printStackTrace();
					}
				}
			else if (heardFromLeader)
					heardFromLeader = false;
			}
		}, leader_delay, silencePeriod);

		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (leaderName != null && !name.equals(leaderName)) {
					sendLeaderMsg("Data from " + name +" Water "+ water_temperature, name, water_temperature);
				}
			}
		}, send_msg_delay, messagePeriod);

		System.out.println(name + " ready.");
	}

	private void sendLeaderMsg(String msg, String node_name, float water_data) {
		try {
			Registry reg = LocateRegistry.getRegistry(host);
			//Registry reg = LocateRegistry.getRegistry();
			leaderName = getLeaderName();
			try {
				ElectionNode leaderNode = (ElectionNode) reg.lookup(leaderName);

				String response = leaderNode.recvMsg(name, msg, water_data);
				System.out.println(leaderName + ": " + response);

				if (!heardFromLeader)
					heardFromLeader = true;
			} catch (NotBoundException e) {
				try {
					System.out.println("Node Error: fml 10" + leaderName + " unbound.");
					reg.unbind(leaderName);
				} catch (NotBoundException er) {
					// Shouldn't happen
				}
			} catch (ConnectException e) {
				try {
					System.out.println("Node Error: fml 11" + leaderName + " unbound.");
					reg.unbind(leaderName);
				} catch (NotBoundException er) {
					// Shouldn't happen
				}
			}
		} catch (RemoteException e) {
			System.out.println("Node Error: FML 12" + e.toString());
			e.printStackTrace();
		}
	}




	/**
	 * Starts the election. If the node has an ignore election counter it will
	 * decrement the counter and throws a DeadNodeException.
	 * If the sender's name is lexicographically greater than or equal to
	 * this node's it will declare it the new leader. Otherwise, it will start
	 * a new election with itself as the candidate.
	 */
	@Override
	public String startElection(String senderName) throws DeadNodeException {
		String ret = "";
		/*
		if (ignoreElection > 0) {
			ignoreElection--;
			System.out.println(ignoreElection + " more elections being ignored.");
			throw new DeadNodeException(name + " is dead.");
		}*/
		if(getLeaderExits())
		{
			System.out.println(" Leader exits.");
			noLeaderFound = false;

		}
		 else if(getLeaderExits() == false){
			System.out.println("Election started.");

			try {
				Registry reg = LocateRegistry.getRegistry(host);
				//Registry reg = LocateRegistry.getRegistry();

				ret = "Leader accepted.";
				System.out.println(ret);
				leaderName = name;
				for (String nodeName : reg.list()) {
					if (!nodeName.equals(name)) {
						try {
							ElectionNode node = (ElectionNode) reg.lookup(nodeName);
							node.newLeader(name);
						} catch (NotBoundException e) {
							try {
								System.out.println("Node Error: FML1" + nodeName + " unbound.");
								reg.unbind(nodeName);
							} catch (NotBoundException er) {
								// Shouldn't happen
							}
						} catch (ConnectException e) {
							try {
								System.out.println("Node Error: FML2" + nodeName + " unbound.");
								reg.unbind(nodeName);
							} catch (NotBoundException er) {
								// Shouldn't happen
							}
						}
					}
				}
				setLeaderExits(true);
				setLeaderName(getLeaderName());
			} catch (RemoteException e) {
				System.out.println("Node Error: FML3" + e.toString());
				e.printStackTrace();
			}
		}

		return ret;
	}

	@Override
	public void newLeader(String newLeaderName) {
		leaderName = newLeaderName;
		System.out.println(newLeaderName + " is the new leader.");
	}

	/**
	 * The current leader receives messages through this method and returns
	 * a message back to the sender in order to let the sender know
	 * the leader is still there and active.
	 */
	@Override
	public String recvMsg(String senderName, String msg, float water_temperature) {
		String ret = "Not the leader.";

		if (leaderName.equals(name)) {
			System.out.println(senderName + ": " + msg);
			All_Water_Temperature.put(senderName, water_temperature);
			ret = "Message received.";
		}

		return ret;
	}


	/**
	 * Allows the ElectionDriver to change the name of the node and rebind it
	 * to the registry under the new name
	 */

	@Override
	public void makeChaos(String newName, int ignore) {
		if (!name.equals(leaderName)) {
			try {
				Registry reg = LocateRegistry.getRegistry(host);
		//Registry reg = LocateRegistry.getRegistry();
				String oldName = name;
				name = newName;
				ignoreElection = ignore;

				reg.unbind(oldName);
				reg.bind(name, this);

				System.out.println("New node name: " + newName);
				System.out.println("Ignoring " + ignore + " elections...");
			} catch(RemoteException e) {
				System.out.println("Node Error: fml4" + e.toString());
				e.printStackTrace();
			} catch (NotBoundException e) {
				System.out.println("Node Error: fml5" + e.toString());
				e.printStackTrace();
			} catch (AlreadyBoundException e) {
				System.out.println("Node Error: fml6" + e.toString());
				e.printStackTrace();
			}
		}
	}

	// i added this new things
	@Override
	public String getNode_name()
	{
		return name;
	}
	@Override
	public boolean getLeaderExits()
	{
		return leaderexits;
	}
	@Override
	public void setLeaderExits(boolean leader)
	{
		leaderexits = leader;
	}
	@Override
	public void setLeaderName(String leader_name)
	{
		new_leadername = leader_name;
	}
	@Override
	public String getLeaderName()
	{
		return new_leadername;
	}


	public static void main(String[] args) {
		String name = (args.length < 1 || args[0].equals("!")) ?
				"Node-" + System.currentTimeMillis() : args[0];
				String host = (args.length < 2) ? null : args[1];
				try {
					Node node = new Node(name, host);
					//Node node = new Node(name, LocateRegistry.getRegistry().toString());

					// Bind stub to the registry
					Registry reg = LocateRegistry.getRegistry(host);
							//Registry reg = LocateRegistry.getRegistry();
					reg.bind(name, node);
				} catch (Exception e) {
					System.out.println("Node Error: fml7" + e.toString());
					e.printStackTrace();
				}
	}

}
