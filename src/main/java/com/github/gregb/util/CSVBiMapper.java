package com.github.gregb.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class CSVBiMapper {

	public static BiMap<String, String> map(String file) throws FileNotFoundException, IOException {
		return map(new FileInputStream(file));
	}

	public static BiMap<String, String> map(InputStream is) throws IOException {
		final BiMap<String, String> map = HashBiMap.create();

		final BufferedReader br = new BufferedReader(new InputStreamReader(is));

		br.lines().map(s -> s.split(",")).forEach(arr -> map.put(arr[0], arr[1]));

		is.close();
		return map;
	}

}
