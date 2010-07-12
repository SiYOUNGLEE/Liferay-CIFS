package com.arcusys.liferay.smb;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.smb.SeekType;

/**
 * Implementation of the NetworkFile abstract class used with the DocumentLibraryDiskDriver
 */
public class DocumentLibraryNetworkFile extends NetworkFile {

    protected RandomAccessFile cachedFile;
    protected String cachedFileName;

    /**
     * Create a DocumentLibraryNetworkFile instance based on a FileInfo object
     * @param info FileInfo object to get infomation from
     */
    public DocumentLibraryNetworkFile(FileInfo info) {
        super(info.getFileName());
        this.setFileSize(info.getSize());
        this.setAttributes(info.getFileAttributes());
    }

    /**
     * Mark the file as open
     * @param createFlag This parameter is currently ignored
     * @throws IOException
     */
    @Override
    public void openFile(boolean createFlag) throws IOException {
        setClosed(false);
    }

    /**
     * Read file content
     * Invoked by the implementation of DiskDriver
     * @param buf Buffer to read content to
     * @param len Number of bytes to read
     * @param pos Position in buffer to start writing to
     * @param fileOff Offset of file to read from
     * @return Number of bytes actually read. 
     * @throws IOException
     */
    @Override
    public int readFile(byte[] buf, int len, int pos, long fileOff) throws IOException {
        if (cachedFile == null) {
            File fileForRead = LiferayStorage.readFile(this.getFullName());
            if (fileForRead == null) {
                return 0;
            }
            cachedFile = new RandomAccessFile(fileForRead, "r");
            cachedFileName = fileForRead.getName();
        }
        if (currentPosition() != fileOff) {
            seekFile(fileOff, SeekType.StartOfFile);
        }
        //  Read from the file
        if (cachedFile != null) {
            return cachedFile.read(buf, pos, len);
        } else {
            return 0;
        }
    }

    /**
     * Get current pointer position of cached file
     * @return File pointer position of cached file
     * @throws IOException
     */
    protected long currentPosition() throws IOException {
        //  Check if the file is open        
        if (cachedFile != null) {
            return cachedFile.getFilePointer();
        }
        return 0;
    }

    /**
     * Write content to file in local disk cache
     * Invoked by the implementation of DiskDriver
     * @param buf Buffer for content
     * @param len Number of bytes to write
     * @param pos Start from this byte in buffer
     * @param fileOff Offset from file start to write to
     * @throws IOException
     */
    @Override
    public void writeFile(byte[] buf, int len, int pos, long offset) throws IOException {
        if (cachedFile == null) {
            cachedFileName = LiferayStorage.getFileNameForWrite(this.getFullName());
            cachedFile = new RandomAccessFile(cachedFileName, "rw");
            if (cachedFile == null) {
                return;
            }
        }
        long fileLen = cachedFile.length();
        if (offset > fileLen) {
            //Extend the file
            cachedFile.setLength(offset + len);
        }
        //Check for a zero length write
        if (len == 0) {
            return;
        }
        //Seek to the write position
        cachedFile.seek(offset);
        //Write to the file
        cachedFile.write(buf, pos, len);
        //Update the write count for the file
        incrementWriteCount();
    }

    /**
     * Seek to a position in a file in local disk cache     
     * Invoked by the implementation of DiskDriver
     * and by PseudoNetworkFile class (if it is used)
     * @param pos Position to seek to
     * @param typ Type of seek to do (SeekType enumeration)
     * @return new position
     * @throws IOException
     */
    @Override
    public long seekFile(long pos, int typ) throws IOException {
        if (cachedFile == null) {
            throw new IOException();
            //openFile(false);
        }
        //  Check if the current file position is the required file position
        switch (typ) {
            case SeekType.StartOfFile:
                if (currentPosition() != pos) {
                    cachedFile.seek(pos);
                }
                break;
            case SeekType.CurrentPos:
                cachedFile.seek(currentPosition() + pos);
                break;
            case SeekType.EndOfFile: {
                long newPos = cachedFile.length() + pos;
                cachedFile.seek(newPos);
            }
            break;
        }
        return currentPosition();       
    }

    /**
     * Flush the buffered output to the file in the local disk cache.
     * Invoked by the implementation of DiskDriver
     * @throws IOException
     */
    @Override
    public void flushFile() throws IOException {
        if (cachedFile != null) {
            cachedFile.getFD().sync();
        }
    }

    /**
     * Truncate file to a given size
     * The operation seems not to be utilized by clients. Stubbed.
     * Invoked by the implementation of DiskDriver
     * @param siz
     * @throws IOException
     */
    @Override
    public void truncateFile(long siz) throws IOException {
    }

    /**
     * Nothing special happens yet when file is closed. This is a stub.
     * Invoked both by JLAN and the implementation of DiskDriver
     */
    @Override
    public void closeFile() throws IOException {
        if (cachedFile != null) {
            //Close the file in local disk cache
            cachedFile.close();
            cachedFile = null;
        }
        if (cachedFileName != null) {
            File file = new File(cachedFileName);
            //If the file was open for writing
            if (cachedFileName.startsWith("write")) {
                //Update its content in Liferay
                LiferayStorage.updateFile(this.getFullName(), file);
            }
            // delete file from local disk cache
            if (file.exists()) {
                System.gc();
                file.delete();
            }
        }
        //Mark the file as closed
        setClosed(true);
    }
}
