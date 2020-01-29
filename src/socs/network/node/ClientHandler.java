import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private LinkStateDatabase _lsd;

    private Link _link;

    private Socket _clientSocket;

    private boolean _clientIsRunning = false;
    
    public ClientHandler(LinkStateDatabase lsd, Link link){
        this._lsd = lsd;
        this._link = link;
    }

    public void run(){
        _clientIsRunning = true;
        while(_clientIsRunning){
            try {
                _clientSocket = new Socket(_link.router2.processIPAddress, _link.router2.processPortNumber);
                System.out.println();
    
                DataOutputStream out = new DataOutputStream(_clientSocket.getOutputStream());
                out.writeUTF();
    
                DataInputStream in = new DataInputStream(_clientSocket.getInputStream());
                System.out.println("Server says " + in.readUTF());
      
              } catch(Exception e){
                  System.err.println(e.toString());
                  System.exit(1);
              }
        }
        //TODO: should server.close() be placed here?
          _clientSocket.close();

    }
    
}