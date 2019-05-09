/**
 * @author Alp Ege Basturk
 * Custom multi-threaded FTP server
 *
 * GPRT does not work correctly
 * PUT is not implemented
 * */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomFTPServer
{
    final static String CR = "\r";
    final static String LF = "\n";
    final static int POSITIVE_RESULT = 1;
    final static int NEGATIVE_RESULT = 1;
    int myFTPPort;
    int serverDataPort;
    final static String localhost = "localhost";
    ServerSocket ftpListenerSocket;
    Socket serverDataSocket;
    boolean terminateFlag;

    public CustomFTPServer(int ftpPort)
    {
        myFTPPort = ftpPort;
        terminateFlag = false;
        try {
            Random random = new Random();
            int rand =  random.nextInt(40000);
            serverDataPort = rand + 20000;
            ftpListenerSocket = new ServerSocket(myFTPPort);
            //serverDataSocket = new Socket(localhost, serverDataPort);
            //serverDataSocket.close();
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

        Path currentAbsolutePath;
        Path rootAbsolutePath;
        public MyRunnable(Socket clientSocket)
        {
            this.clientControlSocket = clientSocket;
            currentAbsolutePath = Paths.get("").toAbsolutePath();
            rootAbsolutePath = Paths.get("").toAbsolutePath();
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
                        System.out.println(currentAbsolutePath.toAbsolutePath().toString());
                    }
                    else if (clientRequestSplittedList.get(0).equals("GPRT")) {
                        // **** Implementation is Buggy, probably wrong ****
                        handleGPRT();
                    }
                    else if (clientRequestSplittedList.get(0).equals("NLST")) {
                        handleNLST();
                    }
                    else if (clientRequestSplittedList.get(0).equals("CWD")) {
                        handleCWD(clientRequestSplittedList);
                        System.out.println(currentAbsolutePath.toAbsolutePath().toString());
                    }
                    else if (clientRequestSplittedList.get(0).equals("PUT"))
                    {
                        // **** Not implemented ****
                        System.out.println("PUT NOT IMPLEMENTED");
                    }
                    else if (clientRequestSplittedList.get(0).equals("MKDR")) {
                        handleMKDR(clientRequestSplittedList);
                    }
                    else if (clientRequestSplittedList.get(0).equals("RETR")) {
                        handleRETR(clientRequestSplittedList);
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
        private void handleRETR(List<String> clientRequestSplittedList) {
            try {
                sendSuccessResponse();
                if (clientDataSocket.isClosed())
                    clientDataSocket = new Socket(localhost, myDataPortNumber);

                File file = new File(currentAbsolutePath.toString() + "/" + clientRequestSplittedList.get(1));
                // Get the size of the file
                int length = (int)file.length();
                byte[] bytes = new byte[length];
                InputStream fileInputStream = new FileInputStream(file);
                OutputStream outputStream = clientDataSocket.getOutputStream();

                int count;
                while ((count = fileInputStream.read(bytes)) > 0) {
                    byte[] bytes1 = new byte[count];
                    outputStream.write(bytes1,0, 2);
                    outputStream.write(bytes, 0, count);
                }
                outputStream.flush();
                outputStream.close();
                fileInputStream.close();
                clientDataSocket.close();


            }catch (IOException ioe)
            {
                ioe.printStackTrace();
            }

        }
        private void handleGPRT() {
            try {
                sendSuccessResponse();
                if (clientDataSocket.isClosed())
                    clientDataSocket = new Socket(localhost, myDataPortNumber);

                OutputStream outputStream = clientDataSocket.getOutputStream();

                byte[] bytes1 = new byte[myDataPortNumber];
                outputStream.write(bytes1,0, bytes1.length);

                outputStream.flush();
                outputStream.close();
                clientDataSocket.close();


            }catch (IOException ioe)
            {
                ioe.printStackTrace();
                terminateFlag = true;
            }
        }
        private void handleDELE(List<String> clientRequestSplittedList) {
            Path currentPath = currentAbsolutePath;
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
            Path currentPath = currentAbsolutePath;
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
            Path currentPath = currentAbsolutePath;
            currentPath = currentPath.resolve(clientRequestSplittedList.get(1));
            File fileTmp = new File(currentPath.toString());
            if (fileTmp.exists()) {
                currentAbsolutePath = currentAbsolutePath.resolve(clientRequestSplittedList.get(1));
                sendSuccessResponse();
            }
            else{
                sendFailResponse();
            }
        }
        private void handleCDUP() {
            if (currentAbsolutePath.toString().equals(rootAbsolutePath.toString())) {
                sendFailResponse();
            }
            else {
                currentAbsolutePath = currentAbsolutePath.getParent();
                sendSuccessResponse();
            }
        }
        private void handleMKDR(List<String> clientRequestSplittedList)
        {
            // Directory Logic
            Path currentPath = currentAbsolutePath;
            currentPath = currentPath.resolve(clientRequestSplittedList.get(1));
            try {
                File fileTmp = new File(currentPath.toString());
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
            String returnString = "xx";// There is a 16 bit problem at the start it seems

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentAbsolutePath)) {
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
            //sendSuccessResponse();
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
                if (!clientControlSocket.isClosed())
                    clientControlSocket.close();
                if (!clientDataSocket.isClosed())
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
                terminateFlag = true;
                ioe.printStackTrace();
            }
            catch (NullPointerException ne)
            {
                ne.printStackTrace();
                terminateFlag = true;
                try {
                    clientDataSocket.close();
                }catch (IOException ioe)
                {

                }
                return null;
            }
            return null;
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
        CustomFTPServer customFTPServer = new CustomFTPServer(Integer.parseInt(args[0]));
        customFTPServer.initServer();
        System.exit(0);
    }
}
