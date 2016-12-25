package halign.utils;

public class CheckInputPath {
	public static String check(String path) {
		if (!path.contains("/")) {
        	System.out.println("Error: Please set full input file path, such as '/home/user/input.txt'");
        	System.exit(0);
        }
		return path.substring(0, path.lastIndexOf("/"));
	}
}
