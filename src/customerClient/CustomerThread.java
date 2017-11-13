package customerClient;

import java.io.IOException;

import javax.swing.JOptionPane;

import javafx.application.Platform;

public class CustomerThread extends Thread {

	@Override
	public void run() {
		try{
			boolean onlyShowOnce = true;
			while(CustomerClient.agent == null && CustomerClient.wantsToQuit == false){
				if(CustomerClient.fromServer.ready()){
					String response = CustomerClient.fromServer.readLine();
					//Check if the customer wanted to quit early, and as such allow the customer to do so
					if(response.equals("Can quit") || response.equals("Agent force quit")){
						CustomerClient.wantsToQuit = true;
						break;
					}
					//Otherwise the response is the agent's name, and it should be stored
					else {
						CustomerClient.agent = new String(response);
						JOptionPane.showMessageDialog(null, 
								"Agent found. Name: " + CustomerClient.agent, 
								"Agent Found", 
								JOptionPane.INFORMATION_MESSAGE
								);
						Platform.runLater(()->{
							CustomerClient.agentName.setText("Connected To: " + CustomerClient.agent);
							CustomerClient.clientText.setDisable(false);
							CustomerClient.send.setDisable(false);
						});
						break;
					}
				}
				else {
					if(onlyShowOnce == true){
						//While we haven't found an agent to talk to yet
						JOptionPane.showMessageDialog(null, 
								"Waiting for agents to be available", 
								"Waiting for agents", 
								JOptionPane.INFORMATION_MESSAGE
								);
						onlyShowOnce = false;
					}
					//If the response is to wait for an agent, inform the user to do so
					try {
						Platform.runLater(()->{
							CustomerClient.agentName.setText("Waiting for agents to be available.");
						});
						Thread.sleep(1000);
						Platform.runLater(()->{
							CustomerClient.agentName.setText("Waiting for agents to be available..");
						});
						Thread.sleep(1000);
						Platform.runLater(()->{
							CustomerClient.agentName.setText("Waiting for agents to be available...");
						});
						Thread.sleep(1000);
						Platform.runLater(()->{
							CustomerClient.agentName.setText("Waiting for agents to be available....");
						});
						Thread.sleep(1000);
						Platform.runLater(()->{
							CustomerClient.agentName.setText("Waiting for agents to be available.....");
						});
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			//While the customer has not decided to quit, and has not received word of being able to quit
			while(CustomerClient.wantsToQuit == false){
				if(CustomerClient.fromServer.ready()){
					String response = CustomerClient.fromServer.readLine();
					//System.out.println(response); //For debugging purposes
					String[] responseSplit = response.split("~", 2);
					//If the response has 2 parts, then it is a message from the server
					if(responseSplit.length == 2){
						if(responseSplit[0].equals(CustomerClient.agent)){
							Platform.runLater(()->{
								CustomerClient.clientTextArea.setText(
									CustomerClient.clientTextArea.getText()
									+ CustomerClient.agent + ": " + responseSplit[1] + "\n");
							});
						}
					}
					//If the agent force quits, search for another
					else if(response.equals("Agent force quit")){
						JOptionPane.showMessageDialog(null, 
								"The agent exited the application. Shutting down.", 
								"Agent Exited Application", 
								JOptionPane.INFORMATION_MESSAGE
								);
						CustomerClient.toServer.println("Quit");
						break;
					}
					//Otherwise, the user requested to quit, and the server should quit
					else{
						CustomerClient.toServer.println("Quit");
						CustomerClient.wantsToQuit = true;
					}
				}
				Thread.sleep(1000);
			}
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, 
					"IO Exception encountered. Details: \n" + e, 
					"IO Exception encountered", 
					JOptionPane.ERROR_MESSAGE
					);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

}
