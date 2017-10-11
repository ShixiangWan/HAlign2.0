package halign.center;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import halign.extreme.ExtremeMSA;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import utils.MSAFileUtils;

public class TreeMSA {
	private static String Pi[]; // 记录每一个序列
	private static String Piname[]; // 记录每一个序列的名字
    private int Spaceevery[][];   //存放中心序列分别与其他序列比对时增加的空格的位置
    private int Spaceother[][];   //存放其他序列与中心序列比对时增加的空格的位置
    private int r;                 //被划分的小段的长度
    private int n;                 //存放序列个数
    private int center;            //存放中心序列编号
    private int spacescore=-1, matchscore=0, mismatchscore=-1; //###########定义匹配，不匹配和空格的罚分###############
    private int Name[][][][];

    public void start(String inputfile, String outputfile, String outputDFS) 
			throws IOException, ClassNotFoundException, InterruptedException {
        n = countnum(inputfile);
        if (outputDFS == null) {
        	input(inputfile);
		} else {
			Pi = new String[n];
			Piname = new String[n];
			System.out.println(">>Clearing HDFS Path & uploading ...");
            MSAFileUtils MSAFileUtils = new MSAFileUtils();
            MSAFileUtils.clear_dfs_path(outputDFS);
			MSAFileUtils.local_to_dfs(inputfile, outputDFS + "/input/input.txt");
			
			System.out.println(">>Map reducing ...");
			Configuration conf = new Configuration();
			conf.set("mapred.task.timeout", "0");
            Job job = Job.getInstance(conf, "msa_tree");
			job.setJarByClass(ExtremeMSA.class);
			job.setInputFormatClass(TextInputFormat.class);
			job.setMapperClass(TreeMapper.class);
			job.setMapOutputKeyClass(NullWritable.class);
			job.setMapOutputValueClass(Text.class);
			FileInputFormat.addInputPath(job, new Path(outputDFS + "/input/input.txt"));
			FileOutputFormat.setOutputPath(job, new Path(outputDFS + "/output"));
			job.setNumReduceTasks(1);
			job.waitForCompletion(true);
		}
        
        //计算划分的序列长度，若第一条序列长度小于300则选择第一条序列的长度，否则选择该长度除10后的值，有待修改
        if (Pi[1].length() < 300) {
            r = Pi[1].length();
        }
        else {
            r = Pi[1].length() / 30;
        }
        center = findCenterSeq();
        //System.out.println("中心序列是第" + center + "条！");

        //多序列比对部分
        Spaceevery = new int[n][Pi[center].length() + 1]; //存放中心序列分别与其他序列比对时增加的空格的位置
        Spaceother = new int[n][computeMaxLength(center) + 1]; //存放其他序列与中心序列比对时增加的空格的位置
        for (int i = 0; i < n; i++) {
            if (i == center) continue;
            //比对Pi[i]与中心序列有重合的前端
            if (Name[center][i][0][0] != 0 && Name[center][i][1][0] != 0) {
                //此处有漏洞，如果Name[center][i][0][0]=0，会报错
                String strC = Pi[center].substring(0, (Name[center][i][0][0] - 1) * r);
                String stri = Pi[i].substring(0, Name[center][i][1][0] - 1);
                int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);//动态规划矩阵计算完毕
                traceBackForDynamicProgram(M, stri.length(), strC.length(), i, 0, 0);//回溯，更改空格数组
            }
            for (int j = 1; j < Name[center][i][0].length - 1; j++) {
                //比对Pi[i]与中心序列有重合的中间部分
                if (Name[center][i][0][j] != 0 && Name[center][i][1][j] >= Name[center][i][1][j - 1] + r
                        && Name[center][i][0][j] - 1 >= Name[center][i][0][j - 1]) {
                    //此处有漏洞，如果Name[center][i][0][0]=0，会报错
                    String strC = Pi[center].substring(Name[center][i][0][j - 1] * r, (Name[center][i][0][j] - 1) * r);
                    String stri = Pi[i].substring(Name[center][i][1][j - 1] + r - 1, Name[center][i][1][j] - 1);
                    int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);//动态规划矩阵计算完毕
                    traceBackForDynamicProgram(M, stri.length(), strC.length(), i,
                            Name[center][i][1][j - 1] + r - 1, Name[center][i][0][j - 1] * r);
                }
            }
            //比对Pi[i]与中心序列有重合的后端
            int j = Name[center][i][0].length - 1;
            if (j != 0) {
                String strC = Pi[center].substring(r * Name[center][i][0][j - 1]);
                String stri = Pi[i].substring(Name[center][i][1][j - 1] - 1 + r);
                int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);//动态规划矩阵计算完毕
                traceBackForDynamicProgram(M, stri.length(), strC.length(), i, Name[center][i][1][j - 1] + r - 1,
                        Name[center][i][0][j - 1] * r);
            } else {
                String strC = Pi[center];
                String stri = Pi[i];
                int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);//动态规划矩阵计算完毕
                traceBackForDynamicProgram(M, stri.length(), strC.length(), i, 0, 0);
            }
        }
        //Space数组长度是中心序列长度+1，元素存入该元素的max spaceevery元素值(就是中心序列应该插入的元素值)
        int Space[] = combine();
        //Space
        output(Space, outputfile);
    }

    //将序列一次读入数组中
    private void input(String inputfile) {
        Pi = new String[n];
        Piname = new String[n];
        int i = 0;
        try {
            BufferedReader br = new BufferedReader( new FileReader(inputfile));
            String BR = br.readLine();
            while (br.ready()) {
                if (BR.length() != 0 && BR.charAt(0) == '>') {
                    Piname[i] = BR;
                    Pi[i] = "";
                    while (br.ready() && (BR = br.readLine()).charAt(0) != '>') {
                        Pi[i] += BR;
                    }
                    i++;
                } else
                    BR = br.readLine();
            }
            n = i;
            br.close();
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            System.exit(0);
        }
    }

    // 建立关键字树
    private NewRoot buildkeywordtree(int i, int r, BuildTree buildTree) {
        NewRoot trie = new NewRoot(); //创建一个新关键字树（树根）
        trie.ID = 0; //树根的ID为0

        for (int j = 0; j < Pi[i].length() / r; j++) { //建树
            // 对于每一个长度为r的划分子串
            buildTree.build(Pi[i], j * r, r, j + 1, trie); //根据这个子串建树
        }
        buildTree.failLink(trie); //建立整个树的失效链接
        return (trie);
    }

    //找出Num[][]数组中和最大的那一行
    private int findNumMax(int Num[][]) {
        int Numsum[] = new int[n];
        for (int i = 0; i < n; i++) {
            Numsum[i] = 0;
            for (int j = 0; j < n; j++)
                Numsum[i] = Numsum[i] + Num[i][j];
        }
        int tmpcenter = 0;
        for (int i = 1; i < n; i++) {
            if (Numsum[i] > Numsum[tmpcenter])
                tmpcenter = i;
        }
        return (tmpcenter);
    }

    //找出中心序列
    private int findCenterSeq() {
        int Num[][] = new int[n][n];   //用来记录出现的个数
        Name = new int[n][n][2][];  //用来存放出现的名字
        BuildTree buildTree= new BuildTree();
        for (int i = 0; i < n; i++)         //初始化
            Num[i][i] = 0;
        for (int i = 0; i < n; i++) {
            NewRoot keywordtreefori = buildkeywordtree(i, r, buildTree);
            for (int j = 0; j < n; j++) {
                if (j == i)
                    continue;
                buildTree.ACsearch(Pi[j], keywordtreefori);
                Name[i][j] = buildTree.out();
                Num[i][j] = Name[i][j][0].length - 1;
            }
        }
        return (findNumMax(Num));
    }

    //在动态规划中计算矩阵积分
    private int[][] computeScoreMatrixForDynamicProgram(String stri, String strC) {
        int len1 = stri.length() + 1;
        int len2 = strC.length() + 1;
        int M[][] = new int[len1][len2];   //定义动态规划矩阵
        //---初始化动态规划矩阵-----------
        int p, q;
        for (p = 0; p < len1; p++)
            M[p][0] = spacescore * p;
        for (q = 0; q < len2; q++)
            M[0][q] = spacescore * q;
        //---初始化结束----------
        //----计算矩阵的值------------
        for (p = 1; p < len1; p++) {
            for (q = 1; q < len2; q++) {//M[p][q]=max(M[p-1][q]-1,M[p][q-1]-1,M[p-1][q-1]+h)
                int h;
                if (stri.charAt(p - 1) == strC.charAt(q - 1)) {
                    h = matchscore;
                }
                else h = mismatchscore;
                M[p][q] = Math.max(M[p - 1][q - 1] + h, Math.max(M[p - 1][q] + spacescore, M[p][q - 1] + spacescore));
            }
        }
        /*for (int i=0; i<len1; i++) {
            for (int j=0; j<len2; j++) {
                System.out.print(M[i][j]+"\t");
            }
            System.out.println();
        }*/
        return (M);
    }

    //在动态规划中回溯
    private void traceBackForDynamicProgram(int[][] M, int p, int q, int i, int k1, int k2) {
        while (p > 0 && q > 0) {
            if (M[p][q] == M[p][q - 1] + spacescore) {
                Spaceother[i][p + k1]++;
                q--;
            } else if (M[p][q] == M[p - 1][q] + spacescore) {
                Spaceevery[i][q + k2]++;
                p--;
            } else {
                p--;
                q--;
            }
        }
        if (p == 0) {
            while (q > 0) {
                Spaceother[i][k1]++;
                q--;
            }
        }
        if (q == 0) {
            while (p > 0) {
                Spaceevery[i][k2]++;
                p--;
            }
        }
        /*for (int k=0; k<n; k++) {
            for (int j=0; j<Pi[center].length() + 1; j++) {
                System.out.print(Spaceevery[i][j]+"\t");
            }
            System.out.println();
        }
        System.out.println("=============================");
        for (int k=0; k<n; k++) {
            for (int j=0; j<computeMaxLength(center) + 1; j++) {
                System.out.print(Spaceother[i][j]+"\t");
            }
            System.out.println();
        }*/
    }

    private int[] combine() {
        int Space[] = new int[Pi[center].length() + 1];//该数组用来记录在P[center]的最终结果各个空隙间插入空格的个数
        int i, j;
        for (i = 0; i < Pi[center].length() + 1; i++) {
            int max = 0;
            for (j = 0; j < n; j++) {
                if (Spaceevery[j][i] > max) {
                    max = Spaceevery[j][i];
                }
            }
            Space[i] = max;
        }
        return (Space);
    }
    
	// 计算序列个数
	public int countnum(String filepath) {
		int num = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filepath));
			String s;
			while (br.ready()) {
				s = br.readLine();
				if (s.charAt(0) == '>')
					num++;
			}
			br.close();
		} catch (Exception ex) {
		    ex.getStackTrace();
        }
		return (num);
	}
	
    //计算除中心序列以外的其他序列的最大长度
    private int computeMaxLength(int center) {
        int maxlength = 0;
        for (int i = 0; i < n; i++) {
            if (i == center)
                continue;
            if (Pi[i].length() > maxlength)
                maxlength = Pi[i].length();
        }
        return (maxlength);
    }

    private void output(int[] Space, String outputfile) {
        int i, j;
        //---------输出中心序列----------
        String PiAlign[] = new String[n];
        PiAlign[center] = "";
        for (i = 0; i < Pi[center].length(); i++) {
            for (j = 0; j < Space[i]; j++) {
                PiAlign[center] = PiAlign[center].concat("-");
            }
            PiAlign[center] = PiAlign[center].concat(Pi[center].substring(i, i + 1));
        }
        for (j = 0; j < Space[Pi[center].length()]; j++) {
            PiAlign[center] = PiAlign[center].concat("-");
        }
        //--------中心序列输出完毕-----
        //---------输出其他序列-------
        for (i = 0; i < n; i++) {
            if (i == center)
                continue;
            //----计算和中心序列比对后的P[i],记为Pi-----
            PiAlign[i] = "";
            for (j = 0; j < Pi[i].length(); j++) {
                String kong = "";
                for (int k = 0; k < Spaceother[i][j]; k++) {
                    kong = kong.concat("-");
                }
                PiAlign[i] = PiAlign[i].concat(kong).concat(Pi[i].substring(j, j + 1));
            }
            String kong = "";
            for (j = 0; j < Spaceother[i][Pi[i].length()]; j++) {
                kong = kong.concat("-");
            }
            PiAlign[i] = PiAlign[i].concat(kong);
            //---Pi计算结束---------
            //----计算差异数组----
            int Cha[] = new int[Pi[center].length() + 1];
            int position = 0;    //用来记录插入差异空格的位置
            for (j = 0; j < Pi[center].length() + 1; j++) {
                Cha[j] = 0;
                if (Space[j] - Spaceevery[i][j] > 0) {
                    Cha[j] = Space[j] - Spaceevery[i][j];
                }
                //----差异数组计算完毕---
                //----填入差异空格----
                position = position + Spaceevery[i][j];
                if (Cha[j] > 0) {  //在位置position处插入Cha[j]个空格
                    kong = "";
                    for (int k = 0; k < Cha[j]; k++) {
                        kong = kong.concat("-");
                    }
                    PiAlign[i] = PiAlign[i].substring(0, position).concat(kong).concat(PiAlign[i].substring(position));
                }
                position = position + Cha[j] + 1;
                //----差异空格填入完毕--
            }
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
            for (i = 0; i < n; i++) {
                bw.write(Piname[i]);
                bw.newLine();
                bw.flush();
                bw.write(PiAlign[i]);
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (Exception ignored) {

        }
        //---------其他序列输出完毕-----
        //---------输出结束--------------
    }
    
	public static class TreeMapper extends Mapper<Object, Text, NullWritable, Text> {
		int count = 0;
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException{
			if (value.charAt(0) == '>') {
				Piname[count] = value.toString();
			} else {
				Pi[count] = value.toString();
				count++;
			}
			context.write(NullWritable.get(), value);
		}
	}
}
