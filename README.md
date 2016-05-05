# cocktail-slackbot

FIXME: description

## Installation

Download from http://example.com/FIXME.

## Usage

FIXME: explanation

    $ java -jar cocktail-slackbot-0.1.0-standalone.jar [args]

## Options

FIXME: listing of options this app accepts.

## Internals

Cocktail Waiter

 - receives messages from Slack

 - handles the following messages:

   - @cocktails I will make cocktails today
   - @cocktails I will make old fashioned
   - @cocktails +N
   - @cocktails -N
   - @cocktails show status

 - keeps track of who's making cocktails, what they are making, and +1's

 - If no-one has volunteered to make cocktails by 2pm Friday, will
   post a question to the channel

 - Once someone has volunteered, sends a request for +1's

 - At 3.50pm, send a call for last-orders

 - At 4pm, send a message saying who is making cocktails

Event Store

 - Persist events to log, store current state in an atom

Slack Client

 - Manage a WebSocket connection. Forward messages from the waiter to the
 Slack channel.

Copyright © 2016 Ray Miller <ray@1729.org.uk>

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
