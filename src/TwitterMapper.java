
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class TwitterMapper extends Mapper<LongWritable, Text, IntWritable, Text> {

	private static Pattern followersPattern = Pattern.compile("\"followers_count\":([\\d]+)");
	private static Pattern tweetTextPattern = Pattern.compile("\"Text\":(.+)\"source");

  public static String removeQuotes(String text) {
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
 
  @Override
  public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {

	  String line = value.toString();
	  Matcher followersMatcher = followersPattern.matcher(line);
	  Matcher tweetTextMatcher = tweetTextPattern.matcher(line);
	  if(followersMatcher.find()) {
		if(tweetTextMatcher.find()) {
		  int followers = Integer.parseInt(followersMatcher.group(1));
		  if (followers < 1000) {
			  followers = 100 * (followers / 100);
		  } else {
			  followers = 1000 * (followers / 1000);
		  }
		String tweetText = tweetTextMatcher.group(1);
		tweetText = removeQuotes(tweetText);
		  context.write(new IntWritable(followers), new Text(tweetText));
		}

		}
	  }
	  
  }
