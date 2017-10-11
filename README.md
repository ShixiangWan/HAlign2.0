# HAlign2.0（MSA2.0）
HAlign2.0 is a software based Java, which can align multiple DNA/RNA/Protein sequences in stand-alone mode or Hadoop cluster mode. Hadoop parallel computing environment has a faster alignment speed. Additionally, if a Hadoop cluster environment is not ready, you can use its stand-alone mode to start your work. But when your sequence files are large (more than 1GB), we recommend that you'd better to run on the Hadoop cluster to save valuable time.

Home page: http://lab.malab.cn/soft/halign/hadoop.html

# Development Environment

* Ubantu 16.04
* Hadoop 2.7.2
* JDK >= 1.7
* Intellij IDEA

# Usage
### 1. Stand-alone mode
```
# java -jar HAlign2.1.jar <mode> <input-file> <output-file> <algorithm>
```
  * mode: **-localMSA**, **-localTree**.
  * input-file: local fasta format file, required.
  * output-file: local fasta format file, just a file name, required.
  * algorithm: sequence alignment algorithms, required for **-localMSA** mode, but none for **-localTree** mode. **0** represents the suffix tree algorithm, the fastest, but only for DNA/RNA; **1** represents the KBand algorithm based BLOSUM62 scoring matrix, only for Protein; **2** represents the KBand algorithm based on affine gap penalty, only for DNA/RNA; **3** represents the trie tree alignment algorithm, but slower and only for DNA/RNA; **4** represents the basic algorithm based the similarity matrix, the slowest and only for DNA/RNA. But it is the most accurate in the case of lower sequences similarity.
  
### 2. Hadoop cluster mode
```
# hadoop jar HAlign2.1.jar <mode> <input-file> <output-file> <algorithm>
```
  * mode: **-hadoopMSA**.
  * input_file/output_file/algorithm type: same as stand-alone mode.

### 3. Spark cluster mode
```
# spark-submit --class main HAlign2.1.jar <mode> <input-file> <output-file> <algorithm>
```
  * mode: **-sparkMSA**, **-sparkTree**.
  * input-file: local fasta format file, required.
  * output-file: local fasta format file, just a file name, required.
  * algorithm: sequence alignment algorithms, required for **-sparkMSA** mode, but none for **-sparkTree** mode. **0** represents the suffix tree algorithm, the fastest, but only for DNA/RNA; **1** represents the KBand algorithm based BLOSUM62 scoring matrix, only for Protein.
  
# Update
* 2016-09-07, version 2.0.0:
  * Basic functions.
* 2016-11-14, version 2.1.0:
  * Add version: comment for english.
* 2016-11-25, version 2.1.1:
  * Fix some bugs about protein sequences alignment on multi-thread technique.
  * Fix some bugs about file I/O.
* 2016-11-25, version 2.1.1:
  * update README.