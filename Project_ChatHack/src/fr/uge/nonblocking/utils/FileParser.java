package fr.uge.nonblocking.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class FileParser {
	
	/**
	 * This method generate a HashMap containing the login and password of the database.
	 * @param filePath to parse
	 * @param separator the delimiting regular expression
	 * @return the HasMap containing the login and the password of the database
	 * @throws IOException if the file doesn't exist
	 */
	public static HashMap<String, String> parsePasswordFile(String filePath, String separator) throws IOException {
		var map = new HashMap<String, String>();

		try(var reader = new BufferedReader(new FileReader(filePath))){
			var line = "";
			while ((line = reader.readLine()) != null)
			{
				var parts = line.split(separator, 2);
				if (parts.length >= 2)
					map.put(parts[0], parts[1]);
				else 
					System.out.println("Ignoring line : " + line);
				
			}			
		}
		// Use for debug !
		map.entrySet().forEach(entry -> { System.out.println(entry.getKey() + "$" + entry.getValue());});
		return map;
	}

}
