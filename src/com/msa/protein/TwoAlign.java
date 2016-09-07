package com.msa.protein;

import jaligner.Alignment;
import jaligner.Sequence;
import jaligner.SmithWatermanGotoh;
import jaligner.matrix.MatrixLoader;
import jaligner.matrix.MatrixLoaderException;
import jaligner.util.SequenceParser;
import jaligner.util.SequenceParserException;

public class TwoAlign {
    static char [] alignResultOne, aligntResultTwo;

    //计算两个序列的比对结果
    public void align (String sequence1, String sequence2) {
        try {
            Sequence s1 = SequenceParser.parse(sequence1);
            Sequence s2 = SequenceParser.parse(sequence2);
            Alignment alignment = SmithWatermanGotoh.align(s1, s2, MatrixLoader.load("BLOSUM62"), 10f, 0.5f);
            alignResultOne = alignment.getSequence1();
            aligntResultTwo = alignment.getSequence2();
        } catch (SequenceParserException | MatrixLoaderException e) {
            e.printStackTrace();
        }
    }
}