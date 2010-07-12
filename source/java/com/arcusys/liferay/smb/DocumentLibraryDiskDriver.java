package com.arcusys.liferay.smb;

import java.io.FileNotFoundException;
import java.io.IOException;
import org.alfresco.jlan.server.SrvSession;
import org.alfresco.jlan.server.core.DeviceContext;
import org.alfresco.jlan.server.core.DeviceContextException;
import org.alfresco.jlan.server.filesys.AccessDeniedException;
import org.alfresco.jlan.server.filesys.DiskDeviceContext;
import org.alfresco.jlan.server.filesys.DiskInterface;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileOpenParams;
import org.alfresco.jlan.server.filesys.NetworkFile;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.server.filesys.TreeConnection;
import org.springframework.extensions.config.ConfigElement;

/**
 * The implementation of DiskInterface utilizing a Liferay document library folder
 * as the underlying share storage
 */
public class DocumentLibraryDiskDriver implements DiskInterface {

    /**
     * Liferay shared folder name
     */
    private String m_liferaySharedFolder;

    /**
     * Get full path to file or folder in Liferay from share-relative name
     * @param shareRelativePath
     * @return
     */
    private String getLiferayFullPath(String shareRelativePath) {
        String liferayFullPath = "";
        if (shareRelativePath.startsWith("\\") || shareRelativePath.equals("")) {
            liferayFullPath = m_liferaySharedFolder + shareRelativePath;
        } else {
            liferayFullPath = shareRelativePath;
        }
        if (liferayFullPath.startsWith("\\")) {
            liferayFullPath = liferayFullPath.substring(1);
        }
        return liferayFullPath;
    }

    /**
     * Initialize a disk share. This method is called by JLAN at startup once for each share.
     * @param name The name of the share
     * @param args The configuration files section corresponding to a share
     * @return The device context with root path in document library used as device name
     * @throws DeviceContextException
     */
    public DeviceContext createContext(String name, ConfigElement args) throws DeviceContextException {
        //The LiferaySharedFolder share configuration parameter stores the name of the
        //Liferay folder we wish to share together with all its content
        //It should start with the topmost folder name, no leading backslash or point allowed
        //One tailing backslash is allowed
        ConfigElement path = args.getChild("LiferaySharedFolder");
        m_liferaySharedFolder = path.getValue();
        //Trim tailing backslash if there is one
        if (m_liferaySharedFolder.endsWith("\\")) {
            m_liferaySharedFolder = m_liferaySharedFolder.substring(0, m_liferaySharedFolder.length() - 1);
        }
        //Use Liferay shared folder name as device name
        DiskDeviceContext context = new DiskDeviceContext(m_liferaySharedFolder);
        //Detect the group ID to access Liferay and remember the URL, username and password
        LiferayStorage.initStorage(args.getChild("LiferayURL").getValue(), args.getChild("LiferayUser").getValue(), args.getChild("LiferayUserId").getValue(), args.getChild("LiferayPassword").getValue());
        return context;
    }

    /**
     * Nothing special is done when connection opened to the device. This method is called by JLAN.
     * @param sess Session details
     * @param tree Tree connection
     */
    public void treeOpened(SrvSession sess, TreeConnection tree) {
    }

    /**
     * Nothing special is done when connection closed to the device. This method is called by JLAN.
     * @param sess Session details
     * @param tree Tree connection
     */
    public void treeClosed(SrvSession sess, TreeConnection tree) {
    }

    /**
     * Check if a file or directory exists in the Liferay-based share.
     * This method is called by JLAN when the SBM client checks file existance.
     * @param sess Session details
     * @param tree Tree connection
     * @param name Full file name starting with a backslash relative to share root
     * @return FileStatus.DirectoryExists if a directory with such a name exists,
     * FileStatus.FileExists if a file with such a name exists,
     * or FileStatus.NotExist otherwise
     */
    public int fileExists(SrvSession sess, TreeConnection tree, String name) {
        return LiferayStorage.fileExists(getLiferayFullPath(name));
    }

    /**
     * Initiate the search (prepare the file list)
     * This method is called by JLAN when the SBM client initiates
     * a file or directoty search or when it gets directory content.
     * The SearchContext class methods will later be invoked by JLAN
     * to retrieve each file record in the result set
     * @param sess Session details
     * @param tree Tree connection
     * @param searchPath Search path starts with a backslash and may contain
     * a single file/folder full name (with path) or wildcards (also with path)
     * @param attrib
     * @return The search context for the initiated search
     * @throws FileNotFoundException
     */
    public SearchContext startSearch(SrvSession sess, TreeConnection tree, String searchPath, int attrib) throws FileNotFoundException {
        DocumentLibrarySearchContext searchContext = new DocumentLibrarySearchContext();
        searchContext.initSearch(getLiferayFullPath(searchPath), attrib);
        return searchContext;
    }

    /**
     * Get file or directory information by name
     * @param sess Session details
     * @param tree Tree connection
     * @param name Full path to file relative to share root, starts with a backslash
     * @return FileInfo object with information regarding file or null if file was not found
     * @throws IOException
     */
    public FileInfo getFileInformation(SrvSession sess, TreeConnection tree, String name) throws IOException {
        FileInfo fileInfo = null;
        try {
            if (name.equals("\\") || name.equals("")) {
                fileInfo = new FileInfo();
                fileInfo.setFileName(m_liferaySharedFolder);
                fileInfo.setFileAttributes(FileAttribute.Directory);
            } else {                
                fileInfo = LiferayStorage.getFileInfo(getLiferayFullPath(name));
            }
            return fileInfo;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * The operation for checking if file is read-only cannot be applied to 
     * Liferay document libaries. This method always returned false.
     * @param sess Session details
     * @param ctx Device context
     * @return True if file is readonly, false otherwise
     * @throws IOException
     */
    public boolean isReadOnly(SrvSession sess, DeviceContext ctx) throws IOException {
        return false;
    }

    /**
     * Create a directory in Liferay
     * @param sess Session details
     * @param tree Tree connection
     * @param params File open parameters
     * @throws IOException
     */
    public void createDirectory(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException {
        LiferayStorage.createFolder(getLiferayFullPath(params.getFullPath()));
    }

    /**
     * Create file in Liferay
     * @param sess Session details
     * @param tree Tree connection
     * @param params File open parameters
     * @return
     * @throws IOException
     */
    public NetworkFile createFile(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException {        
        FileInfo info = LiferayStorage.createFile(getLiferayFullPath(params.getFullPath()),params.getAllocationSize());
        String liferayFullPath = getLiferayFullPath(params.getPath());        
        if (info == null) {            
            throw new UnsupportedOperationException("CreateFileError");
        }
        NetworkFile networkFile = new DocumentLibraryNetworkFile(info);
        networkFile.setFullName(liferayFullPath);
        return networkFile;        
    }

    /**
     * Delete folder in Liferay
     * @param sess Session details
     * @param tree Tree connection
     * @param dir Share-relative path to folder
     * @throws IOException
     */
    public void deleteDirectory(SrvSession sess, TreeConnection tree, String dir) throws IOException {
        LiferayStorage.deleteFolder(getLiferayFullPath(dir));
    }

    /**
     * Delete file in Liferay
     * @param sess Session details
     * @param tree Tree connection
     * @param name Share-relative path to file
     * @throws IOException
     */
    public void deleteFile(SrvSession sess, TreeConnection tree, String name) throws IOException {
        LiferayStorage.deleteFile(getLiferayFullPath(name));
    }

    /**
     * Rename/move file in Liferay
     * @param sess Session details
     * @param tree Tree connection
     * @param oldName Share-relative path to file
     * @param newName New share-relative path
     * @throws IOException
     */
    public void renameFile(SrvSession sess, TreeConnection tree, String oldName, String newName) throws IOException {
        if (LiferayStorage.getFileInfo(getLiferayFullPath(oldName)).isDirectory()) {
            LiferayStorage.renameFolder(getLiferayFullPath(oldName), getLiferayFullPath(newName));
        } else {
            LiferayStorage.renameFile(getLiferayFullPath(oldName), getLiferayFullPath(newName));
        }        
    }

    /**
     * The operation for setting file properties is not applicable to Liferay document libraries.
     * This method does nothing
     * @param sess
     * @param tree
     * @param name
     * @param info
     * @throws IOException
     */
    public void setFileInformation(SrvSession sess, TreeConnection tree, String name, FileInfo info) throws IOException {
    }

    /**
     * The operation for truncating the file to a given size is not applicable
     * to Liferay document libraries and does not seem to be utilized by clients.
     * This method does nothing.
     * @param sess
     * @param tree
     * @param file
     * @param siz
     * @throws IOException
     */
    public void truncateFile(SrvSession sess, TreeConnection tree, NetworkFile file, long siz) throws IOException {
    }

    /**
     * Open a file (get the NetworkFile instance for a file)
     * @param sess Session details
     * @param tree Tree connection
     * @param params File open parameters
     * @return The NetworkFile class descibing the file being opened
     * @throws IOException
     */
    public NetworkFile openFile(SrvSession sess, TreeConnection tree, FileOpenParams params) throws IOException {        
        String liferayFullPath = getLiferayFullPath(params.getPath());
        //Check if a file exists
        FileInfo info = getFileInformation(sess, tree, params.getPath());
        if (info != null && info.getFileName() != null) {
            NetworkFile networkFile = new DocumentLibraryNetworkFile(info);
            networkFile.setFullName(liferayFullPath);            
            return networkFile;        
        } else {
            throw new UnsupportedOperationException("Error: info is null-" + (info == null) + "params.GetPath:" + params.getPath());
        }
    }

    /**
     * Close the previously opened file. This method actually deletes a file if needed
     * (hasDeleteOnClose() return true on the param NetworkFile)
     * @param sess Session details
     * @param tree Tree connection
     * @param param The network file to close
     * @throws IOException
     */
    public void closeFile(SrvSession sess, TreeConnection tree, NetworkFile param) throws IOException {
        if (param.hasDeleteOnClose()) {
            if (param.isDirectory()) {
                LiferayStorage.deleteFolder(param.getFullName());
            } else {
                LiferayStorage.deleteFile(param.getFullName());
            }
        }
        param.closeFile();       
    }

    /**
     * The operation for positioning in a file is not utilized by clients.
     * This method does nothing and returns 0.
     * @param sess
     * @param tree
     * @param file
     * @param pos
     * @param typ
     * @return
     * @throws IOException
     */
    public long seekFile(SrvSession sess, TreeConnection tree, NetworkFile file, long pos, int typ) throws IOException {
        return 0;        
    }

    /**
     * Read Liferay file data.
     * Reading directories leads to an AccessDeniedException
     * @param sess Session details
     * @param tree Tree connection
     * @param file NetworkFile object desribing the file to read
     * @param buf Buffer to receive file data
     * @param bufPos Position in buffer where to start putting data
     * @param siz Number of bytes to read
     * @param filePos Position in file where to start reading
     * @return Number of bytes actually read
     * @throws IOException
     */
    public int readFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufPos, int siz, long filePos) throws IOException {
        if (file.isDirectory()) {
            throw new AccessDeniedException();
        }                
        int rdlen = file.readFile(buf, siz, bufPos, filePos);
        //  If we have reached end of file return a zero length read
        if (rdlen == -1) {
            rdlen = 0;
        }
        //  Return the actual read length
        return rdlen;
    }

    /**
     * Write data to a Liferay file. This method actually writes data to a temporary
     * file on local disk. Data gets to Liferay on file close.
     * @param sess Session details
     * @param tree Tree connection
     * @param file NetworkFile object describing the file to write to
     * @param buf Buffer with data to write
     * @param bufoff Position in buffer where to start reading from
     * @param siz Number of bytes to write
     * @param fileoff Position in file where to start writing to
     * @return Number of bytes actually written
     * @throws IOException
     */
    public int writeFile(SrvSession sess, TreeConnection tree, NetworkFile file, byte[] buf, int bufoff, int siz, long fileoff) throws IOException {
        file.writeFile(buf, siz, bufoff, fileoff);
        return siz;        
    }

    /**
     * The operation for flushing the buffered output to the file is not applicable.
     * This method does nothing.
     * @param sess Session details
     * @param tree Tree connection
     * @param file The NetworkFile object describing the file to flush
     * @throws IOException
     */
    public void flushFile(SrvSession sess, TreeConnection tree, NetworkFile file) throws IOException {
    }
}
