package pt.tecnico.ulisboa.hbbft.vbroadcast.echo.utils;

import pt.tecnico.ulisboa.hbbft.vbroadcast.VBroadcastMessage;

public class FakeMessage extends VBroadcastMessage {

    public static final int ECHO = 1337;

    public FakeMessage(String pid, Integer sender) {
        super(pid, ECHO, sender);
    }
}
