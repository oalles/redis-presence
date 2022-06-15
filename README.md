# Description
The goal is to implement a `presence platform` based on SSE and Redis (Streams, RedisGears, redisjson, ...)

Realtime Platform will have two channels - Redis Streams -with the Presence Platform: 
* `Heartbeat Upstream`: RT platform will periodically publish ONLINE client ids to the Presence Platform. [ id | server ]
* `Presence Downstream`: Presence platfrom will publish `presence messages` [server: | clientId: | status: 'OFFLINE''] to that stream. 

Presence platform logic will be implemented as RedisGears functions, event processors. ¿Cómo puedo mostrar estos diagramas, ver en redisgears examples?

1. Heartbeat upstream processor: `SET key: online.clients.id - value: server` + `EXPIRE key: online.clients.id 1.5 * d` where d is the heartbeat frequency
2. Keyspace notifications subscriber:
        * on key expiration -> produce a presences message: [server: | clientId: | status: 'OFFLINE'']
        * on key creation -> produce a presences message: [server: | clientId: | status: 'ONLINE'']
        * on key update or key expiration update: do nothing. 
NOTA: Cambiar Java. On every message in the presence stream, notifyClientStatusToAllBut the id in the PresenceMessage

Useful Links: 
* [Keyspace Notifications](https://redis.io/docs/manual/keyspace-notifications/)
* [Keyspace Notifications how to](https://medium.com/nerd-for-tech/redis-getting-notified-when-a-key-is-expired-or-changed-ca3e1f1c7f0a)
* [Redisgears](https://oss.redis.com/redisgears) 
* [Keyspace notifications using RedisGears](https://medium.com/@vsharathis/redis-journey-and-keyspace-notification-processing-using-redisgears-6811edb888f8)
* [EXPIRE command](https://redis.io/commands/expire/)


## Must Read
> Key expiration IS NOT REAL TIME, while it seems like a real time if you try on local (small key), it’s already stated in the doc that the key might not be notified real time upon expiration event due to redis expiration logic (read the Timing of expired events section of the doc)

[Timing of Expired Events](https://redis.io/docs/manual/keyspace-notifications/#timing-of-expired-events)
