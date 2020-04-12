package socs.network.node;

import socs.network.message.SOSPFPacket;
import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  Link[] ports = new Link[4];

  private ServerSocket _serverSocket;

  private boolean _routerIsRunning = false;

  public Router(Configuration config) {
    short port;

    try{
      this.rd.simulatedIPAddress = config.getString("socs.network.router.ip");
  
      port = (short) ((Math.random() * (10000 - 1024)) + 1024);
      this.rd.processPortNumber = port;

      this.rd.processIPAddress = java.net.InetAddress.getLocalHost().getHostAddress();
    }catch(Exception e){
      System.err.println(e.toString());
      //System.exit(1);
    }

    this.lsd = new LinkStateDatabase(this.rd);
    
    System.out.println("New router instantiated with " + 
      "IP " + this.rd.processIPAddress + 
    	", Port: " + this.rd.processPortNumber +
    	", Simulated IP Address: " + this.rd.simulatedIPAddress);
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
    boolean alreadyAttached;
    int portIndex;
    RouterDescription remoteRouterDescription;

    //Check if there exists an available port or if it is already attached
	  alreadyAttached = false;
	  portIndex = -1;
	  for (int i = 0; i < this.ports.length && !alreadyAttached && portIndex < 0; i++) {
      if (this.ports[i] == null)
        portIndex = i;
      else if (this.ports[i].router2.simulatedIPAddress.equals(simulatedIP))
        alreadyAttached = true;
	  }
	  
	  //Failure cases for the above
	  if (alreadyAttached) {
      System.out.println(simulatedIP + " is already attached to this router");
      return;
    }
	  if (portIndex == -1) {
		  System.out.println("This router has no more ports available.");
		  return;
	  }
	  
	  //Success case
	  remoteRouterDescription = new RouterDescription(processIP, processPort, simulatedIP);
    this.ports[portIndex] = new Link(this.rd, remoteRouterDescription, weight);

    System.out.println(this.rd.simulatedIPAddress +
      " is now attached to  " + 
      remoteRouterDescription.simulatedIPAddress);
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    Thread clientThread;

    if (!this._routerIsRunning){
      Thread serverThread;

      try{
        this._serverSocket = new ServerSocket(this.rd.processPortNumber);
      }catch(Exception e){
        System.err.println(e.toString());
        //System.exit(1);
      }

      serverThread = new Thread(new Server(this._serverSocket, this.rd, this.lsd, this.ports));
      serverThread.start();

      this._routerIsRunning = true;
      System.out.println("This router is now running.");
    }

    for (int i = 0; i < ports.length; i++){
      Link link = ports[i];
      if (link == null) continue;
      if (link.router2.status == RouterStatus.TWO_WAY) continue;
      try{
        clientThread = new Thread(new ClientHandler(rd, lsd, ports, i));
        clientThread.start();
      }catch(Exception e){
        System.err.println(e.toString());
        //System.exit(1);
      }
    }
  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {
    if (!this._routerIsRunning) System.out.println("This router has not started yet.");
    else{
      int neigborCount = 0;
      try{
        for (Link link : this.ports){
          if (link == null) continue;
          if (link.router2.status != RouterStatus.TWO_WAY) continue;
          neigborCount++;
          System.out.println("IP Address of Neigbor " + 
            neigborCount + ": " + 
            link.router2.simulatedIPAddress);
        }
      }catch(Exception e){  //In case remote router decriptions is null
        System.err.println(e.toString());
        //System.exit(1);
      }
    }
  }

  /**
   * output the shortest path to the given destination ip
   * <p/>
   * format: source ip address  -> ip address -> ... -> destination ip
   *
   * @param destinationIP the ip adderss of the destination simulated router
   */
  private void processDetect(String destinationIP) {
    if (!this._routerIsRunning) System.out.println("This router has not started yet.");
    else{
      //System.out.println(lsd.toString());
      System.out.println(lsd.getShortestPath(destinationIP));
    }
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
  */
   
  private void processConnect(String processIP, short processPort, String simulatedIP, short weight) {
    if (!this._routerIsRunning) System.out.println("This router has not started yet.");
    else{
      boolean alreadyAttached = false;
      int portIndex = -1;
      Thread clientThread;
      
      for (int i = 0; i < this.ports.length && !alreadyAttached && portIndex < 0; i++){
        Link candidate = this.ports[i];
        if (candidate == null)
          portIndex = i;
        else if (candidate.router2.simulatedIPAddress.equals(simulatedIP) || candidate.router2.processPortNumber == processPort) 
          alreadyAttached = true;
      }
      if (alreadyAttached){
        System.out.println(simulatedIP + " is already attached to this router.");
        return;
      }
      if (portIndex < 0){
        System.out.println("This router has no available ports to allow for a new connection.");
        return;
      }
      this.ports[portIndex] = new Link(this.rd, new RouterDescription(processIP, processPort, simulatedIP), weight);
      try{
        clientThread = new Thread(new ClientHandler(rd, lsd, ports, portIndex));
        clientThread.start();
      }catch(Exception e){
        System.err.println(e.toString());
        //System.exit(1);
      }
    }
  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(String simulatedIPAddress) {
    Link link;
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket socket;
    SOSPFPacket packet, acknowledgmentPacket;

    if (!this._routerIsRunning) System.out.println("This router has not started yet.");
    else{
      int portIndex = -1;
      for (int i = 0; i < this.ports.length && portIndex < 0; i++){
        Link candidate = this.ports[i];
        if (candidate == null) continue;
        if (candidate.router2.simulatedIPAddress.equals(simulatedIPAddress)) portIndex = i;
      }
      if (portIndex < 0){
        System.out.println("This router does not have a link with IP Address = " + simulatedIPAddress + ".");
      }
      link = this.ports[portIndex];
      packet = NetworkHelper.constructByePacketToBroadcast(link);
      try{
          socket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
          out = new ObjectOutputStream(socket.getOutputStream());

          out.writeObject(packet);

          in = new ObjectInputStream(socket.getInputStream());
          acknowledgmentPacket = (SOSPFPacket) in.readObject();

          in.close();
          out.close();
          socket.close();
      }catch(Exception e){
          System.err.println(e.toString());
          //System.exit(1);
      }
      System.out.printf("Connection with %s is now terminated.\n", link.router2.simulatedIPAddress);
      this.ports[portIndex] = null;
      NetworkHelper.UpdateDatabaseWithNewRouterInformation(this.rd, this.ports, this.lsd);
      NetworkHelper.BroadcastLSA(this.rd, this.ports, this.lsd);
    }
  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {
    ObjectInputStream in;
    ObjectOutputStream out;
    Socket socket;
    SOSPFPacket packet, acknowledgmentPacket;

    for (Link link : this.ports){
      if (link == null) continue;
      packet = NetworkHelper.constructByePacketToBroadcast(link);
      try{
          socket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);
          out = new ObjectOutputStream(socket.getOutputStream());

          out.writeObject(packet);

          in = new ObjectInputStream(socket.getInputStream());
          acknowledgmentPacket = (SOSPFPacket) in.readObject();

          in.close();
          out.close();
          socket.close();
      }catch(Exception e){
          System.err.println(e.toString());
          //System.exit(1);
      }
      System.out.printf("Connection with %s is now terminated.\n", link.router2.simulatedIPAddress);
      link = null;
    }
    System.out.println("All connections closed. Router is shutting down.");
    this._routerIsRunning = false;
    System.exit(0);
  }

  public void terminal() {
    try {
      InputStreamReader isReader = new InputStreamReader(System.in);
      BufferedReader br = new BufferedReader(isReader);
      System.out.print(">> ");
      String command = br.readLine();
      while (true) {
        if (command.startsWith("detect ")) {
          String[] cmdLine = command.split(" ");
          processDetect(cmdLine[1]);
        } else if (command.startsWith("disconnect ")) {
          String[] cmdLine = command.split(" ");
          processDisconnect(cmdLine[1]);
        } else if (command.startsWith("quit")) {
          processQuit();
          break;
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.startsWith("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("neighbors")) {
          processNeighbors();
        } else {
          System.out.printf("Invalid command: %s\n", command);
        }
        System.out.print(">> ");
        command = br.readLine();
      }
      isReader.close();
      br.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
