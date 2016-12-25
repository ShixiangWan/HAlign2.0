package tree;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;


public class Phylogenetic_Cluster_Mapper extends Mapper<Object, Text, Text, Text> {//第一个map函数是用中心序列和每个快中数据进行两两比较
    private Text clustertKey = new Text();//这个变量用来保存key值
    private Text clusterValue = new Text();//这个变量用来保存value值
    private String filename = new String();

    class cluster_data {
        String label;
        String name;
        String sequence;
    }

    private Set<Set> sum_set = new HashSet<>();

    //用于中心序列保存在本地文件系统，如果在map的时候要用上,需要用上下文来进行传递
    public void setup(Context context) throws IOException {
        Configuration conf = context.getConfiguration();
        filename = conf.get("inputFile", "Empty");
        FileSystem fs = FileSystem.get(context.getConfiguration());
        InputStream inputStream = fs.open(new Path(filename));
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        int first_set_flag = 0;
        String pre_label = "";
        String cur_label;
        Set<cluster_data> pre_set = new HashSet<>();
        while (br.ready()) {
            String str[] = br.readLine().split("\t");
            cur_label = str[0];
            if (first_set_flag == 0) {  // add the first data
                first_set_flag = 1;
                pre_label = cur_label;
                cluster_data clusterdata = new cluster_data();
                clusterdata.label = str[0];
                clusterdata.name = str[1];
                clusterdata.sequence = str[2];
                pre_set.add(clusterdata);
                continue;
            }
            // if the current label no equal to the pre label add the set to sum-set
            if (!cur_label.equals(pre_label)) {
                sum_set.add(pre_set);
                pre_label = cur_label;
                pre_set = new HashSet<>();
                cluster_data clusterdata = new cluster_data();
                clusterdata.label = str[0];
                clusterdata.name = str[1];
                clusterdata.sequence = str[2];
                pre_set.add(clusterdata);
            } else {
                cluster_data clusterdata = new cluster_data();
                clusterdata.label = str[0];
                clusterdata.name = str[1];
                clusterdata.sequence = str[2];
                pre_set.add(clusterdata);
            }
        }// end while
        sum_set.add(pre_set);
        br.close();
    }

    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] value_temp = value.toString().split("\t");
        String name = value_temp[0];
        String sequence = value_temp[1];
        String label = "";
        double min_distance = 100000;
        for (Set<cluster_data> set : sum_set) {
            Iterator iter = set.iterator();
            cluster_data clusterdata = (cluster_data) iter.next(); // random select one sample to calculate the distance
            double p = JukeCantor(sequence, clusterdata.sequence);
            p = 1.0 - 0.75 * p;
            double distance = (double) (int) (-0.75 * Math.log(p) * 1000) / 1000;  //根据公式计算距离
            if (distance < min_distance) {
                min_distance = distance;
                label = clusterdata.label;
            }
        }
        clustertKey.set(label);
        clusterValue.set(name + "\t" + sequence);
        context.write(clustertKey, clusterValue);
    }


    /*input two sequences, return differ parts rate of them.
    * */
    public double JukeCantor(String gene1, String gene2) {
        int rate = 0;
        int i;
        double result;
        int length = gene1.length();
        if (gene1.length() > gene2.length()) {
            length = gene2.length();
        }
        for (i = 0; i < length; i++) {
            if (gene1.charAt(i) != gene2.charAt(i)) {
                rate++;
            }
        }
        result = (double) rate / (double) gene1.length();
        return result;
    }
}
