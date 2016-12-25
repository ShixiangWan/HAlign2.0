package tree;

import halign.utils.FileUtils;
import halign.utils.FormatUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.File;

public class MSATree {
    public static void main(String[] args) throws Exception {
        String local_path = "/home/shixiang/";
        String filename = "genome-out.fasta";
        String dfs_path = "msa/";
        String reduce_number = "3";
        new MSATree().GenerateTree(local_path, filename, dfs_path, reduce_number);
    }

    public void GenerateTree(String local_path, String filename, String dfs_path, String reduce_number) throws Exception {
        FileUtils utils = new FileUtils();
        utils.clear_local_path(new File(local_path + "NeighbourJoining_subTree_OutPut"));
        utils.clear_dfs_path(dfs_path);

        System.out.println(">> (Spark mode for tree) loading data ...");
        long startTime = System.currentTimeMillis();
        FormatUtils formatUtils = new FormatUtils();
        formatUtils.formatKVFasta(local_path + filename, local_path + "inputKV");
        int allNum = formatUtils.getAllNum();
        System.out.println("total number: " + allNum);
        System.out.println((System.currentTimeMillis() - startTime) + "ms");

        System.out.println(">>clustering process ...");
        Pre_Cluster_process cluster = new Pre_Cluster_process(local_path + "inputKV", reduce_number);
        cluster.Get_Cluster(local_path + "single_cluster_output", allNum);
        int cluster_number = cluster.get_cluster_number();

        utils.local_to_dfs(local_path+"single_cluster_output", dfs_path+"single_cluster_output");
        utils.local_to_dfs(local_path+"inputKV", dfs_path+"inputKV");

        Configuration configuration = new Configuration();
        configuration.set("inputFile", dfs_path + "single_cluster_output"); //上传文件路径
        configuration.set("mapred.task.timeout", "0");
        Job job = Job.getInstance(configuration, "clustering");
        job.setJarByClass(MSATree.class);
        FileInputFormat.addInputPath(job, new Path(dfs_path + "inputKV"));//mapreduce的输入目录位置
        FileOutputFormat.setOutputPath(job, new Path(dfs_path + "Cluster_OutPut/"));//设置输出文件的位置
        job.setInputFormatClass(TextInputFormat.class);
        job.setMapperClass(Phylogenetic_Cluster_Mapper.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);//设置reduce的key的输出格式
        job.setOutputValueClass(Text.class);//设置reduce的value的输出格式
        job.setNumReduceTasks(1);//设置reduce的个数
        job.waitForCompletion(true);

        /*local_path: Cluster_OutPut -> OnBalance_OutPut*/
        utils.dfs_to_local(dfs_path + "Cluster_OutPut/part-r-00000", local_path + "Cluster_OutPut");
        OnBalance onbalance = new OnBalance(local_path + "Cluster_OutPut");
        onbalance.Balance(cluster_number, local_path);

        //new File(local_path + "/inputKV").delete();
        //new File(inputfile);.delete();
        System.out.println(">> construct phylogenetic tree ...");
        Configuration configuration1 = new Configuration();
        configuration1.set("mapred.task.timeout", "0");
        Job job1 = Job.getInstance(configuration1, "neighbourJoining");
        job1.setJarByClass(MSATree.class);

        utils.local_to_dfs(local_path + "OnBalance_OutPut", dfs_path + "neighbourJoining");

        FileInputFormat.addInputPath(job1, new Path(dfs_path + "neighbourJoining"));//mapreduce的输入目录位置
        FileOutputFormat.setOutputPath(job1, new Path(dfs_path + "neighbourJoining_OutPut"));//设置输出文件的位置
        job1.setInputFormatClass(TextInputFormat.class);
        job1.setMapperClass(Phylogenetic_neighbourJoining_Mapper.class);
        job1.setMapOutputKeyClass(LongWritable.class);
        job1.setMapOutputValueClass(Text.class);
        job1.setOutputKeyClass(Text.class);
        job1.setOutputValueClass(Text.class);
        job1.setNumReduceTasks(Integer.parseInt(reduce_number));
        job1.setReducerClass(Phylogenetic_neighbourJoining_Reducer.class);
        job1.waitForCompletion(true);


        utils.dfs_to_local(dfs_path + "neighbourJoining_OutPut", local_path + "NeighbourJoining_subTree_OutPut");
        NeighbourJoining_Summary nj_summary = new NeighbourJoining_Summary(
                local_path + "NeighbourJoining_subTree_OutPut", local_path + "tree.tre");
        nj_summary.Merge();//合并文件夹下的所有reduce输出结果
        nj_summary.Summary();//子树汇总

        System.out.println(">>Success! Time cost:" + (System.currentTimeMillis() - startTime) / 1000 + "s");
    }
}
