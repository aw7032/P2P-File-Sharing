package src;
/**
 * Created by Owner on 5/5/2016.
 */



public class P2Ptest {

    public static void main(String[] args) {
        P2PServer serverThread = null;
        try {

            String directoryPath = "C:\\Users\\Owner\\Desktop\\client1Files";
            String destinationPath = "C:\\Users\\Owner\\Desktop\\testFileInfo";
            // Start server
            serverThread = new P2PServer("Server", 49000, directoryPath);
            serverThread.start();

            String serverIP = "10.0.0.4";
            System.out.println("serverIP value: " + serverIP);
            // Create client
            P2PClient client1 = new P2PClient("CLIENT1", 49000, destinationPath);
            client1.setFileRequest(serverIP, "C1-99.txt");
            client1.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}