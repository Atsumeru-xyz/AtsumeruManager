package xyz.atsumeru.manager.utils.globalutils;

import java.text.SimpleDateFormat;

public class GUDate {

    public GUDate() {
        super();
    }

    /**
     * ISO 8601
     */
    public enum DateFormat {
        /**
         * 1997-07-16
         **/
        Complete("yyyy-MM-dd");

        private final String format;

        DateFormat(String format) {
            this.format = format;
        }

        public static String format(DateFormat format, long time) {
            try {
                return new SimpleDateFormat(format.toString()).format(time);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        public String toString() {
            return format;
        }
    }
}