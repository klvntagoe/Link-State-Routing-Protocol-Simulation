package socs.network.node;

import java.util.Vector;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public abstract class Handler implements Runnable {

    protected LinkStateDatabase _lsd;

    protected Link[] _ports;

    protected int _linkIndex = -1;
    
    public Handler(LinkStateDatabase lsd, Link[] ports){
        this._lsd = lsd;
        this._ports = ports;
    }
    
    public Handler(LinkStateDatabase lsd, Link[] ports, int index){
        this._lsd = lsd;
        this._ports = ports;
        this._linkIndex = index;
    }

    public void addPacketToLinkQueues(SOSPFPacket packet){
        Link link;
        if (packet.sospfType != SOSPFType.HELLO){
            for (int i = 0; i < this._ports.length; i++){
                //Add packet to queues of all links except for caller link
                link = this._ports[i];
                if (link == null) continue;
                if (i == this._linkIndex) continue;
                try{
                    link.lock.acquire();
                    link.PacketQueue.add(packet);
                }catch(Exception e){
                    System.err.println(e.toString());
                    System.exit(1);
                }finally{
                    link.lock.release();
                }
            }
        }
    }

    public SOSPFPacket constructLSAUpdatePacket(SOSPFType packetType){
        SOSPFPacket packet = new SOSPFPacket();
        Link link = this._ports[this._linkIndex];
        
        packet.srcProcessIP = link.router1.processIPAddress;
        packet.srcProcessPort = link.router1.processPortNumber;

        packet.srcIP = link.router1.simulatedIPAddress;
        packet.dstIP = link.router2.simulatedIPAddress;

        packet.sospfType = packetType;
        packet.routerID = packet.srcIP;
        packet.neighborID = packet.srcIP;

        if (packetType == SOSPFType.HELLO) packet.cost = link.cost;
        if (packetType == SOSPFType.LinkStateUpdate) packet.lsaArray.add(constructLSA());
        
        return packet;
    }

    public LSA constructLSA(){
        LSA linkStateAdvertisement = new LSA();
        linkStateAdvertisement.linkStateID = this._ports[this._linkIndex].router1.simulatedIPAddress;
        try{
            _lsd.lock.acquire();
            linkStateAdvertisement.lsaSeqNumber = _lsd.sequenceNumber;
            _lsd.sequenceNumber++;
        }catch(Exception e){
            System.err.println(e.toString());
            System.exit(1);
        }finally{
            _lsd.lock.release();
        }
        for (Link link : this._ports){
            if (link == null) continue;
            
            LinkDescription linkDescription = new LinkDescription();
            linkDescription.linkID = this._ports[this._linkIndex].router2.simulatedIPAddress;
            linkDescription.portIndex = this._ports[this._linkIndex].router2.processPortNumber;
            linkDescription.tosMetrics = this._ports[this._linkIndex].cost;
            
            linkStateAdvertisement.links.add(linkDescription);
        }
        return linkStateAdvertisement;
    }
}