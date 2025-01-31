package socs.network.message;

import java.io.*;
import java.util.Vector;

public class SOSPFPacket implements Serializable {

  //for inter-process communication
  public String srcProcessIP;
  public short srcProcessPort;

  //simulated IP address
  public String srcIP;
  public String dstIP;

  //common header
  public SOSPFType sospfType;
  public String routerID;

  //used by HELLO message to identify the sender of the message
  //e.g. when router A sends HELLO to its neighbor, it has to fill this field with its own
  //simulated IP address
  public String neighborID; //neighbor's simulated IP address

  //used by HELLO
  public int cost = Integer.MAX_VALUE;

  //used by LSAUPDATE
  public Vector<LSA> lsaArray = new Vector<LSA>();

}
