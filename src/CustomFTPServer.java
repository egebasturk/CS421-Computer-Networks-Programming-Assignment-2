import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class CustomFTPServer
{
    final static String CR = "\r";
    final static String LF = "\n";
    final static int POSITIVE_RESULT = 1;
    final static int NEGATIVE_RESULT = 1;
    final static int myFTPPort = 22222;
    ServerSocket ftpListenerSocket;
    Socket clientConnection;

    public CustomFTPServer()
    {
        try {
            ftpListenerSocket = new ServerSocket(myFTPPort);
        }catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
    public void initServer()
    {
        while (true)
        {
            try {
                Socket clientSocket = ftpListenerSocket.accept();
                if (clientSocket != null) {
                    MyRunnable myRunnable = new MyRunnable(clientSocket);
                    Thread thread = new Thread(myRunnable);
                    thread.start();
                }
            }catch (IOException ioe)
            {
                ioe.printStackTrace();
            }
        }
    }

    OutputStreamWriter outToClient;
    public int sendStringToPort(String str, Socket clientSocket)
    {
        try {
            outToClient = new OutputStreamWriter(clientSocket.getOutputStream(), "US-ASCII");
            outToClient.write(str, 0, str.length());
            outToClient.flush();

            InputStreamReader inputStreamReader =
                    new InputStreamReader(clientSocket.getInputStream());
            BufferedReader bufferedReader =
                    new BufferedReader(inputStreamReader);

            String tmp = bufferedReader.readLine();
            System.out.println(tmp.split(" ")[0]);
            int responseCode = Integer.parseInt(tmp.split(" ")[0]);
            if (responseCode == 400)
                System.out.println("CODE 400 Received");
            return responseCode;
        } catch (IOException e)
        {
            e.printStackTrace();
            return NEGATIVE_RESULT;
        }
    }


    private class MyRunnable implements Runnable
    {
        Socket clientSocket;
        public MyRunnable(Socket clientSocket)
        {
            this.clientSocket = clientSocket;
        }
        @Override
        public void run() {
            System.out.println("Thread run");
        }
    }
    public static void main(String args[])
    {
        CustomFTPServer customFTPServer = new CustomFTPServer();
        customFTPServer.initServer();
    }
}
