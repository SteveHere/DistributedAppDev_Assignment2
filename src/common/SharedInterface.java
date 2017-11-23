package common;

import java.nio.channels.AlreadyConnectedException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface SharedInterface extends Remote {
	
	public long agentAuth(String username, String password) throws AlreadyConnectedException, RemoteException;
	
	public long customerAuth(String username) throws RemoteException;
	
	public boolean agentAlreadyLoggedIn(String username) throws RemoteException;

	public boolean isCustomerNameTaken(String username) throws RemoteException;
	
	public String getAgentIPAddress(String agent) throws RemoteException;
	
	public String getCustomerIPAddress(String customer) throws RemoteException;
	
	public String getAgentMulticastAddress(String agent) throws RemoteException;

	public void removeAgentMulticastAddress(String agent) throws RemoteException;
	
}
