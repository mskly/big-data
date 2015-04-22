package src;


import java.io.IOException;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Cluster; 
import org.apache.hadoop.conf.Configuration;



public class TwitterReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

	private static long highCount;
	private static long lowCount;

	@Override
    public void setup(Context context) throws IOException, InterruptedException{
        Configuration conf = context.getConfiguration();
        Cluster cluster = new Cluster(conf);
        Job currentJob = cluster.getJob(context.getJobID());
        highCount = currentJob.getCounters().findCounter(TwitterMapper.TwitterCount.LOW).getValue();  
	lowCount = currentJob.getCounters().findCounter(TwitterMapper.TwitterCount.HIGH).getValue();  
    }

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
