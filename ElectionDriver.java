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
	private int data_count = 0;
	private ArrayList<ArrayList<String>> All_Sensors_Data_Server = new ArrayList<ArrayList<String>>();

	// Min and max optimal values for all sensors
	private static final double min_W_lvl = 18.0;
	private static final double max_W_lvl = 26.0;
	private static final double min_PH_lvl = 5.5;
	private static final double max_PH_lvl = 6.5;
	private static final double min_H_lvl = 50.0;
	private static final double max_H_lvl = 70.0;	
	
	// Date Time Format
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd yyy HH:mm:ss");
	
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
							
							if(node.getLeaderExist() == false && leaderexist == false)
							{
								//System.out.println("This is node name: and this will be the leader "+String.valueOf(node.getLeaderExist()));
								//System.out.println("This is node name: and this will be the leader "+node.getNode_name());
							}
							else if (node.getLeaderExist() == true)
							{
								if(count == 0)
								{
									leaderexist = true;
									new_leadername = node.getNode_name();
									node.setLeaderExist(leaderexist);
									node.setLeaderName(new_leadername);
									count++;
								}
							}
							else if (node.getLeaderExist() == false &&  leaderexist == true )
							{
								node.setLeaderExist(leaderexist);
								node.setLeaderName(new_leadername);
							}
							//set number of client
							node.setNumberOfClient(reg.list().length);
									
							if(node.getNode_name().equals(node.getLeaderName()) && reg.list().length == node.getAllData().size())
							{
								data_count++;
								Set<String> keys = node.getAllData().keySet();
								  
								for(String k:keys)
								{
									Date date = new Date(node.getAllData().get(k).get(3).longValue());
									ArrayList<String> node_data = new ArrayList<String>();
									node_data.add(k);
									node_data.add(node.getAllData().get(k).get(0).toString());
									node_data.add(node.getAllData().get(k).get(1).toString());
									node_data.add(node.getAllData().get(k).get(2).toString());
									node_data.add(sdf.format(date).toString());
									All_Sensors_Data_Server.add(node_data);
									System.out.println("Client Name: "+k+", Water Temperature: "+node.getAllData().get(k).get(0)+", PH Level: "+node.getAllData().get(k).get(1)+", Humidity: "+node.getAllData().get(k).get(2)+"%, Date: "+ sdf.format(date));
								}
								// Update CSV File every 10 updates
								if(data_count == 10)
								{
									data_count = 0;									  
									create_csv(All_Sensors_Data_Server);
								}
								node.clearMap(); 								 
							}
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
	
	public void create_csv(ArrayList<ArrayList<String>> Final_Data)throws FileNotFoundException
	{
		// Filewriter
		PrintWriter pw = new PrintWriter(new File("Sensors_Data.csv"));
	    StringBuilder sb = new StringBuilder();
	    // Header (First Line in Excel File)
	    sb.append("Client,Water Temperature,PH Level,Humiditity Level,Date,Error\r\n");
	    // Loop and print all the data line by line
	    for(int i=0; i<Final_Data.size(); i++)
	    {
	    	sb.append(Final_Data.get(i).get(0));
	        sb.append(',');
	        sb.append(Final_Data.get(i).get(1));
	        sb.append(',');
	        sb.append(Final_Data.get(i).get(2));
	        sb.append(',');
	        sb.append(Final_Data.get(i).get(3) + "%");
	        sb.append(',');
	        sb.append(Final_Data.get(i).get(4));
	        
	         // Check whether sensors are within optimal values
	        String temp = "Please check on sensors";
	        if(Double.parseDouble(Final_Data.get(i).get(1)) > max_W_lvl || Double.parseDouble(Final_Data.get(i).get(1)) < min_W_lvl){
	        	temp += " Water Temperature";
	        }
	        if(Double.parseDouble(Final_Data.get(i).get(2)) > max_PH_lvl || Double.parseDouble(Final_Data.get(i).get(2)) < min_PH_lvl){
	        	temp += " PH Level";
	        }
	        if(Double.parseDouble(Final_Data.get(i).get(3)) > max_H_lvl || Double.parseDouble(Final_Data.get(i).get(3)) < min_H_lvl){
	        	temp += " Humidity Level";
	        }
	        if(!temp.equals("Please check on sensors")){
	        	sb.append(',');
	        	sb.append(temp);
	        }
	        sb.append("\r\n");
	    }
	    pw.write(sb.toString());
	    pw.close();
		System.out.println("Exported to CSV File!");
	}

	public static void main(String[] args) {
		String host = (args.length < 1) ? null : args[0];
		new ElectionDriver(host);
	}
}
