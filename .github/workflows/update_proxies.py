import base64
import json
import urllib.parse
from collections import defaultdict

import requests
import yaml
from test_proxies import test_proxies

CONFIG_URL = "https://raw.githubusercontent.com/oezeb/ClashX-V2Ray-TopFreeProxy/main/combine/clash.config.yaml"
IP_API = "http://ip-api.com/json"

def get(url):
    print("GET", url)
    try:
        response = requests.get(url, timeout=10)
        if response.status_code == 200:
            return response.text
    except Exception as e:
        print(e)
    return None

def config2url(config):
    """Convert Shadowsocks config to URL"""
    userInfo = '%s:%s' % (config['method'], config['password'])
    host = '%s:%s' % (config['server'], config['server_port'])
    plugin = None
    if 'plugin' in config:
        plugin = '%s;%s' % (config['plugin'], config['plugin_opts'])
        plugin = urllib.parse.quote(plugin)

    userInfo = base64.b64encode(userInfo.encode()).decode()
    userInfo = userInfo.replace('=', '')


    url = 'ss://%s@%s' % (userInfo, host) 
    if plugin:
        url += '?plugin=%s' % plugin
    if 'name' in config:
        url += '#%s' % config['name']
    
    return url

def main():
    content = get(CONFIG_URL)
    if content is None:
        return
    
    data = yaml.load(content, Loader=yaml.FullLoader)
    proxies = [proxy for proxy in data["proxies"] if proxy["type"] == "ss"]
    results = test_proxies(proxies)
    proxies = [proxy for proxy, delay, _ in results if delay is not None]

    text, names = "", defaultdict(int)
    for proxy in proxies:
        print(proxy['name'])
        if 'plugin' in proxy and proxy['plugin'] != 'obfs':
            continue

        url = f"{IP_API}/{proxy['server']}?fields=status,countryCode,regionName"
        content = get(url)
        if content is None:
            continue
        
        data = json.loads(content)
        if data["status"] == "fail":
            continue

        name = data['countryCode']
        if 'regionName' in data:
            name += "-" + data['regionName']
        names[name] += 1
        if names[name] > 1:
            sep = "-" if len(name) == 2 else " "
            name += sep + str(names[name])

        proxy['name'] = urllib.parse.quote(name)
        proxy['method'] = proxy.pop('cipher')
        proxy['server_port'] = proxy.pop('port')
        if 'plugin' in proxy:
            proxy['plugin'] = 'obfs-local'
            proxy['plugin-opts']['obfs'] = proxy['plugin-opts'].pop('mode')
            proxy['plugin-opts']['obfs-host'] = proxy['plugin-opts'].pop('host')
            proxy['plugin_opts'] = ';'.join(['%s=%s' % (k, v) for k, v in proxy['plugin-opts'].items()])
        
        text += config2url(proxy) + "\n"
    
    with open("proxies.txt", "w") as f:
        f.write(text)
        
if __name__ == "__main__":
    main()
