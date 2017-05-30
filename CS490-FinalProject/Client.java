package src;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Scanner;
import java.io.File;


public class Client {

    static String clientName;
    static String clientIP;
    static String dirServerIp;
    static RDTReceiver clientReceiver;
    static RDTSender clientSender;
    static P2PServer p2pServer;
    static P2PClient p2pClient;
    static String directoryPath;
    static File[] fList;
    static boolean slowMode;
    static String messageType;
    static String[] queryResults;
    static int startingPort = 2011;
    static boolean done = false;

    public static void main(String[] args) {
        try {
            // Get name and IP info
            initClient();
            acceptPeerConnections();
            
            while(!done) {
                // Communicate with directory server
                String cMsg = selectMessage();
                String rcvMsg = sendAndGetResponse(cMsg);
                handleResponse(rcvMsg);
              
                // If client has files to obtain, allow P2P connection
		if(queryResults != null) {
                    Scanner scan = new Scanner(System.in);
                    boolean connectWithPeers = true;
                    while (connectWithPeers) {
                        System.out.println("Connect to peer?(y/n)");
                        String choice = scan.nextLine();
                        if (choice.equalsIgnoreCase("Y")) {
                            connectToPeer();
                        } else connectWithPeers = false;
                    }
                }		
                if(cMsg.substring(0, cMsg.indexOf(" ")).equals("EXIT"))
                    done = true;
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void initClient() {
        try{
            Scanner scan = new Scanner(System.in);
            System.out.println("Run in slow mode? (Y/N)");
            String choice = scan.nextLine();
            if(choice.equalsIgnoreCase("Y"))
                slowMode = true;
            else
                slowMode = false;
	
            // Get client name and ip
            clientName = InetAddress.getLocalHost().getHostName();
            directoryPath = getDirectoryPath();
            updateDirectory(); 
            clientName = clientName.concat("-P2Pclient");
            System.out.println("Enter your IP");
            clientIP = scan.nextLine();

			
            System.out.println("Enter the IP of the Directory Server:");
            dirServerIp = scan.nextLine();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
    
    public static void acceptPeerConnections() {
        p2pServer = new P2PServer("P2Pserver", startingPort, directoryPath);
        p2pServer.start();    
    }
    
    public static void connectToPeer() {
        printQueryResults();
        Scanner scan = new Scanner(System.in);

        String hostIP = "";
        String filename = "";

        while (hostIP.equals("")) {
            System.out.println("Type the file name you want to obtain from the results above");
            filename = scan.nextLine();

            String fileInfo[];

            // line format - fileName fileSize hostName hostIP
            for (String line : queryResults) {
                fileInfo = line.split(" ");
                if (fileInfo[0].equalsIgnoreCase(filename)) {
                    hostIP = fileInfo[3];
                }
            }
            if(hostIP.equals(""))
                System.out.println("Host with that file not found.");
        }

            
        
        p2pClient = new P2PClient("Client", startingPort, directoryPath);
        p2pClient.setFileRequest(hostIP, filename);
        p2pClient.start();       
        
    }
    
    public static String sendAndGetResponse(String sendMsg) {
        String rcvMsg = "";
        try {
            // Thread to receive server's response
            clientReceiver = new RDTReceiver(clientName, startingPort+1000, slowMode);
            // Thread to send client's message
            clientSender = new RDTSender(clientName, dirServerIp, startingPort, slowMode);
            clientSender.rdtSend(sendMsg);
            clientReceiver.start();
            
            boolean doneReceiving = false;
            while(!doneReceiving) {
                Thread.sleep(1000);
                if(clientReceiver.doneReceiving()) {
                    rcvMsg = clientReceiver.deliverData();
                    clientReceiver.stopThread(); 
                    doneReceiving = true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rcvMsg;
    }
    
    
    
    public static String selectMessage() {
        int menuSelection = 0;
        Scanner scan = new Scanner(System.in);

        String message, filename;

        System.out.println("Please choose a menu option:");
        System.out.println("\t1. Inform and Update\n\t2. Query\n\t3. Exit");
        menuSelection = scan.nextInt();
        scan.nextLine();
        
        //System.out.println("Sending message to IP: " + serverIP);
        switch (menuSelection) {
            case 1: 
                messageType = "INFORM";
                message = "INFORM " + clientName + " " + clientIP + "\r\n" + getDirectoryContents(directoryPath);
                // Debug - System.out.println("MSG BEING SENT: " + message);
                break;
            case 2:
                messageType = "QUERY";
                System.out.println("Would you like a directory listing (Y/N)");
                String choice = scan.nextLine();
                if(choice.equalsIgnoreCase("Y"))
                    message = "QUERY " + clientName + " " + clientIP + " \r\n" + " \r\n";
                else {
                    System.out.println("Enter the file name you are searching for");
                    filename = scan.nextLine();
                    message = "QUERY " + clientName + " " + clientIP + "\r\n" + filename + "\r\n";
                }
                break;
            case 3:
                messageType = "EXIT";
                message = "EXIT " + clientName + " " + clientIP + "\r\n";
                break;
            default:
                messageType = "EXIT";
                message = "EXIT " + clientName + " " + clientIP + "\r\n";
                break;
        }

        return message;
    }
    
    public static void handleResponse(String response) {
        switch (messageType) {
            case "QUERY":
                // store query and print query results
                int headerEnd = response.indexOf("\n")+1;
                queryResults = response.substring(headerEnd, response.length()).split("\\r\\n");
                printQueryResults();
                break;
            default:
                // do nothing if response to INFORM or EXIT
                break; 
        }    
    }
    
    public static void printQueryResults() {
        System.out.println("Query Results");
        System.out.println("FORMAT - fileName fileSize hostName hostIP");
        for(String result : queryResults)
            System.out.println(result);
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
        
        public static String getDirectoryPath() {
            Scanner scan = new Scanner(System.in);
            System.out.println("Enter directory path: ");
            String path = scan.nextLine();
            return path;
        }
        
        public static void updateDirectory() {
        File fDir = new File(directoryPath);
        fList = fDir.listFiles();
        }
}
