package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public class ServerHandler extends Handler {

    private RouterDescription _rd;

    private Socket _socket;

    private boolean _serverHandlerIsRunning = false;
    
    public ServerHandler(Socket socket, RouterDescription rd, LinkStateDatabase lsd, Link[] ports){
        super(lsd, ports);
        this._rd = rd;
        this._socket = socket;
    }

    public void run(){

        Link link;
        ConnectionAvailability linkAvailability;
        ObjectInputStream in;
        ObjectOutputStream out;
        RouterDescription newClient;
        SOSPFPacket firstHelloMessageRecieved, secondHelloMessageRecieved, helloMessageToSend;
        SOSPFPacket lsaUpdatePacket, outgoingPacket, incomingPacket;
        
        try{
            in = new ObjectInputStream(_socket.getInputStream());
            out = new ObjectOutputStream(_socket.getOutputStream());

            //Recieve First Hello
            firstHelloMessageRecieved = (SOSPFPacket) in.readObject();
            if (firstHelloMessageRecieved.sospfType != SOSPFType.HELLO) throw new Exception("First HELLO message was never recieved");
            else System.out.println("Recieved HELLO from " + firstHelloMessageRecieved.srcIP);
            
            newClient = new RouterDescription(firstHelloMessageRecieved.srcProcessIP, firstHelloMessageRecieved.srcProcessPort, firstHelloMessageRecieved.srcIP);
            linkAvailability = determineLinkAvailability(newClient);
            if (linkAvailability == ConnectionAvailability.PORTS_FULL){
                System.out.println("Linking process cancelled for " + 
                    firstHelloMessageRecieved.srcIP + 
                    ". There are no available ports");
                _socket.close();
                return;
            }else if (linkAvailability == ConnectionAvailability.ALREADY_ATTACHED){
                System.out.println("Linking process cancelled for " + 
                    firstHelloMessageRecieved.srcIP + 
                    ". This link or it is already attached");
                _socket.close();
                return;
            }else if (linkAvailability == ConnectionAvailability.AVAILABLE_PORT){
                //Create link then set remote router status to INIT
                link = new Link(this._rd, newClient, firstHelloMessageRecieved.cost);
                this._ports[this._linkIndex] = link;
                link.router2.status = RouterStatus.INIT;
                System.out.println("Set " + firstHelloMessageRecieved.srcIP + " state to INIT");

                //Send first HELLO
                helloMessageToSend = constructLSAUpdatePacket(SOSPFType.HELLO);
                out.writeObject(helloMessageToSend);

                //Recieve second HELLO
                secondHelloMessageRecieved = (SOSPFPacket) in.readObject();
                if (secondHelloMessageRecieved.sospfType != SOSPFType.HELLO) throw new Exception("Second HELLO message was never recieved");
                else System.out.println("Recieved HELLO from " + secondHelloMessageRecieved.srcIP);

                //set remote router to TWO_WAY
                link.router2.status = RouterStatus.TWO_WAY;
                System.out.println("Set " + secondHelloMessageRecieved.srcIP + " state to TWO_WAY");

                //Send first LSAUPDATE
                lsaUpdatePacket = constructLSAUpdatePacket(SOSPFType.LinkStateUpdate);
                addPacketToLinkQueues(lsaUpdatePacket);
                lsaUpdatePacket = null;
                
                //Handle Remaining packets
                _serverHandlerIsRunning = true;
                while(_serverHandlerIsRunning){
                    //Socket Writing Process
                    try{
                        link.lock.acquire();
                        while (!link.PacketQueue.isEmpty()){
                            outgoingPacket = link.PacketQueue.poll();
                            if (outgoingPacket.sospfType != SOSPFType.HELLO) out.writeObject(outgoingPacket);
                        }
                    }catch(Exception e){
                        System.err.println(e.toString());
                        System.exit(1);
                    }finally{
                        link.lock.release();
                    }
                    
                    //Socket Reading Process
                    if (in.available() > 0){ //NOTE: Available does not tell us if we have our object of interest in the buffer, only if there is something to read
                        incomingPacket = (SOSPFPacket) in.readObject();
                        if (incomingPacket.sospfType == SOSPFType.LinkStateUpdate){
                            // Update Database
                            try{
                                this._lsd.lock.acquire();
                                HashMap<String, LSA> db = this._lsd.store;
                                for (LSA lsa : incomingPacket.lsaArray){
                                    if (db.containsKey(lsa.linkStateID)){
                                        LSA previousLSA = db.get(lsa.linkStateID);
                                        if (previousLSA.lsaSeqNumber <= lsa.lsaSeqNumber) db.replace(lsa.linkStateID, lsa);
                                    } else db.put(lsa.linkStateID, lsa);
                                }
                            }catch(Exception e){
                                System.err.println(e.toString());
                                System.exit(1);
                            }finally{
                                this._lsd.lock.release();
                            }
    
                            // Place in queues
                            addPacketToLinkQueues(incomingPacket);
                        }else if (incomingPacket.sospfType == SOSPFType.BYE){
                            //TODO: HANDLE THREAD SAFETY WHEN NULLIFYING LINKS
                            out.close();
                            in.close();
                            _socket.close();
                            _serverHandlerIsRunning = false;
                        }
                    }
                }
            }
        }catch (Exception e){
            System.err.println(e.toString());
            System.exit(1);
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