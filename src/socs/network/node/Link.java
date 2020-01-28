package socs.network.node;

public class Link {

  RouterDescription router1;	//Router's Socket
  RouterDescription router2;	//Remote Socket

  public Link(RouterDescription r1, RouterDescription r2) {
    router1 = r1;
    router2 = r2;
  }
}
