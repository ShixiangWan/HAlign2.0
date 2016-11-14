package com.msa.extreme;

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

public class ExtremeMSA {
	private String filepath = ""; // record file name
	private static String Pi[]; // record each sequence
	private static String Piname[];// record the name of each sequence
	private int Spaceevery[][];// The location of the added space when the center sequences are aligned with other sequences, respectively
	private int Spaceother[][];// where additional spaces are added when comparing other sequences with the center sequence
	private int n;// storage sequence number
	private int center;// storage center sequence number
	private int spacescore = -1;// ########### Define the match, mismatch, and space penalty ########### ####
	private int Name[][][][];
	
	
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
			new ClearDfsPath().run(outputDFS);
			CopyFile copyFile = new CopyFile();
			copyFile.local_to_dfs(inputfile, outputDFS + "/input/input.txt");
			
			System.out.println(">>Map reducing ...");
			Configuration conf = new Configuration();
			conf.set("mapred.task.timeout", "0");
			Job job = new Job(conf, "msa_extreme");
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
		
		center = centerAlignEachOne();
		// The position of the added space when the center sequences are aligned with other sequences, respectively
		Spaceevery = new int[n][Pi[center].length() + 1];
		// where additional spaces are added when comparing other sequences with central sequences
        Spaceother = new int[n][computeMaxLength(center) + 1];
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

	// align Pi[i] with the center sequence coincides with the frontier part
    private void prealign(int i) {
        String strC = Pi[center].substring(0, Name[center][i][0][0]);
        String stri = Pi[i].substring(0, Name[center][i][1][0]);
        int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);
        traceBackForDynamicProgram(M, stri.length(), strC.length(), i, 0, 0);
    }

    //align Pi[i] with the center sequence coincides with the middle part
    private void midalign(int i, int j) {
        int lamda = Math.max(Name[center][i][1][j - 1] + Name[center][i][2][j - 1] - Name[center][i][1][j],
                Name[center][i][0][j - 1] + Name[center][i][2][j - 1] - Name[center][i][0][j]);
        if (lamda > 0) {
            Name[center][i][0][j] += lamda;
            Name[center][i][1][j] += lamda;
            Name[center][i][2][j] -= lamda;
        }
        if (Name[center][i][2][j] < 0) System.out.println("Error");

        String strC = Pi[center].substring(Name[center][i][0][j - 1] + Name[center][i][2][j - 1], Name[center][i][0][j]);

        String stri = Pi[i].substring(Name[center][i][1][j - 1] + Name[center][i][2][j - 1], Name[center][i][1][j]);

        int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);
        traceBackForDynamicProgram(M, stri.length(), strC.length(), i,
                Name[center][i][1][j - 1] + Name[center][i][2][j - 1],
                Name[center][i][0][j - 1] + Name[center][i][2][j - 1]);

    }

    // align Pi[i] with the center sequence coincides with the back part
    private void postalign(int i) {
        int j = Name[center][i][0].length;
        if (j > 0) {
            int cstart = Name[center][i][0][j - 1] + Name[center][i][2][j - 1];
            if (cstart > Pi[center].length())
                cstart--; // Do not forget to add a $ when you build the suffix tree
            int istart = Name[center][i][1][j - 1] + Name[center][i][2][j - 1];
            if (istart > Pi[i].length())
                istart--;

            String strC = Pi[center].substring(cstart);
            String stri = Pi[i].substring(Name[center][i][1][j - 1] + Name[center][i][2][j - 1]);
            int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);
            traceBackForDynamicProgram(M, stri.length(), strC.length(), i,istart,cstart);
        } else {
            String strC = Pi[center];
            String stri = Pi[i];
            int M[][] = computeScoreMatrixForDynamicProgram(stri, strC);
            traceBackForDynamicProgram(M, stri.length(), strC.length(), i, 0, 0);
        }
    }

    // Calculate the number of sequences
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

	// The sequence is read into the array once
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
	 * String formatting, delete illegal characters (only retain agctn, 
	 * all the rest of the characters replaced by n), all converted to lowercase, u all replaced by t
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
		Name = new int[n][n][3][];
		SuffixTree st1 = new SuffixTree();
		st1.build(Pi[0] + "$");
		for (int j = 1; j < n; j++) {
			int index = 0;
			ArrayList<Integer> result = new ArrayList<>();
            while (index < Pi[j].length()) {
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

	// Matrix integration is calculated in dynamic programming
	public int[][] computeScoreMatrixForDynamicProgram(String stri, String strC) {
		int len1 = stri.length() + 1;
		int len2 = strC.length() + 1;
		int M[][] = new int[len1][len2];
		// Initializes the dynamic programming matrix
		int p, q;
		for (p = 0; p < len1; p++)
			M[p][0] = spacescore * p;
		for (q = 0; q < len2; q++)
			M[0][q] = spacescore * q;
		// Calculate the value of the matrix
		for (p = 1; p < len1; p++)
			for (q = 1; q < len2; q++) {
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

	// Backtracking in Dynamic Programming
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
		if (p == 0)
			while (q > 0) {
				Spaceother[i][k1]++;
				q--;
			}
		if (q == 0)
			while (p > 0) {
				Spaceevery[i][k2]++;
				p--;
			}
	}

	private int[] combine() {
		int Space[] = new int[Pi[center].length() + 1];
		int i, j;
		for (i = 0; i < Pi[center].length() + 1; i++) {
			int max = 0;
			for (j = 0; j < n; j++)
				if (Spaceevery[j][i] > max)
					max = Spaceevery[j][i];
			Space[i] = max;
		}
		return (Space);
	}

	// The maximum length of the sequence other than the central sequence is calculated
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
		//Output center sequence
		String PiAlign[] = new String[n];
		PiAlign[center] = "";
		for (i = 0; i < Pi[center].length(); i++) {
			for (j = 0; j < Space[i]; j++)
				PiAlign[center] = PiAlign[center].concat("-");
			PiAlign[center] = PiAlign[center].concat(Pi[center].substring(i, i + 1));
		}
		for (j = 0; j < Space[Pi[center].length()]; j++)
			PiAlign[center] = PiAlign[center].concat("-");
		//Output other sequences
		for (i = 0; i < n; i++) {
			if (i == center)
				continue;
			//P[i] after calculation and central sequence alignment is denoted by Pi
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

			//Pi calculation ends
            //Calculate the difference array
			int Cha[] = new int[Pi[center].length() + 1];
			int position = 0; //Used to record the insertion of different spaces
			for (j = 0; j < Pi[center].length() + 1; j++) {
				Cha[j] = 0;
				if (Space[j] - Spaceevery[i][j] > 0)
					Cha[j] = Space[j] - Spaceevery[i][j];
				//Fill in the difference space
				position = position + Spaceevery[i][j];
				if (Cha[j] > 0) {
					kong = "";
					for (int k = 0; k < Cha[j]; k++)
						kong = kong.concat("-");
					PiAlign[i] = PiAlign[i].substring(0, position).concat(kong).concat(PiAlign[i].substring(position));
				}
				position = position + Cha[j] + 1;
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
