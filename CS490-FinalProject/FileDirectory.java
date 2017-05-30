package src;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

;

import java.util.*;

public class FileDirectory {
    
    private class FileInfo {
        private String fileName;
        private String fileSize;
        private String hostname;
        private String hostIP;
    
        public FileInfo(String aFile,String size,String host, String ip) {
            this.fileName = aFile;
            this.fileSize = size;
            this.hostname = host;
            this.hostIP = ip;
        }
    }
    
    ArrayList<FileInfo> fileDir = new ArrayList<FileInfo>();
    
    public void addFile(String aFile,String size,String host, String ip) {
        fileDir.add(new FileInfo(aFile,size,host,ip));
    }
   
    public void addFiles(String[] files, String clientName, String clientIP)
    {
        String fileInfo[];
            for (String file : files) {
                fileInfo = file.split(" ");

                System.out.println("fileInfo[0] = " + fileInfo[0]);
                System.out.println("fileInfo[1] = " + fileInfo[1]);
                try {
                fileDir.add(new FileInfo(fileInfo[0], fileInfo[1], clientName, clientIP));
                } catch (Exception e) {
                    System.out.println(e);
                    e.printStackTrace();
                }
            }
    }
       
    public void removeFiles(String clientName)
    { 
        ArrayList<FileInfo> temp = new ArrayList<FileInfo>();
        for(FileInfo file : fileDir)
        {
            if(!file.hostname.equals(clientName))
                temp.add(file);
        }
        fileDir = temp;
        
    }
    
    // return string of fileDir from user
    public String findName(String aFile)
    {
        ArrayList<FileInfo> findList = new ArrayList<FileInfo>();
        String sHeader = "";
        
        //Add all FileInfo to temp Array
        for(int i = 0; i < fileDir.size(); i++)
        {
            if(fileDir.get(i).fileName.equals(aFile))
            {
                findList.add(fileDir.get(i));
            }
        }
        
        //Loop temp Array and build string
        for(int i = 0; i < findList.size();i++)
        {
            sHeader = sHeader + findList.get(i).fileName + " " + findList.get(i).fileSize + " "
                    + findList.get(i).hostname + " " + findList.get(i).hostIP+ "\r\n";
        }
        
        return sHeader;
    }
    
    public String getFiles() {
        String fileList = "";
        for(FileInfo fInfo : fileDir) {
            String line = "";
            line = line.concat(fInfo.fileName + " ");
            line = line.concat(fInfo.fileSize + " ");
            line = line.concat(fInfo.hostname + " ");
            line = line.concat(fInfo.hostIP + "\r\n");
            fileList = fileList.concat(line);
        }
        return fileList;
    }
            
}
