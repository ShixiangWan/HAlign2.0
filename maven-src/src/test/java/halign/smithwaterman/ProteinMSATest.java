package halign.smithwaterman;

public class ProteinMSATest {
    public static void main(String[] args) {
        String inputFile = "E:\\Programs\\intellij\\HAlign2.1\\example\\smithwaterman.fasta";
        String outputFile = "E:\\Programs\\intellij\\HAlign2.1\\example\\smithwaterman-out.fasta";
        new ProteinMSA().start(inputFile, outputFile);
    }
}
