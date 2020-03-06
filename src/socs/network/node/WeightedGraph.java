package socs.network.node;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import socs.network.message.LSA;
import socs.network.message.LinkDescription;

public class WeightedGraph {

    public final int length;
    private String[] _names;        //Map between vertex name and graph ID
    private boolean[][] _neighbors; //Boolean matrix to indicate if pairs of vertices are neighbors
    private int[][] _weights;       //Integer matrix to store weights of edges connected by adjacent vertices

    public static WeightedGraph LoadLinkStateDatabase(HashMap<String, LSA> store){
        if (store.values().size() < 1) return null;
        else return new WeightedGraph(store);
    }

    public int GetWeight(String a, String b){
        int u = GetIndexOfName(a);
        int v = GetIndexOfName(b);
        if (u != -1 && v != -1 && _neighbors[u][v]) return _weights[u][v];
        else return Integer.MAX_VALUE;
    }

    public ArrayList<String> FindShortestPath(String src, String dest){
        int source = GetIndexOfName(src);
        int destination = GetIndexOfName(dest);
        if (source == -1 || destination == -1) return new ArrayList<String>();
        else{
            ArrayList<Integer> pathIndices = ShortestPath(source, destination);
            ArrayList<String> path = new ArrayList<String>();
            if (pathIndices.size() >= 1){
                for (int x : pathIndices) path.add(_names[x]);
            }
            return path;
        }
    }

    private WeightedGraph(HashMap<String, LSA> store){
        HashMap<String, Integer> nodes = IdentifyNodes(store);
        this.length = nodes.size();
        this._names = new String[length];
        this._neighbors = new boolean[length][length];
        this._weights = new int[length][length];

        for (Map.Entry<String, Integer> e : nodes.entrySet()){
            this._names[e.getValue()] = e.getKey();
        }
        for (int i = 0; i < length; i++){
            Arrays.fill(this._neighbors[i], false);
            Arrays.fill(this._weights[i], 0);
        }
        for (LSA lsa : store.values()){
            int index = GetIndexOfName(lsa.linkStateID);

            for (LinkDescription linkDescription : lsa.links){
                int neighborIndex = GetIndexOfName(linkDescription.linkID);

                _neighbors[index][neighborIndex] = true;
                _weights[index][neighborIndex] = linkDescription.tosMetrics;

                _neighbors[neighborIndex][index] = true;
                _weights[neighborIndex][index] = linkDescription.tosMetrics;
            }
        }
    }

    public HashMap<String, Integer> IdentifyNodes(HashMap<String, LSA> db){
        int index = 0;
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        for (LSA lsa : db.values()){
            //Store ID of LSA
            if (!map.containsKey(lsa.linkStateID)){
                map.put(lsa.linkStateID, index);
                index++;
            }
            //Store IDs of Links
            for (LinkDescription linkDescription : lsa.links){
                if (!map.containsKey(linkDescription.linkID)){
                    map.put(linkDescription.linkID, index);
                    index++;
                }
            }
        }
        return map;
    }

    private int GetIndexOfName(String simulatedIPAddress) {
        for (int i = 0; i < this._names.length; i++) {
            if (this._names[i].equals(simulatedIPAddress))  return i;
        }
        return -1;
    }

    private ArrayList<Integer> ShortestPath(int source, int destination){
        //Initializations
        boolean [] visited = new boolean[length];
        int [] distanceToSource = new int[length];
        int [] parent = new int[length];    
        Arrays.fill(visited, false);
        Arrays.fill(distanceToSource, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);

        //Trigger algorithm to compute shortest path from source node
        distanceToSource[source] = 0;

        //Base case for path recovery
        parent[source] = source;

        //Compute Shortest Path
        int closestUnvisitedNodeToSource = -1;
        for (int i = 0; i < length; i++){
            int smallestDistanceToSource = Integer.MAX_VALUE;
            for (int candidateNode = 0; candidateNode < length; candidateNode++){
                if (smallestDistanceToSource > distanceToSource[candidateNode] && !visited[candidateNode]){
                    smallestDistanceToSource = distanceToSource[candidateNode];
                    closestUnvisitedNodeToSource = candidateNode;
                }
            }
            for (int currentNode = 0; currentNode < length; currentNode++){
                if (_neighbors[closestUnvisitedNodeToSource][currentNode] && !visited[currentNode]){
                    int edgeDistance = _weights[closestUnvisitedNodeToSource][currentNode];
                    if (distanceToSource[currentNode] > smallestDistanceToSource + edgeDistance){
                        distanceToSource[currentNode] = smallestDistanceToSource + edgeDistance;
                        parent[currentNode] = closestUnvisitedNodeToSource;
                    }
                }
            }
            visited[closestUnvisitedNodeToSource] = true;
        }

        //Recover Shortest Path
        ArrayList<Integer> path = new ArrayList<Integer>();
        while(source != destination){
            path.add(0, destination);
            destination = parent[destination];
            if (destination == -1) return new ArrayList<Integer>();
        }

        return path;
    }
}
