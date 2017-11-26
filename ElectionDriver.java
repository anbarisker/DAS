
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Timer;
import java.util.TimerTask;

public class ElectionDriver {

	int delay = 5000;

	private final int min = 10;
	private final int max = 15;

	private String host;
	private boolean leaderexits = false;
	private String new_leadername = "";
	private int count = 0;

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
					System.out.println("reg :"+reg);
					for (String nodeName : reg.list()) {
						try {
							int silence = (int) (Math.random() * 5);


								ElectionNode node = (ElectionNode) reg.lookup(nodeName);
								//leaderexits = true;
								if(node.getLeaderExits() == false && leaderexits == false)
								{
									System.out.println("This is node name: and this will be the leader "+String.valueOf(node.getLeaderExits()));
									System.out.println("This is node name: and this will be the leader "+node.getNode_name());
								}



							else if (node.getLeaderExits() == true)
							{
								System.out.println("leaderexits "+node.getLeaderName());
								if(count == 0)
								{
								leaderexits = true;
								new_leadername = node.getNode_name();
								node.setLeaderExits(leaderexits);
								node.setLeaderName(new_leadername);
								count++;
								}
								node.setLeaderName(new_leadername);
								//node.setLeaderName(new_leadername);
							}

							else if (node.getLeaderExits() == false &&  leaderexits == true )
							{
								System.out.println("sorry leaderexits "+node.getLeaderName());
								node.setLeaderExits(leaderexits);
								node.setLeaderName(new_leadername);
								//node.setLeaderName(new_leadername);
							}


							//node.makeChaos("Node-" + System.currentTimeMillis(), silence);





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
