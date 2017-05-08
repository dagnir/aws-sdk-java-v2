/*
 * Copyright 2010-2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.s3.internal;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.annotation.ThreadSafe;
import software.amazon.awssdk.utils.IoUtils;

/**
 * An internal utility used to provide both inter and intra JVM file locking.
 * This works well as long as this class is loaded with the same class loader in
 * a JVM. Otherwise, intra-JVM file locking is not guaranteed.
 * <p>
 * Per javadoc of {@link FileLock}, "File locks are held on behalf of the entire
 * Java virtual machine. They are not suitable for controlling access to a file
 * by multiple threads within the same virtual machine."
 * <p>
 * Hence the need for this utility class.
 */
@ThreadSafe
public enum FileLocks {
    ;
    // External file lock doesn't seem to work correctly on Windows, 
    // so disabling for now (Ref: TT0047889941)
    private static final boolean EXTERNAL_LOCK = false;
    private static final Log log = LogFactory.getLog(FileLocks.class);
    private static final Map<File, RandomAccessFile> LOCKED_FILES = new TreeMap<File, RandomAccessFile>();

    /**
     * Acquires an exclusive lock on the specified file, creating the file as
     * necessary. Caller of this method is responsible to call the
     * {@link #unlock(File)} method to prevent release leakage.
     *
     * @return true if the locking is successful; false otherwise.
     *
     * @throws FileLockException if we failed to lock the file
     */
    public static boolean lock(File file) {
        synchronized (LOCKED_FILES) {
            if (LOCKED_FILES.containsKey(file)) {
                return false;   // already locked
            }
        }
        FileLock lock = null;
        RandomAccessFile raf = null;
        try {
            // Note if the file does not already exist then an attempt will be
            // made to create it because of the use of "rw".
            raf = new RandomAccessFile(file, "rw");
            FileChannel channel = raf.getChannel();
            if (EXTERNAL_LOCK) {
                lock = channel.lock();
            }
        } catch (Exception e) {
            IoUtils.closeQuietly(raf, log);
            throw new FileLockException(e);
        }
        final boolean locked;
        synchronized (LOCKED_FILES) {
            RandomAccessFile prev = LOCKED_FILES.put(file, raf);
            if (prev == null) {
                locked = true;
            } else {
                // race condition: some other thread got locked it before this
                locked = false;
                LOCKED_FILES.put(file, prev);    // put it back
            }
        }
        if (locked) {
            if (log.isDebugEnabled()) {
                log.debug("Locked file " + file + " with " + lock);
            }
        } else {
            IoUtils.closeQuietly(raf, log);
        }
        return locked;
    }

    /**
     * Returns true if the specified file is currently locked; false otherwise.
     */
    public static boolean isFileLocked(File file) {
        synchronized (LOCKED_FILES) {
            return LOCKED_FILES.containsKey(file);
        }
    }

    /**
     * Unlocks a file previously locked via {@link #lock(File)}.
     *
     * @return true if the unlock is successful; false otherwise. Successful
     *         unlock means we have found and attempted to close the locking
     *         file channel, but ignoring the fact that the close operation may
     *         have actually failed.
     */
    public static boolean unlock(File file) {
        synchronized (LOCKED_FILES) {
            final RandomAccessFile raf = LOCKED_FILES.get(file);
            if (raf == null) {
                return false;
            } else {
                // Must close out the channel before removing it from the map;
                // or else risk giving a false negative (of no lock but in fact
                // the file is still locked by the file system.)
                IoUtils.closeQuietly(raf, log);
                LOCKED_FILES.remove(file);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Unlocked file " + file);
        }
        return true;
    }
}
