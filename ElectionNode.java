
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public interface ElectionNode extends Remote {


	// Node methods
	public String startElection(String senderName) throws RemoteException, DeadNodeException;
	public void newLeader(String newLeaderName) throws RemoteException;
	public String recvMsg(String senderName, String msg, ArrayList<Double> sensors_data) throws RemoteException;

	// Election Driver methods
	public void makeChaos(String newName, int ignore) throws RemoteException;


	//new things add
	public String getNode_name()throws RemoteException;

	//set leader
	public void setLeaderExist(boolean leader)throws RemoteException;
	public boolean getLeaderExist()throws RemoteException;
	public void setLeaderName(String leader_name) throws RemoteException;
	public String getLeaderName()throws RemoteException;
	//
	public LinkedHashMap<String,ArrayList<Double>> getAllData() throws RemoteException;
	public void setNumberOfClient(int num) throws RemoteException; 
	public int getNumberOfClient() throws RemoteException; 
	public void clearMap() throws RemoteException;




}
