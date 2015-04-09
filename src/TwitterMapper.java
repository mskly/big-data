import java.io.InputStream;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.commons.lang.StringEscapeUtils;
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
		}
		catch (IOException e) {
		  e.printStackTrace();
		}
	}
	
	private static SentenceDetectorME sentenceDetector;
	private static TokenizerME tokenizer;
	private static POSTaggerME tagger;
	
	private static Pattern followersPattern = Pattern.compile("\"followers_count\":([\\d]+)");
	private static Pattern langPattern = Pattern.compile("\"lang\":\"en\"");
	private static Pattern tweetTextPattern = Pattern.compile("\"text\":\"([^\"]+)\"");

  public String removeQuotes(String text) {
	int i;
	int j;
	String newText = text;
	for(i = 0; i < newText.length(); i++)
		if(newText.charAt(i) == '\"') {
			newText = newText.substring(i + 1, newText.length());
			break;
			}
	for(j = newText.length() - 1; j >= 0; j--)
		if(newText.charAt(j) == '\"') {
			newText = newText.substring(0, j);
			break;  }
	return newText;
  }

  public String getTweet(String text) {
	String compSource = "\"source\":";
	String compText = "\"text\":";
	int i = 0;
	while(!text.substring(i, i + 7).equals(compText) )
		i++; 

	int j = i + 7;
	i = j;
	while(!text.substring(j, j + 9).equals(compSource) )
		j++;
	return text.substring(i, j - 1);
	}
 
  @Override
  public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {

	  String line = value.toString();
	  	Matcher followersMatcher = followersPattern.matcher(line);
		Matcher languageMatcher = langPattern.matcher(line);
		Matcher tweetTextMatcher = tweetTextPattern.matcher(line);
		if (languageMatcher.find() && tweetTextMatcher.find() && followersMatcher.find()) {
				int followers = Integer.parseInt(followersMatcher.group(1));
				// I figured this was a good split between very few and very many followers
				if(followers < 100 || followers > 10000) {
					String tweetText = tweetTextMatcher.group(1);
					// Convert escape sequences into normal characters
					tweetText = StringEscapeUtils.unescapeJava(tweetText).toLowerCase();
					
					// Remove usernames and hashtags
					tweetText = tweetText.replaceAll("[@#][\\w]+", " ");
	
					String[] sentences = sentenceDetector.sentDetect(tweetText);
					for (String sentence : sentences) {
						String[] tokens = tokenizer.tokenize(sentence);
						String[] tags = tagger.tag(tokens);
						for (int i = 0; i < tokens.length; i++) {
							// Only use words tagged as nouns that don't contain special characters
							if (tags[i].matches("NNP?S?") && tokens[i].matches("[a-z]+")) {
								context.write(new Text(tokens[i]), new IntWritable(followers));
							}
						}
					}
				}
			}
		}

	}


