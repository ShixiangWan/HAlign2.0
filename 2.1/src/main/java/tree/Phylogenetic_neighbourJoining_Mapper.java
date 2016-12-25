package tree;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;


public class Phylogenetic_neighbourJoining_Mapper extends Mapper<Object, Text, LongWritable, Text> {//这里map是把 ID seqence_name sequence 中的ID分离出来，key=ID , value =sequence_name seqence
    private LongWritable neighbourJoining_key = new LongWritable();//类型ID是int类型的
    private Text neighbourJoining_Value = new Text();//value是字符串类型的


    public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
        String[] list = value.toString().split("\t");// 把三个字段分离出来
        int tmp_key = Integer.parseInt(list[0]);//第一个字段设置为key
        neighbourJoining_key.set(tmp_key);//the key is the set number
        neighbourJoining_Value.set(list[1] + "\t" + list[2]);//后两个字段设置为value
        context.write(neighbourJoining_key, neighbourJoining_Value);//输出到reduce
    }
}
