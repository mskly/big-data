import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class TwitterReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

  @Override
  public void reduce(Text key, Iterable<IntWritable> values, Context context)
      throws IOException, InterruptedException {

	  int countLow = 0;
	  int countHigh = 0;
	  for (IntWritable value : values) {
		  if(value.get() < 100) {
			  countLow++;
		  } else {
			  countHigh++;
		  }
	  }
	  if(countLow >= 10 || countHigh >= 10) {
		  // TODO: Store the number of occurrences of each word in HBase (with different columns for the counts among low and high-follower users)?
		  context.write(new Text(key.toString() + "_LOW"), new IntWritable(countLow));
		  context.write(new Text(key.toString() + "_HIGH"), new IntWritable(countHigh));
	  }
  }
}
