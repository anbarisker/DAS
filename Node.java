
import java.rmi.AlreadyBoundException;
import java.rmi.ConnectException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;
import java.text.DecimalFormat;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.*;

@SuppressWarnings("serial")
public class Node extends UnicastRemoteObject implements ElectionNode {
	// Range for the period to check if heard from leader, [1,5]
	private static final int min = 1;
	private static final int max = 2;

	private static final int delay = 5000;
	private static final int leader_delay = 5000;
	private static final int send_msg_delay = 10000;
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
	private static final double min_W_lvl = 15.0;
	private static final double max_W_lvl = 30.0;
	private static final double min_PH_lvl = 4.0;
	private static final double max_PH_lvl = 10.0;
	private static final double min_H_lvl = 35.0;
	private static final double max_H_lvl = 100.0;
	Random rand = new Random();
	private double sensor_data;
	DecimalFormat df = new DecimalFormat("#.#");
	private LinkedHashMap<String,ArrayList<Double>> All_Sensors_Data = new LinkedHashMap<String,ArrayList<Double>>();
	private int number_of_clients = 0;	

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
		}

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (!name.equals(leaderName) &&
						!heardFromLeader && !getLeaderExist()) {
					try {
						System.out.println("Calling election...");
						Registry reg = LocateRegistry.getRegistry(host);
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
									reg.unbind(nodeName);
								} catch (NotBoundException er) {
									// Shouldn't happen
								}
							} catch (ConnectException e) {
								try {
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
							} catch (DeadNodeException e) {}
						}
					} catch (RemoteException e) {
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
				if((System.currentTimeMillis()/1000) %10 == 0){
					if (leaderName != null && !name.equals(leaderName)) {		
						 ArrayList<Double> sensors_data = new  ArrayList<Double>();
						 //water
						 sensor_data = rand.nextDouble()*(max_W_lvl - min_W_lvl + 1) + min_W_lvl;
						 sensors_data.add(Double.valueOf(df.format(sensor_data)));
						//ph-lvl
						 sensor_data = rand.nextDouble()*(max_PH_lvl - min_PH_lvl + 1) + min_PH_lvl;
						 sensors_data.add(Double.valueOf(df.format(sensor_data)));
						//h-lvl
						 sensor_data = rand.nextDouble()*(max_H_lvl - min_H_lvl + 1) + min_H_lvl;
						 sensors_data.add(Double.valueOf(df.format(sensor_data)));
						 sensors_data.add(Double.valueOf(System.currentTimeMillis()));							 
						try {
							sendLeaderMsg("Data from " + name +", Water Level: "+ sensors_data.get(0)+", PH Level: "+ sensors_data.get(1)+", Humidity Level: "+ sensors_data.get(2) , name, sensors_data);
						} catch (NumberFormatException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (DeadNodeException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}, 5000, 1000);
		System.out.println(name + " ready.");
	}

	private void sendLeaderMsg(String msg, String node_name,ArrayList<Double> sensors_data ) throws DeadNodeException {
		try {
			Registry reg = LocateRegistry.getRegistry(host);
			leaderName = getLeaderName();
			try {
				ElectionNode leaderNode = (ElectionNode) reg.lookup(leaderName);
				String response = leaderNode.recvMsg(node_name, msg, sensors_data);
				System.out.println(leaderName + ": " + response);
				if (!heardFromLeader)
					heardFromLeader = true;
			} catch (NotBoundException e) {
				try {
					reg.unbind(leaderName);
					setLeaderExist(false);
					startElection(node_name);	
				} catch (NotBoundException er) {
					// Shouldn't happen
				}
			} catch (ConnectException e) {
				try {
					reg.unbind(leaderName);
					setLeaderExist(false);
					startElection(node_name);
				} catch (NotBoundException er) {
					// Shouldn't happen
				}
			}
		} catch (RemoteException e) {
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
		if(getLeaderExist())
		{
			System.out.println("Leader exits.");
			noLeaderFound = false;
		}
		 else if(getLeaderExist() == false){
			System.out.println("Election started.");
			try {
				Registry reg = LocateRegistry.getRegistry(host);
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
								reg.unbind(nodeName);
							} catch (NotBoundException er) {
								// Shouldn't happen
							}
						} catch (ConnectException e) {
							try {
								reg.unbind(nodeName);
							} catch (NotBoundException er) {
								// Shouldn't happen
							}
						}
					}
				}
				setLeaderExist(true);
				setLeaderName(name);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

	@Override
	public void newLeader(String newLeaderName) {
		leaderName = newLeaderName;
		setLeaderName(newLeaderName);
		System.out.println(newLeaderName + " is the new leader.");
	}

	/**
	 * The current leader receives messages through this method and returns
	 * a message back to the sender in order to let the sender know
	 * the leader is still there and active.
	 */
	@Override
	public String recvMsg(String senderName, String msg, ArrayList<Double> sensors_data) {
		String ret = "Not the leader.";
		if (leaderName.equals(name)) {
			if(All_Sensors_Data.isEmpty())
			{
				 ArrayList<Double> sensors_data_leader = new  ArrayList<Double>();				 	
			 	//water
				 sensor_data = rand.nextDouble()*(max_W_lvl - min_W_lvl + 1) + min_W_lvl;
				 sensors_data_leader.add(Double.valueOf(df.format(sensor_data)));
				//ph-lvl
				 sensor_data = rand.nextDouble()*(max_PH_lvl - min_PH_lvl + 1) + min_PH_lvl;
				 sensors_data_leader.add(Double.valueOf(df.format(sensor_data)));
				//h-lvl
				 sensor_data = rand.nextDouble()*(max_H_lvl - min_H_lvl + 1) + min_H_lvl;
				 sensors_data_leader.add(Double.valueOf(df.format(sensor_data)));
				 sensors_data_leader.add(Double.valueOf(System.currentTimeMillis()));			 
				 All_Sensors_Data.put(leaderName, sensors_data_leader);
			}
			System.out.println(senderName + ": " + msg);
			All_Sensors_Data.put(senderName, sensors_data);
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
				String oldName = name;
				name = newName;
				ignoreElection = ignore;
				reg.unbind(oldName);
				reg.bind(name, this);
				System.out.println("New node name: " + newName);
				System.out.println("Ignoring " + ignore + " elections...");
			} catch(RemoteException e) {
				//System.out.println("Node Error: fml4" + e.toString());
				e.printStackTrace();
			} catch (NotBoundException e) {
				//System.out.println("Node Error: fml5" + e.toString());
				e.printStackTrace();
			} catch (AlreadyBoundException e) {
				//System.out.println("Node Error: fml6" + e.toString());
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
	public boolean getLeaderExist()
	{
		return leaderexits;
	}
	@Override
	public void setLeaderExist(boolean leader)
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
	@Override
	public LinkedHashMap<String,ArrayList<Double>> getAllData()
	{
		return All_Sensors_Data;
	}
	@Override
	public void clearMap()
	{
		All_Sensors_Data.clear();
	}
	
	@Override
	public int getNumberOfClient()
	{
		return number_of_clients;
	}
	@Override
	public void setNumberOfClient(int num) 
	{
		number_of_clients =num;
	}

	public static void main(String[] args) {
		String name = (args.length < 1 || args[0].equals("!")) ?
		"Node-" + System.currentTimeMillis() : args[0];
		String host = (args.length < 2) ? null : args[1];
		try {
			Node node = new Node(name, host);
			// Bind stub to the registry
			Registry reg = LocateRegistry.getRegistry(host);;
			reg.bind(name, node);
		} catch (Exception e) {
			//System.out.println("Node Error: fml7" + e.toString());
			e.printStackTrace();
		}
	}
}
