def show(x):
    print(x)

# Create and register a gear that for each message in the stream
gb = GearsBuilder('StreamReader')
gb.map(lambda x: x['value'])
# gb.foreach(show)
gb.foreach(lambda x: execute('SETNX', x['client'],  x['server']))
gb.foreach(lambda x: execute('EXPIRE', x['client'], 20))
gb.register('heartbeat', mode='sync', readValue=True)
# gb.register('heartbeat')
