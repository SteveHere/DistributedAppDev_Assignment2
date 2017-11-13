package customerClient;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

public class CustomerClient extends Application {

	static Socket socket;
	static BufferedReader fromServer;
	static PrintWriter toServer;
	static String username;
	static String agent = null;
	static boolean wantsToQuit = false;
	
	static Label agentName;
	static TextArea clientTextArea;
	static TextField clientText;
	static Button send;
	
	@Override
	public void start(Stage primaryStage){
		Stage stage = new Stage();
		String serverAddress = JOptionPane.showInputDialog(null,  "Enter IP Address of the CVT server: ", 
				"Enter CVT Server IP Address", JOptionPane.QUESTION_MESSAGE);

		try{
			boolean correctUsernameFormat = false; //assumed incorrect by default
			String input = null;

			do{
				input = loginPrompt();
				//input is only correct if the username has no tilde, or if nothing was entered
				correctUsernameFormat = (input == null) || !input.contains("~");
				if(correctUsernameFormat == false){
					JOptionPane.showMessageDialog(
							null, 
							"Incorrect username format detected. Please try again.", 
							"Incorrect Username Format", 
							JOptionPane.ERROR_MESSAGE
							);
				}
			} while(correctUsernameFormat == false);
			
			
			if(input == null){
				JOptionPane.showMessageDialog(null, 
						"Login aborted. Exiting program.", 
						"Login aborted", 
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
				
				//Main logic only starts when the server sends back "Login Authentication Required."
				if(initialResponse.equals("Client request required")){						
					toServer.println("Customer~" + input); 

					String response = fromServer.readLine();
					if(response.equals("Connection established")){
						username = new String(input);
						stage.setTitle("CVT Customer Client - User: " + username);
						stage.setScene(getCustomerApplicationScene());
						stage.setResizable(false);
						
						CustomerThread thread = new CustomerThread();
						thread.start();
						
						stage.showAndWait();
						if(CustomerClient.wantsToQuit == false){
							toServer.println("Force quit");
						}
					}
					else if(response.equals("Username already taken")){
						JOptionPane.showMessageDialog(null, 
								"Username has already been taken. Exiting program.", 
								"Username Already Taken", 
								JOptionPane.INFORMATION_MESSAGE
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
	
	private static String loginPrompt(){
		JTextField username = new JTextField();

		JPanel myPanel = new JPanel(new BorderLayout(0, 0));
		
		myPanel.add(new JLabel("Username: "), BorderLayout.WEST);
		myPanel.add(username, BorderLayout.CENTER);
		
		int result = JOptionPane.showOptionDialog(null, myPanel, 
				"CVT Customer: Enter Username", JOptionPane.OK_CANCEL_OPTION, 
				JOptionPane.QUESTION_MESSAGE, null, new String[]{"Connect", "Cancel"}, null);
		if (result == JOptionPane.OK_OPTION) {
			return username.getText();
		}
		return null;
	}
	
	private Scene getCustomerApplicationScene() {
		GridPane result = new GridPane();
		result.setPadding(new Insets(5));
		result.setVgap(5);
		result.setHgap(5);
		
		//Creation of components
		agentName = new Label("Connected To: ");
		
		Button quit = new Button("Quit");
		
		clientTextArea = new TextArea();
		
		clientText = new TextField ();
		
		send = new Button("Send");
		
		//Styling of components
		clientTextArea.setMinHeight(390);
		clientTextArea.setMinWidth(440);
		clientTextArea.setEditable(false);
		
		clientText.setMinWidth(420);
		clientText.setDisable(true);
		
		quit.setMinWidth(55);
		
		send.setMinWidth(55);
		send.setDisable(true);
		
		//Event handling
		send.setOnMouseClicked(e -> {
			if(!clientText.getText().equals("") && agent != null){
				clientTextArea.setText(clientTextArea.getText() 
						+ username + ": " + clientText.getText() + "\n");
				clientTextArea.positionCaret(clientTextArea.getLength());
				toServer.println(agent + "~" + clientText.getText());
				toServer.flush();
				clientText.setText("");
			}
		});
		
		quit.setOnMouseClicked(e -> {
			toServer.println("Quit");
			wantsToQuit = true;
		});
		
		//Positioning of components
		result.add(agentName, 0, 0);
		result.add(quit, 1, 0);
		result.add(clientTextArea, 0, 1, 2, 1);
		result.add(clientText, 0, 2);
		result.add(send, 1, 2);
		
		return new Scene(result, 480, 450);
	}

}
