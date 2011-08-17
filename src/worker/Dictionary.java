package worker;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

/**
 * Dictionary class for generating random dictionary words and paragraphs.
 */

public class Dictionary {
	String words[];
	Random rand = new Random();
	
	/**
	 * Creates a new <code>Dictionary</code> instance.
	 * Reads dictionary words.
	 *
	 * @param filename text file containing dictionary words
	 */
	
	public Dictionary(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(filename));
		int count = 0;
		while(br.readLine() != null) {
			count++;
		}
		br.close();
		words = new String[count];
		
		br = new BufferedReader(new FileReader(filename));
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
	
	/**
	 * Generates random dictionary word.
	 *
	 */
	
	public String getWord() {
		return words[rand.nextInt(words.length)];
	}
	
	/**
	 * Generates paragraph with random dictionary words.
	 *
	 * @param length paragraph length
	 * @return generated paragraph
	 */
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
	
	/**
	 * Generates random punctuation.
	 *
	 */
	
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
