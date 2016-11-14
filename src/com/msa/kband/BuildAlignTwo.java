package com.msa.kband;

class BuildAlignTwo {
    private static int M=1, misM=-1, g=10, h=2, rate=2;
    private static int maxk;

    void run(String s, String t){
        int mn = Math.abs(s.length()-t.length());
        int k = rate*mn+1;
        int pok, m=s.length(), n=t.length();//M is the matching integral, and pok is used for control termination
        do{
            AffineGapPenalty aff = new AffineGapPenalty(m, n, g, h, k, mn, s, t, M, misM);
            maxk = aff.Init(); //You must call this method to initialize the array

            //a[m,n]>=M*(n-k-1)-2*(k+1)*(h+g) controls terminates
            //The worst case estimates are at least k + 1 for spaces and not adjacent

            pok = M*(n-k-1)-2*(k+1)*(h+g);
            if(maxk < pok) {
                k=k*2;
            }
            else break;
        }while(k<=pok);

        int ch = 1;//1.2.3 correspond with a.b.c
        if(maxk == AffineGapPenalty.a[m][n+k-m]){ch=1;}
        else if(maxk == AffineGapPenalty.b[m][n+k-m]){ch=2;}
        else {ch=3;}//(k==AffineGapPenalty.c[m][n+k-m])

        RunAlignTwo make = new RunAlignTwo(g,h,m,n,k,mn,s,t,true);
        make.run(ch);
    }


}
