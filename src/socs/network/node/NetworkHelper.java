package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;
import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public class NetworkHelper {

    public static void UpdateDatabase(LinkStateDatabase lsd, SOSPFPacket packet){
        HashMap<String, LSA> db = lsd.store;
        for (LSA currentLsa : packet.lsaArray){
            if (db.containsKey(currentLsa.linkStateID)){
                LSA previousLsa = db.get(currentLsa.linkStateID);
                if (previousLsa.lsaSeqNumber <= currentLsa.lsaSeqNumber) {
                    RemoveStaleLinksFromDatabase(lsd, previousLsa, currentLsa);
                    db.replace(currentLsa.linkStateID, currentLsa);
                }
            } else db.put(currentLsa.linkStateID, currentLsa);
        }
    }

    public static void UpdateDatabaseWithNewRouterInformation(RouterDescription rd, Link[] ports, LinkStateDatabase lsd){
        HashMap<String, LSA> db;
        LSA currentLsa, previousLsa;

        db = lsd.store;
        previousLsa = db.get(rd.simulatedIPAddress);
        currentLsa = constructLSA(rd, ports, lsd);
        
        RemoveStaleLinksFromDatabase(lsd, previousLsa, currentLsa);
        db.replace(currentLsa.linkStateID, currentLsa);
    }

    public static void RemoveStaleLinksFromDatabase(LinkStateDatabase lsd, LSA previousLsa, LSA currentLsa){
        HashMap<String, LSA> db;
        HashSet<String> currentLinks;

        db = lsd.store;
        currentLinks = new HashSet<String>();
        currentLinks.add(currentLsa.linkStateID);

        if (db.containsKey(currentLsa.linkStateID)){
            //Identify all new links
            for (LinkDescription linkDescription : currentLsa.links) currentLinks.add(linkDescription.linkID);

            //Remove link information if not found in current links
            for (LinkDescription linkDescription : previousLsa.links)
                if (!currentLinks.contains(linkDescription.linkID)) db.remove(linkDescription.linkID);
        }
    }

    public static void BroadcastLSA(RouterDescription rd, Link[] ports, LinkStateDatabase lsd){
        Link link;
        ObjectInputStream in;
        ObjectOutputStream out;
        Socket socket;
        SOSPFPacket lsaPacket, acknowledgmentPacket;

        for (int i = 0; i < ports.length; i++){
            link = ports[i];

            if (link == null) continue;

            lsaPacket = constructUpdatePacketToBroadcast(rd, ports, i, lsd);
            
            try{
                socket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
                out = new ObjectOutputStream(socket.getOutputStream());

                out.writeObject(lsaPacket);

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

    public static SOSPFPacket constructUpdatePacketToBroadcast(RouterDescription rd, Link[] ports, int linkIndex, LinkStateDatabase lsd){
        Link currentLink = ports[linkIndex];
        SOSPFPacket packet = new SOSPFPacket();
        
        packet.srcProcessIP = currentLink.router1.processIPAddress;
        packet.srcProcessPort = currentLink.router1.processPortNumber;

        packet.srcIP = currentLink.router1.simulatedIPAddress;
        packet.dstIP = currentLink.router2.simulatedIPAddress;

        packet.sospfType = SOSPFType.LinkStateUpdate;
        packet.routerID = currentLink.router1.simulatedIPAddress;
        packet.neighborID = currentLink.router1.simulatedIPAddress;
        
        packet.lsaArray.add(constructLSA(rd, ports, lsd));
        
        return packet;
    }
    public static SOSPFPacket constructHelloPacketToBroadcast(RouterDescription rd, Link[] ports, Link currentLink, LinkStateDatabase lsd){
        SOSPFPacket packet = new SOSPFPacket();
        
        packet.srcProcessIP = currentLink.router1.processIPAddress;
        packet.srcProcessPort = currentLink.router1.processPortNumber;

        packet.srcIP = currentLink.router1.simulatedIPAddress;
        packet.dstIP = currentLink.router2.simulatedIPAddress;

        packet.sospfType = SOSPFType.HELLO;
        packet.routerID = currentLink.router1.simulatedIPAddress;
        packet.neighborID = currentLink.router1.simulatedIPAddress;
        
        packet.cost = currentLink.cost;
        packet.lsaArray.add(constructLSA(rd, ports, lsd));
        
        return packet;
    }
    
    public static SOSPFPacket constructByePacketToBroadcast(Link currentLink){
        SOSPFPacket packet = new SOSPFPacket();
        
        packet.srcProcessIP = currentLink.router1.processIPAddress;
        packet.srcProcessPort = currentLink.router1.processPortNumber;

        packet.srcIP = currentLink.router1.simulatedIPAddress;
        packet.dstIP = currentLink.router2.simulatedIPAddress;

        packet.sospfType = SOSPFType.BYE;
        packet.routerID = currentLink.router1.simulatedIPAddress;
        packet.neighborID = currentLink.router1.simulatedIPAddress;
        
        return packet;
    }

    public static LSA constructLSA(RouterDescription rd, Link[] ports, LinkStateDatabase lsd){
        LSA linkStateAdvertisement = new LSA();
        linkStateAdvertisement.linkStateID = rd.simulatedIPAddress;
        linkStateAdvertisement.lsaSeqNumber = lsd.sequenceNumber;
        lsd.sequenceNumber++;
        for (Link link : ports){
            if (link == null) continue;
            
            LinkDescription linkDescription = new LinkDescription();
            linkDescription.linkID = link.router2.simulatedIPAddress;
            linkDescription.portIndex = link.router2.processPortNumber;
            linkDescription.tosMetrics = link.cost;
            
            linkStateAdvertisement.links.add(linkDescription);
        }
        return linkStateAdvertisement;
    }
}