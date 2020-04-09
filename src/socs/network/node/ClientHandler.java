package socs.network.node;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;

import socs.network.message.SOSPFPacket;
import socs.network.message.SOSPFType;

public class ClientHandler extends Handler {

    public ClientHandler(RouterDescription rd, LinkStateDatabase lsd, Link[] ports, int index) {
        super(rd, lsd, ports, index);
    }

    public void run() {
        Link link;
        ObjectInputStream in;
        ObjectOutputStream out;
        Socket socket;
        SOSPFPacket helloMessageToSend, helloMessageRecieved;

        try {
            link = this._ports[this._linkIndex];
            socket = new Socket(link.router2.processIPAddress, link.router2.processPortNumber);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // Send First Hello
            helloMessageToSend = NetworkHelper.constructHelloPacketToBroadcast(this._rd, this._ports, link, this._lsd);
            out.writeObject(helloMessageToSend);

            // Recieve First Hello
            helloMessageRecieved = (SOSPFPacket) in.readObject();
            if (helloMessageRecieved.sospfType != SOSPFType.HELLO) {
                in.close();
                out.close();
                socket.close();
                throw new Exception("HELLO message was never recieved");
            } else
                System.out.println("Recieved HELLO from " + helloMessageRecieved.srcIP);

            // Set remote router status to TWO_WAY
            link.router2.status = RouterStatus.TWO_WAY;
            System.out.println("Set " + helloMessageRecieved.srcIP + " state to TWO_WAY");

            // Send Second Hello
            out.writeObject(helloMessageToSend);

            // Close socket connection
            in.close();
            out.close();
            socket.close();

            // Update Database
            NetworkHelper.UpdateDatabaseWithNewRouterInformation(this._rd, this._ports, this._lsd);
            NetworkHelper.UpdateDatabase(this._lsd, helloMessageRecieved);

            // broadcast frist LSA
            ForwardLSA(NetworkHelper.constructUpdatePacketToBroadcast(this._rd, this._ports, this._linkIndex, this._lsd), helloMessageRecieved.srcIP);
        } catch (ConnectException e) {
            System.err.printf("The router with IP address %s is not listening to it's port.\n",
                    this._ports[this._linkIndex].router2.simulatedIPAddress);
            this._ports[this._linkIndex] = null;
        }catch(Exception e){
            System.err.println(e.toString());
            //System.exit(1);
        }
    }
}