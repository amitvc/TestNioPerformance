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
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Server {
    private static Logger log;
    private static final int PORT = 9000;
    public static final int MESSAGE_CNT = 1000;
    private AtomicInteger messageCounter = new AtomicInteger(0);
    static {
        log = Logger.getLogger(Server.class.getName());
        log.addHandler(new ConsoleHandler());
    }

    public void init() throws IOException {
        log.info("Inializing the server at port "+ PORT);
        ServerSocket ss =  new ServerSocket(PORT);

        while(true) {
            Socket clientSocket = ss.accept();
            log.info("Accepted new client connection "+ clientSocket.getInetAddress().getCanonicalHostName());
            new Thread(new ClientConnectionHandler(clientSocket, messageCounter)).start();
        }
    }

    public void init2() throws Exception {
        log.info("Inializing the server at port "+ PORT);
        ServerSocket ss =  new ServerSocket(PORT);
        ExecutorService service = Executors.newFixedThreadPool(5000);
        int clientConnectionCount = 0;
        while(true) {
            Socket clientSocket = ss.accept();
            if (clientSocket != null) {
                clientConnectionCount++;
            }

            log.info("Accepted new client connection "+ clientSocket.getInetAddress().getCanonicalHostName());
            if (clientConnectionCount >= TestHarness.NO_OF_CLIENTS) {
                log.info("Finished connecting "+ TestHarness.NO_OF_CLIENTS + " clients");
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
                        log.info("Received "+ messageCounter.get() + " messages so far " + new Date().toString());
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
        s.init2();
    }
}
