package src;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TimerTask;
import java.util.Timer;

public class RDTSender extends Thread {

    private final int MTU = 15;                // maximum transfer unit size
    private final int RDT_HEADER_SIZE = 6;      // size of the RDT header (bytes)
    private final double ALPHA = 0.125;         
    private final double BETA = 0.25;           
    
    private int serverPort = 0;
    private DatagramSocket socket = null;
    private InetAddress internetAddress = null;
    private int state = 0;
    private String clientName;
    private String message;
    private String name;
    private ArrayList<String> packets;
    private double startTime;
    private double estimatedRTT = 100.0; // 100 ms
    private double devRTT = 0.0;
    private double timeoutInterval = 1000.0; // 1 sec
    private boolean timeoutOccured = false;
    private boolean finalPktSent = false;
    private int numPackets;
    private int currentPktIndex = 0;
    private int expectedACKnum = 0;
    private boolean isSlowMode;
    private DatagramPacket rcvpkt;
    private Timeout timeout;
    
    // Constructor for the RDTSender class
    public RDTSender(String name, String ip, int serverPort) throws Exception {
        super(name);
        this.name = name;
        this.clientName = name;
        this.serverPort = serverPort;
        this.isSlowMode = false;
        socket = new DatagramSocket();
        internetAddress = InetAddress.getByName(ip);
        packets = new ArrayList<String>();
    }
    
    public RDTSender(String name, String ip, int serverPort, boolean isSlow) throws Exception {
        super(name);
        this.name = name;
        this.clientName = name;
        this.serverPort = serverPort;
        this.isSlowMode = isSlow;
        socket = new DatagramSocket();
        internetAddress = InetAddress.getByName(ip);
        packets = new ArrayList<String>();
    }


    // Starts the thread to connect and begin sending
    @Override
    public void run() {
        System.out.println(this.clientName + " RDTSender connected.");
        createPackets(message);
        
        for(int i=0; i<packets.size();i++)
               System.out.println(packets.get(i));
        while(currentPktIndex < numPackets) {
            try {
               
                evalCurrentState();

                if(currentPktIndex >= numPackets-1){
                    finalPktSent = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
                try {
                    if (socket != null) {
                        socket.close();
                    }
                } catch (Exception cse) {
                    // ignore exception here
                }
            }

        }
        stopThread();

    }

    // breaks down data into packets and appends RDT headers
    public void createPackets(String data) {

        try {
        String seqNum;
        // calculates the size allowed for data within each packet
        int sizeWithoutHeader = MTU - RDT_HEADER_SIZE;
        // calculates the number of packets required to fit all data
        numPackets = (int) Math.ceil(data.length() / (float) sizeWithoutHeader);

        String lastPktNum = padHeader((numPackets-1)+"");
        String packet;

        for (int i = 0; i < numPackets; i++) {
            seqNum = i + "";
            seqNum = padHeader(seqNum);
            if ((i + 1) * sizeWithoutHeader > message.length()) // last packet < max packet size
                packet = seqNum.concat(lastPktNum).concat(message.substring(i * sizeWithoutHeader, message.length()));
            else {
                packet = seqNum.concat(lastPktNum).concat(message.substring(i * sizeWithoutHeader, (i + 1) * sizeWithoutHeader));
            }
            packets.add(packet);
        }
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        

    }

    public class Timeout {

        Timer timer;

        public Timeout(long timeoutInterv) {
            timeoutOccured = false; // reset timeout flag
            timer = new Timer();
            startTime = System.nanoTime(); // startTime in nanoseconds
            timer.schedule(new TimeoutTask(), timeoutInterv);
        }

        class TimeoutTask extends TimerTask {

            public void run() {
                timeoutOccured = true;
                timeoutInterval *= 2;
                //calcTimeoutInterval(currentTimeout);
                System.out.println("Timeout Occurred");
                timer.cancel(); //Terminate the timer thread
            }
        }
    }

    public void udtSend(String sendPacket) throws Exception {
        if(isSlowMode)
            Thread.sleep(4000);
        System.out.println("Sending Packet " + (currentPktIndex+1) + " of " + numPackets
                + " with sequence number " + expectedACKnum + " being sent, current timeout: "
                + timeoutInterval + " ms");
        byte[] packetData = new byte[sendPacket.length()];
        packetData = sendPacket.getBytes();
        DatagramPacket packet = new DatagramPacket(packetData, packetData.length, internetAddress, serverPort);
        socket.send(packet);

    }

    
    public boolean startReceiving(boolean bool) {
        return bool;
    }
    
    public boolean rdtRcv() throws Exception {
        byte[] ackBuf = new byte[16];
        rcvpkt = new DatagramPacket(ackBuf, ackBuf.length);
        socket.receive(rcvpkt);
        return true;
    }

    public void startTimer() {
        
        if(timeout != null) // Stop previous timer if it exists
            stopTimer();
        timeout = new Timeout((long) timeoutInterval);
    }

    public void stopTimer() {
        timeout.timer.cancel();
    }
    
    // Returns RTT in milliseconds
    public double elapsedTime() {
        return (System.nanoTime() - startTime) / 1000000.0;
    }
    // checks if received ACK matches expected
    public boolean isACK(int expectedACK) {
        String packet = new String(rcvpkt.getData());
        return packet.contains(""+expectedACK);
    }
    
    // Logic based on state diagram for RDT sender
    public void evalCurrentState() throws Exception {
        if (state == 0) {    // Wait for call 0 from above
            String packet = packets.get(currentPktIndex);
            udtSend(packet);
            startTimer();
            state = 1;
        }
        else if (state == 1) {		// Wait for ACK0
            if (timeoutOccured) {
                String packet = packets.get(currentPktIndex);
                udtSend(packet);
                startTimer();
            }
            else if(rdtRcv() && isACK(expectedACKnum) && !timeoutOccured) { // received correct ACK
                System.out.println("ACK received with sequence number " + expectedACKnum);
                System.out.println("Observed RTT: " + elapsedTime() + "ms");
                calcTimeoutInterval(elapsedTime());
                stopTimer();
                currentPktIndex++;
                expectedACKnum++; 
                state = 2;
            }
        }
        else if (state == 2) {   // Wait for call 1 from above
            String packet = packets.get(currentPktIndex);
            udtSend(packet);
            startTimer();
            state = 3;
        }
        else if (state == 3) {
            if (timeoutOccured) {
                String packet = packets.get(currentPktIndex);
                udtSend(packet);
                startTimer();
            }
            else if (rdtRcv() && isACK(expectedACKnum) && !timeoutOccured) { // received correct ACK
                System.out.println("ACK received with sequence number " + expectedACKnum);
                System.out.println("Observed RTT: " + elapsedTime() + "ms");
                calcTimeoutInterval(elapsedTime());
                stopTimer();
                currentPktIndex++;
                expectedACKnum++; 
                state = 0;
            }
        }
    }


    // sampleRTT = RTT in milliseconds
    public void calcTimeoutInterval(double sampleRTT) {
        double EstimatedRTT, DevRTT;
        EstimatedRTT = (1 - ALPHA) * estimatedRTT + ALPHA * sampleRTT;
        DevRTT = (1 - BETA) * devRTT + BETA * Math.abs(sampleRTT - EstimatedRTT);
        timeoutInterval = EstimatedRTT + 4 * DevRTT; // 
        estimatedRTT = EstimatedRTT;
        devRTT = DevRTT;

    }
    
    public boolean doneSending() {
        return finalPktSent;
    }
    
    // pads sequence numbers to meet size criteria
    public String padHeader(String seqNum) {
        int numSpaces = 3-seqNum.length();
        //Pad
        for(int i = 0; i < numSpaces; i++) {
            seqNum = seqNum.concat(" ");
        }
        
        return seqNum; // return 3 byte string padded with spaces
    }
    
    // starts the sender to transfer given msg
    public void rdtSend(String msg) {
        message = msg;
        this.start();
    }
    
    public void stopThread() {
        socket.close();
    }
}