# RedisEdge initialization script - RG scripts loader
import argparse
import os
from urllib.parse import urlparse

import redis

if __name__ == '__main__':

    # Parse arguments
    parser = argparse.ArgumentParser()
    parser.add_argument('-u', '--url', help='Redis URL', type=str, default='redis://localhost:6379')
    args = parser.parse_args()

    # Set up Redis connection
    url = urlparse(args.url)
    conn = redis.Redis(host=url.hostname, port=url.port)
    if not conn.ping():
        raise Exception('Redis unavailable')

    # Load the gears
    print('Loading gears - ', end='')
    dir_path = r'./'
    for file in os.listdir(dir_path):
        if file.startswith('rg-'):
            with open(file, 'rb') as f:
                gear = f.read()
                res = conn.execute_command('RG.PYEXECUTE', gear)
                print(res)

    print('Flag initialization as done - ', end='')
