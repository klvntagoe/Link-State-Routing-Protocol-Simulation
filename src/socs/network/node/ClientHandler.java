package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

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
        SOSPFPacket helloMessageToSend, helloMessageRecieved;
        ObjectInputStream in;
        ObjectOutputStream out;

        try{
            _clientSocket = new Socket(_link.router2.processIPAddress, _link.router2.processPortNumber);
            
            out = new ObjectOutputStream(_clientSocket.getOutputStream());
            in = new ObjectInputStream(_clientSocket.getInputStream());

            //Send First Hello
            helloMessageToSend = new SOSPFPacket();
            helloMessageToSend.srcProcessIP = this._link.router1.processIPAddress;
            helloMessageToSend.srcProcessPort = this._link.router1.processPortNumber;
            helloMessageToSend.srcIP = this._link.router1.simulatedIPAddress;
            helloMessageToSend.dstIP = this._link.router2.simulatedIPAddress;
            helloMessageToSend.sospfType = SOSPFType.HELLO;
            helloMessageToSend.routerID = this._link.router1.simulatedIPAddress;
            helloMessageToSend.neighborID = this._link.router1.simulatedIPAddress;
            out.writeObject(helloMessageToSend);

            //Recieve First Hello
            helloMessageRecieved = (SOSPFPacket) in.readObject();
            System.out.println("Recieved HELLO from " + helloMessageRecieved.srcIP);

            //Set remote router status to TWO_WAY
            this._link.router2.status = RouterStatus.TWO_WAY;
            System.out.println("Set " + helloMessageRecieved.srcIP + " state to TWO_WAY");
            
            //Send Second Hello
            out.writeObject(helloMessageToSend);
                
            //Synchronize link state databases
            _clientIsRunning = true;
            while(_clientIsRunning){
                _clientIsRunning = false;
            }
            _clientSocket.close();
        }catch(Exception e){
            System.err.println(e.toString());
            System.exit(1);
        }
    }
}