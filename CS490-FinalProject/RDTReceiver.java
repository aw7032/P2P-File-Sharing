package src;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;

public class RDTReceiver extends Thread {

    private final int MTU = 15;
    private int port;
    private int state = 0;
    private int currentACKnum = 0;
    private int expectedSeqNum = 0;
    private int actualSeqNum;
    private int finalSeqNum = 0;
    private String receivedMsg;
    private String name;
    private boolean finalPktReceived = false;
    private boolean firstPktReceived = false;
    private boolean stop = false;
    private boolean isSlowMode;
    private DatagramSocket receivingSocket = null;
    private DatagramPacket rcvpkt;
    private InetAddress internetAddress = null;
    private ArrayList<String> receivedPackets;
    
	// constructor for RDT receiver
    public RDTReceiver(String name, int port) throws UnknownHostException {
        super(name);
        this.name = name;
        this.port = port;
        this.isSlowMode = false;
        receivedPackets = new ArrayList<String>();
    }
    
    public RDTReceiver(String name, int port, boolean isSlow) throws UnknownHostException {
        super(name);
        this.name = name;
        this.port = port;
        this.isSlowMode = isSlow;
        receivedPackets = new ArrayList<String>();
    }

    public void run() {

        try {
            int ACKnum = 0;
            receivingSocket = new DatagramSocket(this.port);
            while (!finalPktReceived) {

                System.out.println(getName() + "- Receiver waiting for packet");

                evalCurrentState();

                if (expectedSeqNum > finalSeqNum) {
                    recreateMessage();
                    System.out.println("Last Packet Received");
                    //System.out.println("Reassembled Message:\n" + deliverData());
                    finalPktReceived = true;
                }

            }

            while (!stop) {
                // Wait for call to stop
            }

        } catch (Exception e) {
            e.getMessage();
            e.printStackTrace();
            stopListening();
        }
    }

    public boolean doneReceiving() {
        return finalPktReceived;
    }

    public void recreateMessage() {

        // Extract the header from the message
        int numPackets = receivedPackets.size();

        String packet;
        String message = "";

        System.out.println("INSIDE recreateMessage()");
        for (int i = 0; i < numPackets; i++) {
            packet = receivedPackets.get(i);
            packet = packet.substring(6,packet.length());
            System.out.println("Packet " + i + ": " + packet);
            // Discard RDT header 
            message = message.concat(packet);
        }

        receivedMsg = message;

    }

    public void calcMsgLength() {
        String packet = new String(rcvpkt.getData());
        String seqNumString = packet.substring(3, 6).trim();
        finalSeqNum = new Integer(seqNumString);

    }

    public boolean rdtRcv() throws Exception {
        byte[] ackBuf = new byte[MTU];
        rcvpkt = new DatagramPacket(ackBuf, ackBuf.length);
        receivingSocket.receive(rcvpkt);
        return true;

    }

    public void udtSend(String sendPacket) throws Exception {
        if (isSlowMode) {
            Thread.sleep(4000);
        }
        System.out.println("Sending ACK with sequence number " + actualSeqNum);
        byte[] packetData = new byte[sendPacket.length()];
        packetData = sendPacket.getBytes();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, rcvpkt.getAddress(), rcvpkt.getPort());
        receivingSocket.send(packet);

    }

    public boolean hasSeq(int expectedSeq) {
        String packet = new String(rcvpkt.getData());
        // First 3 bytes are seq number
        String seqNum = packet.substring(0, 3).trim();
        actualSeqNum = new Integer(seqNum);
        return (actualSeqNum == expectedSeqNum);
    }

    public void evalCurrentState() throws Exception {

        if (state == 0) {
            if (rdtRcv()) {
                if (!hasSeq(expectedSeqNum)) {
                    String ACK = "ACK Packet " + actualSeqNum;
                    udtSend(ACK);
                } else if (hasSeq(expectedSeqNum)) {
                    System.out.println("Packet with sequence number " + actualSeqNum + " received.");
                    String ACK = "ACK Packet " + actualSeqNum;
                    deliverData(rcvpkt.getData());
                    udtSend(ACK);
                    expectedSeqNum++;
                    state = 1;
                    if (!firstPktReceived) {
                        firstPktReceived = true;
                        internetAddress = rcvpkt.getAddress();
                        calcMsgLength();
                    }
                }
            }
        } else if (state == 1) {
            if (rdtRcv()) {
                if (!hasSeq(expectedSeqNum)) {
                    String ACK = "ACK Packet " + actualSeqNum;
                    udtSend(ACK);
                } else if (hasSeq(expectedSeqNum)) {
                    String ACK = "ACK Packet " + actualSeqNum;
                    deliverData(rcvpkt.getData());
                    udtSend(ACK);
                    expectedSeqNum++;
                    state = 0;
                }

            }

        }
    }

    public void deliverData(byte[] data) {
        String receivedPacket = new String(data, 0, MTU);
        receivedPackets.add(receivedPacket);
    }

    public void stopListening() {
        if (receivingSocket != null) {
            receivingSocket.close();
        }
    }

    public String getIp() {
        return internetAddress.getHostAddress();
    }

    public String deliverData() {
        return receivedMsg;
    }

    public void stopThread() {
        stopListening();
        stop = true;
    }
}
