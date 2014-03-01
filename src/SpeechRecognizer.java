import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class SpeechRecognizer {
	private Map<String, Map<String, Integer>> wordMap; // 1) the type of speech it is 2) the word 3) the frequency
	
	public SpeechRecognizer() {
		wordMap = new HashMap<String, Map<String, Integer>>();
	}
	
	public void training() throws Exception {
		BufferedReader words = new BufferedReader(new FileReader("inputs/Brown-words.txt")); 
		BufferedReader tags = new BufferedReader(new FileReader("inputs/Brown-tags.txt")); 
		
		String wordLine = new String();
		String tagLine = new String();
		
		ArrayList<String> wordList = new ArrayList<String>();
		ArrayList<String> tagList = new ArrayList<String>();
		Set<String> tagSet= new HashSet<String>();
		
		String individualWord = "";
		String individualTag = "";
		
		
		int counter = 0;
		
		while ((wordLine = words.readLine()) != null) { //passage
			wordLine = wordLine + " ";
			for(int i = 0; i < wordLine.length(); i++) { //line 				
				if(wordLine.charAt(i) == ' ' ) { //word
					wordList.add(individualWord);
					individualWord = "";
				}
				if(Character.isDigit(wordLine.charAt(i)) ) {
					int j = 0;
					String temp = "";
					while(wordLine.charAt(i+j) != ' ') {
						temp = temp + wordLine.charAt(i+j);
						j++;
					}
					wordList.add(temp);
					i= i + j;
				}
				else {
					String temp = new String();
					temp = individualWord + wordLine.charAt(i);
					if(temp.equals(" ")) {
						temp = "";
					}
					individualWord = temp;
				} 
			}
			counter++;
			if(counter == 1000) {
				break;
			}
		}	
		counter = 0;
		while ((tagLine = tags.readLine()) != null) { //passage
			for(int i = 0; i < tagLine.length(); i++) { //line 
				if(tagLine.charAt(i) == ' ' ) { //word
					tagList.add(individualTag);
					tagSet.add(individualTag);
					individualTag = "";
				}
				else {
					String temp = new String();
					temp = individualTag + tagLine.charAt(i);
					if(temp.equals(" ")) {
						temp = "";
					}
					individualTag = temp;
				}
			}
			counter++;
			if(counter == 1000) {
				break;
			}
		}
		System.out.println(tagList.size() + " " + wordList.size());
		/*for(String s : tagSet) {
			System.out.println(s);
		} */
		for(int i = 0; i < tagList.size(); i ++) {
				System.out.println(i + " " + wordList.get(i) + "//" + tagList.get(i));
		}  
		for(String tag : tagSet) {
			Map<String, Integer> temp = new HashMap<String, Integer>();
			for(int i = 0; i < tagList.size(); i++) {
				if(tagList.get(i).equals(tag)) {
					if(!temp.containsKey(wordList.get(i))) {
						temp.put(wordList.get(i), 1);
					}
					else {
						int frequency = temp.get(wordList.get(i)) +1;
						temp.put(wordList.get(i), frequency);
					}
				}
			}
			wordMap.put(tag, temp);
		} 
		
		for(String t : wordMap.keySet()) {
			System.out.println(t + "=" + wordMap.get(t));
		}
	}
	public static void main(String[] args) throws Exception {
		SpeechRecognizer s = new SpeechRecognizer();
		s.training();
	}
}