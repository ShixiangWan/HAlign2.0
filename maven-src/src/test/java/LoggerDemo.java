import org.apache.log4j.Logger;

public class LoggerDemo {
    private static Logger logger = Logger.getLogger(LoggerDemo.class);
    public static void main(String[] args) {
        String version = "HAlign2.1.jar";
        String suffix = " <mode> <input-file> <output-file> <algorithm>";
        System.out.println("Kindly note: error params.");
        System.out.println();
        System.out.println("1. if you are a single core user, command is: java -jar "+version+suffix);
        System.out.println("mode: -localMSA, -localTree.");
        System.out.println("input-file: local fasta format file, required.");
        System.out.println("output-file: local fasta format file, just a file name, required.");
        System.out.println("algorithm: 0, 1, 2, 3, or 4, optional.");
        System.out.println();
        System.out.println("2. if you are a hadoop user, command is: hadoop jar "+version+suffix);
        System.out.println("mode: -hadoopMSA.");
        System.out.println("input-file: local fasta format file, required.");
        System.out.println("output-file: local fasta format file, just a file name, required.");
        System.out.println("algorithm: 0, 1, 2, 3, or 4, optional.");
        System.out.println();
        System.out.println("3. if you are a spark user, command is: spark-submit --class main "+version+suffix);
        System.out.println("mode: -sparkMSA, -sparkTree.");
        System.out.println("input-file: local fasta format file, required.");
        System.out.println("output-file: local fasta format file, just a file name, required.");
        System.out.println("algorithm: 0, 1, required for '-sparkMSA' mode.");
        System.out.println();
        System.out.println("Visit https://github.com/ShixiangWan/HAlign2.0 for detailed usages. Thanks.");
    }
}
