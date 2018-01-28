package halign.suffixtree;

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

import utils.HDFSUtils;

public class ExtremeMSA {
	private String filepath = ""; // 记录文件名称
	private static String Pi[]; // 记录每一个序列
	private static String Piname[]; // 记录每一个序列的名字
	private int Spacecenter[][]; // 存放中心序列分别与其他序列比对时增加的空格的位置
	private int Spaceother[][]; // 存放其他序列与中心序列比对时增加的空格的位置
	private int n; // 存放序列个数
	private int center; // 存放中心序列编号
	private int spacescore = -1;
	private int Name[][][][]; // 树搜索完，记录匹配的区域;四维数组，第一位是Pi[i],第二位是pi[j]，记录两个序列的标号，第3维是3，Name[i][j][0][k]记录的是Pi[i]中的起始位置

	public void start(String inputfile, String outputfile, String outputDFS) 
			throws IOException, ClassNotFoundException, InterruptedException {
        filepath = inputfile;
		n = countnum();
		if (outputDFS == null) {
			input();
		} else {
			Pi = new String[n];
			Piname = new String[n];
			System.out.println(">>Clearing HDFS Path & uploading ...");
			HDFSUtils MSAFileUtils = new HDFSUtils();
			MSAFileUtils.clear_dfs_path(outputDFS);
			MSAFileUtils.local_to_dfs(inputfile, outputDFS + "/input/input.txt");
			
			System.out.println(">>Map reducing ...");
			Configuration conf = new Configuration();
			conf.set("mapred.task.timeout", "0");
			Job job = Job.getInstance(conf, "msa_extreme");
			job.setJarByClass(ExtremeMSA.class);
			job.setInputFormatClass(TextInputFormat.class);
			job.setMapperClass(ExtremeMapper.class);
			job.setMapOutputKeyClass(NullWritable.class);
			job.setMapOutputValueClass(Text.class);
			FileInputFormat.addInputPath(job, new Path(outputDFS + "/input/input.txt"));
			FileOutputFormat.setOutputPath(job, new Path(outputDFS + "/output"));
			job.setNumReduceTasks(1);
			job.waitForCompletion(true);
		}
		
		center = centerAlignEachOne(); // center的值固定为0，即第一条序列
		Spacecenter = new int[n][Pi[center].length() + 1];// 存放中心序列分别与其他序列比对时增加的空格的位置
        Spaceother = new int[n][computeMaxLength(center) + 1];// 存放其他序列与中心序列比对时增加的空格的位置
        for (int i = 0; i < n; i++) {
            if (i == center)
                continue;
            prealign(i);
            for (int j = 1; j < Name[center][i][0].length; j++) {
                midalign(i, j);
            }
            postalign(i);
        }
        int Space[] = combine();
        output(Space, outputfile);
	}

    // 比对Pi[i]与中心序列有重合的前端
    private void prealign(int i) {
        String strC = Pi[center].substring(0, Name[center][i][0][0]);
        String stri = Pi[i].substring(0, Name[center][i][1][0]);
        int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);// 动态规划矩阵计算完毕
        traceBackForDynamicProgram(M, stri.length(), strC.length(), i, 0, 0);// 回溯，更改空格数组
    }

    // 比对Pi[i]与中心序列有重合的中间部分
    private void midalign(int i, int j) {
        int lamda = Math.max(Name[center][i][1][j - 1] + Name[center][i][2][j - 1] - Name[center][i][1][j],
                Name[center][i][0][j - 1] + Name[center][i][2][j - 1] - Name[center][i][0][j]);
        // lamda是为了防止前后两块完全匹配有覆盖，把覆盖部分删除
        if (lamda > 0) {
            Name[center][i][0][j] += lamda;
            Name[center][i][1][j] += lamda;
            Name[center][i][2][j] -= lamda;
        }
        if (Name[center][i][2][j] < 0)
            System.out.println("此处有错误！！！！");

        String strC = Pi[center].substring(Name[center][i][0][j - 1] + Name[center][i][2][j - 1],
                Name[center][i][0][j]);// 此处有漏洞，如果Name[centerstar][i][0][0]=0，会抱错

        String stri = Pi[i].substring(Name[center][i][1][j - 1] + Name[center][i][2][j - 1], Name[center][i][1][j]);

        int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);// 动态规划矩阵计算完毕
        traceBackForDynamicProgram(M, stri.length(), strC.length(), i,
                Name[center][i][1][j - 1] + Name[center][i][2][j - 1],
                Name[center][i][0][j - 1] + Name[center][i][2][j - 1]);

    }

    // 比对Pi[i]与中心序列有重合的后端
    private void postalign(int i) {
        int j = Name[center][i][0].length;
        if (j > 0) {
            int cstart = Name[center][i][0][j - 1] + Name[center][i][2][j - 1];
            if (cstart > Pi[center].length())
                cstart--;// 别忘了，建后缀树时加了个$
            int istart = Name[center][i][1][j - 1] + Name[center][i][2][j - 1];
            if (istart > Pi[i].length())
                istart--;

            String strC = Pi[center].substring(cstart);
            String stri = Pi[i].substring(Name[center][i][1][j - 1] + Name[center][i][2][j - 1]);
            int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);// 动态规划矩阵计算完毕
            traceBackForDynamicProgram(M, stri.length(), strC.length(), i,istart,cstart);
        } else {
            String strC = Pi[center];
            String stri = Pi[i];
            int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);// 动态规划矩阵计算完毕
            traceBackForDynamicProgram(M, stri.length(), strC.length(), i, 0, 0);
        }
    }

	// 计算序列个数
	public int countnum() {
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

	// 将序列一次读入数组中
	public void input() {
		Pi = new String[n];
		Piname = new String[n];
		int i = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filepath));

			String BR = br.readLine();
			while (br.ready()) {

				if (BR.length() != 0 && BR.charAt(0) == '>') {
					Piname[i] = BR;
					Pi[i] = "";
					while (br.ready() && (BR = br.readLine()).charAt(0) != '>') {
						Pi[i] += BR;
					}
					Pi[i] = format(Pi[i]);
					i++;
				} else
					BR = br.readLine();
			}

			br.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			System.exit(0);
		}
	}

	/**
	 * 对String格式化,删除非法字符(只保留agctn,其余字符全部替换成n),全部转换成小写,u全部换成t
	 */
    private String format(String s) {
        s = s.toLowerCase();
        s = s.replace('u', 't');
        StringBuffer sb = new StringBuffer(s);

        for (int i = 0; i < sb.length(); i++) {
            switch (sb.charAt(i)) {
                case 'a':
                    break;
                case 'c':
                    break;
                case 'g':
                    break;
                case 't':
                    break;
                case 'n':
                    break;
                default:
                    sb = sb.replace(i, i + 1, "n");
            }
        }
        return (sb.toString());
    }

	public int centerAlignEachOne() {
		Name = new int[n][n][3][]; // 用来存放出现的名字，Name[i][j][0][k]和Name[i][j][1][k]表示了Pi[i]序列的第Name[i][j][0][k]个片段出现在Pi[j]序列的Name[i][j][1][k]位置上
		SuffixTree st1 = new SuffixTree();
		st1.build(Pi[0] + "$");
		for (int j = 1; j < n; j++) {
			int index = 0;
			ArrayList<Integer> result = new ArrayList<>();
            while (index < Pi[j].length()) {
                //在Pi[j]序列中，从index位置开始的序列段与Pi[0]进行匹配，返回Pi[0]中从a[0]开始的长度为a[1]的序列索引
                int[] a = st1.selectPrefixForAlignment(Pi[j], index);
                if (a[1] > Math.abs(a[0] - index)) {
                    result.add(a[0]);
                    result.add(index);
                    result.add(a[1]);
                    index += a[1];
                } else if (a[1] > 0) {
                    index += a[1];
                }
				else {
                    index++;
                }
            }
			int[][] tmp = new int[3][result.size() / 3];
			int k = 0;
			while (k < result.size()) {
				tmp[0][k / 3] = result.get(k);
				k++;
				tmp[1][k / 3] = result.get(k);
				k++;
				tmp[2][k / 3] = result.get(k);
				k++;
			}
			Name[0][j] = tmp;
        }
		return (0);
	}

	// 在动态规划中计算矩阵积分
	public int[][] computeScoreMatrixForDynamicProgram(String stri, String strC) {
		int len1 = stri.length() + 1;
		int len2 = strC.length() + 1;
		int M[][] = new int[len1][len2]; // 定义动态规划矩阵
		// ---初始化动态规划矩阵-----------
		int p, q;
		for (p = 0; p < len1; p++)
			M[p][0] = spacescore * p;
		for (q = 0; q < len2; q++)
			M[0][q] = spacescore * q;
		// ---初始化结束----------
		// ----计算矩阵的值------------
		for (p = 1; p < len1; p++)
			for (q = 1; q < len2; q++) {// M[p][q]=max(M[p-1][q]-1,M[p][q-1]-1,M[p-1][q-1]+h)
				int h;
				int matchscore = 0;
				int mismatchscore = -1;
				if (stri.charAt(p - 1) == strC.charAt(q - 1))
					h = matchscore;
				else
					h = mismatchscore;
				M[p][q] = Math.max(M[p - 1][q - 1] + h, Math.max(M[p - 1][q] + spacescore, M[p][q - 1] + spacescore));
			}
		return (M);
	}

	// 在动态规划中回溯
	private void traceBackForDynamicProgram(int[][] M, int p, int q, int i, int k1, int k2) {
		while (p > 0 && q > 0) {
			if (M[p][q] == M[p][q - 1] + spacescore) {
				Spaceother[i][p + k1]++;
				q--;
			} else if (M[p][q] == M[p - 1][q] + spacescore) {
				Spacecenter[i][q + k2]++;
				p--;
			} else {
				p--;
				q--;
			}
		}
		if (p == 0)
			while (q > 0) {
				Spaceother[i][k1]++;
				q--;
			}
		if (q == 0)
			while (p > 0) {
				Spacecenter[i][k2]++;
				p--;
			}
	}

	private int[] combine() {
		int Space[] = new int[Pi[center].length() + 1];// 该数组用来记录在P[centerstar]的最终结果各个空隙间插入空格的个数
		int i, j;
		for (i = 0; i < Pi[center].length() + 1; i++) {
			int max = 0;
			for (j = 0; j < n; j++)
				if (Spacecenter[j][i] > max)
					max = Spacecenter[j][i];
			Space[i] = max;
		}
		return (Space);
	}

	// 计算除中心序列以外的其他序列的最大长度
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
		// ---------输出中心序列----------
		String PiAlign[] = new String[n];
		PiAlign[center] = "";
		for (i = 0; i < Pi[center].length(); i++) {
			for (j = 0; j < Space[i]; j++)
				PiAlign[center] = PiAlign[center].concat("-");
			PiAlign[center] = PiAlign[center].concat(Pi[center].substring(i, i + 1));
		}
		for (j = 0; j < Space[Pi[center].length()]; j++)
			PiAlign[center] = PiAlign[center].concat("-");
		// --------中心序列输出完毕------
		// ---------输出其他序列-------
		for (i = 0; i < n; i++) {
			if (i == center)
				continue;
			// ----计算和中心序列比对后的Pi[i],记为PiAlign-----
			/*spaceother[i][j]中储存的是对比后第i个序列的空格个数如0,2,3...*/
			PiAlign[i] = "";
			for (j = 0; j < Pi[i].length(); j++) {
				String kong = "";
				for (int k = 0; k < Spaceother[i][j]; k++)
					kong = kong.concat("-");
				PiAlign[i] = PiAlign[i].concat(kong).concat(Pi[i].substring(j, j + 1));
			}
			String kong = "";
			for (j = 0; j < Spaceother[i][Pi[i].length()]; j++)
				kong = kong.concat("-");
			PiAlign[i] = PiAlign[i].concat(kong);

			// ---Pi计算结束---------
			// ----计算差异数组----
			int Cha[] = new int[Pi[center].length() + 1];
			int position = 0; // 用来记录插入差异空格的位置
			for (j = 0; j < Pi[center].length() + 1; j++) {
				Cha[j] = 0;
				if (Space[j] - Spacecenter[i][j] > 0)
					Cha[j] = Space[j] - Spacecenter[i][j];
				// ----差异数组计算完毕---
				// ----填入差异空格------
				position = position + Spacecenter[i][j];
				if (Cha[j] > 0) { // 在位置position处插入Cha[j]个空格
					kong = "";
					for (int k = 0; k < Cha[j]; k++)
						kong = kong.concat("-");
					PiAlign[i] = PiAlign[i].substring(0, position).concat(kong).concat(PiAlign[i].substring(position));
				}
				position = position + Cha[j] + 1;
				// ----差异空格填入完毕--
			}
		}
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
			for (i = 0; i < n; i++) {
				// System.out.println(Piname[i]);
				bw.write(Piname[i]);
				bw.newLine();
				bw.flush();
				// System.out.println(PiAlign[i]);
				bw.write(PiAlign[i]);
				bw.newLine();
				bw.flush();

			}
			bw.close();
		} catch (Exception ignored) {

		}
		// ---------其他序列输出完毕-----
		// ---------输出结束--------------
	}
	
	public static class ExtremeMapper extends Mapper<Object, Text, NullWritable, Text> {
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
