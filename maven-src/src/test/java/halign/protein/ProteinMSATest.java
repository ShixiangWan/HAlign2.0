package halign.protein;

public class ProteinMSATest {
    public static void main(String[] args) {
        String inputFile = "E:\\Programs\\intellij\\HAlign2.1\\example\\protein.fasta";
        String outputFile = "E:\\Programs\\intellij\\HAlign2.1\\example\\protein-out.fasta";
        new ProteinMSA().start(inputFile, outputFile);
    }
}
