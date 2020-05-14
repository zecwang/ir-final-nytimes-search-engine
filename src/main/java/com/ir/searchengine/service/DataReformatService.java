package com.ir.searchengine.service;

import com.ir.searchengine.path.Path;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.UUID;

@Service
public class DataReformatService {
	private static BufferedWriter writer;
	
	public static void process() {
		JSONParser jsonParser = new JSONParser();
		try (BufferedReader reader = new BufferedReader(new FileReader(Path.itemsJSON))) {
			Object obj = jsonParser.parse(reader);
			JSONArray items = (JSONArray) obj;
			writer = new BufferedWriter(new FileWriter(createFile(Path.dataJSON)));
			writer.write("[");
			writer.newLine();
			items.forEach(el -> reformat((JSONObject) el));
			writer.write("]");
			writer.flush();
			writer.close();
		} catch (IOException | ParseException e) {
			e.printStackTrace();
		}
	}
	
	private static void reformat(JSONObject el) {
		JSONArray authorList = (JSONArray) el.get("author");
		StringBuilder builder = new StringBuilder();
		for (Object obj : authorList) {
			String it = (String) obj;
			builder.append(it).append(", ");
		}
		if (authorList.size() > 0) builder.delete(builder.length() - 2, builder.length());
		el.remove("author");
		el.put("author", builder.toString());
		
		JSONArray contentList = (JSONArray) el.get("content");
		builder = new StringBuilder();
		for (Object obj : contentList) {
			String it = (String) obj;
			boolean htmlTagHold = false; // true if it is a html tag
			for (int i = 0, len = it.length(); i < len; i++) {
				char current = it.charAt(i);
				if (htmlTagHold) { // htmlTagHold = true, skip each character until we get a '>'
					if (current == '>') htmlTagHold = false;
					continue;
				}
				
				// htmlTagHold = false
				if (current == '<') htmlTagHold = true;
				else if (current == '“' || current == '”') builder.append('"');
				else if (current == '‘' || current == '’') builder.append('\'');
				else builder.append(current);
			}
			builder.append(' ');
		}
		builder.deleteCharAt(builder.length() - 1);
		el.remove("content");
		el.put("content", builder.toString());
		
		String docID = String.valueOf(UUID.randomUUID());
		el.put("docID", docID);
		try {
			writer.write(el.toJSONString());
			writer.write(',');
			writer.newLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static File createFile(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			File dir = new File(file.getParent());
			if (!dir.exists() && !dir.mkdirs())
				throw new IOException("Failed to create directory");
			if (!file.createNewFile())
				throw new IOException("Failed to create file: " + filePath);
		}
		return file;
	}
}
