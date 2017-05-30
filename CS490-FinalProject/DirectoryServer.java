package src;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.net.InetAddress;
import java.util.Scanner;

public class DirectoryServer {

    static String serverName;
    static String serverIP;
    static RDTReceiver serverReceiver;
    static RDTSender serverSender;
    static boolean startResponse = false;
    static int startingPort = 2011;
    static boolean isSlowMode;
    static FileDirectory fileDir = new FileDirectory();

    public static void main(String[] args) {
        System.out.println("DIRECTORY SERVER STARTED");
        Scanner scan = new Scanner(System.in);
        System.out.println("Run in slow mode? (Y/N)");
        String choice = scan.nextLine();
        if(choice.equalsIgnoreCase("Y"))
            isSlowMode = true;
        else
            isSlowMode = false;
        
        try{
            serverName = InetAddress.getLocalHost().getHostName();
            serverName = serverName.concat("-DirServer");
            serverIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        
        while (true) {
            try {
                receiveAndRespond();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } 

    }
    
    public static String getDirectoryContents(String sDir)
    {
        
        File fDir = new File(sDir);
        File[] fList = fDir.listFiles();
        String sFileName, sFileSize, sFileType, sReturn;
        
        sReturn = "";
        
        for (File fList1 : fList) {
            sFileName = fList1.getName().replaceAll(" ", "_");
            sFileSize = "" + fList1.length();
            sFileType = sFileName.substring(sFileName.lastIndexOf(".") + 1);
            sReturn = sReturn + sFileName + " " + sFileSize + "\r\n";
        }
        return sReturn;
    }     
    
    public static String serverResponse(String receivedMsg) {
     
        
        String msgType, clientName, clientIP, headerlessMsg;
        String returnMsg = "";
        
        // Split header into variables for response msg
        int headerEnd = receivedMsg.indexOf("\r");
        String[] headerSections = receivedMsg.substring(0, headerEnd).split(" ");                
        msgType = headerSections[0].trim();
        clientName = headerSections[1];
        clientIP = headerSections[2];
        
        if(msgType.equals("EXIT")) {
            // Update directory 
            fileDir.removeFiles(clientName);
            // Create response
            returnMsg = "200 OK\r\n";           
        }
        else {
            
            // Ex. Format - "INFORM Andrew-PC 10.0.0.1\r\nFile1.txt 1024\r\nFile2.txt 512\r\n"
            if(msgType.equals("INFORM")) {
                headerlessMsg = receivedMsg.substring(headerEnd+2).trim();
                // Strip away the header and grab the file info from the message
                String[] splitMsg = headerlessMsg.split("\r\n");
                
                // Update directory
                fileDir.addFiles(splitMsg, clientName, clientIP);
               
                // Create response
                returnMsg = "200 OK\r\n";    
            }
            // Format - "QUERY Andrew-PC 10.0.0.1\r\nFile1.txt\r\n"
            // Format - "QUERY Andrew-PC 10.0.0.1\r\n \r\n"
            else if(msgType.equals("QUERY")) {
                headerlessMsg = receivedMsg.substring(headerEnd+2, receivedMsg.lastIndexOf("\r"));
                // Strip away the header and grab the file info from the message
                
                String[] splitMsg = headerlessMsg.split("\r\n");
                
                
                returnMsg = "200 OK\r\n";
                if(splitMsg[0].equals(" "))
                    returnMsg += fileDir.getFiles();
                else {
                    String fName = splitMsg[0];
                    returnMsg += fileDir.findName(fName);
                }
                return returnMsg;
            }
        }
        
        return returnMsg;
    }
    
    public static void receiveAndRespond() {
        try {
            
            boolean doneResponding = false;
            serverReceiver = new RDTReceiver(serverName, startingPort, isSlowMode);
            serverReceiver.start();

                String rcvMsg, clientIP;
                while (!doneResponding) {
                    Thread.sleep(1000); // Magical nap time
                    if (serverReceiver.doneReceiving()) {
                        rcvMsg = serverReceiver.deliverData();
                        clientIP = serverReceiver.getIp();
                        serverReceiver.stopThread();
                        System.out.println("DIRSRVR RECEIVED MSG: " + rcvMsg);
                        serverSender = new RDTSender(serverName, clientIP, startingPort+1000);
                        serverSender.rdtSend(serverResponse(rcvMsg));
                        doneResponding = true;
                    }
                }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
