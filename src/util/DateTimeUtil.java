package util;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateTimeUtil {

    private DateTimeUtil() {

    }

    public static String time(long millis) {
        return new SimpleDateFormat("HH:mm:ss").format(new Date(millis));
    }

    public static String timeMillis(long millis) {
        return new SimpleDateFormat("HH:mm:ss.SSS").format(new Date(millis));
    }

    public static String date(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(millis));
    }

    public static String dateTime(long millis) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(millis));
    }

    public static String today() {
        return date(System.currentTimeMillis());
    }

    public static long parseDate(String text) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            return sdf.parse(text.trim()).getTime();
        } catch (Exception e) {
            return -1L;
        }
    }

    public static boolean isValidDate(String text) {
        return text != null && parseDate(text) > 0;
    }
}
