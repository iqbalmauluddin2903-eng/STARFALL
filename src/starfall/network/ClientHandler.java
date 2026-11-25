package starfall.network;

import starfall.utils.Logger;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final GameServer server;
    private final BlockingQueue<String> outboundQueue = new LinkedBlockingQueue<>();

    private PrintWriter out;
    private BufferedReader in;
    private Thread writerThread;

    private volatile boolean running = true;
    private volatile String playerId = null;

    public ClientHandler(Socket socket, GameServer server) {
        this.socket = socket;
        this.server = server;
    }

    public void send(NetworkMessage message) {
        if (running && message != null) {
            outboundQueue.offer(message.serialize());
        }
    }

    public void close() {
        if (!running) return;
        running = false;
        if (writerThread != null) {
            writerThread.interrupt();
        }
        try {
            socket.close();
        } catch (IOException ignored) {}
    }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getRemoteAddress() { return socket.getRemoteSocketAddress().toString(); }

    @Override
    public void run() {
        try {
            socket.setTcpNoDelay(true);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            writerThread = new Thread(this::writeLoop, "Starfall-ServerWriter");
            writerThread.setDaemon(true);
            writerThread.start();

            String line;
            while (running && (line = in.readLine()) != null) {
                NetworkMessage msg = NetworkMessage.deserialize(line);
                if (msg != null) {
                    server.handleClientMessage(this, msg);
                }
            }
        } catch (IOException e) {
            if (running) {
                Logger.info("Client connection closed (" + getRemoteAddress() + "): " + e.getMessage());
            }
        } finally {
            close();
            server.onClientDisconnected(this);
        }
    }

    private void writeLoop() {
        try {
            while (running) {
                String payload = outboundQueue.take();
                out.println(payload);
                if (out.checkError()) {
                    close();
                    break;
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}