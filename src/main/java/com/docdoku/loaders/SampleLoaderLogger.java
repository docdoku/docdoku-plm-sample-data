package com.docdoku.loaders;


import java.util.logging.*;

/**
 * Custom logger for the sample loader application
 *
 * @author Morgan GUIMARD
 */

public class SampleLoaderLogger {

    private static Logger LOGGER = Logger.getLogger(SampleLoaderLogger.class.getName());

    static {
        LOGGER.setUseParentHandlers(false);
        CustomFormatter formatter = new CustomFormatter();
        ConsoleHandler handler = new ConsoleHandler();
        handler.setFormatter(formatter);
        LOGGER.addHandler(handler);
    }

    public static Logger getLOGGER() {
        return LOGGER;
    }

    private static class CustomFormatter extends Formatter {
        public String format(LogRecord record) {
            StringBuilder builder = new StringBuilder(1000);
            builder.append("[").append(record.getLevel()).append("] - ");
            builder.append(formatMessage(record));
            builder.append("\n");
            Throwable thrown = record.getThrown();
            if (null != thrown) {
                builder.append(thrown.getMessage());
            }
            return builder.toString();
        }

        public String getHead(Handler h) {
            return super.getHead(h);
        }

        public String getTail(Handler h) {
            return super.getTail(h);
        }
    }
}
