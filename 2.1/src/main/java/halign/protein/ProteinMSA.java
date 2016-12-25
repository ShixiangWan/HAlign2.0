package halign.protein;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import halign.utils.FileUtils;

import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParserException;

public class ProteinMSA {
    static ArrayList<String> s_key = new ArrayList<>();
    static ArrayList<String> s_val = new ArrayList<>();
    static ArrayList<String> s_out1 = new ArrayList<>();
    static ArrayList<String> s_out2 = new ArrayList<>();

    public static void main(String[] args) throws ClassNotFoundException, SequenceParserException, InterruptedException, MatrixLoaderException, IOException {
        new ProteinMSA().start("/home/shixiang/protein.fasta", "/home/shixiang/out.txt", "hdfs://localhost:9000/msa");
    }

	public void start(String inputfile, String outputfile, String outputDFS) throws SequenceParserException, 
    						MatrixLoaderException, IOException, ClassNotFoundException, InterruptedException {
        System.out.println(">>Loading data ... ");
        if (outputDFS == null) {
        	String line;
            try {
                BufferedReader brReader = new BufferedReader(new FileReader(inputfile));

                StringBuilder stringBuilder = new StringBuilder();
                while(brReader.ready()) {
                    line = brReader.readLine();
                    if (line.equals("")) continue;
                    if (line.charAt(0) == '>') {
                        s_key.add(line);
                        if (stringBuilder.length() != 0) {
                            s_val.add(stringBuilder.toString());
                            stringBuilder.setLength(0);
                        }
                    } else {
                        stringBuilder.append(line);
                    }
                }
                if (stringBuilder.length() != 0) {
                    s_val.add(stringBuilder.toString());
                }
                brReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
        	System.out.println(">>Clearing HDFS Path & uploading ...");
            FileUtils fileUtils = new FileUtils();
            fileUtils.clear_dfs_path(outputDFS);
    		fileUtils.local_to_dfs(inputfile, outputDFS + "/input/input.txt");
    		
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

        /*将第一条序列作为中心序列*/
        String sequence1 = s_val.get(0);
        int sequenceLen1 = sequence1.length();
        int sequence1_id = 0;

        /*将sequence1序列作为根，与其他每个序列进行双序列比对，保留第一个序列的比对结果s_out1*/
        int total_num = s_val.size(); // 序列总个数
        /*初始化s_out1和s_out2*/
        for (String line : s_val) {
            s_out1.add("");
            s_out2.add("");
        }
        // 创建一个线程池
        System.out.println(">>MultiThread MSA ... ");
        int taskSize = 8;
        ExecutorService pool = Executors.newFixedThreadPool(taskSize);
        for (int i=0; i<total_num; i++) {
            String line = s_val.get(i);
            AlignThread alignThread = new AlignThread(i, sequence1, line);
            pool.execute(new Thread(alignThread));
        }
        pool.shutdown();
        while (!pool.isTerminated());

        /*统计中心序列的比对结果，得到它的归总比对结果centerSpaces,oneSpace[]*/
        int index;
        int oneSpaceLen = sequenceLen1 + 1;
        int oneSpace[] = new int[oneSpaceLen];
        int centerSpaces[][] = new int[total_num][oneSpaceLen];
        for (int i=0; i<total_num; i++) {
            String line = s_out1.get(i);
            index = 0;
            for (int j=0; j<line.length(); j++) {
                if (line.charAt(j) == '-') {
                    centerSpaces[i][index]++;
                } else {
                    if (oneSpace[index] < centerSpaces[i][index]) {
                        oneSpace[index] = centerSpaces[i][index];
                    }
                    index++;
                }
            }
        }
        /*以第一条序列为中心序列，计算中心序列sequence1*/
        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<oneSpaceLen; i++){
            for(int j=0; j<oneSpace[i]; j++) {
                stringBuilder.append('-');
            }
            if(i != oneSpaceLen-1) {
                stringBuilder.append(sequence1.charAt(i));
            }
        }
        sequence1 = stringBuilder.toString();
        /*清空list，降低空间复杂度*/
        s_val = null;
        s_out1 = null;

        System.out.println(">>Merging sequences ... ");
        /*根据centerSpace，oneSpace和s_out2合并差异数组，对齐所有序列*/
        for (int i=0; i<total_num; i++) {
            String line = s_out2.get(i);
            int position = 0; // 用来记录插入差异空格的位置
            for (int j=0; j<oneSpaceLen; j++) {
                int gap = oneSpace[j]-centerSpaces[i][j];
                position += centerSpaces[i][j];
                if (gap > 0) {
                    if (position < line.length()) {
                        /*正常情况下插入空格*/
                        line = line.substring(0, position).concat(insert(gap)).concat(line.substring(position));
                    } else {
                        /*对于有的序列，超出了比对范围，此时需要截取并中断空格插入*/
                        if (line.length() > sequence1.length()) {
                            line = line.substring(0, sequence1.length());
                        }
                        break;
                    }
                }
                position += gap + 1;
            }
            /*有的特殊序列插入空格后依旧不满足相等条件，在它们后面补空格符*/
            if (line.length() < sequence1.length()) {
                line = line.concat(insert(sequence1.length()-line.length()));
            }
            s_out2.set(i, line);
        }

        /*写入结果*/
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
            for (int i=0; i<s_key.size(); i++) {
                bw.write(s_key.get(i));
                bw.newLine();
                if (i == sequence1_id) {
                    bw.write(sequence1);
                } else {
                    bw.write(s_out2.get(i));
                }
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public class AlignThread implements Runnable {
        private int id;
        private String sequence1;
        private String sequence2;

        AlignThread(int id, String sequence1, String sequence2) {
            this.id = id;
            this.sequence1 = sequence1;
            this.sequence2 = sequence2;
        }

        @Override
        public void run() {
            ProteinAlignTwo centerAlign = new ProteinAlignTwo();
            ArrayList<String> alignResult = centerAlign.align(sequence1, sequence2);
            s_out1.set(id, alignResult.get(0));
            s_out2.set(id, alignResult.get(1));
        }
    }


    private String insert(int num) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int k=0; k<num; k++) {
            stringBuilder.append('-');
        }
        return stringBuilder.toString();
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
