def show(x):
    log(f"Key '{x}'", level='warning')
    # log(f"Key '{x['value']['key']}' expired at {x['id'].split('-')[0]}", level='warning')


gb = GB('KeysReader')
gb.foreach(show)
gb.foreach(lambda x: execute('XADD', 'presence', '*', 'status', 'OFFLINE', 'client', x['key']))
gb.register(prefix='*',
            mode='async',
            eventTypes=['expired'],
            readValue=True)
