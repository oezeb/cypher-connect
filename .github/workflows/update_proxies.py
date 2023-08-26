import base64
import json
import urllib.parse
from datetime import datetime

import requests
import yaml

CONFIG_URL = "https://raw.githubusercontent.com/WilliamStar007/ClashX-V2Ray-TopFreeProxy/main/combine/clash.config.yaml"
IP_API = "http://ip-api.com/json"
LOG_FILE = ".github/workflows/update.log"

def get(url):
    try:
        response = requests.get(url, timeout=10)
        if response.status_code == 200:
            return response.text
    except Exception as e:
        with open(LOG_FILE, "a") as f:
            f.write(f"{datetime.now()} Error getting {url}: {e}\n")
    return None

def config2url(config):
    """
    Config format:
    ```
    {
        "name": "Proxy Name",
        "type": "ss",
        "server": "server",
        "server_port": 1234,
        "method": "cipher",
        "password": "password",
        "plugin": "obfs-local",
        "plugin-opts": "obfs=tls;obfs-host=example.com"
    }
    ```
    """
    userInfo = '%s:%s' % (config['method'], config['password'])
    host = '%s:%d' % (config['server'], config['server_port'])
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
    text = ""
    for proxy in proxies:
        print(proxy['name'])
        if 'plugin' in proxy and proxy['plugin'] != 'obfs':
            continue

        url = f"{IP_API}/{proxy['server']}?fields=status,countryCode"
        content = get(url)
        if content is None:
            continue
        
        data = json.loads(content)
        if data["status"] == "fail":
            continue
        
        proxy['name'] = data['countryCode']
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