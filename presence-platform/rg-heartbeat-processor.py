def processHeartbeat(x):
    print(x)

# creating execution plane
# Create and register a gear that for each message in the stream
gb = GearsBuilder('StreamReader')
gb.foreach(processHeartbeat)
gb.register('heartbeat')
