import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CustomFTPServer
{
    final static String CR = "\r";
    final static String LF = "\n";
    final static int POSITIVE_RESULT = 1;
    final static int NEGATIVE_RESULT = 1;
    final static int myFTPPort = 22222;
    final static String localhost = "localhost";
    ServerSocket ftpListenerSocket;
    Socket clientConnection;
    boolean terminateFlag;

    public CustomFTPServer()
    {
        terminateFlag = false;
        try {
            ftpListenerSocket = new ServerSocket(myFTPPort);
        }catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
    public void initServer()
    {
        Thread thread = null;
        ExecutorService executorService = Executors.newCachedThreadPool();
        while (!terminateFlag)
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
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        }catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("MAIN: Terminated");
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

        public MyRunnable(Socket clientSocket)
        {
            this.clientControlSocket = clientSocket;
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
            createDataConnection();
            while (!terminateFlag)
            {
                List<String> clientRequestSplittedList = readFromControlPort();
                if (clientRequestSplittedList != null) {
                    if (clientRequestSplittedList.get(0).equals("QUIT")) {
                        System.out.println("THREAD: Got QUIT");
                        terminateFlag = true;
                        sendSuccessResponse();
                    }
                    else if (clientRequestSplittedList.get(0).equals("CDUP"))
                    {
                        System.out.println("THREAD: Got CDUP");
                        sendSuccessResponse();
                    }
                    else if (clientRequestSplittedList.get(0).equals("GPRT"))
                    {
                        sendStringToPort(clientDataSocket.getPort() + CR + LF);
                        sendSuccessResponse();
                    }
                    else if (clientRequestSplittedList.get(0).equals("NLST"))
                    {
                        Path currentRelativePath = Paths.get("");
                        try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentRelativePath)) {
                            for (Path file: stream) {
                                System.out.println(file.getFileName());
                            }
                        } catch (IOException | DirectoryIteratorException x) {
                            // IOException can never be thrown by the iteration.
                            // In this snippet, it can only be thrown by newDirectoryStream.
                            System.err.println(x);
                        }
                        sendFailResponse();
                    }
                    else if (clientRequestSplittedList.get(0).equals("CWD"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("PUT"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("MKDR"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("RETR"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("DELE"))
                    {

                    }
                    else if (clientRequestSplittedList.get(0).equals("DDIR"))
                    {

                    }
                    else
                    {
                        sendFailResponse();
                    }
                }
            }
            terminateThread();
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
        public void GPRT()
        {
            command = "GPRT" + CR + LF;
            sendStringToPort(command);
        }
        public int sendStringToPort(String str)
        {
            try {
                outToClient = new OutputStreamWriter(clientControlSocket.getOutputStream(), "US-ASCII");
                outToClient.write(str, 0, str.length());
                outToClient.flush();
                return 0;
            } catch (IOException e)
            {
                e.printStackTrace();
                return NEGATIVE_RESULT;
            }
        }
        private void sendSuccessResponse() {
            sendStringToPort(200 + CR + LF);
        }
        private void sendFailResponse() {
            sendStringToPort(400 + CR + LF);
        }
    }
    public static void main(String args[])
    {
        CustomFTPServer customFTPServer = new CustomFTPServer();
        customFTPServer.initServer();
        System.exit(0);
    }
}
