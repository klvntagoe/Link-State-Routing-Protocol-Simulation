package socs.network.node;

import java.net.ServerSocket;
import java.net.Socket;

public class Server implements Runnable {

    private RouterDescription _rd;

    private LinkStateDatabase _lsd;

    private Link[] _ports;

    private ServerSocket _serverSocket;

    private boolean _serverIsRunning = false;

    public Server(ServerSocket serverSocket, RouterDescription rd, LinkStateDatabase lsd, Link[] ports) {
        this._serverSocket = serverSocket;
        this._rd = rd;
        this._lsd = lsd;
        this._ports = ports;
    }

    public void run(){
        Socket socket;
        Thread clientServiceThread;

        _serverIsRunning = true; 
        while(_serverIsRunning){
            if (NumberOfConnectedPorts() >= this._ports.length) continue;
            try {
                socket = _serverSocket.accept();
                clientServiceThread = new Thread(new ServerHandler(socket, this._rd, this._lsd, this._ports));
                clientServiceThread.start();
            } catch (Exception e){
                System.err.println(e.toString());
                //System.exit(1);
            }
        }
    }

    public int NumberOfConnectedPorts(){
        int numConnectedPorts = 0;
        for (Link link : this._ports){
            if (link != null) numConnectedPorts++;
        }
        return numConnectedPorts;
    }
}