/*
 * DocDoku, Professional Open Source
 * Copyright 2006 - 2017 DocDoku SARL
 *
 * This file is part of DocDokuPLM.
 *
 * DocDokuPLM is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DocDokuPLM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with DocDokuPLM.  If not, see <http://www.gnu.org/licenses/>.
 */

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
