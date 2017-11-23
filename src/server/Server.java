package server;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.LinkedList;
import common.Pair;
import common.SharedInterface;

public class Server {
	
	static HashMap<String, RequestHandler> customerThreads;
	static HashMap<String, RequestHandler> agentThreads;
	static HashMap<String, Pair<String, String>> agentToCustomer;
	static HashMap<Long, String> agentLoginMapping;
	static HashMap<Long, String> customerLoginMapping;
	static HashMap<String, String> agentToMultiCastIP;
	static LinkedList<String> waitingCustomers; 
	
	static PrintWriter logFile;
	
	public static void main(String[] args) {
		customerThreads = new HashMap<String, RequestHandler>();
		agentThreads = new HashMap<String, RequestHandler>();
		agentToCustomer = new HashMap<String, Pair<String, String>>();
		agentLoginMapping = new HashMap<Long, String>();
		customerLoginMapping = new HashMap<Long, String>();
		agentToMultiCastIP = new HashMap<String, String>();
		waitingCustomers = new LinkedList<String>();
		
		//If the 'transcripts' folder does not exist, create it
		if(!(new File("transcripts").exists())){
			new File("transcripts").mkdirs();
		}
		
		ServerSocket listener;  
		Socket connection; 
		
		// First off, open log.txt
		try{
			logFile = new PrintWriter(new FileWriter("log.txt", true));
		} catch (IOException e){
			System.err.println(getTimeStamp() + ": Cannot open log.txt. Please fix the problem and try again.");
			System.exit(1);
		}
		
		// Next, load the RMI
		SharedInterface object;
		try {
			LocateRegistry.createRegistry(1099);
			object = new SharedImplementation();
			Naming.rebind("sharedObject", object);
		} catch (RemoteException | MalformedURLException e1) {
			System.out.println(e1.getMessage());
			logFile.println(getTimeStamp() + ": Server shutting down due to RMI problems.");
			System.exit(2);
		}
		
		System.out.println("RMI registry server is started!");
		
		// Accept and process connections forever, or until some error occurs.
		try {
			listener = new ServerSocket(9090);
			System.out.println("Server is Up, Running and Waiting....");
			while (true) {
				// Accept connection request and create the thread handler needed to resolve this.
				connection = listener.accept(); 
				// Set up the writers and readers
				
				RequestHandler handler = new RequestHandler(connection); 
				handler.start();
			}
		}
		catch (IOException e) {
			System.out.println("Sorry, the server has encountered IO problems. Shutting down.");
			logFile.println(getTimeStamp() + ": Server shutting down due to IO problems.");
		}
		finally {
			logFile.flush();
			logFile.close();
		}
	}
	
	public static long getTimeStamp(){
		return (System.currentTimeMillis() / 1000L);
	}
}
