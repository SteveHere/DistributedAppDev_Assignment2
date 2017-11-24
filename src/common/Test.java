package common;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Test {

	public static void main(String[] args) throws AccessException, RemoteException, NotBoundException {
		Registry registry = LocateRegistry.getRegistry("127.0.0.1");
		SharedInterface rmiObject = (SharedInterface) registry.lookup("sharedObject");
		System.out.println(rmiObject.getAgentMulticastAddress("agent1"));

	}

}
