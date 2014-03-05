
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
	ArrayList<ArrayList<String[]>> wordChunks;
	ArrayList<ArrayList<String[]>> tagChunks;
	private final double reallySmallValue = -1000.0;

	/**
	 * Constructor
	 * initializes the word map and the tag map
	 */
	public SpeechRecognizer() {
//		wordMap = new HashMap<String, Map<String, Integer>>(); //start wordMap as a hashmap
//		tagMap = new HashMap<String, Map<String, Integer>>(); //also start tagMap
		transitionMatrix = new HashMap<String, Map<String, Double>>(); //start transititon matrix
		emissionMatrix = new HashMap<String, Map<String, Double>>(); //start emissions matrix
	
	}

	/**
	 * creates the maps for tags to words and tags to next possible tags in linear time 
	 * sets up the data on the fly as we read through the two files
	 * @throws Exception
	 */
	private void quickTrain() throws Exception {

		long startTime = System.currentTimeMillis();
		//create readers for all the word in puts and the tag inputs
		BufferedReader words = new BufferedReader(new FileReader("inputs/Brown-words.txt")); //where do we get our words from input document 
		BufferedReader tags = new BufferedReader(new FileReader("inputs/Brown-tags.txt"));   //get our tags from other input doc

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

		long endTime = System.currentTimeMillis();
		System.out.println("Total Time = " + (endTime - startTime));
	}

	/**
	 * 
	 * @param numberOfChunks
	 * @param lines
	 * @return
	 * @throws Exception
	 */
	public double testOnNumberOfChunksAndLines(int numberOfChunks, int lines) throws Exception {
		double score = 0.0;
		wordChunks = new ArrayList<ArrayList<String[]>>(numberOfChunks);
		tagChunks = new ArrayList<ArrayList<String[]>>(numberOfChunks);
		for (int c = 0; c < numberOfChunks; c ++) {
			ArrayList<String[]> wordPlaceHolder = new ArrayList<String[]>();
			wordChunks.add(wordPlaceHolder);
			ArrayList<String[]> tagPlaceHolder = new ArrayList<String[]>();
			tagChunks.add(tagPlaceHolder);
		}
		int counter = 0;
		//create readers for all the word in puts and the tag inputs
		BufferedReader words = new BufferedReader(new FileReader("inputs/Brown-words.txt")); //where do we get our words from input document 
		BufferedReader tags = new BufferedReader(new FileReader("inputs/Brown-tags.txt"));   //get our tags from other input doc

		String wordLine = new String(); //string that takes each line of the words file
		String tagLine = new String();  //string that takes each line of the tags  file

		while (counter < lines && (wordLine = words.readLine()) != null && (tagLine = tags.readLine()) != null) { //while we can continue reading through the words and tags
			wordChunks.get(counter % numberOfChunks).add(wordLine.split(" "));
			tagChunks.get(counter % numberOfChunks).add(tagLine.split(" "));
			counter ++;
		}

		System.out.println("word chunks has " + wordChunks.size() + " chunks and " + wordChunks.get(0).size() + " lines in the first chunk");

		words.close();
		tags.close();

		for (int i = 0; i < numberOfChunks; i++) {
			doNotTestOn(i);
			transitionMatrix = createTransitionMatrix();
			emissionMatrix = createEmissionMatrix();
			//System.out.println(numberOfChunks + " " + score);
			score += testOnChunk(i);
		}

		
		/*for(int i = 0; i < tagChunks.size(); i ++) {
			for(int j = 0; j < tagChunks.get(i).size(); j++) {
				for(int k = 0; k < tagChunks.get(i).get(j).length; k++) {
					System.out.println(tagChunks.get(i).get(j)[k] + " " + wordChunks.get(i).get(j)[k]);
				}
			}
		} */
		
		return score / numberOfChunks;
	}
	
	public double testOnNumberOfChunksWithAllLines(int numberOfChunks) throws Exception {
		double score = 0.0;
		wordChunks = new ArrayList<ArrayList<String[]>>(numberOfChunks);
		tagChunks = new ArrayList<ArrayList<String[]>>(numberOfChunks);
		for (int c = 0; c < numberOfChunks; c ++) {
			ArrayList<String[]> wordPlaceHolder = new ArrayList<String[]>();
			wordChunks.add(wordPlaceHolder);
			ArrayList<String[]> tagPlaceHolder = new ArrayList<String[]>();
			tagChunks.add(tagPlaceHolder);
		}
		int counter = 0;
		//create readers for all the word in puts and the tag inputs
		BufferedReader words = new BufferedReader(new FileReader("inputs/Brown-words.txt")); //where do we get our words from input document 
		BufferedReader tags = new BufferedReader(new FileReader("inputs/Brown-tags.txt"));   //get our tags from other input doc

		String wordLine = new String(); //string that takes each line of the words file
		String tagLine = new String();  //string that takes each line of the tags  file

		while ((wordLine = words.readLine()) != null && (tagLine = tags.readLine()) != null) { //while we can continue reading through the words and tags
			wordChunks.get(counter % numberOfChunks).add(wordLine.split(" "));
			tagChunks.get(counter % numberOfChunks).add(tagLine.split(" "));
			counter ++;
		}

		System.out.println("word chunks has " + wordChunks.size() + " chunks and " + wordChunks.get(0).size() + " lines in the first chunk");

		words.close();
		tags.close();

		for (int i = 0; i < numberOfChunks; i++) {
			System.out.println("Testing..." + (double) i/numberOfChunks*100 + "%");
			doNotTestOn(i);
			transitionMatrix = createTransitionMatrix();
			emissionMatrix = createEmissionMatrix();
			//System.out.println(numberOfChunks + " " + score);
			score += testOnChunk(i);
		}
		return score / numberOfChunks;
	}

	/**
	 * 
	 * @param doNotTest
	 */
	private void doNotTestOn(int doNotTest) {		
		wordMap = new HashMap<String, Map<String, Integer>>(); //start wordMap as a hashmap
		tagMap = new HashMap<String, Map<String, Integer>>(); //also start tagMap
		for (int i = 0; i < wordChunks.size(); i ++) {
			if (i != doNotTest) {
				for (int j = 0; j < wordChunks.get(i).size(); j ++) {
					putTagInTagMapOfPrevTag(tagChunks.get(i).get(j)); //handle tags and prev tags map
					putWordsInMapsOfTags(wordChunks.get(i).get(j), tagChunks.get(i).get(j)); //map words into wordmap of tags
				}
			}
		}			
	}

	/**
	 * 
	 * @param doTest
	 * @return
	 */
	private double testOnChunk(int doTest) {
		double accuracy = 0;
		for (int i = 0; i < tagChunks.get(doTest).size(); i ++) {
			String[] predictedTags = predictOnString(wordChunks.get(doTest).get(i));
			String[] actualTags = tagChunks.get(doTest).get(i);
			
			
			/*for(int z = 0; z < predictedTags.length; z++) {
				System.out.println(predictedTags[z] + " " + actualTags[z]);
			} */
			
			
			double percentage = 0;
			for (int j = 0; j < predictedTags.length; j ++) {
				if (actualTags[j].equalsIgnoreCase(predictedTags[j])) {
					percentage ++;
				}
			}
			accuracy += (percentage/actualTags.length);
		}
		return accuracy/tagChunks.get(doTest).size();
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
	private Map<String, Map<String, Double>> createTransitionMatrix() {
		int denominator; //divides the frequencies in second sub-for-loop
		Map<String, Map<String, Double>> tempTransitionMatrix = new HashMap<String, Map<String, Double>>();
		for(String state : tagMap.keySet()) { //for every tag in the tagMap
			denominator = 0; //set denominator as zero when we start calculating the new probabilities
			for(Integer frequencies : tagMap.get(state).values()) { //get the frequency of all next possible states coming from our start tag 
				denominator += frequencies; //sum up all the frequencies
			}
			Map<String, Double> element = new HashMap<String, Double>(); //create our element in the transition matrix
			for(String nextState : tagMap.get(state).keySet()) { //for every 2nd tag of each tag1 in the tagMap
				double transitionProbability = Math.log((double) tagMap.get(state).get(nextState) / denominator); //calculate the transition probability from state i to state j
				element.put(nextState, transitionProbability); //populate that element
			}
			tempTransitionMatrix.put(state, element); //put our transition probability in for going from state to next state
		}
		return tempTransitionMatrix;
	}

	/**
	 * create the emission matrix we will use in the future to calculate probabilities
	 */
	private Map<String, Map<String, Double>> createEmissionMatrix() {
		int denominator; //divides the frequencies 
		Map<String, Map<String, Double>> tempEmissionMatrix = new HashMap<String, Map<String, Double>>();
		for(String state: wordMap.keySet()) { //for every tag in wordMap
			denominator = 0; //start over our denominator
			for(Integer frequencies : wordMap.get(state).values()) { //for all frequencies of a state's words
				denominator += frequencies; //summ all of the words for that state
			}
			Map<String, Double> element = new HashMap<String, Double>(); //create our element placeholder
			for(String word : wordMap.get(state).keySet()) { //for all words in that tag type
				double emissionProbability = Math.log((double) wordMap.get(state).get(word) / denominator); //calculate the emission probabilitye for a word within a state
				element.put(word, emissionProbability); //populate the element
			}
			tempEmissionMatrix.put(state, element); //put the emission probability in our emission matrix
		}
		return tempEmissionMatrix;
	}

	/**
	 * 
	 * @param nextState
	 * @param word
	 * @return
	 */
	private double getEmission(String nextState, String word) {
		if(emissionMatrix.get(nextState).containsKey(word)) {
			return emissionMatrix.get(nextState).get(word);
		}
		else {
			return reallySmallValue;
		}
	}

	/**
	 * 
	 * @param state
	 * @param nextState
	 * @return
	 */
	private double getTransition(String state, String nextState) {
		if(transitionMatrix.get(state).containsKey(nextState)) {
			return transitionMatrix.get(state).get(nextState);
		}
		else {
			return reallySmallValue;
		}
	}
	
	
	/**
	 * 
	 * @param observations
	 * @return
	 */
	private String[] predictOnString(String[] observations) {
		ArrayList<Map<String, String>> backTrace = new ArrayList<Map<String, String>>(); //create a backtrace arraylist of maps
		Map<String, Double> states = new HashMap<String, Double>();
		states.put("START", (double) 0); //put in 0 for the start state's probability since log(1) = 0
		for(String word : observations) { //for each word
			Map<String, Double> scores = new HashMap<String, Double>();
			Map<String, String> backPath = new HashMap<String, String>();
			for(String state: states.keySet()) { //for each state
				
				
				if(tagMap.containsKey(state)) {
					
					for(String nextState: tagMap.get(state).keySet()) {
						double score = states.get(state) + getTransition(state, nextState) + getEmission(nextState, word);
						//System.out.println("This is the score " + score);
						if(!scores.containsKey(nextState) || score > scores.get(nextState)) { //if overwrite
							scores.put(nextState, score); //put the score in the score map
							backPath.put(nextState, state); //put it in our map
						}
					}
				}
			}
			backTrace.add(backPath); //add the backpath at that word level
			states = scores; //get ready to iterate on the next set of states
		}
		
		
		/*for(Map<String, String> m : backTrace) {
			System.out.println(m.toString());
		}*/
		
		
		String[] predictedTags = new String[observations.length];
		String maxKey=null;
		Double maxValue = reallySmallValue;
		for(String state : states.keySet()) {
			if(states.get(state) > maxValue) {
				maxValue = states.get(state);
				maxKey = state;
			}
		}
		predictedTags[predictedTags.length - 1] = maxKey;
		for(int i = predictedTags.length - 2; i > -1; i --) {
			predictedTags[i] = backTrace.get(i + 1).get(predictedTags[i+1]);
		}
		return predictedTags;
	}

	/**
	 * 
	 * @param observations
	 * @return
	 */
	/*private Map<String, Map<Integer, Double>> createProbabilityTable(String[] observations) {
		Map<String, Map<Integer, Double>> probabilityTable = new HashMap<String, Map<Integer, Double>>();
		for(String state : tagMap.keySet()) {

		}
		return probabilityTable;
	} */

	/**
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		SpeechRecognizer s = new SpeechRecognizer();
		//s.quickTrain();
		/*String test = new String("The Fulton County Grand Jury said Friday an investigation of Atlanta's recent primary election produced `` no evidence '' that any irregularities took place .");
		String[] backTrace = s.predictOnString(test.split(" "));
		String tags = new String();
		for(String tag : backTrace) {
					tags += (tag + " ");
				} 
		System.out.println("length is " + backTrace.length + " and values are " + tags);
		*/
		//System.out.println("Accuracy is " + s.testOnNumberOfChunksAndLines(1, 1));
		System.out.println("Accuracy is " + s.testOnNumberOfChunksWithAllLines(100));

	}
}