#!/usr/bin/env python3

class Topic():

    base_url = "https://www.jeuxvideo.com"


    def __init__(self, url):
        self.url = self.base_url +  url

    def __eq__(self, obj):
        if isinstance(obj, Topic):
            return self.url == obj.url
        return False
    
    def __hash__(self):
        return hash(self.url)
    
    