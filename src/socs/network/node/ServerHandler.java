package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public class ServerHandler implements Runnable {

    private RouterDescription _rd;
    
    private LinkStateDatabase _lsd;

    private Link[] _ports;

    private Socket _socket;

    private boolean _serverHandlerIsRunning = false;
    
    public ServerHandler(Socket socket, RouterDescription rd, LinkStateDatabase lsd, Link[] ports){
        this._rd = rd;
        this._lsd = lsd;
        this._ports = ports;
        this._socket = socket;
    }

    public void run(){

        Link newLink;
        LinkAvailabilityType linkAvailability;
        ObjectInputStream in;
        ObjectOutputStream out;
        RouterDescription newClient;
        short portIndex;
        SOSPFPacket firstHelloMessageRecieved, secondHelloMessageRecieved, helloMessageToSend;
        
        try{
            in = new ObjectInputStream(_socket.getInputStream());
            out = new ObjectOutputStream(_socket.getOutputStream());

            //Recieve First Hello
            firstHelloMessageRecieved = (SOSPFPacket) in.readObject();
            System.out.println("Recieved HELLO from " + firstHelloMessageRecieved.srcIP);
            newClient = new RouterDescription(firstHelloMessageRecieved.srcProcessIP, firstHelloMessageRecieved.srcProcessPort, firstHelloMessageRecieved.srcIP);
            linkAvailability = determineLinkResult(newClient);
            if (linkAvailability == LinkAvailabilityType.PORTS_FULL){
                System.out.println("Linking process cancelled for " + 
                    firstHelloMessageRecieved.srcIP + 
                    ". There are either no available ports");
                _socket.close();
                return;
            }else if (linkAvailability == LinkAvailabilityType.ALREADY_ATTACHED){
                System.out.println("Linking process cancelled for " + 
                    firstHelloMessageRecieved.srcIP + 
                    ". This link or it is already attached");
                _socket.close();
                return;
            }else if (linkAvailability == LinkAvailabilityType.AVAILABLE_PORT){
                //Find available port
                portIndex = -1;
                for (short i = 0; i < this._ports.length; i++){
                    if (this._ports[i] == null){
                        portIndex = i;
                        break;
                    }
                }

                //Create link then set remote router status to INIT
                portIndex = FindAvailablePort(newClient);
                newLink = new Link(this._rd, newClient);
                this._ports[portIndex] = newLink;
                newLink.router2.status = RouterStatus.INIT;
                System.out.println("Set " + firstHelloMessageRecieved.srcIP + " state to INIT");

                //Send first HELLO
                helloMessageToSend = new SOSPFPacket();
                helloMessageToSend.srcProcessIP = newLink.router1.processIPAddress;
                helloMessageToSend.srcProcessPort = newLink.router1.processPortNumber;
                helloMessageToSend.srcIP = newLink.router1.simulatedIPAddress;
                helloMessageToSend.dstIP = newLink.router2.simulatedIPAddress;
                helloMessageToSend.sospfType = SOSPFType.HELLO;
                helloMessageToSend.routerID = newLink.router1.simulatedIPAddress;
                helloMessageToSend.neighborID = newLink.router1.simulatedIPAddress;
                out.writeObject(helloMessageToSend);

                //Recieve second HELLO
                secondHelloMessageRecieved = (SOSPFPacket) in.readObject();
                System.out.println("Recieved HELLO from " + secondHelloMessageRecieved.srcIP);

                //set remote router to TWO_WAY
                newLink.router2.status = RouterStatus.TWO_WAY;
                System.out.println("Set " + secondHelloMessageRecieved.srcIP + " state to TWO_WAY");
            }

            _serverHandlerIsRunning = true;
            while(_serverHandlerIsRunning){
                _serverHandlerIsRunning = false;
            }
            _socket.close();
        }catch (Exception e){
            System.err.println(e.toString());
            System.exit(1);
        }
    }

    public LinkAvailabilityType determineLinkResult(RouterDescription remoteRouter) {
        for (short i = 0; i < this._ports.length; i++) {
            if (this._ports[i] == null) 
                return LinkAvailabilityType.AVAILABLE_PORT;
            else if (this._ports[i].router2.simulatedIPAddress.equals(remoteRouter.simulatedIPAddress)) 
                return LinkAvailabilityType.ALREADY_ATTACHED;
        }
        return LinkAvailabilityType.PORTS_FULL;
    }

    public short FindAvailablePort(RouterDescription remoteRouter) {
        boolean alreadyAttached = false;
        short portIndex = -1;
        for (short i = 0; i < this._ports.length && !alreadyAttached && portIndex < 0; i++) {
            if (this._ports[i] == null) 
                portIndex = i;
            else if (this._ports[i].router2.simulatedIPAddress.equals(remoteRouter.simulatedIPAddress)) 
                alreadyAttached = true;
        }
        return portIndex;
    }
}