package halign.extreme;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.broadcast.Broadcast;
import scala.Tuple2;
import utils.FormatUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class SparkDNAMSA {
    public static void main(String[] args) {

        String inputKVFile = args[0];
        String outputfile = args[1];

        SparkConf conf = new SparkConf().setAppName("SparkDNAMSA");
//        conf.setMaster("local[16]");
        conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
        conf.set("spark.kryoserializer.buffer.max", "2000m");
        conf.registerKryoClasses(new Class[]{SparkDNAMSA.class});
        JavaSparkContext jsc = new JavaSparkContext(conf);
        new SparkDNAMSA().start(jsc, inputKVFile, outputfile);
        jsc.stop();
    }


    public void start(JavaSparkContext jsc, String inputKVFile, String outputfile) {

        System.out.println(">>(Spark mode for DNA) Loading data ... " + inputKVFile);
        long start = System.currentTimeMillis();
        FormatUtils formatUtils = new FormatUtils();
        formatUtils.readFasta(inputKVFile, true);
        int allNum = formatUtils.getAllNum();
        int maxLength = formatUtils.getMaxLength();
        List<String> fastaKeyList = formatUtils.getS_key();
        List<String> fastaValList = formatUtils.getS_val();
        final String firstVal = fastaValList.get(0);
        System.out.println((System.currentTimeMillis() - start) + "ms");


        System.out.println(">>MultiThread MSA ... ");
        SuffixTree suffixTree = new SuffixTree();
        suffixTree.build(firstVal + "$");
        List<int[][]> nameList = new ArrayList<>();
        for (int i = 0; i < allNum; i++) {
            AlignSubstring alignSubstring = new AlignSubstring(suffixTree, fastaValList.get(i));
            nameList.add(alignSubstring.findCommonSubstrings());
        }
        suffixTree = null;
        System.out.println((System.currentTimeMillis() - start) + "ms");


        JavaRDD<String> fastaValRDD = jsc.parallelize(fastaValList).cache();
        fastaValList = null;
        final Broadcast<String> firstValBC = jsc.broadcast(firstVal);
        JavaPairRDD<int[], int[]> spacePairsRDD = fastaValRDD.zip(jsc.parallelize(nameList)).mapToPair(
                (PairFunction<Tuple2<String, int[][]>, int[], int[]>) tuple2 -> {
                    DNAPairAlign pairAlign = new DNAPairAlign(firstValBC.value(), tuple2._1, tuple2._2, maxLength);
                    pairAlign.pwa();
                    int[] se = pairAlign.get_spaceevery();
                    int[] so = pairAlign.get_spaceother();
                    return new Tuple2(se, so);
                });
        spacePairsRDD = spacePairsRDD.coalesce(16, false).cache();

        int firstSpaceArray[] = spacePairsRDD.keys().reduce(
                (Function2<int[], int[], int[]>) (int1, int2) -> {
                    for (int i = 0; i < int2.length; i++)
                        if (int1[i] < int2[i]) int1[i] = int2[i];
                    return int1;
                });
        StringBuilder stringBuilder = new StringBuilder();
        String firstVal0 = firstVal;
        int firstValLen = firstVal0.length() + 1;
        for (int i = 0; i < firstValLen; i++) {
            for (int j = 0; j < firstSpaceArray[i]; j++) stringBuilder.append('-');
            if (i != firstValLen - 1) stringBuilder.append(firstVal0.charAt(i));
        }
        int lastNum = firstSpaceArray[firstSpaceArray.length - 1];
        for (int i = 0; i < lastNum; i++)
            stringBuilder = stringBuilder.append('-');
        firstVal0 = stringBuilder.toString();
        System.out.println((System.currentTimeMillis() - start) + "ms");

        System.out.println(">>Converting results ... ");
        final Broadcast<String> firstVal0BC = jsc.broadcast(firstVal0);
        final Broadcast<int[]> firstSpaceArrayBC = jsc.broadcast(firstSpaceArray);
        JavaRDD<String> fastaMSAOutJavaRDD = fastaValRDD.zip(spacePairsRDD).map(
                (Function<Tuple2<String, Tuple2<int[], int[]>>, String>) t -> {
                    String pi1 = t._1().trim();
                    int[] spaceevery = t._2()._1();
                    int[] spaceother = t._2()._2();
                    GenAlignOut genAlignOut = new GenAlignOut();
                    return genAlignOut.get_every_sequeces(firstVal0BC.value().trim(), pi1,
                            allNum, firstVal, firstVal.length(),
                            firstSpaceArrayBC.value(), spaceevery, spaceother);
                });
        fastaMSAOutJavaRDD = fastaMSAOutJavaRDD.coalesce(16, false).cache();
        System.out.println(">>" + (System.currentTimeMillis() - start) + "ms");

        System.out.println(">>Saving final results ... ");
        List<String> pi_re_list = fastaMSAOutJavaRDD.collect();
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(outputfile));
            for (int i = 0; i < allNum; i++) {
                bw.write(fastaKeyList.get(i) + "\n");
                bw.write(pi_re_list.get(i) + "\n");
            }
            bw.close();
        } catch (Exception ignored) {

        }
        System.out.println(">>" + (System.currentTimeMillis() - start) + "ms");
    }
}
