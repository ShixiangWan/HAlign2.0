package halign.kband;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import utils.HDFSUtils;
import utils.IOUtils;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author ShixiangWan
 * */
public class KbandMSA {

    static List<String> s_key = new ArrayList<>();
    static List<String> s_val = new ArrayList<>();
    private static List<String> s_out1 = new ArrayList<>();
    private static List<String> s_out2 = new ArrayList<>();

    /**
     * @param inputFile input file "path+name", fasta format.
     * @param outputFile output file "path+name", fasta format.
     * */
    public void start(String inputFile, String outputFile, String outputDFS)
            throws IOException, ClassNotFoundException, InterruptedException {
        System.out.println("Loading data ... " + inputFile);
        long startTime = System.currentTimeMillis();

        if (outputDFS == null) {
            IOUtils formatUtils = new IOUtils();
            formatUtils.readFasta(inputFile, true);
            s_key = formatUtils.getS_key();
            s_val = formatUtils.getS_val();
        } else {
            System.out.println(">>Clearing HDFS Path & uploading ...");
            HDFSUtils MSAFileUtils = new HDFSUtils();
            MSAFileUtils.clear_dfs_path(outputDFS);
            MSAFileUtils.local_to_dfs(inputFile, outputDFS + "/input/input.txt");

            System.out.println(">>Map reducing ...");
            Configuration conf = new Configuration();
            conf.set("mapred.task.timeout", "0");
            Job job = Job.getInstance(conf, "msa_kband");
            job.setJarByClass(KbandMSA.class);
            job.setInputFormatClass(TextInputFormat.class);
            job.setMapperClass(KbandMSA.KbandMapper.class);
            job.setMapOutputKeyClass(NullWritable.class);
            job.setMapOutputValueClass(Text.class);
            FileInputFormat.addInputPath(job, new Path(outputDFS + "/input/input.txt"));
            FileOutputFormat.setOutputPath(job, new Path(outputDFS + "/output"));
            job.setNumReduceTasks(1);
            job.waitForCompletion(true);
        }

        //The first sequence is used as the centerstar sequence.
        String sequence1 = s_val.get(0);
        int sequenceLen1 = sequence1.length();
        int sequence1_id = 0;

        // The "sequence1" is used as the "root", which compared with other sequences.
        // The first alignment is preserved as "s_out1".
        int total_num = s_val.size();
        for (String ignored : s_val) {
            s_out1.add("");
            s_out2.add("");
        }
        System.out.println("MultiThread MSA ... ");
        int taskSize = 16;
        //ExecutorService pool = Executors.newFixedThreadPool(taskSize);
        for (int i = 0; i < total_num; i++) {
            //String line = s_val.get(i);
            //AlignThread alignThread = new AlignThread(i, sequence1, line);
            KbandAlignTwo kbandAlignTwo = new KbandAlignTwo();
            kbandAlignTwo.align(sequence1, s_val.get(i));
            s_out1.add(new String(KbandMake.aligns));
            s_out2.add(new String(KbandMake.alignt));
            //pool.execute(new Thread(alignThread));
        }
        //pool.shutdown();
        //while(!pool.isTerminated());
        System.out.println("Aligned, " + (System.currentTimeMillis() - startTime) + "ms");

        // Statistic the alignments with centerstar sequence, and get the merge results:
        // - centerSpaces: record the spaces in each alignment.
        // - oneSpace[]: record the spaces in centerstar alignment.
        int index;
        int oneSpaceLen = sequenceLen1 + 1;
        int oneSpace[] = new int[oneSpaceLen];
        int centerSpaces[][] = new int[total_num][oneSpaceLen];
        for (int i = 0; i < total_num; i++) {
            String line = s_out1.get(i);
            index = 0;
            for (int j = 0; j < line.length(); j++) {
                if (line.charAt(j) == '-') {
                    centerSpaces[i][index]++;
                } else {
                    index++;
                }
                // bug fixed.
                if (oneSpace[index] < centerSpaces[i][index]) {
                    oneSpace[index] = centerSpaces[i][index];
                }
            }
        }

        // Get the centerstar alignment "sequence1".
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < oneSpaceLen; i++) {
            for (int j = 0; j < oneSpace[i]; j++) stringBuilder.append('-');
            if (i != oneSpaceLen - 1) stringBuilder.append(sequence1.charAt(i));
        }
        sequence1 = stringBuilder.toString();

        // Merge array based on "centerSpace", "oneSpace" and "s_out2",
        // and align all sequences.
        System.out.println("Merging sequences ... ");
        for (int i = 0; i < total_num; i++) {
            String line = s_out2.get(i);
            // Record the position of inserted spaces.
            int position = 0;
            for (int j = 0; j < oneSpaceLen; j++) {
                int gap = oneSpace[j] - centerSpaces[i][j];
                // bug fixed.
                //position += centerSpaces[i][j];
                if (gap > 0) {
                    if (position < line.length()) {
                        // Insert spaces.
                        line = line.substring(0, position).concat(insert(gap)).concat(line.substring(position));
                    } else {
                        // For some special sequences out of limited length, which need to
                        // be cut off and broken off insertion.
                        if (line.length() > sequence1.length()) {
                            line = line.substring(0, sequence1.length());
                        }
                        break;
                    }
                }
                position += gap + 1;
            }

            // Some special sequences is not equal to others after inserting spaces,
            // which need to add some extra spaces.
            if (line.length() < sequence1.length()) {
                line = line.concat(insert(sequence1.length() - line.length()));
            }
            s_out2.set(i, line);
        }

        // delete inconsistent results.
        List<Integer> deleteIndex = new IOUtils().deleteSimilarityOutput(s_key, s_val, s_key, s_out2);
        if (deleteIndex.size() > 0)
            System.out.println("There are "+deleteIndex.size()+" low similarity sequences (deleted):");
        for (int deleted : deleteIndex) {
            System.out.println("deleted sequences: "+s_key.get(deleted));
            s_key.set(deleted, "");
        }

        // Save alignment to hard disk.
        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
            for (int i = 0; i < s_key.size(); i++) {
                if (s_key.get(i).equals("")) continue;
                bufferedWriter.write(s_key.get(i)+ "\n");
                bufferedWriter.write(s_out2.get(i)+ "\n");
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Saved, " + (System.currentTimeMillis() - startTime) + "ms");
    }


    private class AlignThread implements Runnable {
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
            KbandAlignTwo kbandAlignTwo = new KbandAlignTwo();
            kbandAlignTwo.align(sequence1, sequence2);
            s_out1.set(id, new String(KbandMake.aligns));
            s_out2.set(id, new String(KbandMake.alignt));
        }
    }


    private String insert(int num) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int k = 0; k < num; k++) {
            stringBuilder.append('-');
        }
        return stringBuilder.toString();
    }

    public static class KbandMapper extends Mapper<Object, Text, NullWritable, Text> {
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException{
            if (value.charAt(0) == '>') {
//                System.out.println("KEY: "+value.toString());
                s_key.add(value.toString());
            } else {
//                System.out.println("VAL: "+value.toString());
                s_val.add(value.toString());
            }
            context.write(NullWritable.get(), value);
        }
    }
}
