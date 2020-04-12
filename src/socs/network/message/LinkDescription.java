package socs.network.message;

import java.io.Serializable;

public class LinkDescription implements Serializable {
  public String linkID;
  public int portIndex;
  public int tosMetrics;

  public String toString() {
    return linkID + ","  + portIndex + "," + tosMetrics;
  }
}
