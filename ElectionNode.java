
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ElectionNode extends Remote {


	// Node methods
	public String startElection(String senderName) throws RemoteException, DeadNodeException;
	public void newLeader(String newLeaderName) throws RemoteException;
	public String recvMsg(String senderName, String msg, float water_temperature) throws RemoteException;

	// Election Driver methods
	public void makeChaos(String newName, int ignore) throws RemoteException;


	//new things add
	public String getNode_name()throws RemoteException;

	//set leader
	public void setLeaderExits(boolean leader)throws RemoteException;
	public boolean getLeaderExits()throws RemoteException;
	public void setLeaderName(String leader_name) throws RemoteException;
	public String getLeaderName()throws RemoteException;




}
