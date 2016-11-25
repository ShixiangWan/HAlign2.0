package halign.utils;

public class ClearDfsPath {
	public void run(String dfs_path) {
		try {
			Process process = Runtime.getRuntime().exec("hadoop fs -rmr "+dfs_path);
			process.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

