package com;

import java.io.Closeable;
import java.io.IOException;

/**
 * A Class for a generic function for closing closable's objects
 */
public class Close {
    /**
     * Try to close Closeable object
     *
     * @param closeable - closeable object
     */
    public static void close(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
