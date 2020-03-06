package socs.network.node;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

import java.util.ArrayList;
import java.util.HashMap;

public class LinkStateDatabase {

  //linkID => LSAInstance
  HashMap<String, LSA> store = new HashMap<String, LSA>();

  int sequenceNumber = Integer.MIN_VALUE + 1;

  private RouterDescription rd = null;

  public LinkStateDatabase(RouterDescription routerDescription) {
    rd = routerDescription;
    LSA l = initLinkStateDatabase();
    store.put(l.linkStateID, l);
  }

  /**
   * output the shortest path from this router to the destination with the given IP address
   */
  String getShortestPath(String destinationIP) {
    WeightedGraph graph = WeightedGraph.LoadLinkStateDatabase(this.store);
    if (graph == null) return "Link State Database is empty";
    else{
      if (destinationIP.equals(rd.simulatedIPAddress)) return "Destination IP Address = IP Address of this router (cost = 0)";
      else{
        ArrayList<String> path = graph.FindShortestPath(rd.simulatedIPAddress, destinationIP);
        StringBuilder s = new StringBuilder();
    
        if (path.size() < 1) return "No shortest path found.";
        else{
          String current = rd.simulatedIPAddress;
          s.append(current);
    
          while (path.size() >= 1){
            int cost = graph.GetWeight(current, path.get(0));
    
            if (cost == Integer.MAX_VALUE) return "Error in calculating cost of adjacent nodes.";
            else{
              s.append(" ---(");
              s.append(cost);
              s.append(")--> ");
    
              current = path.remove(0);
              s.append(current);
            }
          }
          s.append("\n");
        }
        return s.toString();
      }
    }
  }

  //initialize the linkstate database by adding an entry about the router itself
  private LSA initLinkStateDatabase() {
    LSA lsa = new LSA();
    lsa.linkStateID = rd.simulatedIPAddress;
    lsa.lsaSeqNumber = Integer.MIN_VALUE;
    LinkDescription ld = new LinkDescription();
    ld.linkID = rd.simulatedIPAddress;
    ld.portIndex = -1;
    ld.tosMetrics = 0;
    lsa.links.add(ld);
    return lsa;
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (LSA lsa: store.values()) {
      sb.append(lsa.linkStateID).append("(" + lsa.lsaSeqNumber + ")").append(":\t");
      for (LinkDescription ld : lsa.links) {
        sb.append(ld.linkID).append(",").append(ld.portIndex).append(",").
                append(ld.tosMetrics).append("\t");
      }
      sb.append("\n");
    }
    return sb.toString();
  }

}
