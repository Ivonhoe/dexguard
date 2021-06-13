package ivonhoe.dexguard.gradle.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ivonhoe.dexguard.gradle.Constants;

class MapUtils {

    public static Map<String, String> parseMap(File hashFile) {
        Map<String, String> result = new HashMap<>();
        if (hashFile.exists()) {
            FileInputStream inputStream = null;
            BufferedReader bufferedReader = null;
            try {
                inputStream = new FileInputStream(hashFile);
                bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
                String str = null;
                while ((str = bufferedReader.readLine()) != null) {
                    List<String> list = Arrays.asList(str.split(Constants.MAP_SEPARATOR));
                    if (list.size() == 2) {
                        result.put(list.get(0), list.get(1));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }

                    if (bufferedReader != null) {
                        bufferedReader.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            Logger.d("$hashFile does not exist");
        }
        return result;
    }

    public static String format(String path, String hash) {
        return path + Constants.MAP_SEPARATOR + hash + "\n";
    }
}
