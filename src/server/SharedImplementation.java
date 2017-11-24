package server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.AlreadyConnectedException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Random;

import common.SharedInterface;

@SuppressWarnings("serial")
public class SharedImplementation extends UnicastRemoteObject implements SharedInterface {

	static HashMap<String, String> agentsLoginInfo;
	
	protected SharedImplementation() throws RemoteException {
		super();
		agentsLoginInfo = new HashMap<String, String>();
		parseLoginInfo();
	}

	@Override
	public long agentAuth(String username, String password) throws AlreadyConnectedException, RemoteException {
		if(agentAlreadyLoggedIn(username)){
			throw new AlreadyConnectedException();
		}
		else if(username != null && password != null){
			if(agentsLoginInfo.containsKey(username)){
				if(agentsLoginInfo.get(username).equals(password)){
					long token = (long) (Math.random() * Long.MAX_VALUE);
					Server.agentLoginMapping.put(token, username);
					return token;
				}
			}
		}
		return -1;
	}
	
	@Override
	public long customerAuth(String username) throws RemoteException {
		if(isCustomerNameTaken(username)){
			return -1;
		}
		else {
			long token = (long) (Math.random() * Long.MAX_VALUE);
			Server.customerLoginMapping.put(token, username);
			return token;
		}
	}
	
	public boolean agentAlreadyLoggedIn(String username) throws RemoteException {
		return Server.agentThreads.containsKey(username);
	}

	public boolean isCustomerNameTaken(String username) throws RemoteException {
		return Server.customerThreads.containsKey(username);
	}

	public String getAgentIPAddress(String agent) throws RemoteException {
		if(Server.agentThreads.containsKey(agent)){
			return Server.agentThreads.get(agent).getIPAddress();
		}
		return null;
	}
	
	public String getCustomerIPAddress(String customer) throws RemoteException {
		if(Server.customerThreads.containsKey(customer)){
			return Server.customerThreads.get(customer).getIPAddress();
		}
		return null;
	}
	
	public String getAgentMulticastAddress(String agent) throws RemoteException{
		if(Server.agentToMultiCastIP.containsKey(agent)){
			return Server.agentToMultiCastIP.get(agent);
		}
		else if(Server.agentThreads.containsKey(agent)){
			String address = "";
			do{
				Random r = new Random();
				address = (224 + r.nextInt(10)) 
						+ "." + (1 + r.nextInt(250))
						+ "." + (1 + r.nextInt(250)) 
						+ "." + (1 + r.nextInt(250));
			}while(Server.agentToMultiCastIP.containsValue(address));
			Server.agentToMultiCastIP.put(agent, address);
			return address;
		}
		else{
			return null;
		}
	}
	
	@Override
	public void removeAgentMulticastAddress(String agent) throws RemoteException {
		if(Server.agentToMultiCastIP.containsKey(agent)){
			Server.agentToMultiCastIP.remove(agent);
		}
	}
	
	private void parseLoginInfo() {
		try(BufferedReader fileReader = new BufferedReader(new FileReader("agents.txt"))){
			String line = null;
			while((line = fileReader.readLine()) != null){
				if(!line.isEmpty()){
					String[] loginInfo = line.split("~", 2);
					if(loginInfo.length == 2) {
						agentsLoginInfo.put(loginInfo[0], loginInfo[1]);
					}
					else {
						throw new IllegalArgumentException();
					}
				}
			}
		} catch (IllegalArgumentException iae) {
			System.err.println("Encountered parsing error with agents.txt.\n"
					+ "Proper format: <Username>~<Password>\n"
					+ "Please fix this formatting issue and try again.");
			Server.logFile.println(Server.getTimeStamp()
					+ ": Encountered parsing error with agents.txt. Exiting program.");
			Server.logFile.close();
			System.exit(2);
		} catch (IOException e) {
			System.err.println("Encountered an IO error with agents.txt.\n"
					+ "Please fix the problem and try again.");
			Server.logFile.println(Server.getTimeStamp()
					+": Encountered IO error with agents.txt. Exiting program.");
			Server.logFile.close();
			System.exit(3);
		} 	
	}


}
