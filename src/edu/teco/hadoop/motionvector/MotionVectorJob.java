package edu.teco.hadoop.motionvector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.common.CommandLineUtil;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.commandline.DefaultOptionCreator;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.ToolRunner;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.DenseMatrix;
import org.apache.mahout.math.MatrixSlice;
import org.apache.mahout.math.VectorWritable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Counts tokenized words from input and counts them. 
 */
public final class MotionVectorJob extends AbstractJob {

  enum Records {
     EMPTYTEXT,
     EMPTYLINE,
     STATEGUESS
  }
  /**
   * Inner mapper class.
   */
  public static class VectorMapper
          extends Mapper<LongWritable, Text, Text, VectorWritable> {
    
    /** {@inheritDoc} */
    @Override
    public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
      String line = value.toString();

      ArrayList<Double> out_values = new ArrayList<Double>();
      String state = null;
      String id = null;

      String[] entries = line.split(";");
      if(entries.length==0||(entries.length==1&&entries[0].trim().length()==0))
      {
        context.getCounter(Records.EMPTYTEXT).increment(1);
        return;
      }
      DenseMatrix in_values = new DenseMatrix(entries.length,9);
      HashMap<String,Integer> states = new HashMap<String,Integer>();
      int row = 0;
      for (String entry : entries) {
        // Entry format: ID,State,Timestamp,X,Y,Z,RotationRateX,RotationRateY,RotationRateZ,MotionSensorInterval,OrientationX,OrientationY,OrientationZ
        String[] parts = entry.split(",");
        if(parts.length!=13)
        {
          context.getCounter(Records.EMPTYLINE).increment(1);
          continue;
        }
        id = parts[0];
        if(states.containsKey(parts[1]))
        {
          states.put(parts[1],states.get(parts[1])+1);
        } else {
          states.put(parts[1],1);
        }
        double[] entry_values = new double[9];                
        for (int i = 3; i <= 12; i++) {
          if(i<9){
            entry_values[i-3] = Double.parseDouble(parts[i]);
          } else if(i==9)
          {
            continue;
          } else {
            entry_values[i-4] = Double.parseDouble(parts[i]);
          }          
        }
        in_values.set(row,entry_values);
        row++;

      }
      // Get the state with maximum occurences in data set
      int max = 0;
      for (Map.Entry<String,Integer> entry : states.entrySet()) {
        if(entry.getValue()>max)
        {
          max = entry.getValue();
          state = entry.getKey();
        }
      }
      if(states.size()>1)
      {
        context.getCounter(Records.STATEGUESS).increment(1);
        //Skip mixed records
        return;
      }
      // Calculate average of all three vector lengths
      out_values.add(calculateAverage(in_values,0,3));
      out_values.add(calculateAverage(in_values,3,3));
      out_values.add(calculateAverage(in_values,6,3));

      // Calculate variance
      out_values.add(calculateVariance(in_values,out_values.get(0),0,3));
      out_values.add(calculateVariance(in_values,out_values.get(1),3,3));
      out_values.add(calculateVariance(in_values,out_values.get(2),6,3));

      
      String vec_key = "/" + state + "/" + id;
      Vector out_vector = new RandomAccessSparseVector(out_values.size(), out_values.size());
      int i = 0;
      for(double out_val:out_values)
      {
        out_vector.set(i,out_val);
        i++;
      }
      log.error(state + " " + out_vector.toString());
      VectorWritable vectorWritable = new VectorWritable(out_vector);
      context.write(new Text(vec_key), vectorWritable);
    }

    protected double calculateAverage(DenseMatrix values, int offset, int length) {
        double retval = 0;
        for(MatrixSlice val : values)
        {
            Vector vec = val.vector().viewPart(offset,length);
            retval+= vec.norm(2);
        }        
        return retval/values.numRows();
    }

    protected double calculateVariance(DenseMatrix values, double average, int offset, int length) {
        double retval = 0;
        for(MatrixSlice val : values)
        {
            Vector vec = val.vector().viewPart(offset,length);
            retval+= (vec.norm(2) - average)*(vec.norm(2) - average);
        }        
        return retval/values.numRows();  
    }
  }

  private static final Logger log = LoggerFactory.getLogger(MotionVectorJob.class);

  public static void main(String[] args) throws Exception {
    ToolRunner.run(new MotionVectorJob(), args);
  }

  @Override
  public int run(String[] args) throws Exception {
    DefaultOptionBuilder obuilder = new DefaultOptionBuilder();
    ArgumentBuilder abuilder = new ArgumentBuilder();
    GroupBuilder gbuilder = new GroupBuilder();

    Option inputDirOpt = DefaultOptionCreator.inputOption().create();

    Option outputDirOpt = DefaultOptionCreator.outputOption().create();
    Option helpOpt = obuilder.withLongName("help").withDescription("Print out help").withShortName("h")
            .create();


    Group group = gbuilder.withName("Options").withOption(outputDirOpt).withOption(inputDirOpt)
            .create();
    try {
      Parser parser = new Parser();
      parser.setGroup(group);
      parser.setHelpOption(helpOpt);
      CommandLine cmdLine = parser.parse(args);

      if (cmdLine.hasOption(helpOpt)) {
        CommandLineUtil.printHelp(group);
        return -1;
      }

      Path input = new Path((String) cmdLine.getValue(inputDirOpt));
      Path output = new Path((String) cmdLine.getValue(outputDirOpt));

      Configuration conf = new Configuration(getConf());

       Job job = new Job(conf);
      job.setJobName("MotionVectorJob: input-folder: " + input + " output-folder: " + output);
      job.setJarByClass(MotionVectorJob.class);
      
      job.setOutputKeyClass(Text.class);
      job.setOutputValueClass(VectorWritable.class);
      FileInputFormat.setInputPaths(job, input);
      
      FileOutputFormat.setOutputPath(job, output);
      
      job.setMapperClass(VectorMapper.class);
      job.setReducerClass(Reducer.class);
      job.setOutputFormatClass(SequenceFileOutputFormat.class);

      HadoopUtil.delete(conf, output);
      
      boolean succeeded = job.waitForCompletion(true);      
    } catch (OptionException e) {
      log.error("Exception", e);
      CommandLineUtil.printHelp(group);
    }
    return 0;
  }
}