package com.msa.protein;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import com.msa.utils.ClearDfsPath;
import com.msa.utils.CopyFile;

import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParserException;

public class ProteinMSA {
    static ArrayList<String> s_key = new ArrayList<>();
    static ArrayList<String> s_val = new ArrayList<>();
    
	public void start(String inputfile, String outputfile, String outputDFS) throws SequenceParserException, 
    						MatrixLoaderException, IOException, ClassNotFoundException, InterruptedException {
        if (outputDFS == null) {
        	String line;
            try {
                BufferedReader br = new BufferedReader(new FileReader(inputfile));
                while (br.ready()) {
                    line = br.readLine();
                    if (line.charAt(0) == '>') {
                        s_key.add(line);
                    } else {
                        s_val.add(line);
                    }
                }
                br.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        	System.out.println(">>Clearing HDFS Path & uploading ...");
    		new ClearDfsPath().run(outputDFS);
    		CopyFile copyFile = new CopyFile();
    		copyFile.local_to_dfs(inputfile, outputDFS + "/input/input.txt");
    		
    		System.out.println(">>Map reducing ...");
    		Configuration conf = new Configuration();
    		conf.set("mapred.task.timeout", "0");
    		Job job = new Job(conf, "msa_protein");
    		job.setJarByClass(ProteinMSA.class);
    		job.setInputFormatClass(TextInputFormat.class);
    		job.setMapperClass(ProteinMapper.class);
    		job.setMapOutputKeyClass(NullWritable.class);
    		job.setMapOutputValueClass(Text.class);
    		FileInputFormat.addInputPath(job, new Path(outputDFS + "/input/input.txt"));
    		FileOutputFormat.setOutputPath(job, new Path(outputDFS + "/output"));
    		job.setNumReduceTasks(1);
    		job.waitForCompletion(true);
        }

        //将第一个序列作为根，与其他每个序列进行双序列比对，保留第一个序列的比对结果s_out1
        ArrayList<String> s_out1 = new ArrayList<>();
        ArrayList<String> s_out2 = new ArrayList<>();
        String sequence1 = s_val.get(0);
        int sequenceLen1 = sequence1.length();
        TwoAlign centerAlign = new TwoAlign();
        for (int i=1; i<s_val.size(); i++) {
            centerAlign.align(sequence1, s_val.get(i));
            s_out1.add(new String(TwoAlign.alignResultOne));
            s_out2.add(new String(TwoAlign.aligntResultTwo));
        }

        //统计第一个序列的比对结果，得到它的归总比对结果insertSpace1[]
        int index;
        int insertSpace1[] = new int[sequenceLen1 + 1];
        for (String line2 : s_out1) {
            int tempSpace1[] = new int[sequenceLen1 + 1];
            index = 0;
            for (int j=0; j < line2.length(); j++) {
                if (line2.charAt(j) == '-') {
                    tempSpace1[index]++;
                } else {
                    if (insertSpace1[index] < tempSpace1[index]) {
                        insertSpace1[index]=tempSpace1[index];
                    }
                    index++;
                }
            }
        }

        //以第一条序列为中心序列，计算中心序列sequence1
        StringBuilder stringBuilder = new StringBuilder();
        int insertSpaceLen1 = insertSpace1.length;
        for(int i=0; i<insertSpaceLen1; i++){
            for(int j=0; j<insertSpace1[i]; j++) {
                stringBuilder.append('-');
            }
            if(i != insertSpaceLen1-1) {
                stringBuilder.append(sequence1.charAt(i));
            }
        }
        sequence1 = stringBuilder.toString();

        //将归纳得到的sequence1再次与第一次比对结果比对，得到最终各序列比对结果存入文件
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
            for (int i=0; i<s_key.size(); i++) {
                bw.write(s_key.get(i));
                bw.newLine();
                if (i == 0) {
                    bw.write(sequence1);
                } else {
                    centerAlign.align(sequence1, s_out2.get(i-1));
                    bw.write(new String(TwoAlign.aligntResultTwo));
                }
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
	public static class ProteinMapper extends Mapper<Object, Text, NullWritable, Text> {
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException{
			if (value.charAt(0) == '>') {
				s_key.add(value.toString());
			} else {
				s_val.add(value.toString());
			}
			context.write(NullWritable.get(), value);
		}
	}
}
