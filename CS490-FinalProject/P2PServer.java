package src;

import java.io.*;
import java.net.*;

public class P2PServer extends Thread {

    private int port;
    private String directory;
    private File[] fList;

    public P2PServer(String name, int port, String dir) {
        super(name);
        this.port = port;
        directory = dir;
        updateFileList();
    }

    /**
     * Start the thread to begin listening
     */
    public void run() {
        ServerSocket serverSocket = null;
        try {
            String receivedMsg;
            String responseMsg;
            serverSocket = new ServerSocket(this.port);
            while (true) {
                updateFileList();
                System.out.println("P2P SERVER accepting connections");
                Socket clientConnectionSocket = serverSocket.accept();
                System.out.println("P2P Server accepted connection to " + clientConnectionSocket.getInetAddress().getHostAddress());
                // This is regarding the server state of the connection
                while (clientConnectionSocket.isConnected() && !clientConnectionSocket.isClosed()) {
                    BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientConnectionSocket.getInputStream()));
                    DataOutputStream outToClient = new DataOutputStream(clientConnectionSocket.getOutputStream());
                    receivedMsg = inFromClient.readLine();
                    // Note if this returns null it means the client closed the connection
                    if (receivedMsg != null) {

                        String[] msgParts = receivedMsg.split(" ");
                        String fileName = msgParts[1];
                        long fileLength = 0;
                        
                        for(File file : fList) {
                            if(file.getName().equals(fileName))
                                fileLength = file.length();
                        }
                         

                        responseMsg = "HTTP/1.1 200 OK\r\nContent-Length: " + fileLength + " \r\n";
                        outToClient.writeBytes(responseMsg);

                        File myFile = new File(directory + "\\" + fileName);
                        byte[] fileBytes = new byte[(int)fileLength];
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
                        bis.read(fileBytes, 0, fileBytes.length);
                        OutputStream os = clientConnectionSocket.getOutputStream();
                        os.write(fileBytes, 0, fileBytes.length);
                        os.flush();

                    } else {
                        clientConnectionSocket.close();
                        System.out.println("P2P Server client connection closed");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    public void updateFileList() {
        File fDir = new File(directory);
        fList = fDir.listFiles();
    }
}
