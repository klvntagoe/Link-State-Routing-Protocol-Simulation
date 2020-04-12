package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
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

    public void ForwardLSA(SOSPFPacket packet, String ingressIP){
        Link link;
        ObjectInputStream in;
        ObjectOutputStream out;
        Socket socket;
        SOSPFPacket packetToForward, acknowledgmentPacket;

        for (int i = 0; i < this._ports.length; i++){
            link = this._ports[i];
            if (link == null) continue;
            if (link.router2.simulatedIPAddress.equals(ingressIP)) continue;
            
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