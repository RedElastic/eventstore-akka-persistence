Demo of very basic event sourcing setup through Akka Persistence and Event Store, with a currently incomplete illustration of aggregate consumption of foreign events (in this case mapping partner match events produced by an external component to the context of the users mentioned in that match event). 

This code depends on an instance of Event Store running, which can be initiated by:
```text
docker run --name eventstore-node -it -p 2113:2113 -p 1113:1113 eventstore/eventstore
```