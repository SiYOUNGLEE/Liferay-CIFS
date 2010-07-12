package com.arcusys.liferay.smb;

import com.arcusys.liferay.httputil.ClientHttpRequest;
import com.liferay.client.soap.portal.service.ServiceContext;
import com.liferay.client.soap.portal.service.http.CompanyServiceSoap;
import com.liferay.client.soap.portal.service.http.CompanyServiceSoapServiceLocator;
import com.liferay.client.soap.portal.service.http.GroupServiceSoap;
import com.liferay.client.soap.portal.service.http.GroupServiceSoapServiceLocator;
import com.liferay.client.soap.portlet.documentlibrary.model.DLFileEntrySoap;
import com.liferay.client.soap.portlet.documentlibrary.model.DLFolderSoap;
import com.liferay.client.soap.portlet.documentlibrary.service.http.DLFileEntryServiceSoap;
import com.liferay.client.soap.portlet.documentlibrary.service.http.DLFileEntryServiceSoapServiceLocator;
import com.liferay.client.soap.portlet.documentlibrary.service.http.DLFolderServiceSoap;
import com.liferay.client.soap.portlet.documentlibrary.service.http.DLFolderServiceSoapServiceLocator;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;

import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.FileStatus;
import org.alfresco.jlan.server.filesys.FileType;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Proxy to Liferay portal document library & utulity operations
 */
public class LiferayStorage {

    /**
     * Liferay web services root URL
     */
    private static String _storageRootUrl;
    /**
     * Liferay "Guest" group ID
     */
    private static long _guestGroupId;
    /**
     * Liferay user ID
     */
    private static long _userId;
    /**
     * Liferay user login
     */
    private static String _user;
    /**
     * Liferay user password
     */
    private static String _password;
    /**
     * The file list cache from latest search
     */
    private static Hashtable<String, FileInfo> _fileListCache;
    /**
     * The value of HTTP header "Cookie" to be used to access URLs that require authorization via raw HTTP
     */
    private static String _cookies;
    /**
     * The name-value map for all cookies to be used to access URLs that require authorization via raw HTTP (semantically the same as _cookies)
     */
    private static Map _cookiesMap;

    /**
     * Store the connection credentials and detect the "Guest" group ID
     * @param url Liferay root URL
     * @param userId Liferay user ID
     * @param password Liferay user password
     */
    public static void initStorage(String url, String user, String userId, String password) {
        _storageRootUrl = url.replaceFirst("http://", "");// + "/tunnel-web/secure/axis/";
        try {
            URL liferayURL = new URL(url);
            _password = password;
            _userId = Integer.parseInt(userId);
            _user = user;
            _fileListCache = new Hashtable<String, FileInfo>();
            _cookies = "";

            CompanyServiceSoap companyService = new CompanyServiceSoapServiceLocator().getPortal_CompanyService(getURL("Portal_CompanyService"));
            long companyId = companyService.getCompanyByVirtualHost(liferayURL.getHost()).getCompanyId();
            GroupServiceSoap groupService = new GroupServiceSoapServiceLocator().getPortal_GroupService(getURL("Portal_GroupService"));
            _guestGroupId = groupService.getGroup(companyId, "Guest").getGroupId();
        } catch (Exception e) {
        }
    }

    /**
     * Get Liferay folder ID by folder name
     * @param path Full path to folder in Liferay, no backslashes at head or tail
     * @return Liferay folder ID. 0 for root folder. -1 if not found
     */
    public static long findFolderId(String path) {
        String pathList[] = FileName.splitAllPaths(path);
        long currentId = 0;
        try {
            boolean pathFound = false;
            for (int count = 0; count < pathList.length; count++) {
                for (DLFolderSoap folder : getDLFoldersByParentFolderId(currentId)) {
                    if (pathList[count].equalsIgnoreCase(folder.getName())) {
                        currentId = folder.getFolderId();
                        pathFound = true;
                        break;
                    } else {
                        pathFound = false;
                    }
                }
            }
            return pathFound ? currentId : -1;
        } catch (Exception ex) {
        }
        return currentId;
    }

    /**
     * Return the Liferay ID of the parent folder of file or folder
     * @param path Full path to file or folder in Liferay, no backslashes at head or tail
     * @return ID of parent Liferay folder. 0 if parent folder is root folder. -1 if not found
     */
    private static long findParentFolderId(String path) {
        String currentPath = trimTailBackslash(path);
        if (_fileListCache.containsKey(path)) {
            return _fileListCache.get(currentPath).getDirectoryIdLong();
        } else {
            return findFolderId(FileName.removeFileName(currentPath));
        }
    }

    /**
     * Get the list of FileInfo objects for all files in a folder.
     * Populates the global cache to speed up the work of GetFileInfo
     * @param folderId Id of the folder we search files in
     * @param path Full Liferay path to the folder we search files in, no backslashes at head or tail
     * @return List of FileInfo objects for all files and folders in the parent folder
     */
    public static ArrayList<FileInfo> getFileList(long folderId, String path) throws Exception {
        ArrayList<FileInfo> result = new ArrayList<FileInfo>();
        for (DLFileEntrySoap entry : getDLFileEntriesByFolderId(folderId)) {
            FileInfo info = getFileInfoFromFileEntry(entry, folderId, path);
            result.add(info);
            String fileName = info.getFileName();
            if (!path.isEmpty()) {
                fileName = path + "\\" + fileName;
            }
            _fileListCache.put(fileName, info);
        }
        for (DLFolderSoap folder : getDLFoldersByParentFolderId(folderId)) {
            FileInfo info = getFileInfoFromFolderEntry(folder, folderId, path);
            result.add(info);
            String folderName = info.getFileName();
            if (!path.isEmpty()) {
                folderName = path + "\\" + folderName;
            }
            _fileListCache.put(folderName, info);
        }
        return result;
    }

    /**
     * Get the "name.ext" formatted file name from a Liferay DLFileEntrySoap object
     * @param file 
     * @return
     */
    private static String getDLFileEntryName(DLFileEntrySoap file) {
        return file.getTitle() + file.getName().substring(file.getName().lastIndexOf("."));
    }

    /**
     * Get a DLFileEntrySoap object for a Liferay file located in a given folder with a given name
     * @param folderId Liferay folder ID to search files in
     * @param name File name in "name.ext" format
     * @return DLFileEntrySoap object for given search values or null if not found
     */
    private static DLFileEntrySoap getDLFileEntryByFolderIdAndName(long folderId, String name) {
        try {
            for (DLFileEntrySoap file : getDLFileEntriesByFolderId(folderId)) {
                if (getDLFileEntryName(file).equals(name)) {
                    return file;
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    /**
     * Get a DLFolderSoap object for a Liferay folder located in a given parent folder with a given name
     * @param folderId Liferay parent folder ID to search folders in, 0 for root
     * @param name Folder name
     * @return DLFolderSoap object for given search values or null if not found
     */
    private static DLFolderSoap getDLFolderByParentFolderIdAndName(long folderId, String name) {
        try {
            for (DLFolderSoap folder : getDLFoldersByParentFolderId(folderId)) {
                if (folder.getName().equals(name)) {
                    return folder;
                }
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    /**
     * Check if a file of folder exists in Liferay storage
     * @param path Full file/folder name in Liferay with path 
     * starting with topmost folder name without leading backslash or period
     * @return FileStatus.DirectoryExists if file exists and is a Liferay folder;
     * FileStatus.FileExists if file exists and is a Liferay file;
     * FileStatus.NotExist if file does not exist in Liferay
     */
    public static int fileExists(String path) {
        if (_fileListCache.containsKey(path)) {
            if (_fileListCache.get(path).isDirectory()) {
                return FileStatus.DirectoryExists;
            } else {
                return FileStatus.FileExists;
            }
        } else {
            long parentFolderId = findFolderId(FileName.removeFileName(path));
            String fileName = path.substring(path.lastIndexOf(FileName.DOS_SEPERATOR) + 1);
            if (getDLFileEntryByFolderIdAndName(parentFolderId, fileName) != null) {
                return FileStatus.FileExists;
            }
            if (getDLFolderByParentFolderIdAndName(parentFolderId, fileName) != null) {
                return FileStatus.DirectoryExists;
            }
            return FileStatus.NotExist;
        }
    }

    /**
     * Splits a path in Liferay into file/folder name and parent folder path
     * @param path Liferay path to file, no head backslash, no tail backslash
     * @return Array of 2 strings. First is parent folder path or empty string 
     * if file/folder resides in root. Second is file/folder name.
     */
    private static String[] splitLiferayPath(String path) {
        String[] pathComponents = FileName.splitPath(path);
        if (pathComponents[1] == null) {
            pathComponents[1] = pathComponents[0];
            pathComponents[0] = "";
        }
        return pathComponents;
    }

    /**
     * Get the FileInfo object by its full Liferay path
     * @param currentPath Full Liferay path to file, no head backslash, possibly tail backslash
     * @return FileInfo object for given path or null if not found
     */
    public static FileInfo getFileInfo(String currentPath) {
        String path = trimTailBackslash(currentPath);
        if (_fileListCache.containsKey(path)) {
            return _fileListCache.get(path);
        } else {
            long parentFolderId = findParentFolderId(path);
            String[] pathComponents = splitLiferayPath(path);
            String fileName = pathComponents[1];
            String filePath = pathComponents[0];
            DLFileEntrySoap file = getDLFileEntryByFolderIdAndName(parentFolderId, fileName);
            if (file != null) {
                return getFileInfoFromFileEntry(file, parentFolderId, filePath);
            }
            DLFolderSoap folder = getDLFolderByParentFolderIdAndName(parentFolderId, fileName);
            if (folder != null) {
                return getFileInfoFromFolderEntry(folder, parentFolderId, filePath);
            }
            return null;
        }
    }

    /**
     * Get the FileInfo record from a DLFileEntrySoap record
     * @param file The DLFileEntrySoap record
     * @return The FileInfo record
     */
    private static FileInfo getFileInfoFromFileEntry(DLFileEntrySoap file, long parentFolderId, String path) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(getDLFileEntryName(file));
        fileInfo.setFileType(FileType.RegularFile);
        fileInfo.setFileAttributes(0);
        fileInfo.setSize(file.getSize());
        fileInfo.setModifyDateTime(file.getModifiedDate().getTimeInMillis());
        fileInfo.setCreationDateTime(file.getCreateDate().getTimeInMillis());
        fileInfo.setDirectoryId((int) parentFolderId);
        fileInfo.setPath(path);
        fileInfo.setShortName(file.getName());
        return fileInfo;
    }

    /**
     * Get the FileInfo record from a DLFolderSoap record
     * @param folder The DLFolderSoap record
     * @return The FileInfo record
     */
    private static FileInfo getFileInfoFromFolderEntry(DLFolderSoap folder, long parentFolderId, String path) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileName(folder.getName());
        fileInfo.setFileType(FileType.Directory);
        fileInfo.setFileAttributes(FileAttribute.Directory);
        fileInfo.setModifyDateTime(folder.getModifiedDate().getTimeInMillis());
        fileInfo.setCreationDateTime(folder.getCreateDate().getTimeInMillis());
        fileInfo.setDirectoryId((int) parentFolderId);
        fileInfo.setPath(path);
        return fileInfo;
    }

    /**
     * Delete file in Liferay by name
     * @param name Liferay path to file, no head backslash, possibly tail backslash
     */
    public static void deleteFile(String name) {
        String path = trimTailBackslash(name);
        long folderId = findParentFolderId(path);
        String[] pathComponents = splitLiferayPath(path);
        String fileName = pathComponents[1];
        try {
            getDLFileEntryService().deleteFileEntryByTitle(folderId, fileName);            
            if (_fileListCache.containsKey(path)) {
                _fileListCache.remove(path);
            }
        } catch (Exception ex) {
        }
    }

    /**
     * Delete folder in Liferay by name
     * @param name Liferay path to folder, no head backslash, possibly tail backslash
     */
    public static void deleteFolder(String name) {
        String path = trimTailBackslash(name);
        long folderId = findFolderId(path);
        try {
            getDLFolderService().deleteFolder(folderId);            
            if (_fileListCache.containsKey(path)) {
                _fileListCache.remove(path);
            }
        } catch (Exception ex) {
        }
    }

    /**
     * Create folder in Liferay
     * @param name Liferay path to new folder, no head backslash, possibly tail backslash
     */
    public static void createFolder(String name) throws FileNotFoundException {
        String path = trimTailBackslash(name);
        long folderId = findParentFolderId(path);
        if (folderId == -1) {
            throw new FileNotFoundException("LiferayCreateFolder FolderId=-1");
        }
        String[] pathComponents = splitLiferayPath(path);
        String folderName = pathComponents[1];
        String folderPath = pathComponents[0];
        try {
            FileInfo info = getFileInfoFromFolderEntry(getDLFolderService().addFolder(_guestGroupId, folderId, folderName, "", new ServiceContext()), folderId, folderPath);
            _fileListCache.put(path, info);
        } catch (Exception ex) {
        }
    }

    /**
     * Create file in Liferay
     * @param name Liferay path to new file, no head backslash
     * @return FileInfo object describing the new file
     * @throws FileNotFoundException
     */
    public static FileInfo createFile(String name, long size ) throws FileNotFoundException {
        long folderId = findParentFolderId(name);
        if (folderId == -1) {
            throw new FileNotFoundException("LiferayCreateFile FolderId=-1");
        }
        String[] pathComponents = splitLiferayPath(name);
        String fileName = pathComponents[1];
        String filePath = pathComponents[0];
        ServiceContext serviceContext = new ServiceContext();
        serviceContext.setGuestPermissions(new String[]{"VIEW"});
        try {            
            DLFileEntrySoap fileEntry = getDLFileEntryService().addFileEntry(folderId, fileName, fileName, "", "", new byte[1], serviceContext);
            FileInfo info = getFileInfoFromFileEntry(fileEntry, folderId, filePath);
            info.setFileSize(size);
            _fileListCache.put(name, info);
            return info;
        } catch (Exception ex) {
        }
        return null;
    }

    /**
     * Update file content in Liferay
     * @param name Liferay path to updated file, no head backslash
     * @param fileForWrite File object describing the new file
     */
    public static void updateFile(String name, File fileForWrite) {
        long folderId = findParentFolderId(name);
        String[] pathComponents = splitLiferayPath(name);
        String fileName = pathComponents[1];        
        DLFileEntrySoap file = getDLFileEntryByFolderIdAndName(folderId, fileName);
        if (file != null) {
            try {
                if (!writeFile(file.getName(), file.getTitle(), folderId, fileForWrite)) {
                    authorize();
                    writeFile(file.getName(), file.getTitle(), folderId, fileForWrite);
                }
            } catch (Exception ex) {
            }
        }       
    }

    /**
     * Rename/move file in Liferay
     * @param oldName Liferay path to file being renamed/moved, no head backslash
     * @param newName New Liferay path of renamed/moved file
     * @throws IOException
     */
    public static void renameFile(String oldName, String newName) throws IOException {
        long oldFolderId = findParentFolderId(oldName);
        long newFolderId = findParentFolderId(newName);
        String[] oldPathComponents = splitLiferayPath(oldName);
        String oldFileName = oldPathComponents[1];

        String[] newPathComponents = splitLiferayPath(newName);
        String newFileName = newPathComponents[1];
        String newFilePath = newPathComponents[0];

        String sourceFileName = "";
        DLFileEntrySoap file = getDLFileEntryByFolderIdAndName(oldFolderId, oldFileName);
        if (file != null) {
            sourceFileName = file.getName();
            if (oldFolderId != newFolderId) {
                boolean renameSuccess = moveFile(file.getName(), file.getTitle(), oldFolderId, newFolderId);
                if (!renameSuccess) {
                    authorize();
                    renameSuccess = moveFile(file.getName(), file.getTitle(), oldFolderId, newFolderId);
                }
                if (renameSuccess) {
                    if (_fileListCache.containsKey(oldName)) {
                        FileInfo newFileInfo = _fileListCache.get(oldName);
                        newFileInfo.setDirectoryId((int) newFolderId);
                        newFileInfo.setPath(newFilePath);
                        _fileListCache.remove(oldName);
                        _fileListCache.put(newName, newFileInfo);
                    } else {
                        try {
                            DLFileEntrySoap fileEntry = getDLFileEntryByFolderIdAndName(newFolderId, file.getName());
                            _fileListCache.put(newName, getFileInfoFromFileEntry(fileEntry, newFolderId, newName));
                        } catch (Exception ex) {
                            throw new IOException(ex);
                        }
                    }
                }

            } else {
                try {
                    DLFileEntrySoap fileEntry = getDLFileEntryService().updateFileEntry(oldFolderId, newFolderId, sourceFileName, null, newFileName, null, null, null, new ServiceContext());
                    _fileListCache.put(newName, getFileInfoFromFileEntry(fileEntry, newFolderId, newName));
                    if (_fileListCache.containsKey(oldName)) {
                        _fileListCache.remove(oldName);
                    }
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }
        }
    }

    /**
     * Rename/move folder in Liferay
     * @param oldName Liferay path to folder being renamed/moved, no head backslash
     * @param newName New Liferay path of renamed/moved folder
     * @throws IOException
     */
    public static void renameFolder(String oldName, String newName) {
        String oldPath = trimTailBackslash(oldName);
        String newPath = trimTailBackslash(newName);
        long oldFolderId = findFolderId(oldPath);
        long newFolderId = findParentFolderId(newPath);
        String[] newPathComponents = splitLiferayPath(newPath);
        String newFileName = newPathComponents[1];

        try {
            DLFolderServiceSoap folderService = getDLFolderService();
            DLFolderSoap folder = folderService.updateFolder(oldFolderId, newFolderId, newFileName, "", new ServiceContext());
            if (_fileListCache.containsKey(oldPath)) {
                _fileListCache.remove(oldPath);
            }
            _fileListCache.put(newPath, getFileInfoFromFolderEntry(folder, newFolderId, newPath));
        } catch (Exception ex) {
        }
    }

    /**
     * Download a copy of a file from Liferay to local disk cache
     * @param name Liferay path to file, no head backslash
     * @return File object pointing to the local copy
     */
    public static File readFile(String name) {
        long folderId = findParentFolderId(name);
        String[] pathComponents = splitLiferayPath(name);
        String fileName = pathComponents[1];       
        DLFileEntrySoap file = getDLFileEntryByFolderIdAndName(folderId, fileName);
        if (file != null) {
            File outputFile = null;
            boolean getFileError = false;
            try {
                outputFile = getFileFromLiferay("http://" + _storageRootUrl + "/c/document_library/get_file?uuid=" + file.getUuid() + "&groupId=" + _guestGroupId, file.getName());
            } catch (java.io.IOException ex) {
                getFileError = true;
            }
            if (getFileError) {
                try {
                    authorize();
                    outputFile = getFileFromLiferay("http://" + _storageRootUrl + "/c/document_library/get_file?uuid=" + file.getUuid() + "&groupId=" + _guestGroupId, file.getName());
                } catch (Exception ex) {
                }
            }
            return outputFile;
        }
        return null;
    }

    /**
     * Get the name of the temporary local disk file to create
     * @param name Liferay path to file, no head backslash
     * @return Local disk file name beign the Liferay internal short name with extension 
     * with a "write" prefix
     */
    public static String getFileNameForWrite(String name) {
        long folderId = findParentFolderId(name);
        String[] pathComponents = splitLiferayPath(name);
        String fileName = pathComponents[1];
        DLFileEntrySoap file = getDLFileEntryByFolderIdAndName(folderId, fileName);
        if (file != null) {
            return "write" + file.getName();
        }
        return null;
    }

    /**
     * Cached reference to the DLFileEntryServiceSoap
     */
    private static DLFileEntryServiceSoap _documentService;

    /**
     * Get a reference to the DLFileEntryServiceSoap
     * @return
     * @throws Exception
     */
    private static DLFileEntryServiceSoap getDLFileEntryService() throws Exception {
        if (_documentService == null) {
            _documentService = new DLFileEntryServiceSoapServiceLocator().getPortlet_DL_DLFileEntryService(getURL("Portlet_DL_DLFileEntryService"));
        }
        return _documentService;
    }

    /**
     * Cached reference to the DLFolderServiceSoap
     */
    private static DLFolderServiceSoap _folderService;

    /**
     * Get a reference to the DLFolderServiceSoap
     * @return
     * @throws Exception
     */
    private static DLFolderServiceSoap getDLFolderService() throws Exception {
        if (_folderService == null) {
            _folderService = new DLFolderServiceSoapServiceLocator().getPortlet_DL_DLFolderService(getURL("Portlet_DL_DLFolderService"));
        }
        return _folderService;
    }

    /**
     * Get all Liferay file entries in a folder with a given Id
     * @param folderId Id of the folder to seach files in
     * @return Array of DLFileEntrySoap objects describing the files in the folder
     * @throws Exception
     */
    private static DLFileEntrySoap[] getDLFileEntriesByFolderId(long folderId) throws Exception {
        return getDLFileEntryService().getFileEntries(folderId);
    }

    /**
     * Get all Liferay folders in a parent folder with a given Id
     * @param folderId Id of the folder to search subfolders in (0 for root folder)
     * @return Array of DLFolderSoap objects describing the subfolders in the parent folder
     * @throws Exception
     */
    private static DLFolderSoap[] getDLFoldersByParentFolderId(long folderId) throws Exception {
        return getDLFolderService().getFolders(_guestGroupId, folderId);
    }

    /**
     * Get the web service URL for given service relative path
     * @param serviceName Service name (relative URL)
     * @return Particular web service URL
     * @throws Exception
     */
    private static URL getURL(String serviceName) throws Exception {
        return new URL("http://" + _userId + ":" + _password + "@" + _storageRootUrl + "/tunnel-web/secure/axis/" + serviceName);
    }

    /**
     * Simulate browser behavior when signing in to Liferay
     * @throws MalformedURLException
     * @throws IOException
     */
    private static void authorize() throws MalformedURLException, IOException {
        HttpURLConnection.setFollowRedirects(false);
        URL url = new URL("http://" + _storageRootUrl + "/web/guest/home?p_p_id=58&p_p_lifecycle=1&p_p_state=normal&p_p_mode=view&p_p_col_id=column-1&p_p_col_pos=1&p_p_col_count=2&saveLastPath=0&_58_struts_action=%2Flogin%2Flogin");
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestMethod("POST");
        urlConn.setRequestProperty("Host", _storageRootUrl);
        urlConn.setRequestProperty("Cookie", "COOKIE_SUPPORT=true");
        urlConn.connect();
        DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
        String content =
                "_58_login=" + URLEncoder.encode(_user) +
                "&_58_password=" + URLEncoder.encode(_password) +
                "&_58_redirect" + URLEncoder.encode("") +
                "&_58_rememberMe" + URLEncoder.encode("false");
        printout.writeBytes(content);
        printout.flush();
        printout.close();
        int headerKeyIndex = 0;
        String headerKey = urlConn.getHeaderFieldKey(headerKeyIndex);
        Map<String, List<String>> headers = urlConn.getHeaderFields();
        String cookies = "COOKIE_SUPPORT=true; ";
        _cookiesMap = new HashMap();
        while (headerKey != null || headerKeyIndex == 0) {
            for (String headerValue : headers.get(headerKey)) {
                if (headerKey != null && headerKey.equals("Set-Cookie")) {
                    cookies = cookies + headerValue.substring(0, headerValue.indexOf(";")) + "; ";
                    try {
                        _cookiesMap.put(headerValue.substring(0, headerValue.indexOf("=")), headerValue.substring(headerValue.indexOf("=") + 1, headerValue.indexOf(";")));
                    } catch (Exception ex) {
                        throw new UnsupportedOperationException(headerValue);
                    } finally {
                    }
                }
            }
            String previousHeaderKey = headerKey;
            headerKey = urlConn.getHeaderFieldKey(++headerKeyIndex);
            while (headerKey != null && headerKey.equals(previousHeaderKey)) {
                headerKey = urlConn.getHeaderFieldKey(++headerKeyIndex);
            }
        }
        urlConn.disconnect();
        _cookies = cookies;
    }

    /**
     * Silulate browser behavior when downloading files
     * @param fileUrl Download URL of the file
     * @param name Name of file to create a local temporary file (use the Liferay internal name with extension)
     * @return File object pointing to the local temporary file just created
     * @throws MalformedURLException
     * @throws IOException
     */
    private static File getFileFromLiferay(String fileUrl, String name) throws MalformedURLException, IOException {
        URL url = new URL(fileUrl);
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestMethod("GET");
        urlConn.setRequestProperty("Host", _storageRootUrl);
        urlConn.setRequestProperty("Cookie", _cookies);
        urlConn.connect();
        urlConn.disconnect();
        DataInputStream input = new DataInputStream(urlConn.getInputStream());
        FileOutputStream str = new FileOutputStream("read" + name);
        byte[] buf = new byte[4 * 1024]; // 4K buffer
        int bytesRead;
        while ((bytesRead = input.read(buf)) != -1) {
            str.write(buf, 0, bytesRead);
        }
        input.close();
        str.close();
        return new File("read" + name);
    }

    /**
     * Silulate browser behavior when moving a file in Liferay
     * @param name Short Liferay file name ("internal-name.ext")
     * @param title Title of the file (user-friendly name without path and extension)
     * @param folderId Id of the folder which contained the file
     * @param newFolderId Id of the new folder to contain the file
     * @return True if move was successful, false otherwise
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     */
    private static boolean moveFile(String name, String title, long folderId, long newFolderId) throws FileNotFoundException, MalformedURLException, IOException {
        URL url;
        HttpURLConnection urlConn;
        HttpURLConnection.setFollowRedirects(false);
        prepareFile(folderId, name);
        url = new URL("http://" + _storageRootUrl + "/web/guest/home?p_p_id=20&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&_20_struts_action=%2Fdocument_library%2Fedit_file_entry");
        ClientHttpRequest client = new ClientHttpRequest(url.openConnection());
        client.setCookies(_cookiesMap);
        client.postCookies();
        client.setParameter("_20_cmd", URLEncoder.encode("update"));
        client.setParameter("_20_redirect", "/web/guest/home?p_p_id=20&p_p_lifecycle=0&p_p_state=maximized&p_p_mode=view&_20_struts_action=%2Fdocument_library%2Fview&_20_folderId=" + folderId);
        client.setParameter("_20_referringPortletResource", URLEncoder.encode(""));
        client.setParameter("_20_uploadProgressId", URLEncoder.encode("dlFileEntryUploadProgress"));
        client.setParameter("_20_folderId", URLEncoder.encode(Long.toString(folderId)));
        client.setParameter("_20_newFolderId", URLEncoder.encode(Long.toString(newFolderId)));
        client.setEmptyFileParameter("_20_file");
        client.setParameter("_20_title", title);
        client.setParameter("_20_description", "");
        client.setParameter("_20_tagsEntries", "");
        client.setParameter("_20_name", URLEncoder.encode(name));
        urlConn = (HttpURLConnection) client.post();
        return urlConn.getResponseCode() == 302;
    }

    /**
     * Simulate browser behavior when posting new file content
     * @param name Short Liferay file name ("internal-name.ext")
     * @param title Title of the file (user-friendly name without path and extension)
     * @param folderId Id of the folder which contains the file
     * @param fileForWrite File object pointing to the temporary file on local disk with needed content
     * @return True if write was successful, false otherwise
     * @throws FileNotFoundException
     * @throws MalformedURLException
     * @throws IOException
     */
    private static boolean writeFile(String name, String title, long folderId, File fileForWrite) throws FileNotFoundException, MalformedURLException, IOException {
        URL url;
        HttpURLConnection urlConn;
        HttpURLConnection.setFollowRedirects(false);
        prepareFile(folderId, name);
        url = new URL("http://" + _storageRootUrl + "/web/guest/home?p_p_id=20&p_p_lifecycle=1&p_p_state=maximized&p_p_mode=view&_20_struts_action=%2Fdocument_library%2Fedit_file_entry");
        ClientHttpRequest client = new ClientHttpRequest(url);
        client.setCookies(_cookiesMap);
        String fileName = title + (name.lastIndexOf(".") > 0 ? name.substring(name.lastIndexOf(".")) : "");
        client.postCookies();
        client.setParameter("_20_cmd", URLEncoder.encode("update"));
        client.setParameter("_20_redirect", "/web/guest/home?p_p_id=20&p_p_lifecycle=0&p_p_state=maximized&p_p_mode=view&_20_struts_action=%2Fdocument_library%2Fview&_20_folderId=" + folderId);
        client.setParameter("_20_referringPortletResource", URLEncoder.encode(""));
        client.setParameter("_20_uploadProgressId", URLEncoder.encode("dlFileEntryUploadProgress"));
        client.setParameter("_20_folderId", URLEncoder.encode(Long.toString(folderId)));
        client.setParameter("_20_newFolderId", URLEncoder.encode(""));
        client.setParameter("_20_file", fileName, new FileInputStream(fileForWrite));
        client.setParameter("_20_title", title);
        client.setParameter("_20_description", "");
        client.setParameter("_20_tagsEntries", "");
        client.setParameter("_20_name", /*URLEncoder.encode(*/ name/*)*/);
        urlConn = (HttpURLConnection) client.post();
        if (fileForWrite.exists()) {
            fileForWrite.delete();
        }
        return urlConn.getResponseCode() == 302;
    }

    /**
     * Simulate opening a file editing form in Liferay in browser
     * @param folderId Id of folder which contains the file
     * @param fileName Short Liferay file name ("internal-name.ext")
     * @throws MalformedURLException
     * @throws IOException
     */
    private static void prepareFile(long folderId, String fileName) throws MalformedURLException, IOException {
        URL url;
        HttpURLConnection urlConn;
        DataOutputStream printout;
        HttpURLConnection.setFollowRedirects(false);
        url = new URL("http://" + _storageRootUrl + "/web/guest/home?p_p_id=20&p_p_lifecycle=0&p_p_state=maximized&p_p_mode=view&_20_struts_action=%2Fdocument_library%2Fedit_file_entry&_20_redirect=%2Fweb%2Fguest%2Fhome%3F_20_struts_action%3D%252Fdocument_library%252Fview%26p_p_lifecycle%3D0%26_20_folderId%3D10322%26p_p_mode%3Dview%26p_p_id%3D20%26p_p_state%3Dmaximized&_20_folderId=" + folderId + "&_20_name=" + fileName);
        urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestMethod("POST");
        urlConn.setRequestProperty("Host", _storageRootUrl);
        urlConn.setRequestProperty("Cookie", _cookies);
        urlConn.setRequestProperty("CONTENT-TYPE", "application/x-www-form-urlencoded");
        urlConn.connect();
        printout = new DataOutputStream(urlConn.getOutputStream());
        String content = " ";
        printout.writeBytes(content);
        printout.flush();
        printout.close();
        urlConn.getResponseCode();
        urlConn.disconnect();
    }

    /**
     * Remove the tailing backslash from the path string if there is one
     * @param currentPath
     * @return
     */
    private static String trimTailBackslash(String currentPath) {
        String path = currentPath;
        if (path.endsWith("\\")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }
}
