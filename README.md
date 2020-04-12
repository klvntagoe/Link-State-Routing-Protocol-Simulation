# Link-State-Routing-Protocol-Simulation

## Requirements
* [Maven](https://maven.apache.org/)

## Instructions:
* To compile application:
```bash
mvn compile assembly:single
```
* To run one of the predefined routers (example router 1):
```bash
java -jar target/Link-State-Routing-Protocol-1.0-jar-with-dependencies.jar conf/router1.conf
```

## Commands:
#### attach [Process IP] [Process Port] [Simulated IP Address] [Link Weight]: <br>
Establishes a link to the remote router which is identified by [Simulated IP Address].

#### start:<br>
Starts current router and initialize the database synchronization process. After links are established by running attach, you can run this again to send TCP three-way handshakes and LSAUPDATEs to all connected routers for the Link State Database synchronization.

#### detect [Simulated IP Address]:<br>
Outputs the routing path from current router to the destination router which is identified by [Simulated IP Address].

#### neighbors:<br>
Outputs the Simulated IP Addresses of all neighbors of the current router.

#### disconnect [Simulated IP Address]:<br>
Remove the link between current router and the remote one which has IP address [Simulated IP Address]. Through this command, you are triggering the synchronization of Link State Database by sending LSAUPDATE message to all neighbors in the topology.

#### quit:
Exit the program. NOTE, this will trigger the synchronization of link state database.
