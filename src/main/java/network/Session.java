package network;

import language.LanguageType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import model.account.Account;
import handler.DefaultHandler;
import handler.MessageHandler;
import game.entity.player.PlayerEntity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j

public class Session implements AutoCloseable {

    private volatile Socket socket;
    private volatile DataInputStream is;
    private volatile DataOutputStream os;

    private final GameServer server;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final BlockingQueue<Message> sendQueue = new LinkedBlockingQueue<>();
    private final MessageHandler handler;
    private Thread receiverThread;
    private Thread senderThread;

    private volatile boolean sendKeyComplete;
    private volatile byte curR;
    private volatile byte curW;
    private final byte[] keys;

    @Getter
    @Setter
    private LanguageType language = LanguageType.INDONESIAN;

    @Getter
    private volatile PlayerEntity player;

    @Setter
    @Getter
    private int zoomLv;

    @Setter
    @Getter
    private Account account;


    public Session(Socket socket, GameServer server) throws IOException {
        this.socket = socket;
        this.server = server;
        this.socket.setKeepAlive(true);
        this.socket.setTcpNoDelay(true);
        this.is = new DataInputStream(socket.getInputStream());
        this.os = new DataOutputStream(socket.getOutputStream());
        keys = "@HSO".getBytes();
        handler = new DefaultHandler();
    }

    public void start() {
        receiverThread = new Thread(this::receiveLoop, "Receiver-" + hashCode());
        senderThread = new Thread(this::sendLoop, "Sender-" + hashCode());

        receiverThread.start();
        senderThread.start();
    }

    // ---------------- RECEIVER ----------------
    private void receiveLoop() {
        try {
            while (!closed.get()) {
                Message msg = readMessage();
                if (msg.command == -40) {
                    sendKeys();
                } else {
                    handler.onMessage(this, msg);
                }
            }
        } catch (IOException e) {
            log.info("Receiver ended: {}", e.getMessage());
        } finally {
            close();
        }
    }


    // ---------------- SENDER ----------------
    private void sendLoop() {
        try {
            while (!closed.get()) {
                Message msg = sendQueue.poll(5, TimeUnit.SECONDS);
                if (msg != null) {
                    try {
                        sendMessage(msg);
                        msg.cleanup();
                    } catch (IOException e) {
                        log.error("Failed to send message: {}", e.getMessage());
                        throw e;
                    }
                }
            }
        } catch (InterruptedException ignored) {
        } catch (IOException e) {
            log.error("Sender ended: {}", e.getMessage());
        } finally {
            close();
        }
    }

    // Public API to send packet
    public void send(Message packet) {
        if (!closed.get()) sendQueue.offer(packet);
    }

    private Message readMessage() throws IOException {
        byte cmd = is.readByte();
        if (sendKeyComplete) {
            cmd = readKey(cmd);
        }
        int size;
        if (sendKeyComplete) {
            byte b1 = is.readByte();
            byte b2 = is.readByte();
            size = (readKey(b1) & 255) << 8 | readKey(b2) & 255;
        } else {
            size = is.readUnsignedShort();
        }
        byte[] data = new byte[size];
        int len = 0;
        int byteRead = 0;
        while (len != -1 && byteRead < size) {
            len = is.read(data, byteRead, size - byteRead);
            if (len > 0) {
                byteRead += len;
            }
        }
        if (sendKeyComplete) {
            for (int i = 0; i < data.length; i++) {
                data[i] = readKey(data[i]);
            }
        }

        log.debug("READ MSG : {} size : {} : {}", cmd, data.length, getIpAddress());
        return new Message(cmd, data);
    }

    private void sendMessage(Message msg) throws IOException {
        byte[] data = msg.getData();
        int dataLength = (data != null) ? data.length : 0;

        if (msg.command == 25) {
            msg.command = 126;
        }

        if (sendKeyComplete) {
            byte b = writeKey(msg.command);
            os.writeByte(b);
        } else {
            os.writeByte(msg.command);
        }

        if (data != null) {
            int size = data.length;
            if (msg.command == -51 || msg.command == -52 || msg.command == -54 || msg.command == 126) {
                if (msg.command == 126) {
                    byte bspec = writeKey((byte) 25);
                    os.writeByte(bspec);
                }
                byte b4 = (byte) (size);
                byte b3 = (byte) (size >> 8);
                byte b2 = (byte) (size >> 16);
                byte b1 = (byte) (size >> 24);
                final int byte4 = this.writeKey(b4);
                final int byte3 = this.writeKey(b3);
                final int byte2 = this.writeKey(b2);
                final int byte1 = this.writeKey(b1);
                this.os.writeByte(byte1);
                this.os.writeByte(byte2);
                this.os.writeByte(byte3);
                this.os.writeByte(byte4);
            } else if (sendKeyComplete) {
                int byte1 = writeKey((byte) (size >> 8));
                os.writeByte(byte1);
                int byte2 = writeKey((byte) (size));
                os.writeByte(byte2);
            } else {
                // Fixed: Properly extract high and low bytes
                os.writeByte((size >> 8) & 0xFF);
                os.writeByte(size & 0xFF);
            }
            if (sendKeyComplete) {
                byte[] encryptedData = new byte[data.length];
                for (int i = 0; i < data.length; i++) {
                    encryptedData[i] = writeKey(data[i]);
                }
                os.write(encryptedData);
            } else {
                os.write(data);
            }
        } else {
            os.writeByte(0);
            os.writeByte(0);
        }
        os.flush();

    }

    private byte readKey(final byte b) {
        final byte currentR = this.curR;
        this.curR = (byte) ((currentR + 1) % keys.length);
        return (byte) ((keys[currentR] & 0xFF) ^ (b & 0xFF));
    }

    private byte writeKey(final byte b) {
        final byte currentW = this.curW;
        this.curW = (byte) ((currentW + 1) % keys.length);
        return (byte) ((keys[currentW] & 0xFF) ^ (b & 0xFF));
    }

    private void sendKeys() {
        Message msg = new Message(-40);
        try {
            msg.out().writeByte(keys.length);
            msg.out().writeByte(keys[0]);
            for (int i = 1; i < keys.length; i++) {
                msg.out().writeByte(keys[i] ^ keys[i - 1]);
            }
            send(msg);

            // Wait briefly to ensure the key message is sent before enabling encryption
            // This is still not perfect but better than immediate flag setting
            // For a robust solution, consider using a callback/acknowledgment mechanism
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            sendKeyComplete = true;
        } catch (Exception e) {
            log.error("Failed to send keys: {}", e.getMessage());
        }
    }

    public void bindPlayer(PlayerEntity player) {
        language = account.getLanguage() == null ? LanguageType.ENGLISH : account.getLanguage();
        if (player != null) {

            this.player = player;
            this.player.bindSession(this);
        }
    }

    public void unbindPlayer() {
        if (player != null) {
            player.unbindSession();
            player = null;
        }
    }

    public String getIpAddress() {
        Socket s = socket;
        return (s != null) ? s.getInetAddress().getHostAddress() : "disconnected";
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        server.unregister(this);

        // Stop threads
        if (receiverThread != null) receiverThread.interrupt();
        if (senderThread != null) senderThread.interrupt();

        // Clear queue before closing streams
        sendQueue.clear();

        // Close streams and socket
        try {
            if (is != null) is.close();
        } catch (IOException ignored) {
        }
        try {
            if (os != null) os.close();
        } catch (IOException ignored) {
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {
        }

        // Reset state
        curR = 0;
        curW = 0;
        sendKeyComplete = false;

        // Set to null (safe now that threads are interrupted and streams closed)
        is = null;
        os = null;
        socket = null;
    }
}