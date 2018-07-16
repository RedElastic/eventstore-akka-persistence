#!/bin/bash
uuid=$(uuidgen)
cat chatMatch.json | sed/UID/$uuid/ | curl -i -H "Content-Type:application/vnd.eventstore.events+json" "http://localhost:2113/streams/chat" -d @-
