package socs.network.node;

import java.util.ArrayList;
import java.util.HashMap;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

public class WeightedGraph {

    private String[] _names;
    private int[][] _adjacencyMatrix;

    public WeightedGraph(HashMap<String, LSA> store){
        int length = store.values().size();
        this._names = new String[length];
        this._adjacencyMatrix = new int[length][length];

        int index = 0;
        for (LSA lsa : store.values()){
            this._names[index] = lsa.linkStateID;
            index++;
        }
        for (LSA lsa : store.values()){
            index = getIndexOfName(lsa.linkStateID);
            for (LinkDescription linkDescription : lsa.links){
                int neighborIndex = getIndexOfName(linkDescription.linkID);
                _adjacencyMatrix[index][neighborIndex] = linkDescription.tosMetrics;
            }
        }
    }

    public ArrayList<String> FindShortestPath(String src, String dest){
        ArrayList<String> path = new ArrayList<String>();
        int source = getIndexOfName(src);
        int destination = getIndexOfName(dest);
        ArrayList<Integer> list = ShortestPath(source, destination);
        for (int x : list) path.add(_names[x]);
        return path;
    }

    private int getIndexOfName(String simulatedIPAddress) {
        for (int i = 0; i < this._names.length; i++) {
            if (this._names[i].equals(simulatedIPAddress))  return i;
        }
        return -1;
    }

    private ArrayList<Integer> ShortestPath(int source, int destination){
        ArrayList<Integer> list = new ArrayList<Integer>();
        return list;
    }
}
