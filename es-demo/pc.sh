#!/bin/bash
jq -n --arg UUID $(uuidgen) --arg U1 $1 --arg U2 $2 '[ { "eventId": $UUID, "eventType": "match", "data" : { "user1": $U1, "user2": $U2 } } ]' | curl -i -H "Content-Type:application/vnd.eventstore.events+json" "http://localhost:2113/streams/chat" -d @-
