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
