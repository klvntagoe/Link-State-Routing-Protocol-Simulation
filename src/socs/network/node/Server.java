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
        Socket server;
        Thread clientServiceThread;

        server = null;
        _serverIsRunning = true; 
        while(_serverIsRunning){
            try {
                server = _serverSocket.accept();
                //TODO: PRINT SOMETHING HERE

                clientServiceThread = new Thread(new ServerHandler(server, this._rd, this._lsd, this._ports));
                clientServiceThread.start();
            } catch (Exception e){
                System.err.println(e.toString());
                System.exit(1);
            }
        }
    }
}