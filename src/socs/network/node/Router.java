package socs.network.node;

import socs.network.util.Configuration;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;


public class Router {

  protected LinkStateDatabase lsd;

  RouterDescription rd = new RouterDescription();

  Link[] ports = new Link[4];

  public Router(Configuration config) {
    Thread serverThread;

    try {
      this.rd.processIPAddress = java.net.InetAddress.getLocalHost().getHostAddress();
    } catch (Exception e) {
      System.err.println(e.toString());
    	System.exit(1);
    }
    this.rd.processPortNumber = config.getShort("socs.network.router.port");
    this.rd.simulatedIPAddress = config.getString("socs.network.router.ip");
    this.lsd = new LinkStateDatabase(this.rd);
    
    //TODO: Move server spin up code to start function
    serverThread = new Thread(new ServerHandler(this.rd, this.lsd, this.ports));
    serverThread.start();
    System.out.println("New router instantiated with " + 
      "IP " + this.rd.processIPAddress + 
    	" Port: " + this.rd.processPortNumber +
    	"Simulated IP Address: " + this.rd.simulatedIPAddress);
  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * NOTE: this command should not trigger link database synchronization
   */
  private void processAttach(String processIP, short processPort, String simulatedIP, short weight) {
	  //TODO: Handle the weight input
    boolean alreadyAttached;
    int portIndex;
    RouterDescription remoteRouterDescription;

    //Check if there exists an available port or if it is already attached
	  alreadyAttached = false;
	  portIndex = -1;
	  for (int i = 0; i < this.ports.length && !alreadyAttached && portIndex < 0; i++) {
		  if (this.ports[i] == null) portIndex = i;
		  else {
			  if (this.ports[i].router2.simulatedIPAddress.equals(simulatedIP)) alreadyAttached = true;
		  }
	  }
	  
	  //Failure cases for the above
	  if (portIndex == -1) {
		  System.out.println("This router (" + this.rd.simulatedIPAddress + ") has no more ports available.");
		  return;
	  }
	  if (alreadyAttached) {
	      System.out.println(simulatedIP + " is already attached to this router");
	      return;
	  }
	  
	  //Success case
	  remoteRouterDescription = new RouterDescription(processIP, processPort, simulatedIP);
    this.ports[portIndex] = new Link(this.rd, remoteRouterDescription);
  }

  /**
   * broadcast Hello to neighbors
   */
  private void processStart() {
    Thread clientThread;
    
    for (Link link : ports){
      clientThread = new Thread(new ClientHandler(lsd, link));
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

  }

  /**
   * disconnect with the router identified by the given destination ip address
   * Notice: this command should trigger the synchronization of database
   *
   * @param portNumber the port number which the link attaches at
   */
  private void processDisconnect(short portNumber) {

  }

  /**
   * attach the link to the remote router, which is identified by the given simulated ip;
   * to establish the connection via socket, you need to indentify the process IP and process Port;
   * additionally, weight is the cost to transmitting data through the link
   * <p/>
   * This command does trigger the link database synchronization
   */
  private void processConnect(String processIP, short processPort,
                              String simulatedIP, short weight) {

  }

  /**
   * output the neighbors of the routers
   */
  private void processNeighbors() {

  }

  /**
   * disconnect with all neighbors and quit the program
   */
  private void processQuit() {

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
          processDisconnect(Short.parseShort(cmdLine[1]));
        } else if (command.startsWith("quit")) {
          processQuit();
        } else if (command.startsWith("attach ")) {
          String[] cmdLine = command.split(" ");
          processAttach(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("start")) {
          processStart();
        } else if (command.equals("connect ")) {
          String[] cmdLine = command.split(" ");
          processConnect(cmdLine[1], Short.parseShort(cmdLine[2]),
                  cmdLine[3], Short.parseShort(cmdLine[4]));
        } else if (command.equals("neighbors")) {
          //output neighbors
          processNeighbors();
        } else {
          //invalid command
          break;
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
