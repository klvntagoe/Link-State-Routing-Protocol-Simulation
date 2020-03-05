package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import socs.network.message.LSA;
import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public class ClientHandler extends Handler {

    protected Socket _clientSocket;

    private boolean _clientHandlerIsRunning = false;

    public ClientHandler(LinkStateDatabase lsd, Link[] ports, int index) {
        super(lsd, ports, index);
    }

    public void run(){
        Link link;
        ObjectInputStream in;
        ObjectOutputStream out;
        SOSPFPacket helloMessageToSend, helloMessageRecieved;
        SOSPFPacket lsaUpdatePacket, outgoingPacket, incomingPacket;

        try{
            link = _ports[this._linkIndex];
            _clientSocket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
            
            out = new ObjectOutputStream(_clientSocket.getOutputStream());
            in = new ObjectInputStream(_clientSocket.getInputStream());

            //Send First Hello
            helloMessageToSend = constructLSAUpdatePacket(SOSPFType.HELLO);
            out.writeObject(helloMessageToSend);

            //Recieve First Hello
            helloMessageRecieved = (SOSPFPacket) in.readObject();
            if (helloMessageRecieved.sospfType != SOSPFType.HELLO) throw new Exception("HELLO message was never recieved");
            else System.out.println("Recieved HELLO from " + helloMessageRecieved.srcIP);

            //Set remote router status to TWO_WAY
            link.router2.status = RouterStatus.TWO_WAY;
            System.out.println("Set " + helloMessageRecieved.srcIP + " state to TWO_WAY");
            
            //Send Second Hello
            out.writeObject(helloMessageToSend);

            //Send first LSAUPDATE
            lsaUpdatePacket = constructLSAUpdatePacket(SOSPFType.LinkStateUpdate);
            addPacketToLinkQueues(lsaUpdatePacket);
            lsaUpdatePacket = null;

            //Handle Remaining packets
            _clientHandlerIsRunning = true;
            while(_clientHandlerIsRunning){
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
                        _clientSocket.close();
                        _clientHandlerIsRunning = false;
                    }
                }
            }
        }catch(Exception e){
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}