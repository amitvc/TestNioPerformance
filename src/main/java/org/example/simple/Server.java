package org.example.simple;

import org.example.TestHarness;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    public static final int PORT = 9000;
    public static final int MESSAGE_CNT = 5;
    private AtomicInteger messageCounter = new AtomicInteger(0);


    public void oneThreadPerConnection() throws IOException {
        System.out.println("Inializing the server at port "+ PORT);
        ServerSocket ss =  new ServerSocket(PORT);

        while(true) {
            Socket clientSocket = ss.accept();
            System.out.println("Accepted new client connection "+ clientSocket.getInetAddress().getCanonicalHostName());
            new Thread(new ClientConnectionHandler(clientSocket, messageCounter)).start();
        }
    }

    public void initWithThreadPool() throws Exception {
        System.out.println("Inializing the server at port "+ PORT);
        ServerSocket ss =  new ServerSocket(PORT);
        ExecutorService service = Executors.newCachedThreadPool();
        int clientConnectionCount = 0;
        while(true) {
            Socket clientSocket = ss.accept();
            if (clientSocket != null) {
                clientConnectionCount++;
            }

            System.out.println("Accepted new client connection "+ clientSocket.getInetAddress().getCanonicalHostName());
            if (clientConnectionCount >= TestHarness.NO_OF_CLIENTS) {
                System.out.println("Finished connecting "+ TestHarness.NO_OF_CLIENTS + " clients");
            }

            service.submit(new ClientConnectionHandler(clientSocket, messageCounter));
        }
    }

    private static class ClientConnectionHandler implements Runnable {


        private final Socket socket;
        private final AtomicInteger messageCounter;

        public ClientConnectionHandler(Socket soc, AtomicInteger cnt) {
            this.socket = soc;
            messageCounter = cnt;
        }

        @Override
        public void run() {
            int i = 0;
            try {
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                String inputLine = null;
                while ((inputLine = in.readLine()) != null && i < MESSAGE_CNT) {
                    //log.info("Received message " + inputLine + " from client "+ socket.getInetAddress().toString()
                      //      + " cnt "+ messageCounter.get());
                    out.println(inputLine);

                    if (messageCounter.getAndIncrement() % 100000 == 0) {
                        System.out.println("Received "+ messageCounter.get() + " messages so far " + new Date().toString());
                    }
                    i++;
                }
                in.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void main(String args[]) throws Exception {
        Server s = new Server();
        if (args.length == 0) {
            System.out.println("Please pass in parameter to indicate either thread/client(1) model or threadpool(2) model");
            System.exit(1);
        }

        int option = Integer.parseInt(args[0].trim());

        if (option == 1) {
            s.oneThreadPerConnection();
        } else if (option == 2) {
            s.initWithThreadPool();
        } else {
            System.out.println("Please use option 1 or 2 to start the server");
            System.exit(1);
        }
    }
}
