package socs.network.node;

public class RouterDescription {
  
	//Used for socket communication
	String processIPAddress;
	short processPortNumber;
	
	//Used to identify the router in the simulated network space
	String simulatedIPAddress;
	
	//Status of the router
	RouterStatus status;
	
	public RouterDescription() {}
	
	public RouterDescription(String processIP, short processPort, String simulatedIP) {
		this.processIPAddress = processIP;
		this.processPortNumber = processPort;
		this.simulatedIPAddress = simulatedIP;
	}
}
