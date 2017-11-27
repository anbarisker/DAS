
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
	private int data_count =0;
	public	LinkedHashMap<String,ArrayList<Double>> All_Sensors_Data = new LinkedHashMap<String,ArrayList<Double>>();
	
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyy HH:mm:ss");
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
									//System.out.println("This is node name: and this will be the leader "+String.valueOf(node.getLeaderExist()));
									//System.out.println("This is node name: and this will be the leader "+node.getNode_name());
								}



							else if (node.getLeaderExist() == true)
							{
								//System.out.println("leaderexist "+node.getLeaderName());
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
								/*System.out.println("NAME NODE :"+ node.getNode_name());
								System.out.println("Leader Node :"+ node.getLeaderName());
								System.out.println("SIZE :"+ reg.list().length);*/
								
									
							if(node.getNode_name().equals(node.getLeaderName()) && reg.list().length == node.getAllData().size())
							{
								
								data_count++;
								//All_Sensors_Data.putAll(node.getAllData());
								
								
								
								 Set<String> keys_initial = All_Sensors_Data.keySet();
								 ArrayList<Double> emp_array = new ArrayList<Double>();
								  for(String k:keys_initial)
								  {
									 for(int i=0; i<All_Sensors_Data.get(k).size();i++)
									 {
									//	 emp_array.add();
									 }
								  }
								
								
								 node.clearMap(); 
								// System.out.println("Size :"+ All_Sensors_Data.size());
								  Set<String> keys = All_Sensors_Data.keySet();
								  
								  for(String k:keys)
								  {
									  Date date = new Date(All_Sensors_Data.get(k).get(3).longValue());
									  System.out.println("Client Name: "+k+", Water Temperature: "+All_Sensors_Data.get(k).get(0)+", PH Level: "+All_Sensors_Data.get(k).get(1)+", Humidity: "+All_Sensors_Data.get(k).get(2)+", Date: "+ sdf.format(date));
								  }
								  
								  if(data_count == 10)
								  {
									  data_count = 0;
									  
									  create_csv(All_Sensors_Data);
								  }
								 
							}
							//System.out.println("Values 2 :"+ node.getAllData().size());




						} catch (NotBoundException e) {
							//System.out.println("Election Driver Error: " + e.toString());
							//e.printStackTrace();
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							//e.printStackTrace();
						}
					}
				} catch (RemoteException e) {
					//System.out.println("Election Driver Error: " + e.toString());
					//e.printStackTrace();
				}
			}
		}, delay, period);
		
		
		
	}
	
	public void create_csv(LinkedHashMap<String,ArrayList<Double>> Final_Data)throws FileNotFoundException
	{
	  //filewriter
	    System.out.println("Exported to csv!");
	  PrintWriter pw = new PrintWriter(new File("Sensors_Data.csv"));
	       StringBuilder sb = new StringBuilder();
	       Set<String> keys =  Final_Data.keySet();

	       for(String k:keys)
	       {
	         for(int i=0; i<Final_Data.get(k).size(); i++)
	         {
	           sb.append(k);
	           sb.append(',');
	           sb.append(Final_Data.get(k).get(0));
	           sb.append(',');
	           sb.append(Final_Data.get(k).get(1));
	           sb.append(',');
	           sb.append(Final_Data.get(k).get(2));
	           sb.append(',');
	           Date date = new Date(All_Sensors_Data.get(k).get(3).longValue());
	           sb.append(sdf.format(date));
	           sb.append('\n');
	         }
	       }
	       pw.write(sb.toString());
	       pw.close();
	       System.out.println("done!");
	}

	public static void main(String[] args) {
		String host = (args.length < 1) ? null : args[0];
		new ElectionDriver(host);
	}

}
