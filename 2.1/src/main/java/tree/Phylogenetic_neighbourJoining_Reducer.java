package tree;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;


public class Phylogenetic_neighbourJoining_Reducer extends Reducer<LongWritable,Text,Text,Text> {
   
	private Text Present_seq = new Text();//设置代表节点为key
    private Text sunSet = new Text();//设置子树为value
    
    public void reduce(LongWritable key , Iterable<Text> values, Context context) throws IOException ,InterruptedException{
    
    	ArrayList<String> seq_name = new ArrayList<String>();//数组用来保存子集合序列名
    	ArrayList<String> seq = new ArrayList<String>();//数组用来保存子集合序列
    	
    	for(Text value:values){
    		String [] line = value.toString().split("\t");
    		seq_name.add(line[0]);// 获取子集合序列名
    		seq.add(line[1]);// 获取子集合序列
    	} 
    	
    	String  sub_tree ;//result用来保存进化树构建返回的结果哦 
    	String  present_seq;
    	NeighbourJoining nj = new NeighbourJoining();//开启进化树构建算法
    	nj.construct( seq_name , seq );
    	sub_tree = nj.getSubTree();//获取结果 
    	present_seq = nj.getPresentNode();
        //System.out.println("the sub tree is OK");
        
    	Present_seq.set(present_seq);
    	sunSet.set(sub_tree);
    	context.write(Present_seq, sunSet);
    }
}
