package src;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Cluster; 
import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobID;
import org.apache.hadoop.mapred.RunningJob;
import org.apache.hadoop.mapred.JobConf;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


public class TwitterReducer extends Reducer<Text, IntWritable, Text, IntWritable> {

	private static long highCount;
	private static long lowCount;

	@Override
	public void setup(Context contex) {
	
	try {

		Path pt = new Path("hdfs:/twittertempfile");
		FileSystem fs = FileSystem.get(new Configuration());
		BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(pt)));
		String line = br.readLine();
		String line2 = br.readLine();
		lowCount = Long.parseLong(line);
		highCount = Long.parseLong(line2);
		}

	catch(IOException e) {
	e.printStackTrace();
	}
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
/**
	  if(countLow >= 10 || countHigh >= 10) {
		  // TODO: Store the number of occurrences of each word in HBase (with different columns for the counts among low and high-follower users)?
		  context.write(new Text(key.toString() + "_LOW"), new IntWritable(countLow));
		  context.write(new Text(key.toString() + "_HIGH"), new IntWritable(countHigh));
	  }

**/
	double lowAmount = countLow * 1.0 / lowCount;
	double highAmount = countHigh * 1.0 / highCount;
	double highDif = highAmount / lowAmount;
	double lowDif = lowAmount / highAmount;
	if( 10 * lowAmount < highAmount ) 
		  context.write(new Text(key.toString() + "_LOW"), new IntWritable((int)highDif));
	else
		  context.write(new Text(key.toString() + "_HIGH"), new IntWritable((int)lowDif));


  }
}
