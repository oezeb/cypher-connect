"""
Test proxies
------------

The script uses [Clash](https://github.com/Dreamacro/clash) to test proxies.

For the proxies format see
https://dreamacro.github.io/clash/configuration/outbound.html#proxies
"""
import os
import subprocess
import threading
import urllib.parse
from queue import Queue

import requests
import yaml

HOST = "127.0.0.1"
PORT = 9090
TEST_URL = "http://www.gstatic.com/generate_204"
TEST_TIMEOUT = 1000 # ms
MAX_CONCURRENT = 25

def test_proxies(proxies: list):
    # Write proxies to config file
    filename =  "clash.config.yaml"
    with open(filename, "w") as f:
        yaml.dump({"proxies": proxies}, f)
    
    # Start clash
    container = "clash-test"
    cmd = f"""
        docker run -d --rm --network host \
            --name {container} \
            -v {os.path.abspath(filename)}:/{filename} \
            dreamacro/clash \
            -ext-ctl {HOST}:{PORT} \
            -f /{filename}
    """
    res = subprocess.run(cmd, shell=True)
    if res.returncode != 0:
        return []

    # # Test proxies
    q = Queue()
    for i in range(0, len(proxies), MAX_CONCURRENT):
        threads = []
        for proxy in proxies[i:i+MAX_CONCURRENT]:
            t = threading.Thread(target=_test_proxy, args=(proxy, q))
            t.start()
            threads.append(t)

        for t in threads:
            t.join()


    # Stop clash
    cmd = f"docker stop {container}"
    subprocess.run(cmd, shell=True)

    # Remove config file
    os.remove(filename)

    # return results
    results = []
    while not q.empty():
        results.append(q.get())
    return results

def _test_proxy(proxy: dict, q: Queue):
    test_url = urllib.parse.quote(TEST_URL)
    url = f"http://{HOST}:{PORT}/proxies/{proxy['name']}/delay?url={test_url}&timeout={TEST_TIMEOUT}"
    data = requests.get(url).json()
    delay, msg = data.get('delay'), data.get('message')
    print(proxy['name'], f"{delay}ms" if delay else msg)
    q.put((proxy, delay, msg))
