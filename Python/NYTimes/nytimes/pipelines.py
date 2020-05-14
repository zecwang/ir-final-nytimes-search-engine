# -*- coding: utf-8 -*-

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html
from scrapy.exceptions import DropItem


class NytimesPipeline(object):

    # def __init__(self):
    #     self.ids_seen = set()

    def process_item(self, item, spider):
        if len(item['content']) != 0:
            return item
        else:
            raise DropItem("Empty content in %s" % item)
        # if len(item['content']) != 0:
        #     if item['url'] in self.ids_seen:
        #         raise DropItem("Duplicate item found: %s" % item)
        #     else:
        #         self.ids_seen.add(item['url'])
        #         return item
        # else:
        #     raise DropItem("Empty content in %s" % item)
