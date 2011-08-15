package worker;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class Dictionary {
	
	String words[];
	Random rand = new Random();
	public Dictionary(String filename) throws IOException {
		FileReader reader = new FileReader(filename);
		BufferedReader br = new BufferedReader(reader);
		int count = 0;
		while(br.readLine() != null) {
			count++;
		}
		br.close();
		reader.close();
		words = new String[count];
		
		reader = new FileReader(filename);
		br = new BufferedReader(reader);
		int pos = 0;
		for(int i = 0; i < count; i++) {
			words[i] = br.readLine();
			pos = words[i].indexOf(' ');
			if(pos != -1)
				words[i] = words[i].substring(0, pos);
		}
	}
	
	public Dictionary(Dictionary d) {
		words = d.words;
	}
	
	public String getWord() {
		return words[rand.nextInt(words.length)];
	}
	public String getParagraph(int length) throws IOException {
		String sentence = "";
		int punctFreq = rand.nextInt(10) + 4;
		for(int i = 0; i < length; i++) {
			if((i + 1) % punctFreq == 0)
				sentence += getWord() + getPunctuation();
			else
				sentence += getWord() + " ";
		}
		return sentence;
	}
	
	public String getPunctuation() {
		String word = null;
		switch (rand.nextInt(10)) {
		case 0:
			word += ", ";
			break;
		case 1:
			word += ". ";
			break;
		case 2:
			word += " ! ";
			break;
		default:
			word += " ";
			break;
		}
		return word;
	}
	
}
