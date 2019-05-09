/**
 * @author Alp Ege Basturk
 * Custom multi-threaded FTP server
 * */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomFTPServer
{
    final static String CR = "\r";
    final static String LF = "\n";
    final static int POSITIVE_RESULT = 1;
    final static int NEGATIVE_RESULT = 1;
    final static int myFTPPort = 22222;
    final static String localhost = "localhost";
    ServerSocket ftpListenerSocket;
    Socket serverDataSocket;
    boolean terminateFlag;

    public CustomFTPServer()
    {
        terminateFlag = false;
        try {
            ftpListenerSocket = new ServerSocket(myFTPPort);
            serverDataSocket = new Socket();
            serverDataSocket.close();
        }catch (IOException ioe)
        {
            ioe.printStackTrace();
        }

    }
    public void initServer()
    {
        Thread thread = null;
        ExecutorService executorService = Executors.newCachedThreadPool();
        while (true)
        {
            try {
                Socket clientSocket = ftpListenerSocket.accept();
                if (clientSocket != null) {
                    MyRunnable myRunnable = new MyRunnable(clientSocket);
                    /*thread = new Thread(myRunnable);
                    thread.start();*/
                    executorService.execute(myRunnable);
                }
            }catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    private class MyRunnable implements Runnable
    {
        Socket clientControlSocket;
        Socket clientDataSocket;
        OutputStreamWriter outToClient;
        boolean dataConnectionExistsFlag;
        String command;
        InputStreamReader inputStreamReader;
        BufferedReader bufferedReader;
        int myDataPortNumber;

        Path currentRelativePath;
        public MyRunnable(Socket clientSocket)
        {
            this.clientControlSocket = clientSocket;
            currentRelativePath = Paths.get("");
            dataConnectionExistsFlag = false;
            try {
                inputStreamReader =
                        new InputStreamReader(clientControlSocket.getInputStream());
                bufferedReader =
                        new BufferedReader(inputStreamReader);
            }catch (Exception e)
            {
                System.out.println("THREAD:Exception at constructor");
            }
        }
        @Override
        public void run() {
            System.out.println("Thread run");
            while (!terminateFlag)
            {
                List<String> clientRequestSplittedList = readFromControlPort();
                if (clientRequestSplittedList != null) {
                    if (clientRequestSplittedList.get(0).equals("QUIT")) {
                        handleQUIT();
                    }
                    else if (clientRequestSplittedList.get(0).equals("PORT")) {
                        handlePORT(clientRequestSplittedList);
                    }
                    else if (clientRequestSplittedList.get(0).equals("CDUP")) {
                        handleCDUP();
                        System.out.println(currentRelativePath.toString());
                    }
                    else if (clientRequestSplittedList.get(0).equals("GPRT"))
                    {
                        sendSuccessResponse();
                    }
                    else if (clientRequestSplittedList.get(0).equals("NLST")) {
                        handleNLST();
                    }
                    else if (clientRequestSplittedList.get(0).equals("CWD")) {
                        handleCWD(clientRequestSplittedList);
                        System.out.println(currentRelativePath.toString());
                    }
                    else if (clientRequestSplittedList.get(0).equals("PUT"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("MKDR")) {
                        handleMKDR(clientRequestSplittedList);
                    }
                    else if (clientRequestSplittedList.get(0).equals("RETR"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("DELE")) {
                        handleDELE(clientRequestSplittedList);
                    }
                    else if (clientRequestSplittedList.get(0).equals("DDIR")) {
                        handleDDIR(clientRequestSplittedList);
                    }
                    else {
                        sendFailResponse();
                    }
                }
            }
            terminateThread();
        }
        private void handleDELE(List<String> clientRequestSplittedList) {
            Path currentPath = currentRelativePath;
            Path childPath = currentPath.resolve(currentPath.toString() + "/" + clientRequestSplittedList.get(1));
            File fileTmp = new File(childPath.toString());
            if (fileTmp.exists()) {
                try {
                    Files.delete(childPath);
                    sendSuccessResponse();
                }catch (IOException ioe) {
                    sendFailResponse();
                }
            }
            else{
                sendFailResponse();
            }
        }
        private void handleDDIR(List<String> clientRequestSplittedList) {
            Path currentPath = currentRelativePath;
            Path childPath = currentPath.resolve(currentPath.toString() + "/" + clientRequestSplittedList.get(1));
            File fileTmp = new File(childPath.toString());
            if (fileTmp.exists()) {
                deleteDirectoryRecursive(childPath);
                sendSuccessResponse();
            }
            else{
                sendFailResponse();
            }
        }
        private void handleCWD(List<String> clientRequestSplittedList) {
            Path currentPath = currentRelativePath;
            Path childPath = currentPath.resolve(currentPath.toString() + "/" + clientRequestSplittedList.get(1));
            File fileTmp = new File(childPath.toString());
            if (fileTmp.exists()) {
                currentRelativePath = childPath;
                sendSuccessResponse();
            }
            else{
                sendFailResponse();
            }
        }
        private void handleCDUP() {
            if (currentRelativePath.toString().equals("/")) {
                sendFailResponse();
            }
            else {
                currentRelativePath = currentRelativePath.getParent();
            }
        }
        private void handleMKDR(List<String> clientRequestSplittedList)
        {
            // Directory Logic
            String returnString = "";
            Path currentRelativePath = Paths.get("");
            try {
                File fileTmp = new File(currentRelativePath.toString() + "/" + clientRequestSplittedList.get(1));
                if (fileTmp.exists())
                    sendFailResponse();
                else {
                    fileTmp.mkdir();
                    sendSuccessResponse();
                }

            } catch (DirectoryIteratorException e) {
                System.err.println(e);
            }
            // End Directory Logic
        }
        private void handleQUIT()
        {
            System.out.println("THREAD: Got QUIT");
            terminateFlag = true;
            sendSuccessResponse();
        }
        private void handlePORT(List<String> clientRequestSplittedList)
        {
            try {
                clientDataSocket = new Socket(localhost, Integer.parseInt(clientRequestSplittedList.get(1)));
                myDataPortNumber = Integer.parseInt(clientRequestSplittedList.get(1));
                dataConnectionExistsFlag = true;
                System.out.println("THREAD: Data port created on port " + Integer.parseInt(clientRequestSplittedList.get(1)));
                sendSuccessResponse();
            }catch (Exception e)
            {
                sendFailResponse();
                e.printStackTrace();
            }
        }
        private void handleNLST()
        {
            //System.out.println("NLST Called");
            // Directory Logic
            String returnString = "";

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentRelativePath)) {
                boolean preventLastCRLFFlag = false;
                for (Path file: stream) {
                    if (!preventLastCRLFFlag)
                    {
                        preventLastCRLFFlag = true;
                    }
                    else
                        returnString = returnString + CR + LF;
                    //System.out.println(file.getFileName());
                    String fileType = "f";
                    if (Files.isDirectory(file))
                        fileType = "d";
                    returnString = returnString + file.getFileName() + ":"
                    + fileType;
                }
            } catch (IOException | DirectoryIteratorException e) {
                System.err.println(e);
            }
            // End Directory Logic
            // Send to client
            try {
                sendSuccessResponse();
                if (clientDataSocket.isClosed())
                    clientDataSocket = new Socket(localhost, myDataPortNumber);
                sendStringToPort(returnString, clientDataSocket);
                clientDataSocket.close();
            }catch (Exception e)
            {

            }
            sendSuccessResponse();
        }
        public void terminateThread()
        {
            try {
                clientControlSocket.close();
                clientControlSocket.close();
            }catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
            System.out.println("THREAD Terminating");

            try {
                clientControlSocket.close();
                clientDataSocket.close();
            }catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        public List<String> readFromControlPort()
        {
            try {
                String tmp = bufferedReader.readLine();
                List<String> splittedCommandList = Arrays.asList(tmp.split(" "));
                return splittedCommandList;
            }catch (IOException ioe)
            {
                System.out.println("THREAD: Couldn't read from control port");
                ioe.printStackTrace();
            }
            return null;
        }
        public void createDataConnection()
        {
            try {
                String tmp = bufferedReader.readLine();
                List<String> splittedCommandList = Arrays.asList(tmp.split(" "));
                String responseCode = splittedCommandList.get(0);

                if (responseCode.equals("PORT")) {
                    clientDataSocket = new Socket(localhost, Integer.parseInt(splittedCommandList.get(1)));
                    dataConnectionExistsFlag = true;
                    System.out.println("THREAD: Data port created on port " + Integer.parseInt(splittedCommandList.get(1)));
                    sendSuccessResponse();
                }
            }catch (IOException ioe)
            {
                System.out.println("THREAD: Cannot Create Data Connection");
                sendFailResponse();
            }
        }

        public int sendStringToPort(String str, Socket socket)
        {
            try {
                outToClient = new OutputStreamWriter(socket.getOutputStream(), "US-ASCII");
                outToClient.write(str, 0, str.length());
                outToClient.flush();
                return 0;
            } catch (IOException e)
            {
                e.printStackTrace();
                return NEGATIVE_RESULT;
            }
        }
        void deleteDirectoryRecursive(Path path) {
            try {
                if (Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
                    try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                        for (Path entry : entries) {
                            deleteDirectoryRecursive(entry);
                        }
                    }
                }
                Files.delete(path);
            }catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
        private void sendSuccessResponse() {
            sendStringToPort(200 + CR + LF, clientControlSocket);
        }
        private void sendFailResponse() {
            sendStringToPort(400 + CR + LF, clientControlSocket);
        }
    }
    public static void main(String args[])
    {
        CustomFTPServer customFTPServer = new CustomFTPServer();
        customFTPServer.initServer();
        System.exit(0);
    }
}
