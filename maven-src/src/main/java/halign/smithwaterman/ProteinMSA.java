package halign.smithwaterman;

import jaligner.Alignment;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.MatrixLoader;
import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParser;
import jaligner.util.SequenceParserException;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of Protein Multiple Sequence Alignment based on Smith Waterman Algorithm.
 *
 * @author ShixiangWan
 * */
public class ProteinMSA {

    public static Logger logger = Logger.getLogger(ProteinMSA.class);

    private static ArrayList<String> s_key = new ArrayList<>();
    private static ArrayList<String> s_val = new ArrayList<>();
    private static ArrayList<String> s_out1 = new ArrayList<>();
    private static ArrayList<String> s_out2 = new ArrayList<>();

    /**
     * Run smithwaterman multiple sequence alignment.
     *
     * @param inputFile input file "path+name", fasta format.
     * @param outputFile output file "path+name", fasta format.
     * */
    public void start(String inputFile, String outputFile) {
        logger.info("Loading data ... " + inputFile);

        long startTime = System.currentTimeMillis();

        String line1;
        try {
            BufferedReader brReader = new BufferedReader(new FileReader(inputFile));

            StringBuilder stringBuilder = new StringBuilder();
            while (brReader.ready()) {
                line1 = brReader.readLine();
                if (line1.equals("")) continue;
                if (line1.charAt(0) == '>') {
                    s_key.add(line1);
                    if (stringBuilder.length() != 0) {
                        s_val.add(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }
                } else {
                    stringBuilder.append(line1);
                }
            }
            if (stringBuilder.length() != 0) {
                s_val.add(stringBuilder.toString());
            }
            brReader.close();
        } catch (IOException e) {
            e.printStackTrace();
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
        logger.info("MultiThread MSA ... ");
        int taskSize = 16;
        ExecutorService pool = Executors.newFixedThreadPool(taskSize);
        for (int i = 0; i < total_num; i++) {
            String line = s_val.get(i);
            AlignThread alignThread = new AlignThread(i, sequence1, line);
            pool.execute(new Thread(alignThread));
        }
        pool.shutdown();
        while(!pool.isTerminated());
        logger.info("Aligned, " + (System.currentTimeMillis() - startTime) + "ms");

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
                    if (oneSpace[index] < centerSpaces[i][index]) {
                        oneSpace[index] = centerSpaces[i][index];
                    }
                    index++;
                }
            }
        }

        // Get the centerstar alignment "sequence1".
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < oneSpaceLen; i++) {
            for (int j = 0; j < oneSpace[i]; j++) {
                stringBuilder.append('-');
            }
            if (i != oneSpaceLen - 1) {
                stringBuilder.append(sequence1.charAt(i));
            }
        }
        sequence1 = stringBuilder.toString();
        s_val = null;
        s_out1 = null;

        // Merge array based on "centerSpace", "oneSpace" and "s_out2",
        // and align all sequences.
        logger.info("Merging sequences ... ");
        for (int i = 0; i < total_num; i++) {
            String line = s_out2.get(i);
            // Record the position of inserted spaces.
            int position = 0;
            for (int j = 0; j < oneSpaceLen; j++) {
                int gap = oneSpace[j] - centerSpaces[i][j];
                position += centerSpaces[i][j];
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

        // Save alignment to hard disk.
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
            for (int i = 0; i < s_key.size(); i++) {
                bw.write(s_key.get(i));
                bw.newLine();
                if (i == sequence1_id) {
                    bw.write(sequence1);
                } else {
                    bw.write(s_out2.get(i));
                }
                bw.newLine();
                bw.flush();
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Saved, " + (System.currentTimeMillis() - startTime) + "ms");
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
            try {
                Sequence s1 = SequenceParser.parse(sequence1);
                Sequence s2 = SequenceParser.parse(sequence2);
                Alignment align = SmithWatermanGotoh.align(s1, s2, MatrixLoader.load("BLOSUM80"), 10f, 0.5f);
                s_out1.set(id, new String(align.getSequence1()));
                s_out2.set(id, new String(align.getSequence2()));
            } catch (SequenceParserException | MatrixLoaderException e) {
                e.printStackTrace();
            }
        }
    }


    private String insert(int num) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int k = 0; k < num; k++) {
            stringBuilder.append('-');
        }
        return stringBuilder.toString();
    }
}
