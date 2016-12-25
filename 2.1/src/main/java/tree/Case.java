package tree;

public class Case {
	 int[][] a,b,c,p;
	    int h,g,k;
	    public Case(int g,int h) {//abcp初始化
	        a=AffineGapPenalty.a;
	        b=AffineGapPenalty.b;
	        c=AffineGapPenalty.c;
	        p=AffineGapPenalty.p;
	        k=AffineGapPenalty.k;
	        this.g=g;
	        this.h=h;
	    }
	    public int caseA(int i,int j){
	        if(a[i][j+k-i]==p[i][j+k-i]+a[i-1][j-1+k-(i-1)]){ return 1;}
	        else if(a[i][j+k-i]==p[i][j+k-i]+b[i-1][j-1+k-(i-1)]){return 2;}
	        else if(a[i][j+k-i]==p[i][j+k-i]+c[i-1][j-1+k-(i-1)]){return 3;}
	        else{return 'F';}
	    }
	    public int caseB(int i,int j){
	        if(b[i][j+k-i]==a[i][j-1+k-i]-(h+g)){ return 1;}
	        else if(b[i][j+k-i]==b[i][j-1+k-i]-g){ return 2;}
	        else if(b[i][j+k-i]==c[i][j-1+k-i]-(h+g)){ return 3;}
	        else{return 'F';}
	    }
	    public int caseC(int i,int j){
	        if(c[i][j+k-i]==a[i-1][j+k-(i-1)]-(h+g)){ return 1;}
	        else if(c[i][j+k-i]==b[i-1][j+k-(i-1)]-(h+g)){ return 2;}
	        else if(c[i][j+k-i]==c[i-1][j+k-(i-1)]-g){ return 3;}
	        else{return 'F';}

	    }
}
