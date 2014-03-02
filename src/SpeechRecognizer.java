
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sudi speech recognizer
 * @author Alex Tsu '14 and Josh Wang '15 for Dartmouth CS 10 14W
 */

public class SpeechRecognizer {
	
	//private variables that are the maps for words and targets to their frequencies
	private Map<String, Map<String, Integer>> wordMap; // 1) the type of speech it is 2) the word 3) the frequency
	private Map<String, Map<String, Integer>> tagMap; // 1) the tag 2) the tag of its next word 3) the frequency of that next word
	
	/**
	 * Constructor
	 * initializes the word map and the tag map
	 */
	public SpeechRecognizer() {
		wordMap = new HashMap<String, Map<String, Integer>>(); //start wordMap as a hashmap
		tagMap = new HashMap<String, Map<String, Integer>>(); //also start tagMap
	}
	
	/**
	 * Read in our word and tag values to tell our speech recognizer how to function
	 * @throws Exception		incorrect file input
	 */
	public void training() throws Exception {
		//create readers for all the word in puts and the tag inputs
		BufferedReader words = new BufferedReader(new FileReader("inputs/Brown-words.txt")); //where do we get our words from input document 
		BufferedReader tags = new BufferedReader(new FileReader("inputs/Brown-tags.txt"));   //get our tags from other input doc
		
		String wordLine = new String(); //string that takes each line of the words file
		String tagLine = new String();  //string that takes each line of the tags  file
		
		ArrayList<String> wordList = new ArrayList<String>();	//list of words keeping track of indices
		ArrayList<String> tagList = new ArrayList<String>();	//list of tags  keeping track of indices to compare with wordlist
		Set<String> tagSet= new HashSet<String>();				//set  of tags to put later into our map as the keySet
		
		String individualWord = "";	//keep track of what are individual words in the word input
		String individualTag = "";	//keep track of what are individual tags  in the tag  input
		
		int upperBound = 3000; //keep track of how many lines we want to go through
		int counter = 0; //start a counter to count how many lines we've read
		
		//----------------------------------------- read over this algorithm so it's absolutely correct
		while ((wordLine = words.readLine()) != null) { //while we can continue reading through the
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
					temp = individualWord + Character.toLowerCase(wordLine.charAt(i));
					if(temp.equals(" ")) {
						temp = "";
					}
					individualWord = temp;
				} 
			}
			counter++;
			if(counter == upperBound) {
				break;
			}
		}	
		counter = 0;
		
		//---------------------------------------------- also read this algorithm
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
			if(counter == upperBound) {
				break;
			}
		}
		
		
		/*
		 * print functions
		 */
		
		//print out tags and words
//		for(int i = 0; i < tagList.size(); i ++) {
//				System.out.println(i + " " + wordList.get(i) + "//" + tagList.get(i));
//		}  
		
		//create the has map of tags to words to frequencies **************************************efficiency problem
		for(String tag : tagSet) { //for each tag
			Map<String, Integer> temp = new HashMap<String, Integer>(); //create a map with that tag as the key and another map of words and frequencies as value
			for(int i = 0; i < tagList.size(); i++) { //for all the tags in the list
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
		
		for(int i = 0; i < tagList.size(); i ++) {
			if(!tagMap.containsKey(tagList.get(i))) {
				Map<String, Integer> temp = new HashMap<String, Integer>();
				temp.put(tagList.get(i+1), 1);
				tagMap.put(tagList.get(i), temp);
			}
			else if ((i+1) < tagList.size()) {
				Map<String, Integer> temp = tagMap.get(tagList.get(i));
				if(!temp.containsKey(tagList.get(i+1))) {
					temp.put(tagList.get(i+1), 1);
				}
				else {
					int frequency = temp.get(tagList.get(i+1)) +1;
					temp.put(tagList.get(i+1), frequency);
				}
				tagMap.put(tagList.get(i), temp);
			}
		}
		
		/*for(String t : wordMap.keySet()) {
			System.out.println(t + "=" + wordMap.get(t));
		}*/
		for(String t : tagMap.keySet()) {
			System.out.println(t + "=" + tagMap.get(t));
		}
	}
	
	public static void main(String[] args) throws Exception {
		SpeechRecognizer s = new SpeechRecognizer();
		s.training();
	}
}