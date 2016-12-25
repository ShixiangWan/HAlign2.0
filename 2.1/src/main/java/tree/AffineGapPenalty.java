package tree;

public class AffineGapPenalty {
	int m,n,g,h,mn,min,M,misM;
    static int [][] a,b,c,p;
    static int k;
    String s,t;
    //m = s.length(),n=t.length();g=gap;h=space;k = rate*mn+1;mn = Math.abs(s.length()-t.length());M=match;misM=mismatch;
    public AffineGapPenalty(int m,int n,int g,int h,int k,int mn,String s,String t,int M,int misM) {
        this.m=m+1;
        this.n=n+1;
        this.g=g;
        this.h=h;
        AffineGapPenalty.k=k;
        this.mn=mn;
        this.s=s;
        this.t=t;
        min=-1073741824;//int类型最小值为-2的31次方即-2147483648,假设min=2的-30次方是负无穷
        this.M = M;
        this.misM = misM;
    }
    public void InitP(String s,String t,int K){
        p=new int[m][K];
        for(int i=1;i<m;i++){
            for (int d =-k; d <=mn+k; d++) {
                int j=i+d;
                if(j<n&&j>=1){
                    char s1, t1;
                    s1 = s.charAt(i-1);//********************
                    t1 = t.charAt(j-1);//********************
                    if (s1 == t1)
                        p[i][j+k-i]=M;//p[i][j] = 1;  p'(i,j+k-i)=p(i,j)
                    else
                        p[i][j+k-i]=misM;//p[i][j] = -1;
                }
            }
        }
    }

    public void InitA(int K){
        a=new int[m][K];//横坐标为序列s的长度，纵坐标为K的二维数组
        for(int i=0;i<m;i++){
            for(int j=0;j<K;j++){
                a[i][j]=min;//遍历所有数组给数组赋予最小值
            }
        }
        a[0][0+k]=0;
    }

    public void InitB(int K){
        b=new int[m][K];
        for(int i=0;i<m;i++){
            for(int j=0;j<K;j++){
                b[i][j]=min;
            }
        }
        for (int j=0;j<K-k;j++)
            b[0][j+k]=-(h+g*j);
    }

    public void InitC(int K){
        c=new int[m][K];
        for(int i=0;i<m;i++){
            for(int j=0;j<K;j++){
                c[i][j]=min;
            }
        }

        for (int i=1;i<=k;i++)
            c[i][0+k-i]=-(h+g*i);
    }

    public int Init(){
        int K=2*k+mn+1;//K为带宽，看图就是两边的k加上中间的m-n
        InitA(K);//矩阵a用于以字符-字符的块终止的序列比对计分
        InitB(K);//b用于序列s以空格终止的比对计分
        InitC(K);//c用于序列t以空格终止的比对计分
        InitP(s,t,K);
        int mk;
        for(int i=1;i<m;i++){
          for(int d=-k;d<=mn+k;d++){
              int j=i+d;
              if(j<n&&j>=1){
                  mk=Max.f(a[i-1][j-1+k-(i-1)],b[i-1][j-1+k-(i-1)],c[i-1][j-1+k-(i-1)]);
                  a[i][j+k-i]=p[i][j+k-i]+mk;
                   if(InsideStrip.f(i,j-1,mn,k)){//如果是在条状带里面
                       mk=Max.f(a[i][j-1+k-i]-(h+g),b[i][j-1+k-i]-g,c[i][j-1+k-i]-(h+g));
                       b[i][j+k-i]=mk;
                   }

                   if(InsideStrip.f(i-1,j,mn,k)){
                       mk=Max.f(a[i-1][j+k-(i-1)]-(h+g),b[i-1][j+k-(i-1)]-(h+g),c[i-1][j+k-(i-1)]-g);
                       c[i][j + k - i] = mk;
                   }
              }
          }
      }
      //  System.out.println("a[m-1][n-1+k-(m-1)]  is :"+a[m-1][n-1+k-(m-1)]);
     //  System.out.println("b[m-1][n-1+k-(m-1)]  is :"+b[m-1][n-1+k-(m-1)]);
    //    System.out.println("c[m-1][n-1+k-(m-1)]  is :"+c[m-1][n-1+k-(m-1)]);
     return Max.f(a[m-1][n-1+k-(m-1)],b[m-1][n-1+k-(m-1)],c[m-1][n-1+k-(m-1)]);
    } 
}
