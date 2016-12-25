package halign.main;

import java.io.IOException;

import halign.center.MatrixMSA;
import halign.center.TreeMSA;
import halign.extreme.ExtremeMSA;
import halign.kband.KbandMSA;
import halign.protein.ProteinMSA;

import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParserException;

public class Controller {
	public static void main(String[] args)  {
		long start = System.currentTimeMillis();
		String inputfile = "D:\\MASTER2016\\1.MSA2.0\\data\\protein.fasta";
		String outputfile = "D:\\MASTER2016\\1.MSA2.0\\data\\output-protein.txt";
		String outputDFS = "hdfs://localhost:9000/msa-extreme";
		int method = 1;
		try {
			if (args.length == 3) { //Single mode
            	inputfile = args[0];
            	outputfile = args[1];
            	method = Integer.parseInt(args[2]);
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
			} else if (args.length == 4) { //Hadoop mode
            	inputfile = args[0];
            	outputfile = args[1];
            	outputDFS = args[2];
            	method = Integer.parseInt(args[3]);
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
			} else {
				errorNote();
			}
		} catch (MatrixLoaderException | SequenceParserException | ClassNotFoundException
				| IOException | InterruptedException e) {
			errorNote();
		}
		long end = System.currentTimeMillis();
		System.out.println(">>Cost time: "+(end - start)+"ms");
		System.out.println(">>Successfully! The result is saved as: "+outputfile);
	}

	public static void errorNote() {
		System.out.println("Error params.");
		System.out.println(">>If you are single user, example command is: ");
		System.out.println("java -jar MSA2.0.jar /home/user/input.txt /home/user/output.txt 0");
		System.out.println(">>If you are hadoop cluster user, example command is: ");
		System.out.println("hadoop jar MSA2.0.jar /home/user/input.txt /home/user/output.txt hdfs://hadoop-master:9000/msa 0");
		System.out.println("Visit https://github.com/ShixiangWan/MSA2.0 for more detailed usage. Thanks.");
		System.exit(0);
	}
}
