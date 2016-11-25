package halign.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class FormatFasta {
	public static void main(String[] args) {
		new FormatFasta().format("D:\\MASTER2016\\1.MSA2.0\\muscle\\output-muscle.txt");
	}
	
	public void format(String input) {
		try {
			BufferedReader brReader = new BufferedReader(new FileReader(input));
			
			ArrayList<String> s_key = new ArrayList<>();
			ArrayList<String> s_val = new ArrayList<>();
			StringBuilder stringBuilder = new StringBuilder();
			while(brReader.ready()) {
				String line = brReader.readLine();
				if (line.charAt(0) == '>') {
					s_key.add(line);
					if (stringBuilder.length() != 0) {
						s_val.add(stringBuilder.toString());
						stringBuilder.setLength(0);
					}
				} else {
					stringBuilder.append(line);
				}
			}
			if (stringBuilder.length() != 0) {
				s_val.add(stringBuilder.toString());
			}
			brReader.close();
			
			BufferedWriter bwWriter = new BufferedWriter(new FileWriter(input));
			
			int total = s_key.size();
			for (int i=0; i<total; i++) {
				bwWriter.write(s_key.get(i)+"\n");
				bwWriter.write(s_val.get(i)+"\n");
			}
			bwWriter.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
