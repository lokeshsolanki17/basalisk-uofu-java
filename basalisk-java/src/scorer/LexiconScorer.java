package scorer;

import java.util.*;
import java.io.*;

public class LexiconScorer {

	/**
	 * Entry point for the lexicon learner
	 * @param args Input args, ordered as such:
	 * 				<semantic_keyfile> <slist_of_lexicons_to_score>
	 * 					-o OutputDir/
	 */
	public static void main(String[] args) {
		if(args.length < 2){
			System.err.println("Wrong number of input arguments. Please supply: <semantic_keyfile> <slist_of_lexicons_to_score> ");
			return;
		}
		
		//Check for optional input arguments
		int argsSeen = 2;
		
		//Set default values
		String outputDir = "";
		
		while ((argsSeen + 2) <= args.length){
			if(args[argsSeen].equalsIgnoreCase("-o"))
				outputDir = args[argsSeen+1];
			else{
				System.err.println("Unknown input options: " + args[argsSeen] + args[argsSeen+1]);
			}
			argsSeen += 2;
		}
		
		//Initialize the lexicon scorer
		LexiconScorer ls = new LexiconScorer(args[0], args[1], outputDir);

	}
	
	public LexiconScorer(String semKeyFileName, String lexiconSlistFileName, String outputDir){
		//Load up our semantic key dictionary
		Map<String, Set<String>> semKeyDictionary = loadSemKeyDictionary(semKeyFileName);
		
		//Load up the lexicon map
		Map<String, List<String>> lexicons = loadMultipleLexicons(lexiconSlistFileName);
		
		//Score each lexicon
		for(String categoryAndSuffix: lexicons.keySet()){
			String category = categoryAndSuffix.replaceAll("-.*", "");
			
			List<String> lexicon = lexicons.get(categoryAndSuffix);
			Set<String> correctLexicon = semKeyDictionary.get(category);
			
			//Create a printstream to record the results of the score
			PrintStream scoreOutput = null;
			try{
				scoreOutput = new PrintStream(outputDir + categoryAndSuffix + ".score");
				System.out.format("Creating a score file for the %s category\n", category);
			}
			catch (Exception e){
				System.err.println(e.getMessage());
				continue;
			}
			scoreOutput.format("#Score output for the %s.lexicon file\n", categoryAndSuffix);
			scoreOutput.format("#%20s %20s\n", "CorrectEntries", "TotalEntries");

			//Create a printstream to record all of the incorrectly labeled words
			PrintStream correctOutput = null;
			try{
				correctOutput = new PrintStream(outputDir + categoryAndSuffix + ".correct");
			}
			catch (Exception e){
				System.err.println(e.getMessage());
				continue;
			}
			correctOutput.format("Entries in the %s.lexicon file that were correctly labeled:\n", categoryAndSuffix);
			
			//Create a printstream to record all of the incorrectly labeled words
			PrintStream incorrectOutput = null;
			try{
				incorrectOutput = new PrintStream(outputDir + categoryAndSuffix + ".incorrect");
			}
			catch (Exception e){
				System.err.println(e.getMessage());
				continue;
			}
			incorrectOutput.format("Entries in the %s.lexicon file that were incorrectly labeled:\n", categoryAndSuffix);
			
			//Create a printstream to record all of the words that weren't in any key (meaning we extracted a non-headnoun
			PrintStream unlabeledOutput = null;
			try{
				unlabeledOutput = new PrintStream(outputDir + categoryAndSuffix + ".unlabeled");
			}
			catch (Exception e){
				System.err.println(e.getMessage());
				continue;
			}
			unlabeledOutput.format("Entries in the %s.lexicon file that weren't found in any key:\n", categoryAndSuffix);
			
			//Compare each word in the lexicon against the appropriate key
			int correct = 0;		//Correctly labeled lexicon entries
			int total = 0;			//Total lexicon entries seen
			System.out.format("Beginning to score the %s category\n", category);
			
			for(String lexiconMember: lexicon){
				total++;
				if(lexiconMember.equals("bogota"))
					System.out.println("Debug point reached");
				
				boolean inAtLeastOneKey = false;
				if(correctLexicon.contains(lexiconMember)){
					correct++;
					correctOutput.println(lexiconMember);
					inAtLeastOneKey = true;
				}
				scoreOutput.format("%20d %20d\n", correct, total);
				
				//Check to see if the key exists in at least one other key
				for(String keyCategory: semKeyDictionary.keySet()){
					//Make sure we're looking at a different category
					if(keyCategory.equalsIgnoreCase(category)) continue;
					Set<String> keyLexicon = semKeyDictionary.get(keyCategory);
					if(keyLexicon.contains(lexiconMember)){
						inAtLeastOneKey = true;
						incorrectOutput.format("%s %s\n", lexiconMember, keyCategory);
					}
				}
				//If it doesn't, record the member as being unlabeled
				if(!inAtLeastOneKey)
					unlabeledOutput.println(lexiconMember);
				
			}
			
			//Close the printstream
			System.out.format("Finished scoring the %s category\n\n", category);
			scoreOutput.close();
			unlabeledOutput.close();
		}
	}

	/**
	 * Creates a map from each category, the lexicon of words learned by those categories. Loads the map from an slist of lexicon files.
	 * Category names are converted into lower case words by default.
	 * 
	 * NOTE: Each lexicon file should be named appropriately according to it's semantic category. This file assumes that the proper name
	 * for each semantic category is the name (minus the extension) of each file. For example, if a file is called "pizza.lexicon", then
	 * this method will assume it is gathering words from the "pizza" category. It is essential that the semKey file have the same
	 * name for categories as does the file name for each lexicon. If the key identifies words as belong to the "pizzas" (plural) 
	 * category, but the lexicon file is "pizza.lexicon", then it won't be able to match the "pizza" category with the "pizzas" 
	 * category to check for correctness.
	 * 
	 * NOTE: It is acceptable to suffix the category name with a description of how it was extracted. So, pizza-mcat.lexicon is an 
	 * acceptable way to label the "pizza" lexicon as well.
	 * 
	 * 
	 * @param lexiconSlistFileName - An slist of the lexicon files. The slist is a file where the first line is the directory, and
	 * 									each subsequent line contains the name of a lexicon inside of that directory.
	 * 								E.g: /example-dir/
	 * 									 category.lexicon	
	 * 							
	 * @return
	 */
	private Map<String, List<String>> loadMultipleLexicons(String lexiconSlistFileName) {
		//Create a file wrapper for the slist file
		File slistFile = new File(lexiconSlistFileName);
		if(!slistFile.exists()){
			System.err.println("Error: could not find slist file. Slist file: " + slistFile.getAbsolutePath());
			return null;
		}
		
		//Create a scanner to read the slist
		Scanner slistScanner = null;
		try{
			slistScanner = new Scanner(slistFile);
		}
		catch (Exception e){
			System.err.println(e.getMessage());
			return null;
		}
		
		//The directory of each lexicon file is the first line of the slist file
		String dir = slistScanner.nextLine();
		
		//Create the lexiconmap
		Map<String, List<String>> lexiconMap = new HashMap<String, List<String>>();
		
		//Create an entry in the map for each file entry in the slist
		while(slistScanner.hasNext()){
			//Each lexicon filename is stored on a separate line
			String fileName = slistScanner.nextLine();
			
			//Read the set of words for each category
			List<String> lexicon = loadSingleLexicon(dir + fileName);
			
			//Determine the category of the file
			String categoryAndSuffix = fileName.replaceAll("\\..*", "").trim().toLowerCase();
			
			//Put the category and it's lexicon in the map
			lexiconMap.put(categoryAndSuffix, lexicon);
		};
		
		return lexiconMap;
	}

	/**
	 * Given the string of the filename of a lexicon file, this method will return a Set<String> containing all of the words from that
	 * file. Each lexicon file is assumed to contain one word on each line.
	 * @param lexiconFileName - A file containing one category word per line.
	 * @return Set<String> - a set containing all of the words that belong to that category.
	 */
	private List<String> loadSingleLexicon(String lexiconFileName) {
		//Create a file wrapper for the lexicon file
		File lexiconFile = new File(lexiconFileName);
		if(!lexiconFile.exists()){
			System.err.println("Lexicon file could not be read. File: " + lexiconFile.getAbsolutePath());
			return null;
		}
		
		//Create a scanner for the lexicon file
		Scanner lexiconScanner = null;
		try{
			lexiconScanner = new Scanner(lexiconFile);
		}
		catch (Exception e){
			System.err.println(e.getMessage());
			return null;
		}
		
		//Scan through the file, storing each line as a word member of the category
		List<String> lexicon = new ArrayList<String>();
		while(lexiconScanner.hasNext()){
			String catMember = lexiconScanner.nextLine().trim().toLowerCase();
			lexicon.add(catMember);
		}
		return lexicon;
	}

	/**
	 * Loads up the semantic key dictionary from a file. This is just a set of strings (categories) that map to a list of other strings (the 
	 * words that belong to each category). Category names and category members are all converted to lower case by default.	  
	 * 
	 * @param semKeyFileName - File name of the semantic key. Each line of the file should be of the form: CATEGORYMEMBER CATEGORY
	 * @return A map relating strings (categories) to sets of other strings (category members) 
	 */
	private Map<String, Set<String>> loadSemKeyDictionary(String semKeyFileName) {
		//Attempt to read the keyfile
		File keyFile = new File(semKeyFileName);
		if(!keyFile.exists()){
			System.err.println("Key file could not be found: " + keyFile.getAbsolutePath());
			return null;
		}
		
		//Create a scanner for the file
		Scanner keyScanner = null;
		try{
			keyScanner = new Scanner(keyFile);
		}
		catch (Exception e){
			System.err.println(e.getMessage());
		}
		
		//Create our map
		Map<String, Set<String>> result = new HashMap<String, Set<String>>();
		
		//Scan each line
		while(keyScanner.hasNext()){
			String catMember = keyScanner.next().trim().toLowerCase();
			String category = keyScanner.next().trim().toLowerCase();
			
			//If the category already has members, update it with the new entry
			if(result.get(category) != null){
				result.get(category).add(catMember);
			}
			//Otherwise, create a new set and add that to the map
			else{
				HashSet<String> memberList = new HashSet<String>();
				memberList.add(catMember);
				result.put(category, memberList);
			}
		}
		return result;
	}

}
