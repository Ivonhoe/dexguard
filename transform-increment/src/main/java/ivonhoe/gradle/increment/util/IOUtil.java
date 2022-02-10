/*
 * Tencent is pleased to support the open source community by making wechat-matrix available.
 * Copyright (C) 2018 THL A29 Limited, a Tencent company. All rights reserved.
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ivonhoe.gradle.increment.util;


import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Created by tangyinsheng on 2017/1/16.
 */
public final class IOUtil {

    /**
     * Judge if {@code input} is a real zip or jar file.
     * For handling special case in Wechat plugin that generates a path list by writing paths
     * into a file whose name ends with {@code PathUtil.DOT_JAR}
     *
     * @param input File to judge with.
     * @return true - if input is a real zip or jar file.
     * false - if input is not a real zip or jar file.
     */
    public static boolean isRealZipOrJar(File input) {
        ZipFile zf = null;
        try {
            zf = new ZipFile(input);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            IOUtil.closeQuietly(zf);
        }
    }

    /**
     * Copy {@code srcFile} to {@code destFile}.
     *
     * @param src  Source file.
     * @param dest Destination file.
     */
    public static void copyFile(File src, File dest) throws IOException {
        if (!dest.exists()) {
            dest.getParentFile().mkdirs();
        }
        Files.copy(src.toPath(), dest.toPath(),
                StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Copy data in {@code is} to {@code os} <b>without</b> closing any of these streams.
     *
     * @param is     Data source.
     * @param os     Data destination.
     * @param buffer Buffer used to temporarily hold copying data, if {@code null} is passed, a new buffer
     *               will be created in each invocation of this method.
     */
    public static void copyStream(InputStream is, OutputStream os, byte[] buffer) throws IOException {
        if (buffer == null || buffer.length == 0) {
            buffer = new byte[4096];
        }
        int bytesCopied;
        while ((bytesCopied = is.read(buffer)) >= 0) {
            os.write(buffer, 0, bytesCopied);
        }
        os.flush();
    }

    /**
     * Copy data in {@code is} to {@code os} <b>without</b> closing any of these streams.
     *
     * @param is Data source.
     * @param os Data destination.
     */
    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        copyStream(is, os, null);
    }

    /**
     * Close {@code target} quietly.
     *
     * @param obj Object to be closed.
     */
    public static void closeQuietly(Object obj) {
        if (obj == null) {
            return;
        }
        if (obj instanceof Closeable) {
            try {
                ((Closeable) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else if (obj instanceof AutoCloseable) {
            try {
                ((AutoCloseable) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else if (obj instanceof ZipFile) {
            try {
                ((ZipFile) obj).close();
            } catch (Throwable ignored) {
                // ignore
            }
        } else {
            throw new IllegalArgumentException("obj " + obj + " is not closeable");
        }
    }

    private IOUtil() {
        throw new UnsupportedOperationException();
    }

    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public static final int BUFFER_SIZE = 16384;

    public static void copyFileUsingStream(File source, File dest) throws IOException {
        FileInputStream is = null;
        FileOutputStream os = null;
        File parent = dest.getParentFile();
        if (parent != null && (!parent.exists())) {
            parent.mkdirs();
        }
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest, false);

            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            closeQuietly(is);
            closeQuietly(os);
        }
    }

    public static void addZipEntry(ZipOutputStream zipOutputStream, ZipEntry zipEntry, InputStream inputStream) throws Exception {
        try {
            zipOutputStream.putNextEntry(zipEntry);
            byte[] buffer = new byte[BUFFER_SIZE];
            int length = -1;
            while ((length = inputStream.read(buffer, 0, buffer.length)) != -1) {
                zipOutputStream.write(buffer, 0, length);
                zipOutputStream.flush();
            }
        } catch (ZipException e) {
            Logger.error("addZipEntry err!");
        } finally {
            closeQuietly(inputStream);

            zipOutputStream.closeEntry();
        }
    }
}
