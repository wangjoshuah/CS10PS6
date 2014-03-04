
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
	long startTime = System.currentTimeMillis();

	//private variables that are the maps for words and targets to their frequencies
	private Map<String, Map<String, Integer>> wordMap; // 1) the type of speech it is 2) the word 3) the frequency
	private Map<String, Map<String, Integer>> tagMap; // 1) the tag 2) the tag of its next word 3) the frequency of that next word
	private Map<String, Map<String, Double>> transitionMatrix; //stores the transition probability of going from state i to state j
	private Map<String, Map<String, Double>> emissionMatrix; // stores the emission probability of a word within a state

	/**
	 * Constructor
	 * initializes the word map and the tag map
	 */
	public SpeechRecognizer() {
		wordMap = new HashMap<String, Map<String, Integer>>(); //start wordMap as a hashmap
		tagMap = new HashMap<String, Map<String, Integer>>(); //also start tagMap
	}

	/**
	 * creates the maps for tags to words and tags to next possible tags in linear time 
	 * sets up the data on the fly as we read through the two files
	 * @throws Exception
	 */
	private void quickTrain() throws Exception {
		
		long startTime = System.currentTimeMillis();
		//create readers for all the word in puts and the tag inputs
		BufferedReader words = new BufferedReader(new FileReader("inputs/test-words.txt")); //where do we get our words from input document 
		BufferedReader tags = new BufferedReader(new FileReader("inputs/test-tags.txt"));   //get our tags from other input doc

		String wordLine = new String(); //string that takes each line of the words file
		String tagLine = new String();  //string that takes each line of the tags  file

		String[] wordTokens;	//list of words keeping track of indices
		String[] tagTokens;	//list of tags  keeping track of indices to compare with wordlist

		String start = "START";
		Map<String, Integer> map = new HashMap<String, Integer>();
		tagMap.put(start, map);
		//----------------------------------------- read over this algorithm so it's absolutely correct
		while ((wordLine = words.readLine()) != null && (tagLine = tags.readLine()) != null) { //while we can continue reading through the words and tags
			wordTokens = wordLine.split(" "); //split word line based on spaces
			tagTokens = tagLine.split(" "); //split tag line based on spaces
			putTagInTagMapOfPrevTag(tagTokens); //handle tags and prev tags map
			putWordsInMapsOfTags(wordTokens, tagTokens); //map words into wordmap of tags
		}
		
		//don't forget to clean up after we are done
		words.close();
		tags.close();
		
		createTransitionMatrix();
		createEmissionMatrix();
		
		for(String t : tagMap.keySet()) {
			System.out.println(t + "=" + tagMap.get(t));
		}
		for(String w : wordMap.keySet()) {
			System.out.println(w + "=" + wordMap.get(w));
		}
		
		
		long endTime = System.currentTimeMillis();
		System.out.println("Total Time = " + (endTime - startTime));
	}

	/**
	 * map the tags to their next possible tags
	 * @param tags		string array of line of tags from the txt file
	 */
	private void putTagInTagMapOfPrevTag(String[] tags) {
		tagLeadsToSecondTag("START", tags[0]); //start tag leads to the first tag in this line
		for (int i = 1; i < tags.length; i ++) { //loop over the whole line of tags except the first one
			tagLeadsToSecondTag(tags[i-1], tags[i]); //each tag is fed in from the prev tag in the array
		}
	}

	/**
	 * put the current tag into the map of the previous tag and increase its frequency in there
	 * @param prevTag		the tag that came before the tag we are at now (START) if its the first tag in the line
	 * @param currentTag	the tag we are looking at now that we need to track
	 */
	private void tagLeadsToSecondTag(String prevTag, String currentTag) {
		if (tagMap.containsKey(prevTag)) { //if the tagMap has already been acquainted with the previous tag
			if (tagMap.get(prevTag).containsKey(currentTag)) { //if the prev tag's map contains the current tag
				tagMap.get(prevTag).put(currentTag, tagMap.get(prevTag).get(currentTag) + 1); //get the frequency with which the tag appears after the prev tag
			}
			else { //if the tag has never shown up after that tag before,
				tagMap.get(prevTag).put(currentTag, 1); //put a new mapping of our current tag with frequency 1 in there
			}
		}
		else { //if the tagMap doesn't know about the prev tag
			Map<String, Integer> nextTag = new HashMap<String, Integer>(); //create a submap for the prev's next tag
			nextTag.put(currentTag, 1); //put the current tag and frequency = 1 in there
			tagMap.put(prevTag, nextTag); //put our new map in there to be the value for the prev tag
		}
	}
	
	/**
	 * map the words to what tags they appear under
	 * @param words		string array of the different words or punctuation types from the words text file
	 * @param tags		string array of the tags associated with those words in the tags text file
	 */
	private void putWordsInMapsOfTags(String[] words, String[] tags) {
		for (int i = 0; i < words.length; i ++) { //for all words
			if (wordMap.containsKey(tags[i])) { //if we already know of this tag type in our wordMap
				if (wordMap.get(tags[i]).containsKey(words[i])) { //if the word appears under that tag
					wordMap.get(tags[i]).put(words[i], wordMap.get(tags[i]).get(words[i]) + 1);
				}
				else { //the word is not found under that tag yet
					wordMap.get(tags[i]).put(words[i], 1); //put that word and frequency 1 in under that tag
				}
			}
			else { //if that tag has never been seen before
				Map<String, Integer> wordForTag = new HashMap<String, Integer>(); //create a sub map for the word under the tag
				wordForTag.put(words[i], 1); //put the first values in there
				wordMap.put(tags[i], wordForTag); //then put that sub map into the map under a new tag
			}
		}
	}

	/**
	 * Calculates log probability of each next part of speech for each part of speech
	 */
	private void createTransitionMatrix() {
		int denominator; //divides the frequencies in second sub-for-loop
		for(String state : tagMap.keySet()) { //for every tag in the tagMap
			denominator = 0; //set denominator as zero when we start calculating the new probabilities
			for(Integer frequencies : tagMap.get(state).values()) { //get the frequency of all next possible states coming from our start tag 
				denominator += frequencies; //sum up all the frequencies
			}
			for(String nextState : tagMap.get(state).keySet()) { //for every 2nd tag of each tag1 in the tagMap
				double transitionProbability = Math.log(tagMap.get(state).get(nextState) / denominator); //calculate the transition probability from state i to state j
				Map<String, Double> element = new HashMap<String, Double>(); //create our element in the transition matrix
				element.put(nextState, transitionProbability); //populate that element
				transitionMatrix.put(state, element); //put our transition probability in for going from state to next state
			}
		}	
	}
	
	/**
	 * create the emission matrix we will use in the future to calculate probabilities
	 */
	private void createEmissionMatrix() {
		int denominator; //divides the frequencies 
		for(String state: wordMap.keySet()) { //for every tag in wordMap
			denominator = 0; //start over our denominator
			for(Integer frequencies : wordMap.get(state).values()) { //for all frequencies of a state's words
				denominator += frequencies; //summ all of the words for that state
			}
			for(String word : wordMap.get(state).keySet()) { //for all words in that tag type
				double emissionProbability = Math.log(wordMap.get(state).get(word) / denominator); //calculate the emission probabilitye for a word within a state
				Map<String, Double> element = new HashMap<String, Double>(); //create our element placeholder
				element.put(word, emissionProbability); //populate the element
				emissionMatrix.put(state, element); //put the emission probability in our emission matrix
			}
		}
	}
	
	private void viterbiTagging(String input) {
		tagMap.keySet();
		String[] wordArray = input.split(" ");
		ArrayList<String> wordList = new ArrayList<String>();
		wordList.add("START");
		for(int i = 0; i < (1+wordArray.length); i++ ) {
			wordList.add(wordArray[i+1]);
		}
		
		ArrayList<String> tagList = new ArrayList<String>();
		tagList.add("");
		
		ArrayList<Double> scoreList = new ArrayList<Double>();
		scoreList.add((double) 0);
		
		/*
		//first word tagger
		String tempTag = new String();
		for(String POS : wordMap.keySet()) { //loop through the wordMap to find 1st word
			int tempInt = 0;
			if(wordMap.get(POS).containsValue(wordMap.get(POS).containsKey(wordArray[0]))) { //if the inner map linked to a given POS contains the word
				if(wordMap.get(POS).get(wordArray[0]) > tempInt) {
					tempInt = wordMap.get(POS).get(wordArray[0]);
					tempTag = POS;
				}
			}
		}
		tagList.add(tempTag);
		*/
		
		
	}
	
	
	
	public static void main(String[] args) throws Exception {
		SpeechRecognizer s = new SpeechRecognizer();
		s.quickTrain();
		
		
	}
}