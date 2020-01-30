package socs.network.node;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

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

        Link newLink;
        ObjectInputStream in;
        ObjectOutputStream out;
        RouterDescription newClient;
        short portIndex;
        Socket server;
        SOSPFPacket firstHelloMessageRecieved, secondHelloMessageRecieved, helloMessageToSend;

        try{
            this._serverSocket = new ServerSocket(this._rd.processPortNumber);
            _serverIsRunning = true;    //Will only be reached if no exception is caught
        } catch(Exception e){
            System.err.println(e.toString());
            System.exit(1);
        }

        server = null;
        while(_serverIsRunning){
            try {
                server = _serverSocket.accept();
                //TODO: PRINT SOMETHING HERE

                in = new ObjectInputStream(server.getInputStream());
                out = new ObjectOutputStream(server.getOutputStream());

                //Recieve First Hello, create link, set remote router to INIT
                firstHelloMessageRecieved = (SOSPFPacket) in.readObject();
                newClient = new RouterDescription(firstHelloMessageRecieved.srcProcessIP, firstHelloMessageRecieved.srcProcessPort, firstHelloMessageRecieved.srcIP);
                portIndex = findAvailablePort(newClient);
                if (portIndex == -1){
                    //TODO: PRINT SOMETHING HERE
                    System.out.println();
                    continue;
                }
                newLink = new Link(this._rd, newClient);
                this._ports[portIndex] = newLink;
                newLink.router2.status = RouterStatus.INIT;
                //TODO: PRINT SOMETHING HERE

                //Send First Hello
                helloMessageToSend = new SOSPFPacket();
                helloMessageToSend.srcProcessIP = newLink.router1.processIPAddress;
                helloMessageToSend.srcProcessPort = newLink.router1.processPortNumber;
                helloMessageToSend.srcIP = newLink.router1.simulatedIPAddress;
                helloMessageToSend.dstIP = newLink.router2.simulatedIPAddress;
                helloMessageToSend.sospfType = SOSPFType.HELLO;
                helloMessageToSend.routerID = newLink.router1.simulatedIPAddress;
                helloMessageToSend.neighborID = newLink.router1.simulatedIPAddress;
                out.writeObject(helloMessageToSend);
                //TODO: PRINT SOMETHING HERE

                //Recieve Second Hello, set remote router to TWO_WAY
                secondHelloMessageRecieved = (SOSPFPacket) in.readObject();
                newLink.router2.status = RouterStatus.TWO_WAY;
                //TODO: PRINT SOMETHING HERE

            } catch (Exception e){
                System.err.println(e.toString());
                System.exit(1);
            }
        }
        //TODO: should server.close() be placed here?
        try{
            this._serverSocket.close();
        }catch(IOException e){
            System.err.println(e.toString());
              System.exit(1);
        }
    }

    public short findAvailablePort(RouterDescription remoteRouter) {
        boolean alreadyAttached = false;
        short portIndex = -1;
        for (short i = 0; i < this._ports.length && !alreadyAttached && portIndex < 0; i++) {
            if (this._ports[i] == null) portIndex = i;
            else {
                if (this._ports[i].router2.simulatedIPAddress.equals(remoteRouter.simulatedIPAddress)) alreadyAttached = true;
            }
        }
        return portIndex;
    }
}