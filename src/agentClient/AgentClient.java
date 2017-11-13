package agentClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import common.Pair;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class AgentClient extends Application {

	Socket socket;
	static BufferedReader fromServer;
	static PrintWriter toServer;
	static String username;
	static String customer1;
	static String customer2;
	static boolean wantsToQuit = false;
	static boolean canQuit = false;

	static Label customer1Label;
	static Label customer2Label;
	
	static TextArea customer1Dialog;
	static TextArea customer2Dialog;
	
	static GridPane customer1Input;
	static GridPane customer2Input;
	static GridPane bothCustomersInput;
	
	@Override
	public void start(Stage primaryStage){
		Stage stage = new Stage();
		String serverAddress = JOptionPane.showInputDialog(null,  "Enter IP Address of the CVT server: ", 
				"Enter CVT Server IP Address", JOptionPane.QUESTION_MESSAGE);

		try{     
			boolean correctUsernameFormat = false; //assumed incorrect by default
			Pair<String, String> input = null;

			do{
				input = loginPrompt();
				//input is only correct if the username has no tilde, or if nothing was entered
				correctUsernameFormat = (input == null) || !input.getLeft().contains("~");
				if(correctUsernameFormat == false){
					JOptionPane.showMessageDialog(
							null, 
							"Incorrect username format detected. Please try again.", 
							"Incorrect Username Format", 
							JOptionPane.ERROR_MESSAGE
							);
				}
			} while(correctUsernameFormat == false);
			
			//If there is no input, quit the program
			if(input == null){
				JOptionPane.showMessageDialog(null, 
						"Login aborted. Exiting program.", 
						"Login Aborted", 
						JOptionPane.INFORMATION_MESSAGE
						);
				System.exit(0);
			}
			else{
				socket = new Socket(serverAddress, 9090);

				// Receiving or reading data from the socket
				fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				toServer = new PrintWriter(socket.getOutputStream(), true);
				
				String initialResponse = fromServer.readLine();
				
				//Main logic only starts when the server sends back "Client request required"
				if(initialResponse.equals("Client request required")){
					toServer.println("Agent~" + input.getLeft() + "~" + input.getRight()); 

					String response = fromServer.readLine();
					if(response.equals("Login successful")){
						username = new String(input.getLeft());
						stage.setTitle("CVT Agent Client - User: " + username);
						stage.setScene(getAgentApplicationScene());
						stage.setResizable(false);
						
						AgentThread thread = new AgentThread();
						thread.start();
						
						stage.showAndWait();
						//The user is quitting via the close button, force quit
						if(AgentClient.wantsToQuit == false){
							toServer.println("Force quit");
						}
					}
					else if(response.equals("Agent has already logged in")){
						JOptionPane.showMessageDialog(null, 
								"Agent has already logged in. Exiting program.", 
								"Agent Already Logged In", 
								JOptionPane.INFORMATION_MESSAGE
								);
						System.exit(0);
					}
					else if(response.equals("Incorrect login details")){
						JOptionPane.showMessageDialog(null, 
								"Incorrect Login Details. Exiting program.", 
								"Incorrect Login Details", 
								JOptionPane.ERROR_MESSAGE
								);
						System.exit(0);
					}
				}
				
				fromServer.close();
				toServer.close();
				socket.close();
			}
		} catch(Exception e){
			JOptionPane.showMessageDialog(
					null, 
					"Exception encountered. Details: \n" + e, 
					"Exception encountered", 
					JOptionPane.ERROR_MESSAGE
					);
		}
		System.exit(0);
	}

	public static void main(String[] args) {
		Application.launch(args);
	}
	
	private static Pair<String, String> loginPrompt(){
		JTextField username = new JTextField();
		JPasswordField password = new JPasswordField();

		JPanel myPanel = new JPanel(new BorderLayout(5, 5));
		
		JPanel labels = new JPanel(new GridLayout(0,1,2,2));
		labels.add(new JLabel("Username: ", SwingConstants.LEFT));
		labels.add(new JLabel("Password: ", SwingConstants.LEFT));
		myPanel.add(labels, BorderLayout.WEST);

		JPanel controls = new JPanel(new GridLayout(0,1,2,2));
		controls.add(username);
		controls.add(password);
		myPanel.add(controls, BorderLayout.CENTER);
		
		int result = JOptionPane.showOptionDialog(null, myPanel, 
				"CVT Agent Login", JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, new String[]{"Login", "Cancel"}, null);
		if (result == JOptionPane.OK_OPTION) {
			return new Pair<String, String>(username.getText(), new String(password.getPassword()));
		}
		return null;
	}
	
	private Scene getAgentApplicationScene() {
		GridPane result = new GridPane();
		result.setPadding(new Insets(5));
		result.setHgap(5);
		result.setVgap(5);
		
		//Initialization of components
		
		customer1Label = new Label("Connected Customer: ");
		customer2Label = new Label("Connected Customer: ");
		
		customer1Dialog = new TextArea();
		customer2Dialog = new TextArea();
		
		TextField inputForCustomer1 = new TextField();
		TextField inputForCustomer2 = new TextField();
		TextField inputForBoth = new TextField();
		
		Button sendToCustomer1 = new Button("Send");
		Button sendToCustomer2 = new Button("Send");
		Button sendToBoth = new Button("Send to Both Customers");
		
		customer1Input = new GridPane();
		customer2Input = new GridPane();
		bothCustomersInput = new GridPane();
		
		Button quit = new Button("Quit Session");
		
		HBox quitHBox = new HBox(quit);
		
		//Styling of components
		quitHBox.setAlignment(Pos.CENTER);
		
		customer1Dialog.setMinHeight(400);
		customer1Dialog.setEditable(false);
		customer2Dialog.setMinHeight(400);
		customer2Dialog.setEditable(false);
		
		
		customer1Input.setHgap(5);
		customer1Input.setDisable(true);
		customer2Input.setHgap(5);
		customer2Input.setDisable(true);
		bothCustomersInput.setHgap(5);
		bothCustomersInput.setDisable(true);
		
		
		inputForCustomer1.setMinWidth(240);
		inputForCustomer2.setMinWidth(240);
		inputForBoth.setMinWidth(450);
		
		sendToCustomer1.setMinWidth(50);
		sendToCustomer2.setMinWidth(50);
		sendToBoth.setMinWidth(120);
		
		quit.setMinWidth(100);
		
		//Event handling
		sendToCustomer1.setOnMouseClicked(e -> {
			if(!inputForCustomer1.getText().equals("") && customer1 != null){
				customer1Dialog.setText(customer1Dialog.getText() 
						+ username + ": " + inputForCustomer1.getText() + "\n");
				customer1Dialog.positionCaret(customer1Dialog.getLength());
				toServer.println(customer1 + "~" + inputForCustomer1.getText());
				toServer.flush();
				inputForCustomer1.setText("");
			}
		});
		
		sendToCustomer2.setOnMouseClicked(e -> {
			if(!inputForCustomer2.getText().equals("") && customer2 != null){
				customer2Dialog.setText(customer2Dialog.getText() 
						+  username + ": " + inputForCustomer2.getText() + "\n");
				customer2Dialog.positionCaret(customer2Dialog.getLength());
				toServer.println(customer2 + "~" + inputForCustomer2.getText());
				inputForCustomer2.setText("");
			}
		});
		
		sendToBoth.setOnMouseClicked(e -> {
			if(!inputForBoth.getText().equals("") && customer2 != null){
				customer1Dialog.setText(customer1Dialog.getText() 
						+ username + ": " + inputForBoth.getText() + "\n");
				customer1Dialog.positionCaret(customer1Dialog.getLength());
				customer2Dialog.setText(customer2Dialog.getText() 
						+  username + ": " + inputForBoth.getText() + "\n");
				customer2Dialog.positionCaret(customer2Dialog.getLength());
				toServer.println("Both~" + inputForBoth.getText());
				inputForCustomer2.setText("");
				inputForCustomer1.setText("");
				inputForBoth.setText("");
			}
		});
		
		quit.setOnMouseClicked(e -> {
			toServer.println("Quit");
			wantsToQuit = true;
		});
		
		customer1Dialog.textProperty().addListener((obs, old, niu)->{
			String[] dialog1 = customer1Dialog.getText().split("\n");
			String[] dialog2 = customer2Dialog.getText().split("\n");
			if(dialog1.length > 0 && dialog2.length > 0){
				String[] lastLine1 = dialog1[dialog1.length - 1].split(": ", 2);
				String[] lastLine2 = dialog2[dialog2.length - 1].split(": ", 2);
				if(lastLine1.length == 2 && lastLine2.length == 2){
					if(lastLine1[1].equals(lastLine2[1]) && !lastLine1[0].equals(username) 
							&& !lastLine2[0].equals(username)){
						bothCustomersInput.setDisable(false);
						customer1Input.setDisable(true);
						customer2Input.setDisable(true);
					}
					else{
						bothCustomersInput.setDisable(true);
						if(customer1 != null){
							customer1Input.setDisable(false);
						}
						if(customer2 != null){
							customer2Input.setDisable(false);
						}
					}
				}
				else{
					bothCustomersInput.setDisable(true);
					if(customer1 != null){
						customer1Input.setDisable(false);
					}
					if(customer2 != null){
						customer2Input.setDisable(false);
					}
				}
			}
			else{
				bothCustomersInput.setDisable(true);
				if(customer1 != null){
					customer1Input.setDisable(false);
				}
				if(customer2 != null){
					customer2Input.setDisable(false);
				}
			}
		});
		
		customer2Dialog.textProperty().addListener((obs, old, niu)->{
			String[] dialog1 = customer1Dialog.getText().split("\n");
			String[] dialog2 = customer2Dialog.getText().split("\n");
			if(dialog1.length > 0 && dialog2.length > 0){
				String[] lastLine1 = dialog1[dialog1.length - 1].split(": ", 2);
				String[] lastLine2 = dialog2[dialog2.length - 1].split(": ", 2);
				if(lastLine1.length == 2 && lastLine2.length == 2){
					if(lastLine1[1].equals(lastLine2[1]) && !lastLine1[0].equals(username)
							&& !lastLine2[0].equals(username)){
						bothCustomersInput.setDisable(false);
						customer1Input.setDisable(true);
						customer2Input.setDisable(true);
					}
					else{
						bothCustomersInput.setDisable(true);
						if(customer1 != null){
							customer1Input.setDisable(false);
						}
						if(customer2 != null){
							customer2Input.setDisable(false);
						}
					}
				}
				else{
					bothCustomersInput.setDisable(true);
					if(customer1 != null){
						customer1Input.setDisable(false);
					}
					if(customer2 != null){
						customer2Input.setDisable(false);
					}
				}
			}
			else{
				bothCustomersInput.setDisable(true);
				if(customer1 != null){
					customer1Input.setDisable(false);
				}
				if(customer2 != null){
					customer2Input.setDisable(false);
				}
			}
		});
		
		//Positioning of components
		customer1Input.addRow(0, inputForCustomer1, sendToCustomer1);
		customer2Input.addRow(0, inputForCustomer2, sendToCustomer2);
		bothCustomersInput.addRow(0, inputForBoth, sendToBoth);
		
		result.add(quitHBox, 0, 0, 3, 1);
		result.add(customer1Label, 0, 1);
		result.add(customer1Dialog, 0, 2);
		result.add(customer1Input, 0, 3);
		
		result.add(customer2Label, 2, 1);
		result.add(customer2Dialog, 2, 2);
		result.add(customer2Input, 2, 3);
		
		result.add(bothCustomersInput, 0, 4, 3, 1);
		
		return new Scene(result, 600, 510);
	}

}
