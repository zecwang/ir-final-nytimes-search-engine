package com.ir.searchengine.service;

import com.ir.searchengine.path.Path;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WordEmbeddingService {
	
	public static boolean ifWordInVocab(String word) {
		String url = Path.python_api_domain + "/vocabulary/" + word;
		RestTemplate restTemplate = new RestTemplate();
		Integer result = restTemplate.getForObject(url, Integer.class);
		return result == 1;
	}
	
	public static double getSimilarity(String w1, String w2) {
		String url = Path.python_api_domain + "/similarity/" + w1 + "/" + w2;
		RestTemplate restTemplate = new RestTemplate();
		String result = restTemplate.getForObject(url, String.class);
		return Double.parseDouble(result.replace("\"", ""));
	}
}
