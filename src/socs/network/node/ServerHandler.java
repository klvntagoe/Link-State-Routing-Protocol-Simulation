package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public class ServerHandler extends Handler {

    private Socket _socket;
    
    public ServerHandler(Socket socket, RouterDescription rd, LinkStateDatabase lsd, Link[] ports){
        super(rd, lsd, ports);
        this._socket = socket;
    }

    public void run(){
        Link link;
        ConnectionAvailability linkAvailability;
        ObjectInputStream in;
        ObjectOutputStream out;
        RouterDescription newClient;
        SOSPFPacket incomingPacket, firstHelloMessageRecieved, secondHelloMessageRecieved, helloMessageToSend;
        
        try{
            in = new ObjectInputStream(_socket.getInputStream());
            out = new ObjectOutputStream(_socket.getOutputStream());
            
            incomingPacket = (SOSPFPacket) in.readObject();
            if (incomingPacket.sospfType == SOSPFType.HELLO){
                //Recieve First Hello
                firstHelloMessageRecieved = incomingPacket;
                if (firstHelloMessageRecieved.sospfType != SOSPFType.HELLO) {
                    in.close();
                    out.close();
                    _socket.close();
                    throw new Exception("First HELLO message was never recieved");
                } else System.out.println("Recieved HELLO from " + firstHelloMessageRecieved.srcIP);

                newClient = new RouterDescription(firstHelloMessageRecieved.srcProcessIP, firstHelloMessageRecieved.srcProcessPort, firstHelloMessageRecieved.srcIP);
                linkAvailability = determineLinkAvailability(newClient);
                if (linkAvailability == ConnectionAvailability.PORTS_FULL){
                    System.out.println("Linking process cancelled for " + 
                        firstHelloMessageRecieved.srcIP + 
                        ". There are no available ports");
                    in.close();
                    out.close();
                    _socket.close();
                    return;
                }else if (linkAvailability == ConnectionAvailability.ALREADY_ATTACHED){
                    System.out.println("Linking process cancelled for " + 
                        firstHelloMessageRecieved.srcIP + 
                        ". This link or it is already attached");
                    in.close();
                    out.close();
                    _socket.close();
                    return;
                }else if (linkAvailability == ConnectionAvailability.AVAILABLE_PORT){
                    //Create link then set remote router status to INIT
                    link = new Link(this._rd, newClient, firstHelloMessageRecieved.cost);
                    this._ports[this._linkIndex] = link;
                    link.router2.status = RouterStatus.INIT;
                    System.out.println("Set " + firstHelloMessageRecieved.srcIP + " state to INIT");
    
                    //Send first HELLO
                    helloMessageToSend = constructLSAPacketToBroadcast(SOSPFType.HELLO, link);
                    out.writeObject(helloMessageToSend);
    
                    //Recieve second HELLO
                    secondHelloMessageRecieved = (SOSPFPacket) in.readObject();
                    if (secondHelloMessageRecieved.sospfType != SOSPFType.HELLO) throw new Exception("Second HELLO message was never recieved");
                    else System.out.println("Recieved HELLO from " + secondHelloMessageRecieved.srcIP);
    
                    //set remote router to TWO_WAY
                    link.router2.status = RouterStatus.TWO_WAY;
                    System.out.println("Set " + secondHelloMessageRecieved.srcIP + " state to TWO_WAY");

                    //Close socket connection
                    in.close();
                    out.close();
                    _socket.close();

                    //Update Database
                    UpdateDatabaseWithNewRouterInformation();
                    UpdateDatabase(secondHelloMessageRecieved);

                    //Forward LSA                  
                    ForwardLSA(secondHelloMessageRecieved);  
                }
            }else if (incomingPacket.sospfType == SOSPFType.LinkStateUpdate){
                //Askowledge LSA Packet
                out.writeObject(new SOSPFPacket());

                //Update Database
                UpdateDatabase(incomingPacket);

                //Forward LSA 
                ForwardLSA(incomingPacket);
            }
        }catch (Exception e){
            System.err.println(e.toString());
            //System.exit(1);
        }
    }

    public ConnectionAvailability determineLinkAvailability(RouterDescription remoteRouter) {
        for (short i = 0; i < this._ports.length; i++) {
            if (this._ports[i] == null){
                this._linkIndex = i;
                return ConnectionAvailability.AVAILABLE_PORT;
            }
            else if (this._ports[i].router2.simulatedIPAddress.equals(remoteRouter.simulatedIPAddress)) 
                return ConnectionAvailability.ALREADY_ATTACHED;
        }
        return ConnectionAvailability.PORTS_FULL;
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