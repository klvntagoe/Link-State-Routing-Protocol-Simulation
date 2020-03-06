package socs.network.node;

public class Link {

  RouterDescription router1;	//Router's Socket
  
  RouterDescription router2;	//Remote Socket

  int cost;
  
  //Semaphore lock;

  public Link(RouterDescription r1, RouterDescription r2, int weight) {
    this.router1 = r1;
    this.router2 = r2;
    this.cost = weight;
    //this.lock = new Semaphore(1);
  }
}
