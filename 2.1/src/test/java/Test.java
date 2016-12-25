import halign.utils.FileUtils;

import java.io.File;

public class Test {
    public static void main(String[] args) {
        FileUtils fileUtils = new FileUtils();
        fileUtils.clear_local_path(new File("/home/shixiang/NeighbourJoining_subTree_OutPut"));
    }
}
