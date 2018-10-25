package org.cmdmac.enlarge.server.eventbus;

public class ConnectedChangeEvent {
    public String[] remotes;
    public ConnectedChangeEvent(String[] remote) {
        this.remotes = remote;
    }
}
