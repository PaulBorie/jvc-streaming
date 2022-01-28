#!/usr/bin/env python3

from requests_html import AsyncHTMLSession
import asyncio
import topic
from bs4 import BeautifulSoup
import post
import aioredis
import socket
import os

# Server info
PORT = 6667
REDIS_HOST = os.getenv("REDIS_HOST", default="localhost")
# clients connecting to the tcp server, they will receive the stream of jvc posts
clients = []

# data for testing
topics = (topic.Topic("/forums/42-51-68703732-1-0-1-0-juge-en-comparution-immediate-martin-1-a-ete-condamne-a-huit-mois-ferme.htm"), )

pages = ("https://www.jeuxvideo.com/forums/0-51-0-1-0-1-0-blabla-18-25-ans.htm", "https://www.jeuxvideo.com/forums/0-51-0-1-0-26-0-blabla-18-25-ans.htm")

async def get_new_messages(session, topic):
    #print(topic.url)
    
    redis = aioredis.from_url("redis://{}".format(REDIS_HOST), decode_responses=True)
    resp = await session.get(topic.url, timeout=15)

    soup = BeautifulSoup(resp.text, 'html.parser')

    title = soup.find("span", {"id": "bloc-title-forum"}).text
    divs = soup.find_all("div", class_= "bloc-message-forum mx-2 mx-lg-0")

    # Si le topic a déjà été traité, il est dans la bdd redis, on récupère l'id du dernier comm qu'on avait traité'
    if await redis.exists(title):
        last_id = await redis.get(title)
    else:
        last_id = 0

    for i, div in enumerate(divs):
        try:
            # Id
            id = div["data-id"]            

            # Message
            enrichi_text = div.findChild("div", class_ = "txt-msg text-enrichi-forum")
            paragraphes = enrichi_text.find_all("p", recursive=False)
            message = ""
            for p in paragraphes:
                message += p.text
               
            # Pseudo
            pseudo = div.findChild("img").get("alt")
            # Posted_hour
            posted_hour = div.findChild("div", class_ = "bloc-date-msg").find("span").text

            # Stickers
            stickers = []            
            img_stickers = enrichi_text.find_all("img")
            for sticker in img_stickers:
                if sticker["alt"] is not None and sticker["alt"].startswith("http"):
                    stickers.append(sticker["alt"])
                else:
                    stickers.append(sticker["src"])

            # Post object creation
            p = post.Post(id, posted_hour, title, pseudo, message, stickers)
            
            if p.id_is_after(last_id):
                await send_post(p)         
                
                ##Je suis le dernier post trouvé sur le topic je met un "marque page" dans la bdd redis pour revenir à ce message là la prochaine fois que je visite le topic
                if i == len(divs)-1:
                    await redis.set(title, id)

        except Exception as e:
            print(e)


async def send_post(post):
    print(post)
    print("\n/////////////\n")
    loop = asyncio.get_event_loop()
    for client in clients:  
        try:
            await loop.sock_sendall(client, post.toJSON() + '\n'.encode())  
        except Exception as e:
            print(e)
            client.close() 
            clients.remove(client)
     
 
async def get_last_topics(numero_page: int, q: asyncio.Queue, session: AsyncHTMLSession) -> None:
    while True:
        try:
            resp =  await session.get(pages[numero_page], timeout=10)
            await resp.html.arender(timeout=20)
            links = resp.html.xpath("//span[@class='topic-date']/a/@href")[1:]
            for link in links:
                t = topic.Topic(link) 
                await q.put(t)
            await asyncio.sleep(1)
        except Exception as e:
            print(e)
        

async def consume(q: asyncio.Queue, session: AsyncHTMLSession) -> None:
    while True:
        try:
            t = await q.get()
            await get_new_messages(session, t)
            q.task_done()
        except Exception as e:
            print(e)


async def run_server():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('', PORT))
    server.listen(8)
    server.setblocking(False)
    loop = asyncio.get_event_loop()
    while True:
        client, addr = await loop.sock_accept(server)
        print('Connected by', addr)
        clients.append(client)
        await asyncio.sleep(10)

    
async def main():
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('', PORT))
    server.listen(1)
    conn, addr = server.accept()
    print('Connected by', addr)

async def main2():
    s = AsyncHTMLSession()  
    q = asyncio.Queue()
    producers = [asyncio.create_task(get_last_topics(n,q,s)) for n in range(1)]
    consumers = [asyncio.create_task(consume(q,s)) for n in range(80)]
    server = [asyncio.create_task(run_server())]
    await asyncio.gather(*server)
    await asyncio.gather(*producers)
    await asyncio.gather(*consumers)

if __name__ == "__main__":  
    asyncio.run(main2())
    


