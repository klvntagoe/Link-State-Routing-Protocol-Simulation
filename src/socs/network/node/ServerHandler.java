package socs.network.node;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerHandler implements Runnable {

    private RouterDescription _rd;
    
    private LinkStateDatabase _lsd;

    private Link[] _ports;

    private ServerSocket _serverSocket;

    private boolean _serverIsRunning = false;
    
    public ServerHandler(RouterDescription rd, LinkStateDatabase lsd, Link[] ports){
        this._rd = rd;
        this._lsd = lsd;
        this._ports = ports;
    }

    public void run(){
        try{
            this._serverSocket = new ServerSocket(this._rd.processPortNumber);
            _serverIsRunning = true;    //Will only be reached if no exception is caught
        } catch(Exception e){
            System.err.println(e.toString());
            System.exit(1);
        }

        Socket server = null;
        while(_serverIsRunning){
            try {
                server = _serverSocket.accept();
                System.out.println();

                DataInputStream in = new DataInputStream(server.getInputStream());
                System.out.println();
                
                DataOutputStream out = new DataOutputStream(server.getOutputStream());
                out.writeUTF();

                //TODO: should server.close() be placed here?
                server.close();

            } catch (Exception e){
                System.err.println(e.toString());
                System.exit(1);
            }

        }

    }
}