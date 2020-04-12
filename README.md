# Link-State-Routing-Protocol-Simulation

## Commands:
#### attach [Process IP] [Process Port] [IP Address] [Link Weight]: <br>
Establishes a link to the remote router which is identified by [IP Address]. This command does not actually instantiates the TCP communication to the proposed routers to connect to.

#### start:<br>
Start this router and initialize the database synchronization process. After you establish the links by running attach , you will run start command to send HELLO messages and LSAUPDATE to all connected routers for the Link State Database synchronization. This command can only be run after start.

#### detect [IP Address]:<br>
Output the routing path from this router to the destination router which is identified by [IP Address].

#### neighbors:<br>
Output the IP Addresses of all neighbors of the router where you run this command.

#### disconnect [Port Number]:<br>
Remove the link between this router and the remote one which is connected at port [Port Number] (port number is between 0 - 3, i.e. four links in the router). Through this command, you are triggering the synchronization of Link State Database by sending LSAUPDATE message to all neighbors in the topology.

#### quit:
Exit the program. NOTE, this will trigger the synchronization of link state database.
