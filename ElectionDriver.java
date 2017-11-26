
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class ElectionDriver {

	int delay = 5000;

	private final int min = 10;
	private final int max = 15;

	private String host;
	private boolean leaderexist = false;
	private String new_leadername = "";
	private int count = 0;
	public	LinkedHashMap<String,Double> All_Water_Temperature_Server = new LinkedHashMap<String,Double>();

	public ElectionDriver(String hostIn) {
		host = hostIn;
		int period = min + (int) (Math.random() * ((max - min) + 1)) * 1000;

		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					//Registry reg = LocateRegistry.getRegistry(host);
						Registry reg = LocateRegistry.getRegistry();
					//System.out.println("reg :"+reg);
					for (String nodeName : reg.list()) {
						try {
							int silence = (int) (Math.random() * 5);


								ElectionNode node = (ElectionNode) reg.lookup(nodeName);
								//leaderexist = true;
								if(node.getLeaderExist() == false && leaderexist == false)
								{
									System.out.println("This is node name: and this will be the leader "+String.valueOf(node.getLeaderExist()));
									System.out.println("This is node name: and this will be the leader "+node.getNode_name());
								}



							else if (node.getLeaderExist() == true)
							{
								System.out.println("leaderexist "+node.getLeaderName());
								if(count == 0)
								{
								leaderexist = true;
								new_leadername = node.getNode_name();
								node.setLeaderExist(leaderexist);
								node.setLeaderName(new_leadername);
								count++;
								}
								//node.setLeaderName(new_leadername);
								//node.setLeaderName(new_leadername);
							}

							else if (node.getLeaderExist() == false &&  leaderexist == true )
							{
								//System.out.println("sorry leaderexist "+node.getLeaderName());
								node.setLeaderExist(leaderexist);
								node.setLeaderName(new_leadername);
								//node.setLeaderName(new_leadername);
							}
							//set number of client
								node.setNumberOfClient(reg.list().length);


							//node.makeChaos("Node-" + System.currentTimeMillis(), silence);
								System.out.println("NAME NODE :"+ node.getNode_name());
								System.out.println("Leader Node :"+ node.getLeaderName());
								System.out.println("SIZE :"+ reg.list().length);
							if(node.getNode_name()==node.getLeaderName()&&reg.list().length == node.getAllData().size())
							{
								System.out.println("testing");
								All_Water_Temperature_Server = node.getAllData();
								
								
								  Set<String> keys = All_Water_Temperature_Server.keySet();

								  for(String k:keys)
								  {
									  System.out.println("Values :"+ All_Water_Temperature_Server.get(k));
								  }
								  node.clearMap();
							}
							//System.out.println("Values 2 :"+ node.getAllData().size());




						} catch (NotBoundException e) {
							System.out.println("Election Driver Error: " + e.toString());
							e.printStackTrace();
						}
					}
				} catch (RemoteException e) {
					System.out.println("Election Driver Error: " + e.toString());
					e.printStackTrace();
				}
			}
		}, delay, period);
	}

	public static void main(String[] args) {
		String host = (args.length < 1) ? null : args[0];
		new ElectionDriver(host);
	}

}
