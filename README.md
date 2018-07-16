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
curl -i "http://localhost:2113/projections/continuous?name=chat-matches&emit=true" -d @chatmatch-projection.json -u admin:changeit
curl http://localhost:9000/user/user-A
curl -i -H "Content-Type:application/vnd.eventstore.events+json" "http://localhost:2113/streams/chat" -d @chatMatch.json
```

```sh
curl -i -X DELETE http://localhost:2113/projction/chat-matches -u admin:changeit
```

Note that EventStore will ignore events with an existing EventID, the following shell magic will insert a generated UUID to work around this:
```
uuid=$(uuidgen); cat chatMatch.json | sed s/UUID/$uuid/ | curl -i -H "Content-Type:application/vnd.eventstore.events+json" "http://localhost:2113/streams/chat" -d @-
```

# Caveats

Deleting a stream: 
- soft-delete: If stream is recreated the version will not start at zero, this is an issue for the Akka persistence plugin.
- hard-delete: Can never create a new stream with same name. Can not restore the stream. Essentially that stream and its name are forever lost.

# Evaluation

The ability to create custom projections and queries on the fly is very powerful. This allows for both construction of view models as well as ad hoc querying of the system.