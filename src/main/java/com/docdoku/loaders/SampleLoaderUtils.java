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

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Some util methods around file names and resources
 *
 * @author Morgan GUIMARD
 */
public class SampleLoaderUtils {

    public static final String RESOURCE_PREFIX = "com/docdoku/loaders/";

    public static String getDomainName(String url) throws URISyntaxException {
        URI uri = new URI(url);
        String domain = uri.getHost();
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    public static File getFile(String fileName) throws IOException {
        InputStream resourceAsStream = SampleLoaderUtils.class.getClassLoader().getResourceAsStream(RESOURCE_PREFIX + fileName);
        return stream2file(resourceAsStream, fileName);
    }

    public static File stream2file(InputStream in, String fileName) throws IOException {
        String tempDir = System.getProperty("java.io.tmpdir");
        final File tempFile = new File(tempDir, fileName);
        tempFile.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            IOUtils.copy(in, out);
        }

        return tempFile;
    }
}
