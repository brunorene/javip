# javip
A VERY simple Load Balancer developed in pure Java

## How to run

1. mvn package
2. comma separated servers:ports inside jetty.xml (example on src/main/resources)
3. java -jar jetty-runner-9.2.9.jar --config jetty.xml pai-vip-2.0-SNAPSHOT.war

## Stuff missing

1. redirect to other servers in case of error
