package com.arcusys.liferay.smb;

import java.util.ArrayList;
import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.SearchContext;
import org.alfresco.jlan.util.WildCard;

/**
 * Search context for resumable search
 */
public class DocumentLibrarySearchContext extends SearchContext {

    /**
     * The search list. Populated during search initialization in case a wildcard search is done
     */
    private ArrayList<FileInfo> m_fileList;
    /**
     * Current file index in search list
     */
    private int m_currentFileIndex;
    /**
     * True if a single file search is done, false if wildcard search is done
     */
    private boolean m_isSingleFileSearch;
    /**
     * Wildcard matcher if wildcard search is done
     */
    private WildCard m_wildcard;
    /**
     * FileInfo for the sinlge file search
     */
    private FileInfo m_singleFileInfo;
    /**
     * Liferay parent folder ID for wildcard search
     */
    private long m_rootId;

    /**
     * Prepare a file list for searching. Invoked from DocumentLibraryDiskDriver.startSearch()
     * @param path Full search path in Liferay, possibly with Wildcards. No leading backslash.
     * Possibly tail backslash
     * @param attr
     * @throws java.io.FileNotFoundException
     */
    public final void initSearch(String path, int attr) throws java.io.FileNotFoundException {
        m_currentFileIndex = 0;
        String fileName = "";
        String filePath = "";
        String searchPath = path;
        if (searchPath.endsWith("\\")) {searchPath = searchPath.substring(0, searchPath.length()-2);}
        int pos = searchPath.lastIndexOf("\\");
        if (pos == -1)
        {
            fileName = searchPath;
        }
        else
        {
            fileName = searchPath.substring(pos+1);
            filePath = searchPath.substring(0,pos);
        }                
        setSearchString(fileName);        
        if (filePath.equals("")) {
            m_rootId = 0;
        } else {
            m_rootId = LiferayStorage.findFolderId(filePath);
        }
        m_isSingleFileSearch = !WildCard.containsWildcards(fileName);
        //Filename, no wildcards
        if (m_isSingleFileSearch) {
            m_singleFileInfo = LiferayStorage.getFileInfo(path);
            if (m_singleFileInfo ==null)
            {
                throw new java.io.FileNotFoundException(path);
            }
        } else {
            //Wildcard
            try {
                m_fileList = LiferayStorage.getFileList(m_rootId, filePath);
            } catch (Exception ex) {
            }            
            m_wildcard = new WildCard(fileName, false);
        }
    }

    /**
     * Retrieve the current search resume ID (file index in list). Invoked by JLAN
     * @return Index of current file in list
     */
    @Override
    public int getResumeId() {
        return m_currentFileIndex;
    }

    /**
     * Check if there are more files in the search list after the current one. Invoked by JLAN
     * @return True if there are more files in the search list; false otherwise
     */
    @Override
    public boolean hasMoreFiles() {
     boolean result =true;
        if (m_isSingleFileSearch && m_currentFileIndex > 0) {
            result= false;
        } else if (m_fileList != null && m_currentFileIndex >= m_fileList.size()) {
            result =  false;
        }
        return result;
    }

    /**
     * Move to the next file in the search list and provide the appropriate information.
     * Invoked by JLAN
     * @param info The FileInfo structure to populate with data regarding the next file in the list
     * @return True is it was possible to retrieve the next file info; false otherwise
     */
    @Override
    public boolean nextFileInfo(FileInfo info) {
        boolean infoValid = false;
        if (m_isSingleFileSearch) {
            if (m_currentFileIndex == 0) {
                m_currentFileIndex++;
                if (m_singleFileInfo == null) {
                } else if (m_singleFileInfo.isDirectory()) {
                    info.setChangeDateTime(m_singleFileInfo.getChangeDateTime());//storage.GetFolderInfo(rootId, fileList[currentFileIndex].Title);
                    info.setCreationDateTime(m_singleFileInfo.getCreationDateTime());
                    info.setFileName(m_singleFileInfo.getFileName());
                    info.setFileAttributes(FileAttribute.Directory);
                } else {
                    info.setFileName(m_singleFileInfo.getFileName());
                    info.setSize(m_singleFileInfo.getSize());//currentFileInfo.getAllocationSize());
                    info.setModifyDateTime(m_singleFileInfo.getModifyDateTime());//(int)currentFileInfo.getChangeDateTime());
                    info.setCreationDateTime(m_singleFileInfo.getCreationDateTime());
                    info.setFileAttributes(0);
                }
                info.setFileId(m_currentFileIndex);
                infoValid = true;
                return infoValid;
            }
        } else if (m_fileList != null && m_currentFileIndex < m_fileList.size()) {
            FileInfo currentInfo = m_fileList.get(m_currentFileIndex);
            if (currentInfo.isDirectory()) {
                info.setChangeDateTime(currentInfo.getChangeDateTime());
                info.setCreationDateTime(currentInfo.getCreationDateTime());
                info.setFileName(m_fileList.get(m_currentFileIndex).getFileName());
                info.setFileAttributes(FileAttribute.Directory);
            } else {
                info.setFileName(m_fileList.get(m_currentFileIndex).getFileName());
                info.setSize(currentInfo.getSize());
                info.setModifyDateTime(currentInfo.getModifyDateTime());
                info.setCreationDateTime(currentInfo.getCreationDateTime());
                info.setFileAttributes(0);
            }
            info.setFileId(m_currentFileIndex);
            infoValid = true;
            m_currentFileIndex++;
        }
        return infoValid;
    }

    /**
     * Move to the next file in the search list and return its name.
     * Invoked by JLAN (in some cases of FTP access)
     * @return Name of the next file in the search list of null if the list is over
     */
    @Override
    public String nextFileName() {
        if (m_isSingleFileSearch) {
            if (m_currentFileIndex == 0) {
                m_currentFileIndex++;
                return m_singleFileInfo.getFileName();
            } else {
                return null;
            }
        } //  Return the next file name from the list
        else if (m_fileList != null && m_currentFileIndex < m_fileList.size()) {
            while (m_currentFileIndex < m_fileList.size()) {
                // Check if the current file name matches the search pattern
                FileInfo info = m_fileList.get(m_currentFileIndex++);
                String fname = info.getPath() + info.getFileName();
                if (m_wildcard.matchesPattern(fname)) {
                    return fname;
                }
            }
        }
        return null;
    }

    /**
     * Move the search position to the provided one. Invoked by JLAN
     * @param resumeId The position of file in the search list
     * @return True if it was possible to position the search pointer; false otherwise
     */
    @Override
    public boolean restartAt(int resumeId) {
        if (resumeId >= m_fileList.size()) {
            return false;
        } else {
            m_currentFileIndex = resumeId;
            return true;
        }
    }

    /**
     * Move the search position back to the provided FileInfo. Invoked by JLAN
     * @param info FileInfo to position to
     * @return True if it was possible to find the given FileInfo
     * in the search list while stepping backwards; false otherwise
     */
    @Override
    public boolean restartAt(FileInfo info) {
        boolean restartOK = false;
        if (m_currentFileIndex >= m_fileList.size()) {
            return restartOK;
        }
        while (m_currentFileIndex > 0 && restartOK == false) {
            //  Check if we found the restart file
            if ((m_fileList.get(m_currentFileIndex).getFileName()).equals(info.getFileName())) {
                restartOK = true;
            } else {
                m_currentFileIndex--;
            }
        }
        return restartOK;
    }
}
