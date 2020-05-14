package com.ir.searchengine.api;

import com.ir.searchengine.service.LuceneService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

@RestController
@RequestMapping("/api")
public class ApiController {
	
	@GetMapping("/field-search/{field}/{from}/{to}/{query}") // [from, to)
	public HashMap<String, Object> search(@PathVariable("field") String field, @PathVariable("from") int from, @PathVariable("to") int to, @PathVariable("query") String query) throws Exception {
		return LuceneService.search(field, query, from, to);
	}
	
	@GetMapping("/field-search/suggest/{field}/{from}/{to}/{query}") // [from, to)
	public HashMap<String, Object> searchBySuggestion(@PathVariable("field") String field, @PathVariable("from") int from, @PathVariable("to") int to, @PathVariable("query") String query) throws Exception {
		return LuceneService.searchBySuggestion(field, query, from, to);
	}
	
	@GetMapping("/autocomplete/{num}/{sentence}")
	public HashSet<String> autoSuggest(@PathVariable("sentence") String sentence, @PathVariable("num") int num) throws IOException {
		return LuceneService.autoSuggest(sentence, num);
	}
}
