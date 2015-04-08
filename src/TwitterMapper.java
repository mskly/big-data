package src;
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class TwitterMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

	private static Pattern followersPattern = Pattern.compile("\"followers_count\":([\\d]+)");
	private static Pattern tweetTextPattern = Pattern.compile("\"text\":(.+)\"source\":");

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
	  Matcher tweetTextMatcher = tweetTextPattern.matcher(line);
	  if(tweetTextMatcher.find()) {
		String tweetText;
		int followers;
		tweetText = getTweet(line);
		tweetText = removeQuotes(tweetText);  
	
		
		if(followersMatcher.find()) { 
	  followers = Integer.parseInt(followersMatcher.group(1));
		  if (followers < 1000) {
			  followers = 100 * (followers / 100);
		  } else {
			  followers = 1000 * (followers / 1000);
		  }      }
		else 
			followers = -1;
		  context.write(new IntWritable(followers), new Text(tweetText));
		  }

			}


	  
  }
