
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

public class TwitterMapper extends Mapper<LongWritable, Text, Text, IntWritable> {

	public enum TwitterCount{
		LOW, HIGH, ALL	
	}
	
	private long lowCount = 0;
	private long highCount = 0;
	private long allCount = 0;
	private HashSet<String> englishWords = new HashSet<String>();
	
	public void setup(Context context) {
		try {
			InputStream modelIn = TwitterMapper.class.getResourceAsStream("en-sent.bin");
			SentenceModel sentenceModel = new SentenceModel(modelIn);
			sentenceDetector = new SentenceDetectorME(sentenceModel);
			modelIn.close();
			
			modelIn = TwitterMapper.class.getResourceAsStream("en-token.bin");
			TokenizerModel tokenizerModel = new TokenizerModel(modelIn);
			tokenizer = new TokenizerME(tokenizerModel);
			modelIn.close();
			
			modelIn = TwitterMapper.class.getResourceAsStream("en-pos-maxent.bin");
			POSModel posModel = new POSModel(modelIn);
			tagger = new POSTaggerME(posModel);
			modelIn.close();
			
			InputStream dictionary = TwitterMapper.class.getResourceAsStream("Englishwords.txt");
			InputStreamReader dictionaryFileReader = new InputStreamReader(dictionary);
			BufferedReader dictionaryBufferedReader = new BufferedReader(dictionaryFileReader);
			
			String currentWord = dictionaryBufferedReader.readLine();
			while(currentWord != null) {
				englishWords.add(currentWord);
				currentWord = dictionaryBufferedReader.readLine();
			}
			dictionaryBufferedReader.close();
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
	}
	
	@Override
	protected void cleanup(Context context) {
		try {
			Path pt = new Path("hdfs:/twittertempdir/" + context.getTaskAttemptID());
			System.out.println(pt.toString() + " " + lowCount+ " " + highCount + " " + allCount);
			FileSystem fs = FileSystem.get(new Configuration());
			BufferedWriter br = new BufferedWriter(new OutputStreamWriter(
					fs.create(pt, true)));
			br.write(Long.toString(lowCount));
			br.write('\n');
			br.write(Long.toString(highCount));
			br.write('\n');
			br.write(Long.toString(allCount));
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static SentenceDetectorME sentenceDetector;
	private static TokenizerME tokenizer;
	private static POSTaggerME tagger;
	
	private static Pattern followersPattern = Pattern.compile("\"followers_count\":([\\d]+)");
	private static Pattern langPattern = Pattern.compile("\"lang\":\"en\"");
	private static Pattern tweetTextPattern = Pattern.compile("\"text\":\"([^\"]+)\"");
 
  @Override
  public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {

		String line = value.toString();
		Matcher followersMatcher = followersPattern.matcher(line);
		Matcher languageMatcher = langPattern.matcher(line);
		Matcher tweetTextMatcher = tweetTextPattern.matcher(line);
		if (languageMatcher.find() && tweetTextMatcher.find() && followersMatcher.find()) {
			int followers = Integer.parseInt(followersMatcher.group(1));
			String tweetText = tweetTextMatcher.group(1);
			// Convert escape sequences into normal characters
			tweetText = StringEscapeUtils.unescapeJava(tweetText).toLowerCase();

			// Remove usernames, hashtags, and URLs
			tweetText.replaceAll(
					"(?:rt )?[@#][\\w]+:?|\n|https?://[a-zA-Z0-9\\./]+", " ")
					.trim();
			if (tweetText.length() > 2) {
				String[] sentences = sentenceDetector.sentDetect(tweetText);
				for (String sentence : sentences) {
					String[] tokens = tokenizer.tokenize(sentence);
					boolean english = false;
					int engWordsFound = 0;
					int engWordsNeeded = tokens.length / 5;
					for(String token : tokens) {
						if(englishWords.contains(token)) {
							engWordsFound++;
							if(engWordsFound >= engWordsNeeded) {
								english = true;
								break;
							}
						}
					}
					if(english) {
						context.getCounter(TwitterCount.ALL).increment(1);
						allCount++;
						if (followers < 100) {
							context.getCounter(TwitterCount.LOW).increment(1);
							lowCount++;
						} else if(followers > 10000) {
							context.getCounter(TwitterCount.HIGH).increment(1);
							highCount++;
						}
						String[] tags = tagger.tag(tokens);
						for (int i = 0; i < tokens.length; i++) {
							// Only use words tagged as nouns or gerunds that don't contain special characters
							if (tags[i].matches("NNP?S?|VBG")
									&& tokens[i].matches("[a-z]+")) {
								context.write(new Text(tokens[i]), new IntWritable(followers));
							}
						}
					}
				}
			}
		}
  }

}


