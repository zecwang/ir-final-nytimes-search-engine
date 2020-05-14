from scrapy.downloadermiddlewares.useragent import UserAgentMiddleware
from nytimes.agents import USER_AGENT_LIST
import random


class RandomUserAgent(UserAgentMiddleware):
    def process_request(self, request, spider):
        ua = random.choice(USER_AGENT_LIST)
        request.headers.setdefault('User-Agent', ua)
