import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import java.io.IOException;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

public class MapReduce {
  
    public static void main( String[] args ) throws Exception {
	Path covid = new Path(args[0]);
	Path stateCand = new Path(args[1]);
	Path totalVote = new Path(args[2]);
	String outputStateDir = "stateOutput";
	String covidOutputDir = "covidOutput";
	Path output = new Path(args[3]);
	Path stateTemp = new Path(outputStateDir);
	Path covidTemp = new Path(covidOutputDir);
	main(new Configuration(), covid, stateCand, totalVote, output, stateTemp, covidTemp);
    }

    public static void main(Configuration conf, Path covid, Path stateCand, Path totalVote, Path output, Path stateTemp, Path covidTemp) 
	throws Exception {
	
	covidJob(covid, covidTemp, new Configuration(conf));
	stateCandJob(stateCand, stateTemp, new Configuration(conf));
	finalJob(stateTemp, totalVote, covidTemp, output, new Configuration(conf));
    }

    protected static void covidJob(Path covid, Path output, Configuration conf) throws Exception {
        Job job = new Job(conf);
        job.setJarByClass(MapReduce.class);
        job.setJobName("MapReduce Step Covid");
        
        
        FileInputFormat.addInputPath(job, covid);
        job.setInputFormatClass(TextInputFormat.class);
        
        job.setMapperClass(CovidMapper.class);
        job.setReducerClass(StateReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(IntWritable.class);
        FileOutputFormat.setOutputPath(job, output);
    
        if (job.waitForCompletion(true)) return;
        else throw new Exception("Covid Job Failed");
    }


    protected static void stateCandJob(Path stateCand, Path output, Configuration conf) 
	throws Exception {
	Job job = new Job(conf);
	job.setJarByClass(MapReduce.class);
	job.setJobName("MapReduce Step Cand Job");
	
	
	FileInputFormat.addInputPath(job, stateCand);
    	job.setInputFormatClass(TextInputFormat.class);
	
	job.setMapperClass(MapLineToStateCandVotes.class);
	job.setReducerClass(ReduceCountyStateVotes.class);
	
	job.setOutputKeyClass(Text.class);
	job.setOutputValueClass(IntWritable.class);
	job.setOutputFormatClass(TextOutputFormat.class);

	job.setMapOutputKeyClass(Text.class);
	job.setMapOutputValueClass(IntWritable.class);
	FileOutputFormat.setOutputPath(job, output);
    
	if (job.waitForCompletion(true)) return;
	else throw new Exception("State Cand Job Failed");
    }

    protected static void finalJob(Path stateCandDir, Path totalVote, Path covidTemp, Path output, Configuration conf) 
        throws Exception {
        Job job = new Job(conf);
        job.setJarByClass(MapReduce.class);
        job.setJobName("MapReduce Step Final");

	job.setPartitionerClass(SecondarySort.SSPartitioner.class);
	job.setGroupingComparatorClass(SecondarySort.SSGroupComparator.class);
	job.setSortComparatorClass(SecondarySort.SSSortComparator.class);
        
        job.setInputFormatClass(TextInputFormat.class);
        
        job.setReducerClass(ReducePercentage.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setOutputFormatClass(TextOutputFormat.class);

	MultipleInputs.addInputPath(job, stateCandDir, TextInputFormat.class, MapStateToCandVote.class);
	MultipleInputs.addInputPath(job, totalVote, TextInputFormat.class, MapTotalVotes.class);
	MultipleInputs.addInputPath(job, covidTemp, TextInputFormat.class, MapStateToCases.class);

        job.setMapOutputKeyClass(TextTuple.class);
        job.setMapOutputValueClass(TextTuple.class);
        FileOutputFormat.setOutputPath(job, output);
    
        if (job.waitForCompletion(true)) return;
        else throw new Exception("Final Job Failed");
    }

    protected static void finalFirstJob(Path covidTemp, Path secondTemp, Path output, Configuration conf) 
        throws Exception {
        Job job = new Job(conf);
        job.setJarByClass(MapReduce.class);
        job.setJobName("MapReduce Final Step");

        job.setPartitionerClass(SecondarySort.SSPartitioner.class);
        job.setGroupingComparatorClass(SecondarySort.SSGroupComparator.class);
        job.setSortComparatorClass(SecondarySort.SSSortComparator.class);
        
        job.setInputFormatClass(TextInputFormat.class);
        
        job.setReducerClass(ReduceCovidAndVotes.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        MultipleInputs.addInputPath(job, covidTemp, TextInputFormat.class, MapStateToCases.class);
        MultipleInputs.addInputPath(job, secondTemp, TextInputFormat.class, MapStateToTabledVotes.class);
        
        job.setMapOutputKeyClass(TextTuple.class);
        job.setMapOutputValueClass(TextTuple.class);
        FileOutputFormat.setOutputPath(job, output);
    
        if (job.waitForCompletion(true)) return;
        else throw new Exception("Final Job Failed");
    }
}
