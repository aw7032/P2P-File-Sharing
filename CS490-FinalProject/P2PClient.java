package src;

import java.io.*;
import java.net.*;

public class P2PClient extends Thread {

    private int serverPort;
    private String serverIP;
    private String fileName;
    private String directory;

    public P2PClient(String name, int serverPort, String dir) {
        super(name);
        this.serverPort = serverPort;
        directory = dir;
    }

    /**
     * Start the thread to connect and begin sending
     */
    @Override
    public void run() {
        Socket clientSocket = null;
        try {
            String httpGet = "GET " + fileName + " HTTP/1.1\r\n";
            String getResponse;
            String fileLength;
            System.out.println("CLIENT opening socket");
            clientSocket = new Socket(serverIP, serverPort);
            System.out.println("CLIENT connected to P2P Server");
            DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToServer.writeBytes(httpGet);
            getResponse = inFromServer.readLine();
            fileLength = inFromServer.readLine();



            fileLength = fileLength.split(" ")[1];
            System.out.println("File Size of Download: " + fileLength + " bytes");
            InputStream is = clientSocket.getInputStream();
            byte[] fileBytes = new byte[Integer.parseInt(fileLength)];

            // writes the file to this peers directory with the same file name
            FileOutputStream fos = new FileOutputStream(directory + "\\" + fileName);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            int bytesRead = is.read(fileBytes, 0, fileBytes.length);
            bos.write(fileBytes, 0, bytesRead);
            bos.close();


            System.out.println(bytesRead + " bytes downloaded.");
            Thread.sleep(1500);
            System.out.println(fileName + " downloaded to your directory.");
            clientSocket.close();

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (clientSocket != null) {
                    clientSocket.close();
                }
            } catch (Exception cse) {
                // ignore exception here
            }
        }
    }

    public void setFileRequest(String hostIP, String filename) {
        serverIP = hostIP;
        fileName = filename;
    }
}
