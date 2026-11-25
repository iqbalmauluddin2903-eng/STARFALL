package starfall.network;

import starfall.utils.Logger;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class GameClient {

    private final ConcurrentLinkedQueue<NetworkMessage> inbox = new ConcurrentLinkedQueue<>();
    private final BlockingQueue<String> outbox = new LinkedBlockingQueue<>();

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread readerThread;
    private Thread writerThread;

    private volatile boolean connected = false;
    private volatile boolean connecting = false;
    private volatile String statusMessage = "Offline";

    public void connectAsync(String host, int port, NetworkMessage helloMessage) {
        if (connected || connecting) return;
        connecting = true;
        statusMessage = "Connecting to " + host + ":" + port + "...";

        Thread connectThread = new Thread(() -> {
            try {
                Socket s = new Socket();
                s.connect(new InetSocketAddress(host, port), 4500);
                s.setTcpNoDelay(true);

                this.socket = s;
                this.out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
                this.in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
                this.connected = true;
                this.connecting = false;
                this.statusMessage = "Connected (" + host + ":" + port + ")";

                writerThread = new Thread(this::writeLoop, "Starfall-ClientWriter");
                writerThread.setDaemon(true);
                writerThread.start();

                readerThread = new Thread(this::readLoop, "Starfall-ClientReader");
                readerThread.setDaemon(true);
                readerThread.start();

                send(helloMessage);
                Logger.info("GameClient connected to " + host + ":" + port);
            } catch (IOException e) {
                this.connected = false;
                this.connecting = false;
                this.statusMessage = "Connection failed: " + e.getMessage();
                Logger.warn("GameClient connection failed: " + e.getMessage());
            }
        }, "Starfall-ClientConnector");
        connectThread.setDaemon(true);
        connectThread.start();
    }

    public void send(NetworkMessage msg) {
        if (connected && msg != null) {
            outbox.offer(msg.serialize());
        }
    }

    public List<NetworkMessage> drainInbox() {
        List<NetworkMessage> list = new ArrayList<>();
        NetworkMessage m;
        while ((m = inbox.poll()) != null) {
            list.add(m);
        }
        return list;
    }

    public void disconnect() {
        if (connected) {
            try {
                if (out != null) out.println(new NetworkMessage(NetworkMessage.Type.DISCONNECT).serialize());
            } catch (Exception ignored) {}
        }
        connected = false;
        connecting = false;
        outbox.clear();
        if (writerThread != null) writerThread.interrupt();
        if (readerThread != null) readerThread.interrupt();
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
        statusMessage = "Disconnected";
    }

    private void readLoop() {
        try {
            String line;
            while (connected && (line = in.readLine()) != null) {
                NetworkMessage msg = NetworkMessage.deserialize(line);
                if (msg != null) {
                    inbox.offer(msg);
                }
            }
        } catch (IOException e) {
            if (connected) {
                statusMessage = "Disconnected from server";
                Logger.warn("GameClient read loop ended: " + e.getMessage());
            }
        } finally {
            connected = false;
        }
    }

    private void writeLoop() {
        try {
            while (connected) {
                String payload = outbox.take();
                out.println(payload);
                if (out.checkError()) {
                    disconnect();
                    break;
                }
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isConnected() { return connected; }
    public boolean isConnecting() { return connecting; }
    public String getStatusMessage() { return statusMessage; }
}