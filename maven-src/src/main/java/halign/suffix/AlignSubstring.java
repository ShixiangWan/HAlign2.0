package halign.suffix;

import java.util.ArrayList;

public class AlignSubstring {
	SuffixTree suffixTree;
	String sequence;

	public AlignSubstring(SuffixTree suffixTree, String sequence){
		this.suffixTree = suffixTree;
        this.sequence = sequence;
	}

	public int[][] findCommonSubstrings(){
		int index = 0;
		//int totalMatch = 0;
		ArrayList<Integer> result = new ArrayList();
		while (index < sequence.length()){
			int[] a = suffixTree.selectPrefixForAlignment(sequence, index);
			if (a[1] > Math.abs(a[0] - index)) {
				result.add(a[0]);
				result.add(index);
				result.add(a[1]);
				index += a[1];
				//totalMatch += a[1];
			} else if (a[1] > 0) {
                index += a[1];
            } else {
                index ++;
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
		return tmp;
	}
}