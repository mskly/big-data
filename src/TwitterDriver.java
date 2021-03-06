
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.Job;
/**import org.apache.hadoop.mrunit.mapreduce.MapDriver;
import org.apache.hadoop.mrunit.mapreduce.MapReduceDriver;
import org.apache.hadoop.mrunit.mapreduce.ReduceDriver;
import org.junit.Before;
import org.junit.Test;
**/
public class TwitterDriver {
	/**
	  MapDriver<LongWritable, Text, Text, IntWritable> mapDriver;
	  ReduceDriver<Text, IntWritable, Text, IntWritable> reduceDriver;
	  MapReduceDriver<LongWritable, Text, Text, IntWritable, Text, IntWritable> mapReduceDriver;
	
	@Before
	  public void setUp() {

	   
	     * Set up the mapper test harness.

	    TwitterMapper mapper = new TwitterMapper();
	    mapDriver = new MapDriver<LongWritable, Text, Text, IntWritable>();
	    mapDriver.setMapper(mapper);

	    
	     * Set up the reducer test harness.
	     
	    TwitterReducer reducer = new TwitterReducer();
	    reduceDriver = new ReduceDriver<Text, IntWritable, Text, IntWritable>();
	    reduceDriver.setReducer(reducer);

	    
	     * Set up the mapper/reducer test harness.
	    
	    mapReduceDriver = new MapReduceDriver<LongWritable, Text, Text, IntWritable, Text, IntWritable>();
	    mapReduceDriver.setMapper(mapper);
	    mapReduceDriver.setReducer(reducer);
	  }
	
	  @Test
	  public void testMapper() {

	   //mapDriver.withInput(new LongWritable(1), new Text(""));
	   // mapDriver.withOutput(new IntWritable(1), new Text(""));
	    //mapDriver.runTest();

	  }
	**/

  public static void main(String[] args) throws Exception {

    /*
     * Validate that two arguments were passed from the command line.
     */
    if (args.length != 2) {
      System.out.printf("Usage: TwitterDriver <input dir> <output dir>\n");
      System.exit(-1);
    }

    Configuration conf = new Configuration();
    try {
		//Path pt = new Path("hdfs:/twittertempfile");
		FileSystem fs = FileSystem.get(new Configuration());
		//fs.delete(pt, true);
		fs.delete(new Path("hdfs:/twittertempdir/"), true);
    } catch(Exception e) {
    	
    }
    // The default of 60 seconds causes too many tasks to time out, which causes the job to fail. This should allow 10 minutes.
    conf.setLong("mapred.task.timeout", 60000);
    Job job = new Job(conf);
    
    job.setJarByClass(TwitterDriver.class);
   
    job.setJobName("Twitter Data Analysis");

    FileInputFormat.setInputPaths(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    
    job.setMapperClass(TwitterMapper.class);
    job.setReducerClass(TwitterReducer.class);

    job.setMapOutputKeyClass(Text.class);
    job.setMapOutputValueClass(IntWritable.class);
    
    job.setOutputKeyClass(DoubleWritable.class);
    job.setOutputValueClass(Text.class);
    
    /*
     * Start the MapReduce job and wait for it to finish.
     * If it finishes successfully, return 0. If not, return 1.
     */
    boolean success = job.waitForCompletion(true);

    System.exit(success ? 0 : 1);
  }
}

