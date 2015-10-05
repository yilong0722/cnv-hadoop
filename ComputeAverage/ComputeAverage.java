import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.lang.Math;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.FloatWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

public class ComputeAverage extends Configured implements Tool{
    
    private static Logger theLogger = Logger.getLogger(ComputeAverage.class.getName());
    
    public static class Map extends Mapper<LongWritable, Text, IntWritable, FloatWritable> {
        
        String record;
        
        protected void map(LongWritable key, Text value, Mapper.Context context) throws IOException, InterruptedException {
            record = value.toString();
            String[] fields = record.split("\\s+");
            
            if(fields.length > 5){
                Integer s_id = new Integer(1);
                
                /**
                float coverage_ratio = Float.parseFloat(fields[4]);
                if((0.5 <= coverage_ratio) && (coverage_ratio <= 2.0)){
                    context.write(new IntWritable(s_id), new FloatWritable(coverage_ratio));
                }
                **/
                
                float coverage_ratio = Float.parseFloat(fields[4]);
                Float ratioLog2 = new Float(Math.log(coverage_ratio) / Math.log(2));
                Float lowerBound = new Float(Math.log(0.5) / Math.log(2));
                Float upperBound = new Float(Math.log(2.0) / Math.log(2));
                
                if((lowerBound <= ratioLog2) && (ratioLog2 <= upperBound)){
                    // theLogger.info(Float.toString(ratioLog2));
                    context.write(new IntWritable(s_id), new FloatWritable(ratioLog2));
                }
            }
        } // end of map method
    } // end of mapper class
    
    
    public static class Reduce extends Reducer<IntWritable, FloatWritable, IntWritable, FloatWritable> {
        
        protected void reduce(IntWritable key, Iterable<FloatWritable> values, Reducer<IntWritable, FloatWritable, IntWritable, FloatWritable>.Context context) throws IOException, InterruptedException {
            Integer s_id = key.get();
            Float sum = new Float(0);
            Integer cnt = 0;
            
            for (FloatWritable value:values) {
                sum = sum + value.get();
                cnt = cnt + 1;
            }
            
            Float avg_m = (float) (sum/cnt);
            context.write(new IntWritable(s_id), new FloatWritable(avg_m));
        }
    }
    
    @Override
    public int run(String[] args) throws Exception {
        Configuration conf = new Configuration();
        args = new GenericOptionsParser(conf, args).getRemainingArgs();
        String input = args[0];
        String output = args[1];
        
        Job job = new Job(conf, "Avg");
        job.setJarByClass(Map.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setMapperClass(Map.class);
        job.setMapOutputKeyClass(IntWritable.class);
        job.setMapOutputValueClass(FloatWritable.class);
        
        job.setReducerClass(Reduce.class);
        job.setOutputFormatClass(TextOutputFormat.class);
        job.setOutputKeyClass(IntWritable.class);
        job.setOutputValueClass(FloatWritable.class);
        
        FileInputFormat.setInputPaths(job, new Path(input));
        Path outPath = new Path(output);
        FileOutputFormat.setOutputPath(job, outPath);
        outPath.getFileSystem(conf).delete(outPath, true);
        
        job.waitForCompletion(true);
        return (job.waitForCompletion(true) ? 0 : 1);
    }
    
    public static void main(String[] args) throws Exception {
        int exitCode = ToolRunner.run(new ComputeAverage(), args);
        System.exit(exitCode);
    }
}
