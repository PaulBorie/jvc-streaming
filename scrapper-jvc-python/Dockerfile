FROM python:3.8-slim-buster

WORKDIR /usr/local/bin

RUN  apt-get update \
    && apt-get install -y gconf-service \
    && apt-get install -y libasound2 \
    && apt-get install -y libatk1.0-0 \
    && apt-get install -y libc6 \
    && apt-get install -y libcairo2 \
    && apt-get install -y libcups2 \
    && apt-get install -y libdbus-1-3 libexpat1 libfontconfig1 libgcc1 libgconf-2-4 libgdk-pixbuf2.0-0 libglib2.0-0 libgtk-3-0 libnspr4 libpango-1.0-0 libpangocairo-1.0-0 libstdc++6 libx11-6 libx11-xcb1 libxcb1 libxcomposite1 libxcursor1 libxdamage1 libxext6 libxfixes3 libxi6 libxrandr2 libxrender1 libxss1 libxtst6 ca-certificates fonts-liberation libappindicator1 libnss3 lsb-release xdg-utils wget


COPY requirements.txt requirements.txt
RUN pip3 install -r requirements.txt
COPY . .
EXPOSE 6667

CMD [ "python3", "-u", "scraper-jvc.py"]
