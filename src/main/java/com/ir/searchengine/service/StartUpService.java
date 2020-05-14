package com.ir.searchengine.service;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class StartUpService {
	
	@PostConstruct
	public void startUpInit() throws Exception {
//		DataReformatService.process();
		
//		LuceneService.deleteAllIndexes();
//		LuceneService.createIndexes();
		
//		LuceneService.deleteAllSuggestionDocuments();
//		LuceneService.createSuggestionDocuments();
	}
	
}
