

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocatedFileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.RemoteIterator;


public class TwitterReducer extends Reducer<Text, IntWritable, DoubleWritable, Text> {

	private static long numHighTweets;
	private static long numLowTweets;
	private static long numAllTweets;
	
	@Override
	public void setup(Context contex) {
		try {
			FileSystem fs = FileSystem.get(new Configuration());
			Path dirPath = new Path("hdfs:/twittertempdir/");
			
			if(fs.exists(dirPath) && fs.isDirectory(dirPath)) {
				RemoteIterator<LocatedFileStatus> ri =  fs.listFiles(dirPath, true);
				while(ri.hasNext()) {
					LocatedFileStatus lfs = ri.next();
					BufferedReader br1 = new BufferedReader(new InputStreamReader(
							fs.open(lfs.getPath())));
					String line = br1.readLine();
					String line2 = br1.readLine();
					String line3 = br1.readLine();
					numLowTweets += Long.parseLong(line);
					numHighTweets += Long.parseLong(line2);
					numAllTweets += Long.parseLong(line3);
					System.out.println(lfs.getPath().toString() + " " + numLowTweets + " " + numHighTweets + " " + numAllTweets);
					br1.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

  @Override
  public void reduce(Text key, Iterable<IntWritable> values, Context context)
      throws IOException, InterruptedException {
	  
	  int countLow = 0;
	  int countHigh = 0;
	  int countAll = 0;
	  for (IntWritable value : values) {
		  countAll++;
		  if(value.get() < 100) {
			  countLow++;
		  } else if(value.get() > 10000){
			  countHigh++;
		  }
	  }

	double probWordGivenHigh = countHigh * 1.0 / numHighTweets;
	double probWordGivenNotHigh = (1 + countAll - countHigh) * 1.0 / (numAllTweets - numHighTweets);
	double probWordGivenLow = countLow * 1.0 / numLowTweets;
	double probWordGivenNotLow = (1 + countAll - countLow) * 1.0 / (numAllTweets - numLowTweets);
	double relativeRiskHigh = probWordGivenHigh / probWordGivenNotHigh;
	double relativeRiskLow = probWordGivenLow / probWordGivenNotLow;
	
	if( countAll > 50 ) { 
		if(relativeRiskHigh > 1.5) {
		  context.write(new DoubleWritable(relativeRiskHigh), new Text(key.toString() + "_HIGH_" + countHigh));
		} else if(relativeRiskLow > 1.5) {
		  context.write(new DoubleWritable(relativeRiskLow), new Text(key.toString() + "_LOW_" + countLow));
		}
	}

  }
}
