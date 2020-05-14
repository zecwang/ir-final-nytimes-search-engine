package com.ir.searchengine.service;

import com.ir.searchengine.model.News;
import com.ir.searchengine.path.Path;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.highlight.Formatter;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.LuceneDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.search.spell.SuggestMode;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

@Service
public class LuceneService {
	private static Analyzer analyzer;
	private static Directory directory;
	private static Analyzer standard_analyzer;
	private static Directory _IndexDirectory;
	private static Directory spellIndexDirectory;
	private static Directory autoSuggestDirectory;
	private static SpellChecker spellChecker;
	private static AnalyzingInfixSuggester suggester;
	private static CharArraySet filtered;
	
	static {
		try {
			analyzer = CustomAnalyzer.builder()
					.withTokenizer("standard")
					.addTokenFilter("lowercase")
					.addTokenFilter("stop")
					.addTokenFilter("porterstem")
					.build();
			directory = FSDirectory.open(new File(Path.indexDir).toPath());
			
			standard_analyzer = CustomAnalyzer.builder()
					.withTokenizer("standard")
					.build();
			_IndexDirectory = FSDirectory.open(new File(Path._index).toPath());
			spellIndexDirectory = FSDirectory.open(new File(Path.spellCheckDir).toPath());
			autoSuggestDirectory = FSDirectory.open(new File(Path.autoSuggestDir).toPath());
			suggester = new AnalyzingInfixSuggester(autoSuggestDirectory, standard_analyzer, analyzer, 4, false, false, false);
			spellChecker = new SpellChecker(spellIndexDirectory);
			filtered = StopAnalyzer.ENGLISH_STOP_WORDS_SET;
//			filtered = new HashSet<>();
//			filtered.add("a");
//			filtered.add("to");
//			filtered.add("of");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void deleteAllIndexes() throws Exception {
		IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));
		indexWriter.deleteAll();
		indexWriter.close();
		IndexWriter _IndexWriter = new IndexWriter(_IndexDirectory, new IndexWriterConfig(standard_analyzer));
		_IndexWriter.deleteAll();
		_IndexWriter.close();
	}
	
	public static void deleteAllSuggestionDocuments() throws Exception {
		spellChecker.clearIndex();
		IndexWriter autoSuggestWriter = new IndexWriter(autoSuggestDirectory, new IndexWriterConfig(standard_analyzer));
		autoSuggestWriter.deleteAll();
		autoSuggestWriter.close();
	}
	
	public static void createIndexes() throws Exception {
		IndexWriter indexWriter = new IndexWriter(directory, new IndexWriterConfig(analyzer));
		IndexWriter _IndexWriter = new IndexWriter(_IndexDirectory, new IndexWriterConfig(standard_analyzer));
		
		JSONParser jsonParser = new JSONParser();
		try (BufferedReader reader = new BufferedReader(new FileReader(Path.dataJSON))) {
			Object obj = jsonParser.parse(reader);
			JSONArray items = (JSONArray) obj;
			items.forEach(it -> {
				JSONObject el = (JSONObject) it;
				Field author = new TextField("author", (String) el.get("author"), Field.Store.YES);
				Field docID = new StringField("docID", (String) el.get("docID"), Field.Store.YES);
				Field title = new TextField("title", (String) el.get("title"), Field.Store.YES);
				Field url = new StoredField("url", (String) el.get("url"));
				Field content = new TextField("content", (String) el.get("content"), Field.Store.YES);
				Document document = new Document();
				document.add(author);
				document.add(docID);
				document.add(title);
				document.add(url);
				document.add(content);
				try {
					indexWriter.addDocument(document);
					_IndexWriter.addDocument(document);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
		indexWriter.close();
		_IndexWriter.close();
	}
	
	public static void createSuggestionDocuments() throws Exception {
		IndexReader _IndexReader = DirectoryReader.open(_IndexDirectory);
		spellChecker.indexDictionary(new LuceneDictionary(_IndexReader, "content"), new IndexWriterConfig(standard_analyzer), false);
		spellChecker.setStringDistance(new JaroWinklerDistance());
		spellChecker.setAccuracy(new Float("0.7"));
		
		suggester.build(new LuceneDictionary(_IndexReader, "content"));
		_IndexReader.close();
	}
	
	public static boolean hasResult(String field, String input) throws Exception {
		QueryParser queryParser = new QueryParser(field, analyzer);
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		if (input.contains("\"")) {
			int quote_occurrence = 0;
			for (int i = 0, len = input.length(); i < len; i++) {
				if (input.charAt(i) == '"') quote_occurrence++;
			}
			if (quote_occurrence % 2 == 1) input = input + '"'; // make sure the quotes matched
		}
		Query query = queryParser.parse(input);
		TopDocs topDocs = indexSearcher.search(query, 1);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		indexReader.close();
		
		return scoreDocs.length > 0;
	}
	
	public static HashMap<String, Object> searchBySuggestion(String field, String input, int from, int to) throws Exception {
		QueryParser queryParser = new QueryParser(field, analyzer);
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		Query query;
		boolean processed = false;
		if (input.contains("\"")) {
			int quote_occurrence = 0;
			for (int i = 0, len = input.length(); i < len; i++) {
				if (input.charAt(i) == '"') quote_occurrence++;
			}
			if (quote_occurrence % 2 == 1) input = input + '"'; // make sure the quotes matched
			query = queryParser.parse(input);
		} else {
			query = queryParser.parse("\"" + input + "\""); // do exact match first
			processed = true;
		}
		TopDocs topDocs = indexSearcher.search(query, to);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		ArrayList<News> results = new ArrayList<>();
		Formatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span>");
		QueryScorer scorer = new QueryScorer(query);
		Highlighter highlighter = new Highlighter(formatter, scorer);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 60);
		highlighter.setTextFragmenter(fragmenter);
		String delimiter = " ... ";
		
		for (int i = from, end = scoreDocs.length; i < end; i++) {
			int id = scoreDocs[i].doc;
			Document document = indexSearcher.doc(id);
			TokenStream stream = TokenSources.getAnyTokenStream(indexReader, id, field, analyzer);
			String[] frags = highlighter.getBestFragments(stream, document.get(field), 3);
			StringBuilder fragment = new StringBuilder();
			for (int j = 0, len = frags.length - 1; j < len; j++) {
				fragment.append(frags[j]).append(delimiter);
			}
			fragment.append(frags[frags.length - 1]);
			News news = new News(document.get("author"), document.get("docID"), document.get("title"), document.get("url"), document.get("content"), fragment.toString());
			results.add(news);
		}
		
		TopDocs topDocs_ORG = null;
		if (processed) {
			query = queryParser.parse(input);
			scorer = new QueryScorer(query);
//			topDocs_ORG = indexSearcher.search(query, Math.max(to - scoreDocs.length, 1));
			topDocs_ORG = indexSearcher.search(query, to);
		}
		
		if (processed && to > scoreDocs.length) {
			HashSet<Integer> idSet = new HashSet<>();
			for (ScoreDoc scoreDoc : scoreDocs) {
				idSet.add(scoreDoc.doc);
			}
			
			ScoreDoc[] scoreDocs_ORG = topDocs_ORG.scoreDocs;
			highlighter = new Highlighter(formatter, scorer);
			fragmenter = new SimpleSpanFragmenter(scorer, 60);
			highlighter.setTextFragmenter(fragmenter);
			for (int i = Math.abs(from - scoreDocs.length), end = Math.min(to - scoreDocs.length, scoreDocs_ORG.length); i < end; i++) {
				int id = scoreDocs_ORG[i].doc;
				if (idSet.contains(id)) continue;
				Document document = indexSearcher.doc(id);
				TokenStream stream = TokenSources.getAnyTokenStream(indexReader, id, field, analyzer);
				String[] frags = highlighter.getBestFragments(stream, document.get(field), 3);
				StringBuilder fragment = new StringBuilder();
				for (int j = 0, len = frags.length - 1; j < len; j++) {
					fragment.append(frags[j]).append(delimiter);
				}
				fragment.append(frags[frags.length - 1]);
				News news = new News(document.get("author"), document.get("docID"), document.get("title"), document.get("url"), document.get("content"), fragment.toString());
				results.add(news);
			}
		}
		
		HashMap<String, Object> map = new HashMap<>();
		map.put("totalHits", topDocs_ORG == null ? topDocs.totalHits : topDocs_ORG.totalHits);
		map.put("results", results);
		
		indexReader.close();
		return map;
	}
	
	public static HashMap<String, Object> search(String field, String input, int from, int to) throws Exception {
		QueryParser queryParser = new QueryParser(field, analyzer);
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
		Query query;
		boolean processed = false;
		input = input.replace("/", "");
		input = input.trim();
		
		// make suggestions here
		StringBuilder suggestion = new StringBuilder();
		StringBuilder builder = new StringBuilder();
		String prev = null;
		boolean wasLetter = false;
		for (int i = 0, len = input.length(); i < len; i++) {
			char el = input.charAt(i);
			if (Character.isLetter(el)) {
				builder.append(el);
				wasLetter = true;
			} else {
				if (wasLetter) {
					builder.append('/').append(el).append('/');
					wasLetter = false;
				} else
					builder.append(el).append('/');
			}
		}
		String[] components = builder.toString().split("/");
		IndexReader spellIndexReader = DirectoryReader.open(spellIndexDirectory);
		for (String component : components) {
			if (component.length() == 1 && !Character.isLetter(component.charAt(0)))
				suggestion.append(component);
			else if (filtered.contains(component))
				suggestion.append(component);
			else if (WordEmbeddingService.ifWordInVocab(component) && hasResult(field, suggestion + component)) {
				suggestion.append(component);
				prev = component;
			} else {
				String[] suggestions = spellChecker.suggestSimilar(component, 11, spellIndexReader, null, SuggestMode.SUGGEST_MORE_POPULAR);
//				System.out.println(Arrays.asList(suggestions));
				if (prev == null) {
					for (String it : suggestions) {
						if (WordEmbeddingService.ifWordInVocab(it) && hasResult(field, suggestion + it)) {
							prev = it;
							suggestion.append(it);
							break;
						}
					}
					if (prev == null) {
						if (suggestions.length > 0)
							suggestion.append(suggestions[0]);
						else
							suggestion.append(component);
					}
				} else {
					double max_value = 0;
					String max_it = null;
					for (String it : suggestions) {
						double similarity = WordEmbeddingService.getSimilarity(prev, it);
						if (similarity > max_value && hasResult(field, suggestion + it)) {
							max_value = similarity;
							max_it = it;
						}
					}
					if (max_it != null) {
						suggestion.append(max_it);
						prev = max_it;
					} else
						suggestion.append(component);
				}
			}
		}
		spellIndexReader.close();
//		if (suggestion.toString().equals(input))
//			suggestion = new StringBuilder();
		
		
		if (input.contains("\"")) {
			int quote_occurrence = 0;
			for (int i = 0, len = input.length(); i < len; i++) {
				if (input.charAt(i) == '"') quote_occurrence++;
			}
			if (quote_occurrence % 2 == 1) input = input + '"'; // make sure the quotes matched
			query = queryParser.parse(input);
		} else {
			query = queryParser.parse("\"" + input + "\""); // do exact match first
			processed = true;
		}
		TopDocs topDocs = indexSearcher.search(query, to);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		ArrayList<News> results = new ArrayList<>();
		Formatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span>");
		QueryScorer scorer = new QueryScorer(query);
		Highlighter highlighter = new Highlighter(formatter, scorer);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 60);
		highlighter.setTextFragmenter(fragmenter);
		String delimiter = " ... ";
		
		for (int i = from, end = scoreDocs.length; i < end; i++) {
			int id = scoreDocs[i].doc;
			Document document = indexSearcher.doc(id);
			TokenStream stream = TokenSources.getAnyTokenStream(indexReader, id, field, analyzer);
			String[] frags = highlighter.getBestFragments(stream, document.get(field), 3);
			StringBuilder fragment = new StringBuilder();
			for (int j = 0, len = frags.length - 1; j < len; j++) {
				fragment.append(frags[j]).append(delimiter);
			}
			if (frags.length - 1 >= 0)
				fragment.append(frags[frags.length - 1]);
			News news = new News(document.get("author"), document.get("docID"), document.get("title"), document.get("url"), document.get("content"), fragment.toString());
			results.add(news);
		}
		
		TopDocs topDocs_ORG = null;
		if (processed) {
			query = queryParser.parse(input);
			scorer = new QueryScorer(query);
//			topDocs_ORG = indexSearcher.search(query, Math.max(to - scoreDocs.length, 1));
			topDocs_ORG = indexSearcher.search(query, to);
		}
		
		if (processed && to > scoreDocs.length) {
			HashSet<Integer> idSet = new HashSet<>();
			for (ScoreDoc scoreDoc : scoreDocs) {
				idSet.add(scoreDoc.doc);
			}
			
			ScoreDoc[] scoreDocs_ORG = topDocs_ORG.scoreDocs;
			highlighter = new Highlighter(formatter, scorer);
			fragmenter = new SimpleSpanFragmenter(scorer, 60);
			highlighter.setTextFragmenter(fragmenter);
//			System.out.println(to - scoreDocs.length);
//			System.out.println(scoreDocs_ORG.length);
//			for (int i = Math.max(from - scoreDocs.length, 0), end = Math.max(to - scoreDocs.length, 0); i < end; i++) {
			for (int i = Math.abs(from - scoreDocs.length), end = Math.min(to - scoreDocs.length, scoreDocs_ORG.length); i < end; i++) {
				int id = scoreDocs_ORG[i].doc;
				if (idSet.contains(id)) continue;
				Document document = indexSearcher.doc(id);
				TokenStream stream = TokenSources.getAnyTokenStream(indexReader, id, field, analyzer);
				String[] frags = highlighter.getBestFragments(stream, document.get(field), 3);
				StringBuilder fragment = new StringBuilder();
				for (int j = 0, len = frags.length - 1; j < len; j++) {
					fragment.append(frags[j]).append(delimiter);
				}
				fragment.append(frags[frags.length - 1]);
				News news = new News(document.get("author"), document.get("docID"), document.get("title"), document.get("url"), document.get("content"), fragment.toString());
				results.add(news);
			}
		}
		
		HashMap<String, Object> map = new HashMap<>();
//		System.out.println(idSet.size());
//		System.out.println(topDocs_ORG == null ? 0 : topDocs_ORG.totalHits);
//		System.out.println(topDocs.totalHits);
//		map.put("totalHits", topDocs_ORG == null ? topDocs.totalHits : topDocs.totalHits + topDocs_ORG.totalHits - idSet.size());
		map.put("totalHits", topDocs_ORG == null ? topDocs.totalHits : topDocs_ORG.totalHits);
		if (!suggestion.toString().equals(input) && hasResult(field, suggestion.toString()))
			map.put("suggestion", suggestion);
		map.put("results", results);
		
		indexReader.close();
		return map;
	}
	
	// suggest num = 9
	public static HashSet<String> autoSuggest(String sentence, int num) throws IOException {
		
		IndexReader spellIndexReader = DirectoryReader.open(spellIndexDirectory);
		sentence = sentence.trim();
		int end = sentence.lastIndexOf(" ");
		String sub_sent;
		String prefix;
		if (end == -1) {
			sub_sent = "";
			prefix = sentence;
		} else {
			sub_sent = sentence.substring(0, end) + " ";
			prefix = sentence.substring(end + 1);
		}
		String[] suggestions = spellChecker.suggestSimilar(prefix, Math.max(num, 30), spellIndexReader, null, SuggestMode.SUGGEST_MORE_POPULAR);
		HashSet<String> results = new HashSet<>();
		for (String suggestion : suggestions) {
			if (suggestion.startsWith(prefix)) {
				results.add(sub_sent + suggestion);
				if (results.size() > num) {
					spellIndexReader.close();
					return results;
				}
			}
		}
		spellIndexReader.close();
		
		return results;
	}
	
	// *******************************************
	// not useful, just a demo. DO NOT USE THIS!!
	// *******************************************
	public static HashMap<String, Object> searchByQueryParser(String field, String input, int from, int to) throws Exception {
		QueryParser queryParser = new QueryParser(field, analyzer);
		IndexReader indexReader = DirectoryReader.open(directory);
		IndexSearcher indexSearcher = new IndexSearcher(indexReader);
//		Query query = queryParser.parse(input);
		Query query = queryParser.parse("\"" + input + "\"");
		TopDocs topDocs = indexSearcher.search(query, to);
		ScoreDoc[] scoreDocs = topDocs.scoreDocs;
		ArrayList<News> results = new ArrayList<>();
		for (int i = from, end = scoreDocs.length; i < end; i++) {
			int id = scoreDocs[i].doc;
			Document document = indexSearcher.doc(id);
			News news = new News(document.get("author"), document.get("docID"), document.get("title"), document.get("url"), document.get("content"));
			results.add(news);
		}
		HashMap<String, Object> map = new HashMap<>();
		map.put("totalHits", topDocs.totalHits);
		map.put("results", results);
//		indexReader.close();
		
		IndexReader spellIndexReader = DirectoryReader.open(spellIndexDirectory);
		String[] suggestions = spellChecker.suggestSimilar("Nelson", 5, spellIndexReader, null, SuggestMode.SUGGEST_MORE_POPULAR);
		System.out.println(Arrays.asList(suggestions));
		Formatter formatter = new SimpleHTMLFormatter("<span class=\"highlight\">", "</span>");
		QueryScorer scorer = new QueryScorer(query);
		Highlighter highlighter = new Highlighter(formatter, scorer);
		Fragmenter fragmenter = new SimpleSpanFragmenter(scorer, 60);
		highlighter.setTextFragmenter(fragmenter);
		int id = scoreDocs[0].doc;
		Document document = indexSearcher.doc(id);
		TokenStream stream = TokenSources.getAnyTokenStream(indexReader, id, field, analyzer);
//		TokenStream stream = analyzer.tokenStream(field, input);
		
		String[] frags = highlighter.getBestFragments(stream, document.get(field), 3);
		int count = 0;
		for (String frag : frags) {
			System.out.println("=================");
			System.out.println(frag);
			count += frag.length();
		}
		System.out.println(count);
		
		indexReader.close();
		return map;
	}
}
