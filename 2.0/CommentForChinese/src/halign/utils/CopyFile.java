package halign.utils;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public class CopyFile {
	public void local_to_dfs(String local_file, String dfs_file) {
		try {
			Configuration conf = new Configuration();
			/*Path hdfs = new Path(dfs_file); // Local setting
			FileSystem fs = hdfs.getFileSystem(conf);*/
			FileSystem fs = FileSystem.get(conf);
			Path src = new Path(local_file);
			Path dst = new Path(dfs_file);
			fs.copyFromLocalFile(src, dst);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void dfs_to_local(String dfs_file, String local_file) {
		try {
			Configuration conf = new Configuration();
			/*Path hdfs = new Path(dfs_file); // Local setting
			FileSystem fs = hdfs.getFileSystem(conf);*/
			FileSystem fs = FileSystem.get(conf);
			Path src = new Path(local_file);
			Path dst = new Path(dfs_file);
			fs.copyToLocalFile(true, dst, src);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
