
import java.io.IOException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

public class TwitterMapper extends Mapper<LongWritable, Text, IntWritable, IntWritable> {

	private static Pattern followersPattern = Pattern.compile("\"followers_count\":([\\d]+)");
  @Override
  public void map(LongWritable key, Text value, Context context)
      throws IOException, InterruptedException {

	  String line = value.toString();
	  Matcher followersMatcher = followersPattern.matcher(line);
	  if(followersMatcher.find()) {
		  int followers = Integer.parseInt(followersMatcher.group(1));
		  if (followers < 1000) {
			  followers = 100 * (followers / 100);
		  } else {
			  followers = 1000 * (followers / 1000);
		  }
		  context.write(new IntWritable(followers), new IntWritable(1));
	  }
	  
  }
}
