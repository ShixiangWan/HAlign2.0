package halign.suffix;

import java.util.ArrayList;
import java.util.List;

public class SparkDNAMSATest {
    public static void main(String[] args) {
        List<String> fastaValList = new ArrayList<>();
        fastaValList.add("AUCGTT");
        fastaValList.add("AUCGT");
        fastaValList.add("ACT");
        fastaValList.add("UCGT");


        SuffixTree suffixTree = new SuffixTree();
        suffixTree.build("AUCGTT" + "$");
        List<int[][]> nameList = new ArrayList<>();
        for (int i = 0; i < fastaValList.size(); i++) {
            AlignSubstring alignSubstring = new AlignSubstring(suffixTree, fastaValList.get(i));
            nameList.add(alignSubstring.findCommonSubstrings());
        }
        System.out.println("OK");
    }

}
