package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public abstract class Handler implements Runnable {

    protected RouterDescription _rd;

    protected LinkStateDatabase _lsd;

    protected Link[] _ports;

    protected int _linkIndex = -1;
    
    public Handler(RouterDescription rd, LinkStateDatabase lsd, Link[] ports){
        this._rd = rd;
        this._lsd = lsd;
        this._ports = ports;
    }
    
    public Handler(RouterDescription rd, LinkStateDatabase lsd, Link[] ports, int index){
        this._rd = rd;
        this._lsd = lsd;
        this._ports = ports;
        this._linkIndex = index;
    }

    public void UpdateDatabase(SOSPFPacket packet){
        HashMap<String, LSA> db = this._lsd.store;
        for (LSA lsa : packet.lsaArray){
            if (db.containsKey(lsa.linkStateID)){
                LSA previousLSA = db.get(lsa.linkStateID);
                if (previousLSA.lsaSeqNumber <= lsa.lsaSeqNumber) db.replace(lsa.linkStateID, lsa);
            } else db.put(lsa.linkStateID, lsa);
        }

    }

    public void ForwardLSA(SOSPFPacket packet){
        Link link;
        ObjectInputStream in;
        ObjectOutputStream out;
        Socket socket;
        SOSPFPacket packetToForward, acknowledgmentPacket;

        for (int i = 0; i < this._ports.length; i++){
            link = this._ports[i];
            if (link == null) continue;
            if (link.router2.simulatedIPAddress.equals(packet.srcIP)) continue;
            
            packetToForward = constructLSAPacketToForward(packet, link);
            try{
                socket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
                out = new ObjectOutputStream(socket.getOutputStream());

                out.writeObject(packetToForward);

                in = new ObjectInputStream(socket.getInputStream());
                acknowledgmentPacket = (SOSPFPacket) in.readObject();

                in.close();
                out.close();
                socket.close();
            }catch(Exception e){
                System.err.println(e.toString());
                //System.exit(1);
            }
        }
    }

    public SOSPFPacket constructLSAPacketToForward(SOSPFPacket incomingPacket, Link currentLink){
        SOSPFPacket packet = new SOSPFPacket();
        packet.srcProcessIP = currentLink.router1.processIPAddress;
        packet.srcProcessPort = currentLink.router1.processPortNumber;

        packet.srcIP = currentLink.router1.simulatedIPAddress;
        packet.dstIP = currentLink.router2.simulatedIPAddress;

        packet.sospfType = SOSPFType.LinkStateUpdate;
        packet.routerID = currentLink.router1.simulatedIPAddress;
        packet.neighborID = currentLink.router1.simulatedIPAddress;
        /*
        packet.srcProcessIP = incomingPacket.srcProcessIP;
        packet.srcProcessPort = incomingPacket.srcProcessPort;

        packet.srcIP = incomingPacket.srcIP;
        packet.dstIP = currentLink.router2.simulatedIPAddress;

        packet.sospfType = SOSPFType.LinkStateUpdate;
        packet.routerID = incomingPacket.srcIP;
        packet.neighborID = currentLink.router1.simulatedIPAddress;
        */
        packet.lsaArray = incomingPacket.lsaArray;
        
        return packet;
    }
}