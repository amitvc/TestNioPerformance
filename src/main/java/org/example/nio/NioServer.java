package org.example.nio;

import org.example.simple.Server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 *
 */
public class NioServer {

    private final Map<SocketChannel, ConcurrentLinkedQueue<ByteBuffer>> pendingData;

    public NioServer() {
        pendingData = new HashMap<>();
    }

    public void init() throws IOException {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress(Server.PORT));
        Selector selector = Selector.open();
        Selector messageProcessSelector = Selector.open();
        serverSocketChannel.configureBlocking(false);
        new Thread(new ClientConnectionHanlder(messageProcessSelector)).start();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        while(true){
            try{
                int cnt = selector.select();
               // System.out.println("select Cnt "+cnt);
                for(SelectionKey selectionKey : selector.selectedKeys()) {
                    if (selectionKey.isValid()) {
                        if (selectionKey.isAcceptable()) {
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            if (socketChannel != null) {
                                socketChannel.configureBlocking(false);
                                System.out.println("Socket accepted: " + socketChannel);
                                socketChannel.register(messageProcessSelector, SelectionKey.OP_READ);
                            }
                        } else if(selectionKey.isReadable()) {
                            System.out.println("Should not happen");
                        }
                    }
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }


    private static class ClientConnectionHanlder implements Runnable {
        private final Selector selector;
        private final Map<SocketChannel, ConcurrentLinkedQueue<ByteBuffer>> pendingData;

        public ClientConnectionHanlder(Selector selector) {
            this.selector = selector;
            pendingData = new HashMap<>();
        }

        @Override
        public void run() {
            System.out.println("Starting ClientConnectionHandler thread");
            while(true) {
                try {
                    selector.select();
                    for(SelectionKey selectionKey : selector.selectedKeys()) {
                        if (selectionKey.isValid()) {
                            if (selectionKey.isReadable()) {
                                read(selectionKey);
                            }
                            if (selectionKey.isWritable()) {
                                write(selectionKey);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void write(SelectionKey key) throws IOException {
            SocketChannel socket = (SocketChannel) key.channel();
            assert pendingData.containsKey(socket);
            if (pendingData.containsKey(socket)) {
                ByteBuffer byteBuffer = pendingData.get(socket).poll();
                byteBuffer.flip();
                socket.write(byteBuffer);
                byteBuffer.flip();
                System.out.println("Writing message back to client " + new String(byteBuffer.array()));
                socket.register(key.selector(), SelectionKey.OP_READ);
            }
        }

        private void read(SelectionKey key) throws IOException {
            SocketChannel socket = (SocketChannel) key.channel();
            ByteBuffer buffer = ByteBuffer.allocate(100);
            int bytesRead = socket.read(buffer);
            if (bytesRead == -1) {
                System.out.println("Client connection closed");
                pendingData.remove(socket);
            }
            if (bytesRead > 0 && buffer.get(buffer.position() -1) == '\n') {
                System.out.println("Message received from " + socket.getRemoteAddress() + " msg "+ new String(buffer.array()));
                if (!pendingData.containsKey(socket)) {
                    pendingData.put(socket, new ConcurrentLinkedQueue<>());
                }
                pendingData.get(socket).offer(buffer);
                socket.register(key.selector(), SelectionKey.OP_WRITE);
            }
        }



    }

    public static void main(String args[]) throws IOException {
        NioServer nioServer = new NioServer();
        nioServer.init();
    }
}
