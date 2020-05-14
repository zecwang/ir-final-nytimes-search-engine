package com.ir.searchengine.model;

public class News {
	private String author;
	private String docID;
	private String title;
	private String url;
	private String content;
	private String fragment;
	
	public News(String author, String docID, String title, String url, String content) {
		this.author = author;
		this.docID = docID;
		this.title = title;
		this.url = url;
		this.content = content;
	}
	
	public News(String author, String docID, String title, String url, String content, String fragment) {
		this.author = author;
		this.docID = docID;
		this.title = title;
		this.url = url;
		this.content = content;
		this.fragment = fragment;
	}
	
	public String getAuthor() {
		return author;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
	
	public String getDocID() {
		return docID;
	}
	
	public void setDocID(String docID) {
		this.docID = docID;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getUrl() {
		return url;
	}
	
	public void setUrl(String url) {
		this.url = url;
	}
	
	public String getContent() {
		return content;
	}
	
	public void setContent(String content) {
		this.content = content;
	}
	
	public String getFragment() {
		return fragment;
	}
	
	public void setFragment(String fragment) {
		this.fragment = fragment;
	}
}
