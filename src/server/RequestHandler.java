package server;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;

import common.Pair;

public class RequestHandler extends Thread {
	
	Socket clientSocket;
	//These members are generated
	private PrintWriter toClient;
	private BufferedReader fromClient;
	private String username;
	private String IPAddress;
	
	//This is only used by customers
	private PrintWriter transcript;
	
	public RequestHandler(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}
	
	public String getIPAddress(){
		return IPAddress;
	}

	@Override
	public void run() {
		IPAddress = clientSocket.getInetAddress().getHostAddress();
		
		try{
	    	//Set up readers and writers
			toClient = new PrintWriter(clientSocket.getOutputStream(), true);
			fromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			
			Server.logFile.println((System.currentTimeMillis() / 1000L) 
					+ " Client from " + IPAddress + " connected");
			Server.logFile.flush();
			
			//Send first request from server to client requesting details and logininfo
			toClient.println("Client authentication required");
			String[] clientResponse = fromClient.readLine().split("~");
			
			//The response must be in either 2 parts: (Customer|Agent)~<Token>
			if(clientResponse.length == 2){
				long token = new Long(clientResponse[1]);
				//The client must have authenticated beforehand
				if(clientResponse[0].equals("Customer")){
					if(Server.customerLoginMapping.containsKey(token)){
						username = Server.customerLoginMapping.get(token);
						Server.customerThreads.put(username, this);
						Server.customerLoginMapping.remove(token);
						toClient.println("Connection established");
						startCustomerConnection();
					}
					else{
						toClient.println("Client has not yet authenticated");
					}
				}
				else if(clientResponse[0].equals("Agent")){
					if(Server.agentLoginMapping.containsKey(token)){
						username = Server.agentLoginMapping.get(token);
						Server.agentThreads.put(username, this);
						Server.agentLoginMapping.remove(token);
						Server.agentToCustomer.put(username, new Pair<String, String>(null, null));
						toClient.println("Login successful");
						startAgentConnection();
					}
					else{
						toClient.println("Client has not yet authenticated");
					}	
				}
				else{
					toClient.println("Client has sent incorrect format");
				}
			}
			//If its not neither, print out an authentication error message to the client
			else{
				toClient.println("Client has sent incorrect format");
			}
			toClient.close();
			fromClient.close();
			clientSocket.close();
		} catch (IOException | InterruptedException e) {
			Server.logFile.println((System.currentTimeMillis() / 1000L) 
					+ ": Exception occured while connecting with "
					+ IPAddress + "\n" 
					+ e.toString());
		}
		
		Server.logFile.println((System.currentTimeMillis() / 1000L) 
				+ " Client from " +IPAddress + " disconnected");
		Server.logFile.flush();
	}
	
	//This method handles connections with agents
	private void startAgentConnection() throws IOException, InterruptedException {
		boolean clientWantsToExit = false;
		while(clientWantsToExit == false){			
			if(fromClient.ready()){
				//Receive a response from the client
				String response = fromClient.readLine();
				//System.out.println(response); //For debugging purposes
				//If the client wants to quit, only allow it to if it has nobody to attend to
				if(response.equals("Quit")){
					if(Server.agentToCustomer.get(username).isEmpty()){
						clientWantsToExit = true;
						Server.agentThreads.remove(username);
						Server.agentToCustomer.remove(username);
						toClient.println("Can quit");
					}
					else{
						toClient.println("Cannot quit");
					}
				}
				//If the agent force quits, close off everything
				else if(response.equals("Force quit")){
					Server.agentThreads.remove(username);
					Pair<String, String> temp = Server.agentToCustomer.remove(username);
					if(Server.customerThreads.containsKey(temp.getLeft())){
						Server.customerThreads.get(temp.getLeft()).toClient.println("Agent force quit");
					}
					if(Server.customerThreads.containsKey(temp.getRight())){
						Server.customerThreads.get(temp.getRight()).toClient.println("Agent force quit");
					}
					break;
				}
				//Otherwise, it's a message to a customer
				//In this case, send it to the appropriate customer
				else{
					String[] clientResponse = response.split("~", 2);
					if(clientResponse.length == 2){
						//If the response is for both customers
						if(clientResponse[0].equals("Both")){
							Pair<String, String> customers = Server.agentToCustomer.get(username);
							//Check if the customers exist and whether they belong to the agent
							if(Server.customerThreads.containsKey(customers.getLeft())
									&& Server.agentToCustomer.get(username).contains(customers.getLeft())){
								Server.customerThreads.get(customers.getLeft()).record((System.currentTimeMillis() / 1000L) 
										+ " " + username + ": " + clientResponse[1]);
								Server.customerThreads.get(customers.getLeft()).transferMessage(username, clientResponse[1]);
							}
							if(Server.customerThreads.containsKey(customers.getRight())
									&& Server.agentToCustomer.get(username).contains(customers.getRight())){
								Server.customerThreads.get(customers.getRight()).record((System.currentTimeMillis() / 1000L) 
										+ " " + username + ": " + clientResponse[1]);
								Server.customerThreads.get(customers.getRight()).transferMessage(username, clientResponse[1]);
							}
						}
						//Otherwise, send the message to the customer that is specified in the agent's response
						else if(Server.customerThreads.containsKey(clientResponse[0])
								&& Server.agentToCustomer.get(username).contains(clientResponse[0])
								){
							Server.customerThreads.get(clientResponse[0]).record((System.currentTimeMillis() / 1000L) 
									+ " " + username + ": " + clientResponse[1]);
							Server.customerThreads.get(clientResponse[0]).transferMessage(username, clientResponse[1]);
						}
					}
				}
			}
			
			//Next, check if the agent is full and whether or not they want to exit
			//If not for both, take from queue and give it to agent
			if(clientWantsToExit == false && !Server.agentToCustomer.get(username).isFull()){
				try{
					String customerToAdd = Server.waitingCustomers.remove();
					toClient.println("NewCustomer~" + customerToAdd);
					Server.customerThreads.get(customerToAdd).toClient.println(username);
					Pair<String, String> t = Server.agentToCustomer.get(username);
					Pair<String, String> newPair = null;
					if(t.isEmpty()){
						newPair = new Pair<String, String>(customerToAdd, null);
					}
					else if(t.getLeft() != null){
						newPair = new Pair<String, String>(t.getLeft(), customerToAdd);
					}
					else if(t.getRight() != null){
						newPair = new Pair<String, String>(customerToAdd, t.getRight());
					}
					Server.agentToCustomer.put(username, newPair);
				} catch (NoSuchElementException nsee){
				}
			}
			Thread.sleep(1000);
		}
	}

	//This method handles connections with customers
	private void startCustomerConnection() throws IOException, InterruptedException {
		//The customer first pushes their own name to the Queue
		Server.waitingCustomers.add(username);
		String agent = null;
		boolean customerQuit = false;
		//The thread then periodically checks whether or not the name's still there
		while(Server.waitingCustomers.contains(username)){
			//If the client has a response here, handle it
			//In this case, remove the customer's name
			String response = fromClient.readLine();
			if(response != null){
				if(response.equals("Quit") || response.equals("Force quit")){
					if(Server.waitingCustomers.contains(username)){
						Server.waitingCustomers.remove(username);
					}
					Server.customerThreads.remove(username);
					for(Pair<String, String> v : Server.agentToCustomer.values()){
						if(v.getLeft().equals(username) || v.getRight().equals(username)){
							for(String k : Server.agentToCustomer.keySet()){
								Pair<String, String> p = Server.agentToCustomer.get(k);
								if(p.getLeft().equals(username) || p.getRight().equals(username)){
									Server.agentThreads.get(k).toClient.println("Remove~" + username);
									Pair<String, String> temp = Server.agentToCustomer.get(k);
									if(temp.getLeft().equals(username)){
										Server.agentToCustomer.put(k, new Pair<String, String>(temp.getRight(), null));
									}
									else if(temp.getRight().equals(username)){
										Server.agentToCustomer.put(k, new Pair<String, String>(temp.getLeft(), null));
									}
									break;
								}
							}
							break;
						}
					}
					customerQuit= true;
					break;
				}
				/*QUICK FIX: FIX THIS LATER*/
				else{
					String[] clientResponse = response.split("~", 2);
					if(clientResponse.length == 2){
						if(Server.agentThreads.containsKey(clientResponse[0])){
							agent = clientResponse[0];
							transcript = new PrintWriter(new FileWriter("transcripts\\" + agent + "~" + username + ".txt", true));
							record((System.currentTimeMillis() / 1000L) + " " + username + ": " + clientResponse[1]);
							Server.agentThreads.get(clientResponse[0]).transferMessage(username, clientResponse[1]);
							break;
						}
					}
				}				
			}
			Thread.sleep(3000);
		}
		if(customerQuit == false){
			boolean clientWantsToExit = false;
			while(clientWantsToExit == false){
				if(fromClient.ready()){
					String response = fromClient.readLine();
					//System.out.println(response); //For debugging purposes
					//Otherwise, the customer is sending a message to the agent
					String[] clientResponse = response.split("~", 2);
					if(clientResponse.length == 2){
						if(Server.agentThreads.containsKey(clientResponse[0])){
							record((System.currentTimeMillis() / 1000L) + " " + username + ": " + clientResponse[1]);
							Server.agentThreads.get(agent).transferMessage(username, clientResponse[1]);
						}
					}
					//If the customer requests to quit, quit.
					else if(response.equals("Quit") || response.equals("Force quit")){
						clientWantsToExit = true;
						Server.customerThreads.remove(username);
						//Remove all traces of the user from the hashmaps
						if(agent != null && Server.agentThreads.get(agent) != null){
							Server.agentThreads.get(agent).toClient.println("Remove~" + username);
							Pair<String, String> temp = Server.agentToCustomer.get(agent);
							if(temp.getLeft().equals(username)){
								Server.agentToCustomer.put(agent, new Pair<String, String>(temp.getRight(), null));
							}
							else if(temp.getRight().equals(username)){
								Server.agentToCustomer.put(agent, new Pair<String, String>(temp.getLeft(), null));
							}
						}
						if(transcript != null){
							transcript.flush();
							transcript.close();
						}
						if(response.equals("Quit")){
							toClient.println("Can quit");
						}
						break;
					}
				}
				Thread.sleep(1000);
			}
		}	
		//System.out.println("Exited customer loop"); //For debugging purposes
	}
	
	//This method transfers messages from external calls to the connected client
	public void transferMessage(String source, String message){
		toClient.println(source + "~" + message);
		
	}
	
	//This method records the messages sent into the customer's transcript 
	private void record(String string) {
		if(transcript != null){
			transcript.println(string);		
			transcript.flush();
		}
	}
}
