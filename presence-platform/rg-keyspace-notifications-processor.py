def show(x):
    log(f"Key '{x}'", level='warning')

gb = GB('KeysReader')
gb.foreach(show)
gb.foreach(lambda x: execute('XADD', 'presence', '*', 'status', 'OFFLINE', 'client', x['key']))
gb.register(prefix='*',
            mode='sync',
            eventTypes=['expired'],
            readValue=False)

gb = GB('KeysReader')
gb.foreach(show)
gb.foreach(lambda x: execute('XADD', 'presence', '*', 'status', 'ONLINE', 'client', x['key']))
gb.register(prefix='*',
            mode='sync',
            eventTypes=['set'],
            readValue=False)
