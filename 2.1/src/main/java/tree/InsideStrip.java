package tree;

public class InsideStrip {
	 public InsideStrip() {
	    }
	    public static boolean f(int i,int j,int mn,int k){
	        if(j-i>=-k&&j-i<=mn+k)
	            return true;
	        else
	            return false;

	    }
}
