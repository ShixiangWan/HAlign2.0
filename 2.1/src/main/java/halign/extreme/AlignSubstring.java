package halign.extreme;

import java.util.ArrayList;

public class AlignSubstring {
	SuffixTree center_tree;
	String si;
	public AlignSubstring(SuffixTree c, String i){
		center_tree=c;
		si=i;
	}
	public int[][] findCommonSubstrings(){
		int index = 0;
		int totalmatch = 0;
		String seq=si;
		SuffixTree st1=center_tree;
		ArrayList<Integer> result = new ArrayList();
		while (index <seq.length()){
			int[] a = st1.selectPrefixForAlignment(seq, index);
			if (a[1] > Math.abs(a[0] - index)) {
				result.add(a[0]);
				result.add(index);
				result.add(a[1]);
				index += a[1];
				totalmatch += a[1];
			} else if (a[1] > 0)
				index += a[1];
			else
				index++;
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