package halign.main;

import java.io.File;
import java.util.Objects;

import halign.center.MatrixMSA;
import halign.center.TreeMSA;
import halign.extreme.ExtremeMSA;
import halign.kband.KbandMSA;
import halign.protein.ProteinMSA;

import halign.utils.FileUtils;
import tree.MSATree;

public class Controller {
	public static void main(String[] args)  {
		long start = System.currentTimeMillis();
		String inputfile = "D:\\MASTER2016\\1.MSA2.0\\data\\protein.fasta";
		String outputfile = "D:\\MASTER2016\\1.MSA2.0\\data\\output-protein.txt";
		String outputDFS = "hdfs://localhost:9000/msa-extreme";
		int method = 1;
		try {
            if (Objects.equals(args[0], "-tree")) { // generate tree mode
                String local_path= args[1];
                String filename= args[2];
                String dfs_path= args[3];
                String reduce_number="3";
                FileUtils fileUtils = new FileUtils();
                fileUtils.clear_dfs_path(dfs_path);
                fileUtils.clear_local_path(new File(local_path+"NeighbourJoining_subTree_OutPut"));
                new MSATree().GenerateTree(local_path, filename, dfs_path, reduce_number);
            } else if (Objects.equals(args[0], "-localMSA")) { // msa mode, Single mode
                inputfile = args[1];
                outputfile = args[2];
                method = Integer.parseInt(args[3]);
                switch (method) {
                    case 0:
                        System.out.println(">>Running suffix tree alignment as single mode");
                        new ExtremeMSA().start(inputfile, outputfile, null);
                        break;
                    case 1:
                        System.out.println(">>Running protein alignment as single mode");
                        new ProteinMSA().start(inputfile, outputfile, null);
                        break;
                    case 2:
                        System.out.println(">>Running kband alignment as single mode");
                        new KbandMSA().start(inputfile, outputfile, null);
                        break;
                    case 3:
                        System.out.println(">>Running trie tree alignment as single mode");
                        new TreeMSA().start(inputfile, outputfile, null);
                        break;
                    case 4:
                        System.out.println(">>Running similarity matrix alignment as single mode");
                        new MatrixMSA().start(inputfile, outputfile, null);
                        break;
                }
                long end = System.currentTimeMillis();
                System.out.println(">>Cost time: "+(end - start)+"ms");
                System.out.println(">>Successfully! The result is saved as: "+outputfile);
            } else if (Objects.equals(args[0], "-hadoopMSA")) { //Hadoop mode
                inputfile = args[1];
                outputfile = args[2];
                outputDFS = args[3];
                method = Integer.parseInt(args[4]);
                switch (method) {
                    case 0:
                        System.out.println(">>Running suffix tree alignment as hadoop mode");
                        new ExtremeMSA().start(inputfile, outputfile, outputDFS);
                        break;
                    case 1:
                        System.out.println(">>Running protein alignment as hadoop mode");
                        new ProteinMSA().start(inputfile, outputfile, outputDFS);
                        break;
                    case 2:
                        System.out.println(">>Running kband alignment as hadoop mode");
                        new KbandMSA().start(inputfile, outputfile, outputDFS);
                        break;
                    case 3:
                        System.out.println(">>Running trie tree alignment as hadoop mode");
                        new TreeMSA().start(inputfile, outputfile, outputDFS);
                        break;
                    case 4:
                        System.out.println(">>Running similarity matrix alignment as hadoop mode");
                        new MatrixMSA().start(inputfile, outputfile, outputDFS);
                        break;
                }
                long end = System.currentTimeMillis();
                System.out.println(">>Cost time: "+(end - start)+"ms");
                System.out.println(">>Successfully! The result is saved as: "+outputfile);
            } else {
                errorNote();
            }
		} catch (Exception e) {
            e.printStackTrace();
        }
	}

	private static void errorNote() {
		System.out.println("Error params.");
		System.out.println(">>If you are single user, example command is: ");
		System.out.println("java -jar MSA2.0.jar /home/user/input.txt /home/user/output.txt 0");
		System.out.println(">>If you are hadoop cluster user, example command is: ");
		System.out.println("hadoop jar MSA2.0.jar /home/user/input.txt /home/user/output.txt hdfs://hadoop-master:9000/msa 0");
		System.out.println("Visit https://github.com/ShixiangWan/MSA2.0 for more detailed usage. Thanks.");
	}
}
