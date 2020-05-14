# IR Final Project - NYTimes Search Engine

## Introduction

This is the final project for Information Storage & Retrieval course, and our goal is to build our news search engine for The New York Times, which supports spell check, autocomplete and can display highlighted snippets.

## Team

- Zechen Wang
- Danlan Huang
- Weihan Chen

## Timeline

- Start Date: 2019/09/22
- Last Update: 2019/12/12

## Data

We crawled the news data between 2017 to 2019 from NYTimes, and we extracted the links which are related to NYTimes in the news articles we crawled, and crawled those links too.

The final data size is 1.34G.

The data we crawled are stored in the format:

![data-original](nytimes-search-engine/data-original.png)

And after reformatting (remove html tags and add unique docIDs):

![data-reformat](nytimes-search-engine/data-reformat.png)

## Flask RESTful API

Check if a token is in vocabulary:

<img src="nytimes-search-engine/p1.png" alt="p1" style="zoom:60%;" />

<img src="nytimes-search-engine/p2.png" alt="p2" style="zoom:58%;" />

1 = in the vocabulary, 0 = not in the vocabulary

<p></p>

Calculate similarity between words:

<img src="nytimes-search-engine/p3.png" alt="p3" style="zoom:58%;" />

<img src="nytimes-search-engine/p4.png" alt="p4" style="zoom:58%;" />

It returns the similarity in string format.

## Demo

Search bar:

![image-20200514114341910](nytimes-search-engine/image-20200514114341910.png)

Search results:

![image-20200514114501118](nytimes-search-engine/image-20200514114501118.png)

![image-20200514114924039](nytimes-search-engine/image-20200514114924039.png)

Pagination:

![image-20200514115056937](nytimes-search-engine/image-20200514115056937.png)

Spell check:

![image-20200514115235534](nytimes-search-engine/image-20200514115235534.png)

![image-20200514115329707](nytimes-search-engine/image-20200514115329707.png)

Autocomplete:

![image-20200514115635133](nytimes-search-engine/image-20200514115635133.png)

![image-20200514115701274](nytimes-search-engine/image-20200514115701274.png)

![image-20200514120240816](nytimes-search-engine/image-20200514120240816.png)

