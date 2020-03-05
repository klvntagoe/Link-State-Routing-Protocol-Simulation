package socs.network.node;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;

import socs.network.message.SOSPFPacket;

public class Link {

  RouterDescription router1;	//Router's Socket
  
  RouterDescription router2;	//Remote Socket

  int cost;
  
  Queue<SOSPFPacket> PacketQueue;
  
  Semaphore lock;

  public Link(RouterDescription r1, RouterDescription r2, int weight) {
    this.router1 = r1;
    this.router2 = r2;
    this.cost = weight;
    this.PacketQueue = new LinkedList<SOSPFPacket>();
    this.lock = new Semaphore(1);
  }
}
