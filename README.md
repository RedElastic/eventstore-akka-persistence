Demo of very basic event sourcing setup through Akka Persistence and Event Store, with a currently incomplete illustration of aggregate consumption of foreign events (in this case mapping partner match events produced by an external component to the context of the users mentioned in that match event). 

This code depends on an instance of Event Store running, which can be initiated by:
```sh
docker run --name eventstore-node -it -p 2113:2113 -p 1113:1113 eventstore/eventstore
```


Example EventStore projection (`partnerfound-proj.json`):
```javascript
fromStream('chat')
.when({
    $init: function(){
        return {
        }
    },
    "match": function(s,e){
        const p = e.data;
        emit(`partner-user-${p.user1}`, "PartnerFound", { partnerId: p.user2 });
        emit(`partner-user-${p.user2}`, "PartnerFound", { partnerId: p.user1 });
    }
})
```

Chat event (`chatMatch.json`):
```json
[
  {
    "eventId": "e54890a7-5120-4c8d-b8d5-76f10d77210b",
    "eventType": "match",
    "data": {
      "user1": "user-1",
      "user2": "user-2"
    }
  }
]
```

```sh
sbt run
```

```sh
curl -i "http://localhost:2113/projections/continuous?name=chat-matches&emit=true" -d @chatmatch-projection.json
curl http://localhost:9000/user/user-A
curl -i -H "Content-Type:application/vnd.eventstore.events+json" "http://localhost:2113/streams/chat" -d @chatMatch.json
```