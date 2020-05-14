import re

import scrapy

from nytimes.items import NytimesItem


class NYTimesSpider(scrapy.Spider):
    name = "nytimes"
    allowed_domains = [
        "spiderbites.nytimes.com",
        "www.nytimes.com",
    ]
    start_urls = [
        "https://spiderbites.nytimes.com/2017/",
        "https://spiderbites.nytimes.com/2018/",
        "https://spiderbites.nytimes.com/2019/",
    ]
    ids_seen = set()

    def parse(self, response):
        for url in response.xpath('//div[@class="articlesMonth"]//li/a/@href').extract():
            url = "https://" + self.allowed_domains[0] + url
            yield scrapy.Request(url, callback=self.parse2)

    # handle month list
    def parse2(self, response):
        for url in response.xpath('//*[@id="headlines"]//li/a/@href').extract():
            yield scrapy.Request(url, callback=self.parse3)

    # handle NYTimes web page
    def parse3(self, response):
        url = re.sub(r'html\?.*', 'html', response.url)
        if url in self.ids_seen:
            return

        self.ids_seen.add(url)

        try:
            title = response.xpath('//h1/span[1]/text()').extract()[0]
        except IndexError:
            title = ""
        try:
            author = response.xpath('//header//p//span/text()').extract()
            if author[0] == '›':
                author = author[1:]
        except IndexError:
            author = []
        content = response.xpath('//*[@id="story"]/section//p').extract()
        to = []
        for link in response.xpath('//*[@id="story"]/section//p/a/@href').extract():
            if link.startswith("https://" + self.allowed_domains[1]):
                link = re.sub(r'html\?.*', 'html', link)
                to.append(link)
                if link not in self.ids_seen:
                    yield scrapy.Request(link, callback=self.parse3)

        item = NytimesItem()
        item['url'] = url
        item['title'] = title
        item['author'] = author
        item['content'] = content
        item['to'] = to
        yield item
