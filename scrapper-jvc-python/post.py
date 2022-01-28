#!/usr/bin/env python3

import json

class Post():

    def __init__(self, id, hour, topic, auteur, message, stickers):
        self.id = id
        self.hour = hour
        self.topic = topic
        self.auteur = auteur
        self.message = message
        self.stickers = stickers
    
    def __str__(self):
     return "Topic: {topic}\nPseudo: {auteur}\n{msg}\n{stickers}\n{hour}".format(id= self.id, topic=self.topic ,auteur=self.auteur, msg = self.message, hour = self.hour, stickers = self.stickers)
   
    
    def id_is_after(self, id):
        return int(self.id) > int(id)
        
    def toJSON(self):
        
        return json.dumps(self.__dict__, ensure_ascii=False).encode("utf-8")