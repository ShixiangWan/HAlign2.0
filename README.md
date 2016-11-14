# HAlign2.0（MSA2.0）
HAlign2.0是一款DNA/RNA/Protein序列的单机、Hadoop多序列比对软件。它基于Java语言开发，在Hadoop的并行计算环境中拥有较快的比对速度。它使用简便，即使您没有完整的Hadoop集群环境，亦可使用其单机命令开始试验。当您的序列文件较大（超过1GB），我们建议您还是在Hadoop集群上运行，节约时间。

Home page: http://lab.malab.cn/soft/halign/hadoop.html

##开发环境

* Ubantu 16.04
* Hadoop 2.7.2
* JDK >= 1.7
* Eclipse Mars

##使用方法
####1. 如果您是单机使用用户：
```
java -jar MSA2.0.jar <input_file> <output_file> <algorithm type>
# 例如：java -jar MSA2.0.jar /home/user/input.txt /home/user/output.txt 0
```
  * input_file：fasta格式的输入文件，位于本地；
  * output_file：输出比对结果文件，位于本地；
  * algorithm type：序列比对算法的类型。0代表后缀树比对算法，速度最快，但只适合DNA/RNA；1代表基于BLOSUM62得分矩阵的KBand比对算法，只适合Protein；2代表基于仿射空隙罚分的KBand比对算法，只适合DNA/RNA；3代表Trie tree比对算法，但速度较慢，只适合DNA/RNA；4代表基于相似度矩阵的比对算法，速度最慢，在序列相似度极低情况下使用，且只适合DNA/RNA。

####2. 如果您是Hadoop集群用户：
```
hadoop jar MSA2.0.jar <input_file> <output_file> <dfs_path> <algorithm type>
# 例如：hadoop jar MSA2.0.jar /home/user/input.txt /home/user/output.txt hdfs://hadoop-master:9000/msa 0
```
  * input_file/output_file/algorithm type：与单机使用相同；
  * dfs_path：Hadoop集群DFS（分布式文件系统）中存放实验文件的路径。

  
##更新日志
* 2016-09-07, version 0.1:
  * 完成基本功能。
* 2016-11-14, version 0.1:
  * 英文源程序注释
