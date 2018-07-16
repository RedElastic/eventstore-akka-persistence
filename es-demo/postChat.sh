#!/bin/bash
jq -n --arg UUID $(uuidgen) '[ { "eventId": $UUID, "eventType": "match", "data" : { "user1": "user-A", "user2": "user-B" } } ]' | curl -i -H "Content-Type:application/vnd.eventstore.events+json" "http://localhost:2113/streams/chat" -d @-
