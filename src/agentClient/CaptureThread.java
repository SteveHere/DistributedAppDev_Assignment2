package agentClient;

import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.sound.sampled.TargetDataLine;

import common.AudioFormatAndBufferSize;

public class CaptureThread extends Thread {

	private String IPAddress;
	private TargetDataLine targetDataLine;
	private byte tempBuffer[] = new byte[AudioFormatAndBufferSize.bufferSize];
	
	public CaptureThread(String IPAddress, TargetDataLine targetDataLine) {
		super();
		this.IPAddress = IPAddress;
		this.targetDataLine = targetDataLine;
	}

	@Override
	public void run() {
		ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        boolean stopaudioCapture = false;
        try {
            DatagramSocket clientSocket = new DatagramSocket(9090);
            InetAddress IPAddress = InetAddress.getByName(this.IPAddress);
            while (!stopaudioCapture) {
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
