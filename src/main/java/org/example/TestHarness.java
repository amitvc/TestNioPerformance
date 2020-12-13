package org.example;

import org.example.simple.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class TestHarness {
    private static Logger log;
    private static final int PORT = 9000;
    public static final int NO_OF_CLIENTS = 10000;
    private final AtomicInteger threadCount = new AtomicInteger(0);
    private CompletableFuture<Void> clientThreads[];
    private ExecutorService service = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    static {
        log = Logger.getLogger(Server.class.getName());
        log.addHandler(new ConsoleHandler());
    }

    public void init_with_thread_pool() throws InterruptedException {
        log.info("Starting the test harness. Spawning "+NO_OF_CLIENTS + " clients");
        clientThreads = new CompletableFuture[NO_OF_CLIENTS];
        for (int i=0; i < NO_OF_CLIENTS; i++) {
            clientThreads[i] = CompletableFuture.runAsync(new ClientHandler(threadCount), service);
            if (i%100 == 0) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 50));
            }
        }
        long st = System.currentTimeMillis();
        CompletableFuture.allOf(clientThreads).join();
        System.out.println("Finished processing all threads " + (System.currentTimeMillis() - st));
    }


    private static class ClientHandler implements Runnable {

        private final AtomicInteger counter;
        public ClientHandler(AtomicInteger threadCount) {
            counter = threadCount;
        }

        @Override
        public void run() {
            try {
                Socket client = new Socket(InetAddress.getLocalHost(), PORT);
                PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream()));
                counter.getAndIncrement();
                int i =0;
                while(i < Server.MESSAGE_CNT) {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(0,1000));
                    if (counter.get() >= NO_OF_CLIENTS) {
                        out.println(UUID.randomUUID() + " - "+ Thread.currentThread().getName()) ;
                        String input = null;
                        while(in.ready() && (input = in.readLine()) != null) {
                            //log.info("Received message from server " + input);
                            i++;
                        }
                    }
                }
                //log.info("Done chatting with the server " + Thread.currentThread().getName() + " threads finished so far "+ counter.getAndIncrement());
                in.close();
                out.close();
                client.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        TestHarness th = new TestHarness();
        th.init();
    }

    private void init() throws InterruptedException {

        log.info("Starting the test harness. Spawning "+NO_OF_CLIENTS + " clients");
        Thread []threads = new Thread[NO_OF_CLIENTS];
        long st = System.currentTimeMillis();
        for (int i=0; i < NO_OF_CLIENTS; i++) {
            threads[i] = new Thread(new ClientHandler(threadCount));
            threads[i].start();
            if (i%100 == 0) {
                Thread.sleep(ThreadLocalRandom.current().nextInt(0, 50));
            }
        }

        log.info("Done connecting all clients");
        for (int i=0; i< NO_OF_CLIENTS; i++) {
            threads[i].join();
        }
        log.info("Finished processing all threads " + (System.currentTimeMillis() - st));
    }
}