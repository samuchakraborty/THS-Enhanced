package ths.server;

import ths.server.db.Database;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;

public class ServerMain {
    private static volatile boolean running = true;
    private static ServerSocket server;
    private static ExecutorService pool;

    public static void main(String[] args) throws Exception {
        // 1) DB first – this is what prevents ds==null
        try {
            Database.init();
        } catch (Exception e) {
            System.err.println("Server aborting due to DB init failure.");
            return; // stop here if DB failed
        }

        int port = Integer.parseInt(System.getProperty("SERVER_PORT", "7777"));
        pool = Executors.newCachedThreadPool();
        server = new ServerSocket(port);
        Runtime.getRuntime().addShutdownHook(new Thread(ServerMain::stop));
        System.out.println("THS Server listening on port " + port);

        try {
            while (running) {
                try {
                    Socket s = server.accept();
                    pool.submit(new ths.server.ClientWorker(s));
                } catch (IOException e) {
                    if (running) e.printStackTrace();
                }
            }
        } finally { stop(); }
    }

    public static void stop() {
        running = false;
        try { if (server != null && !server.isClosed()) server.close(); } catch (IOException ignored) {}
        if (pool != null) pool.shutdownNow();
        System.out.println("THS Server stopped.");
    }
}
