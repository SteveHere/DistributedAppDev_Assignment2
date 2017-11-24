package agentClient;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.sound.sampled.*;
import javax.swing.*;

import common.AudioFormatAndBufferSize;
import common.Pair;
import common.SharedInterface;
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
	
	static SharedInterface rmiObject;
	static Socket socket;
	static BufferedReader fromServer;
	static PrintWriter toServer;
	static String multicastIPAddress;
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
	static HBox customer1VoiceChat;
	static HBox customer2VoiceChat;
	static HBox bothVoiceChat;
	
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
				Registry registry = LocateRegistry.getRegistry(serverAddress);
				rmiObject = (SharedInterface) registry.lookup("sharedObject");
				socket = new Socket(serverAddress, 9090);

				// Receiving or reading data from the socket
				fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				toServer = new PrintWriter(socket.getOutputStream(), true);
				
				String initialResponse = fromServer.readLine();
				
				//Main logic only starts when the server sends back "Client request required"
				if(initialResponse.equals("Client authentication required")){
					long token = rmiObject.agentAuth(input.getLeft(), input.getRight());
					toServer.println("Agent~" + token); 

					String response = fromServer.readLine();
					if(response.equals("Login successful")){
						username = new String(input.getLeft());
						stage.setTitle("CVT Agent Client - User: " + username);
						stage.setScene(getAgentApplicationScene());
						stage.setResizable(false);
						VOIPThread voip = new VOIPThread();
						voip.start();
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
				rmiObject.removeAgentMulticastAddress(username);
				stopAudioCapture = true;
				targetDataLine.close();
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
		
		Button startVoiceChatCust1 = new Button("Start voice chat"),
				stopVoiceChatCust1 = new Button("Stop voice chat ");
		
		customer1VoiceChat = new HBox(5, startVoiceChatCust1, stopVoiceChatCust1);
		
		Button startVoiceChatCust2 = new Button("Start voice chat"),
				stopVoiceChatCust2 = new Button("Stop voice chat");
		
		customer2VoiceChat = new HBox(5, startVoiceChatCust2, stopVoiceChatCust2);
		
		Button startVoiceChatBoth = new Button("Start voice chat with both"),
				stopVoiceChatBoth = new Button("Stop voice chat with both");
		
		bothVoiceChat = new HBox(5, startVoiceChatBoth, stopVoiceChatBoth);
		
		//Styling of components
		quitHBox.setAlignment(Pos.CENTER);
		
		customer1Dialog.setMinHeight(400);
		customer1Dialog.setMaxWidth(295);
		customer1Dialog.setWrapText(true);
		customer1Dialog.setEditable(false);
		customer2Dialog.setMinHeight(400);
		customer2Dialog.setMaxWidth(295);
		customer2Dialog.setWrapText(true);
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
		
		stopVoiceChatCust1.setDisable(true);
		customer1VoiceChat.setDisable(true);
		customer1VoiceChat.setAlignment(Pos.CENTER);
		
		stopVoiceChatCust2.setDisable(true);
		customer2VoiceChat.setDisable(true);
		customer2VoiceChat.setAlignment(Pos.CENTER);
		
		stopVoiceChatBoth.setDisable(true);
		bothVoiceChat.setDisable(true);
		bothVoiceChat.setAlignment(Pos.CENTER);
		
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
			TextAreaListener();
		});
		
		customer2Dialog.textProperty().addListener((obs, old, niu)->{
			TextAreaListener();
		});
		
		startVoiceChatCust1.setOnAction(e -> {
			startVoiceChatCust1.setDisable(true);
			try {
				captureAudio(rmiObject.getCustomerIPAddress(customer1));
			} catch (RemoteException e1) {
				JOptionPane.showMessageDialog(
						null, 
						"Exception encountered. Details: \n" + e1, 
						"Exception encountered", 
						JOptionPane.ERROR_MESSAGE
						);
				System.exit(1);
			}
			stopVoiceChatCust1.setDisable(false);
		});
		
		startVoiceChatCust2.setOnAction(e -> {
			startVoiceChatCust2.setDisable(true);
			try {
				captureAudio(rmiObject.getCustomerIPAddress(customer2));
			} catch (RemoteException e1) {
				JOptionPane.showMessageDialog(
						null, 
						"Exception encountered. Details: \n" + e1, 
						"Exception encountered", 
						JOptionPane.ERROR_MESSAGE
						);
				System.exit(1);
			}
			stopVoiceChatCust2.setDisable(false);
		});
		
		startVoiceChatBoth.setOnAction(e -> {
			startVoiceChatBoth.setDisable(true);
			captureMulticastAudio(multicastIPAddress);
			stopVoiceChatBoth.setDisable(false);
		});
		
		stopVoiceChatCust1.setOnMouseClicked(e -> {
			stopVoiceChatCust1.setDisable(true);
			stopAudioCapture = true;
			targetDataLine.close();
			startVoiceChatCust1.setDisable(false);
		});
		
		stopVoiceChatCust2.setOnMouseClicked(e -> {
			stopVoiceChatCust2.setDisable(true);
			stopAudioCapture = true;
			targetDataLine.close();
			startVoiceChatCust2.setDisable(false);
		});
		
		stopVoiceChatBoth.setOnMouseClicked(e -> {
			stopVoiceChatBoth.setDisable(true);
			stopAudioCapture = true;
			targetDataLine.close();
			startVoiceChatBoth.setDisable(false);
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
		
		result.add(customer1VoiceChat, 0, 5);
		result.add(customer2VoiceChat, 2, 5);
		
		result.add(bothVoiceChat, 0, 6, 3, 1);
		
		return new Scene(result);
	}

	private void TextAreaListener(){
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
	}

	ByteArrayOutputStream byteOutputStream;
	TargetDataLine targetDataLine;
	public boolean stopAudioCapture = false;
	
	private void captureAudio(String customerIPAddress) {
		try {
	        AudioFormat adFormat = AudioFormatAndBufferSize.getAudioFormat();
	        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
	        targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
	        targetDataLine.open(adFormat);
	        targetDataLine.start();
	
	        Thread captureThread = new UnicastCaptureThread(customerIPAddress);
	        captureThread.start();
	    } catch (Exception e) {
	        System.err.println(e.getMessage());
	        System.exit(0);
	    }
	}
	
	private void captureMulticastAudio(String customerIPAddress) {
		try {
	        AudioFormat adFormat = AudioFormatAndBufferSize.getAudioFormat();
	        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
	        targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
	        targetDataLine.open(adFormat);
	        targetDataLine.start();
	
	        Thread captureThread = new MulticastCaptureThread(multicastIPAddress);
	        captureThread.start();
	    } catch (Exception e) {
	        System.err.println(e.getMessage());
	        System.exit(0);
	    }
	}
	
	public class UnicastCaptureThread extends Thread {
		private String IPAddress;
		private byte tempBuffer[] = new byte[AudioFormatAndBufferSize.bufferSize];
		
		public UnicastCaptureThread(String IPAddress) {
			super();
			this.IPAddress = IPAddress;
		}

		@Override
		public void run() {
			stopAudioCapture = false;
	        try {
	            DatagramSocket clientSocket = new DatagramSocket(10000);
	            InetAddress IPAddress = InetAddress.getByName(this.IPAddress);
	            while (!stopAudioCapture) {
	                int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
	                if (cnt > 0) {
	                    DatagramPacket sendPacket = new DatagramPacket(tempBuffer, tempBuffer.length, IPAddress, 9093);
	                    clientSocket.send(sendPacket);
	                }
	            }
	            byteOutputStream.close();
	            clientSocket.close();
	        } catch (Exception e) {
	            System.out.println("CaptureThread::run()" + e);
	            System.exit(0);
	        }
		}
	}
	
	public class MulticastCaptureThread extends Thread {
		private String IPAddress;
		private byte tempBuffer[] = new byte[AudioFormatAndBufferSize.bufferSize];
		
		public MulticastCaptureThread(String IPAddress) {
			super();
			this.IPAddress = IPAddress;
		}

		@Override
		public void run() {
			stopAudioCapture = false;
	        try {
	        	MulticastSocket multicastSocket = new MulticastSocket(10002);
	            InetAddress multicastgroupAddress = InetAddress.getByName(this.IPAddress);
	            while (!stopAudioCapture) {
	                int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
	                if (cnt > 0) {
	                	DatagramPacket packet = new DatagramPacket(tempBuffer, tempBuffer.length, multicastgroupAddress, 9094);
	                    multicastSocket.send(packet);
	                }
	            }
	            byteOutputStream.close();
	            multicastSocket.close();
	        } catch (Exception e) {
	            System.out.println("CaptureThread::run()" + e);
	            System.exit(0);
	        }
		}
	}	
	
	AudioInputStream InputStream;
	SourceDataLine sourceLine;
	
	public class VOIPThread extends Thread {
		public void run() {
		    try {
		        @SuppressWarnings("resource")
				DatagramSocket serverSocket = new DatagramSocket(9092);
		        byte[] receiveData = new byte[AudioFormatAndBufferSize.bufferSize];
		        while (true) {
		            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		            serverSocket.receive(receivePacket);
		            try {
		                byte audioData[] = receivePacket.getData();
		                InputStream byteInputStream = new ByteArrayInputStream(audioData);
		                AudioFormat adFormat = AudioFormatAndBufferSize.getAudioFormat();
		                InputStream = new AudioInputStream(byteInputStream, adFormat, audioData.length / adFormat.getFrameSize());
		                DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, adFormat);
		                sourceLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
		                sourceLine.open(adFormat);
		                sourceLine.start();
		                Thread playThread = new PlayThread();
		                playThread.start();
		            } catch (Exception e) {
		                System.out.println(e);
		                System.exit(0);
		            }
		        }
		    } catch (Exception e) {
		        e.printStackTrace();
		    }
		}
	}
	
	class PlayThread extends Thread {	
	    byte tempBuffer[] = new byte[AudioFormatAndBufferSize.bufferSize];
	    
	    public void run() {
	        try {
	            int cnt;
	            while ((cnt = InputStream.read(tempBuffer, 0, tempBuffer.length)) != -1) {
	                if (cnt > 0) {
	                    sourceLine.write(tempBuffer, 0, cnt);
	                }
	            }
	        } catch (Exception e) {
	            System.out.println(e);
	            System.exit(0);
	        }
	    }
	}
}
