package handler;

import network.Message;
import network.Session;

import java.io.IOException;

public interface MessageHandler {
    void onMessage(Session s, Message m) throws IOException;
}
