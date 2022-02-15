package ivonhoe.android.mine.trace;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author : yifan
 * @e-mail : yangfan09359@hellobike.com
 * @date : 2022/2/11
 */
public class DurationTrace {

    private static Map<Long, Long> timestamp = new ConcurrentHashMap<>();

    public static void begin(long id) {
        timestamp.put(id, System.currentTimeMillis());
    }

    public static void end(long id) {
        Long timeMillis = timestamp.get(id);
        if (timeMillis != null) {
            long duration = System.currentTimeMillis() - timeMillis.longValue();

            Log.d("simply", "---------------duration:" + duration);
        }
    }
}
