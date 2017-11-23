package customerClient;

import java.awt.BorderLayout;
import java.io.*;
import java.net.*;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.sound.sampled.*;
import javax.swing.*;

import common.AudioFormatAndBufferSize;
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

public class CustomerClient extends Application {

	static SharedInterface rmiObject;
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
				Registry registry = LocateRegistry.getRegistry(serverAddress);
				rmiObject = (SharedInterface) registry.lookup("sharedObject");
				socket = new Socket(serverAddress, 9090);

				// Receiving or reading data from the socket
				fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				toServer = new PrintWriter(socket.getOutputStream(), true);
				
				String initialResponse = fromServer.readLine();
				
				//Main logic only starts when the server sends back "Login Authentication Required."
				if(initialResponse.equals("Client authentication required")){
					long token = rmiObject.customerAuth(input);
					if(token != -1){
						toServer.println("Customer~" + token); 

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
						else{
							JOptionPane.showMessageDialog(null, 
									"User has not yet authenticated. Exiting program.", 
									"User Not Yet Authenticated", 
									JOptionPane.INFORMATION_MESSAGE
									);
							System.exit(0);
						}
					}
					else{
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
		
		Button startVoiceChat = new Button("Start Voice Chat"),
				stopVoiceChat = new Button("Stop Voice Chat");
		
		HBox VoiceChatHBox = new HBox(5, startVoiceChat, stopVoiceChat);
		
		//Styling of components
		clientTextArea.setMinHeight(390);
		clientTextArea.setMinWidth(440);
		clientTextArea.setEditable(false);
		
		clientText.setMinWidth(420);
		clientText.setDisable(true);
		
		quit.setMinWidth(55);
		
		send.setMinWidth(55);
		send.setDisable(true);
		
		stopVoiceChat.setDisable(true);
		
		VoiceChatHBox.setAlignment(Pos.CENTER);
		
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
		
		startVoiceChat.setOnMouseClicked(e -> {
			startVoiceChat.setDisable(true);
			try {
				captureAudio(rmiObject.getAgentIPAddress(agent));
			} catch (RemoteException e1) {
				JOptionPane.showMessageDialog(
						null, 
						"Exception encountered. Details: \n" + e1, 
						"Exception encountered", 
						JOptionPane.ERROR_MESSAGE
						);
				System.exit(1);
			}
			stopVoiceChat.setDisable(false);
		});
		
		stopVoiceChat.setOnMouseClicked(e -> {
			stopVoiceChat.setDisable(true);
			stopAudioCapture = true;
			targetDataLine.close();
			startVoiceChat.setDisable(false);
		});
		
		//Positioning of components
		result.add(agentName, 0, 0);
		result.add(quit, 1, 0);
		result.add(clientTextArea, 0, 1, 2, 1);
		result.add(clientText, 0, 2);
		result.add(send, 1, 2);
		result.add(VoiceChatHBox, 0, 3, 2, 1);
		
		return new Scene(result);
	}
	
	ByteArrayOutputStream byteOutputStream;
	TargetDataLine targetDataLine;
	AudioInputStream InputStream;
	SourceDataLine sourceLine;
	public boolean stopAudioCapture = false;
	
	private void captureAudio(String customerIPAddress) {
		try {
	        AudioFormat adFormat = AudioFormatAndBufferSize.getAudioFormat();
	        DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, adFormat);
	        targetDataLine = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
	        targetDataLine.open(adFormat);
	        targetDataLine.start();
	
	        Thread captureThread = new CaptureThread(customerIPAddress);
	        captureThread.start();
	    } catch (Exception e) {
	        System.err.println(e.getMessage());
	        System.exit(0);
	    }
	}
	
	public void runVOIP() {
	    try {
	        @SuppressWarnings("resource")
			DatagramSocket serverSocket = new DatagramSocket(9091);
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
	                Thread playThread = new Thread(new PlayThread());
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
	
	public class CaptureThread extends Thread {
		private String IPAddress;
		private byte tempBuffer[] = new byte[AudioFormatAndBufferSize.bufferSize];
		
		public CaptureThread(String IPAddress) {
			super();
			this.IPAddress = IPAddress;
		}

		@Override
		public void run() {
			ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
	        stopAudioCapture = false;
	        try {
	            DatagramSocket clientSocket = new DatagramSocket(9090);
	            InetAddress IPAddress = InetAddress.getByName(this.IPAddress);
	            while (!stopAudioCapture) {
	                int cnt = targetDataLine.read(tempBuffer, 0, tempBuffer.length);
	                if (cnt > 0) {
	                    DatagramPacket sendPacket = new DatagramPacket(tempBuffer, tempBuffer.length, IPAddress, 9091);
	                    clientSocket.send(sendPacket);
	                    byteOutputStream.write(tempBuffer, 0, cnt);
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
