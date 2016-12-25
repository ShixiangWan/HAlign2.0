package tree;

public class Make {
	int len,h,g,m,n,mn,k;
    int [][] a,b,c,p;
    Case ca;
    static char []alignsQ,aligntQ;
    static String  aligns,alignt;
    String s,t;
    boolean flag;
    public Make(int g,int h,int m,int n,int k,int mn,String S,String T,boolean flag) {
        a= AffineGapPenalty.a;
        b= AffineGapPenalty.b;
        c= AffineGapPenalty.c;
        p= AffineGapPenalty.p;
        this.g=g;
        this.h=h;
        this.m=m;
        this.n=n;
        this.mn=mn;
        this.k=k;
        this.flag = flag;
        s=S;
        t=T;
        int ff=Math.max(m,n)+2+2*(k+1);
        alignsQ=new char[ff];//max(m.n)+2+2(k+1);m+n+2
        aligntQ=new char[ff];
        for(int i=0;i<alignsQ.length;i++){
            alignsQ[i]='W';
            aligntQ[i]='W';
        }
        ca=new Case(g,h);//判别数组入口的类

    }
    public void f(int ch){
        int i=m,j=n;//m = s.length(),n=t.length();
        len=0;
        boolean tem=true;
        do{
                if(ch==1){
                    ch=ca.caseA(i,j);
                    alignsQ[len]=s.charAt(i-1);//存储比对过的序列元素，但是len是从0开始的，所以后面要做一个反转
                    aligntQ[len]=t.charAt(j-1);//这种情况是两个序列元素比对相等
                    len=len+1;
                    i--;
                    j--;
                }
                else if(ch==2){
                    ch=ca.caseB(i,j);
                    alignsQ[len]='-';
                    aligntQ[len]=t.charAt(j-1);
                    len=len+1;
                    j--;
                }
                else if(ch==3){
                    ch=ca.caseC(i,j);
                    alignsQ[len]= s.charAt(i-1) ;
                    aligntQ[len]= '-';
                    len=len+1;
                    i--;
                }
                else{System.out.println("error");System.exit(0);}
                if(i==0&&j==0)tem=false;
        }while(tem);

//以下完成字符串转置和移除多余
        int p1,p2;
        String str1=new String(alignsQ);//把char[]类型的保存为String 类型

        String str2=new String(aligntQ);

        StringBuffer stb1=new StringBuffer(str1);//把String 类型保存为StringBuffer类型
        StringBuffer stb2=new StringBuffer(str2);
        stb1.reverse();//翻转
        stb2.reverse();

        for(p1=0;p1<stb1.length();p1++){//找到第一个不是W的地方
            if(stb1.charAt(p1)=='W');
            else break;
        }
        for(p2=0;p2<stb2.length();p2++){//找到第一个不是W的地方
            if(stb2.charAt(p2)=='W');
            else break;
        }
        stb1.delete(0,p1);//删除从0到第一个不是W的地方的W
        stb2.delete(0,p2);

        String st1=new String(stb1);//把StringBuffer重新弄成是String类型
        String st2=new String(stb2);
        if(flag){
            aligns = st1;//把String类型保存成char[] 类型
            alignt = st2;
        }
        else{
            alignt = st1;
            aligns = st2;
        }

    }
}
