package halign.protein;

import jaligner.Alignment;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.MatrixLoader;
import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParser;
import jaligner.util.SequenceParserException;

import java.util.ArrayList;

public class ProteinAlignTwo {
    //计算两个序列的比对结果
    public ArrayList<String> align (String sequence1, String sequence2) {
        ArrayList<String> alignResult = new ArrayList<>();
        try {
            Sequence s1 = SequenceParser.parse(sequence1);
            Sequence s2 = SequenceParser.parse(sequence2);
            Alignment alignment = SmithWatermanGotoh.align(s1, s2, MatrixLoader.load("BLOSUM80"), 10f, 0.5f);
            alignResult.add(new String(alignment.getSequence1()));
            alignResult.add(new String(alignment.getSequence2()));
        } catch (SequenceParserException | MatrixLoaderException e) {
            e.printStackTrace();
        }
        return alignResult;
    }
}