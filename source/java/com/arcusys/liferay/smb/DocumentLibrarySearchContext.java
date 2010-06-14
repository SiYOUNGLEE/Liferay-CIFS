/* 
 * Copyright © 2010 Arcusys Ltd. - http://www.arcusys.fi/
 * 
 * This file is part of "LiferayCIFS".
 *
 * "LiferayCIFS" is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * "LiferayCIFS" is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with "LiferayCIFS".  If not, see <http://www.gnu.org/licenses/>.
 */

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
	 * Prepare a file list for searching. Invoked from
	 * DocumentLibraryDiskDriver.startSearch()
	 * 
	 * @param path Full search path in Liferay, possibly with Wildcards. No
	 *            leading backslash. Possibly tail backslash
	 * @param attr
	 * @throws java.io.FileNotFoundException
	 */
	public final void initSearch(String path, int attr)
		throws java.io.FileNotFoundException {

		// try{
		// FileWriter fstream = new FileWriter("initSearch.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("path:"+path);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }

		_m_currentFileIndex = 0;
		String fileName = "";
		String filePath = "";
		String searchPath = path;
		if (searchPath.endsWith("\\")) {
			searchPath = searchPath.substring(0, searchPath.length() - 2);
		}
		int pos = searchPath.lastIndexOf("\\");
		if (pos == -1) {
			fileName = searchPath;
		}
		else {
			fileName = searchPath.substring(pos + 1);
			filePath = searchPath.substring(0, pos);
		}
		setSearchString(fileName);
		if (filePath.equals("")) {
			_m_rootId = 0;
		}
		else {
			_m_rootId = LiferayStorage.findFolderId(filePath);
		}
		_m_isSingleFileSearch = !WildCard.containsWildcards(fileName);

		// Filename, no wildcards

		if (_m_isSingleFileSearch) {
			_m_singleFileInfo = LiferayStorage.getFileInfo(path);
			if (_m_singleFileInfo == null) {
				throw new java.io.FileNotFoundException(path);
			}
		}
		else {

			// Wildcard

			try {
				_m_fileList = LiferayStorage.getFileList(_m_rootId, filePath);
			}
			catch (Exception ex) {
			}
			_m_wildcard = new WildCard(fileName, false);
		}
	}

	/**
	 * Retrieve the current search resume ID (file index in list). Invoked by
	 * JLAN
	 * 
	 * @return Index of current file in list
	 */
	@Override
	public int getResumeId() {

		// try{
		// FileWriter fstream = new FileWriter("getResumeId.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("m_currentFileIndex:"+m_currentFileIndex);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }

		return _m_currentFileIndex;
	}

	/**
	 * Check if there are more files in the search list after the current one.
	 * Invoked by JLAN
	 * 
	 * @return True if there are more files in the search list; false otherwise
	 */
	@Override
	public boolean hasMoreFiles() {
		boolean result = true;
		if (_m_isSingleFileSearch && _m_currentFileIndex > 0) {
			result = false;
		}
		else if (_m_fileList != null && _m_currentFileIndex >=
					_m_fileList.size()) {
			result = false;
		}

		// try{
		// FileWriter fstream = new FileWriter("hasMoreFile.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("m_isSingleFileSearch:"+m_isSingleFileSearch+
		// " m_currentFileIndex"+m_currentFileIndex);
		// out.newLine();
		// out.write("result:"+result);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }
		// boolean result =true;

		return result;
	}

	/**
	 * Move to the next file in the search list and provide the appropriate
	 * information. Invoked by JLAN
	 * 
	 * @param info The FileInfo structure to populate with data regarding the
	 *            next file in the list
	 * @return True is it was possible to retrieve the next file info; false
	 *         otherwise
	 */
	@Override
	public boolean nextFileInfo(FileInfo info) {

		// try{
		// FileWriter fstream = new FileWriter("nextFileInfo.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }

		boolean infoValid = false;
		if (_m_isSingleFileSearch) {
			if (_m_currentFileIndex == 0) {
				_m_currentFileIndex++;
				if (_m_singleFileInfo == null) {
				}
				else if (_m_singleFileInfo.isDirectory()) {
					info.setChangeDateTime(
						_m_singleFileInfo.getChangeDateTime());
					
					// storage.GetFolderInfo(rootId,
					// fileList[currentFileIndex].Title);
					
					info.setCreationDateTime(
						_m_singleFileInfo.getCreationDateTime());
					info.setFileName(_m_singleFileInfo.getFileName());
					info.setFileAttributes(FileAttribute.Directory);
				}
				else {
					info.setFileName(_m_singleFileInfo.getFileName());
					info.setSize(_m_singleFileInfo.getSize());
					
						// currentFileInfo.getAllocationSize());
					
					info.setModifyDateTime(
						_m_singleFileInfo.getModifyDateTime());
					
						// (int)currentFileInfo.getChangeDateTime());
					
					info.setCreationDateTime(
						_m_singleFileInfo.getCreationDateTime());
					info.setFileAttributes(0);
				}
				info.setFileId(_m_currentFileIndex);
				infoValid = true;
				return infoValid;
			}
		}
		else if (_m_fileList != null && _m_currentFileIndex <
						_m_fileList.size()) {
			FileInfo currentInfo = _m_fileList.get(_m_currentFileIndex);
			if (currentInfo.isDirectory()) {
				info.setChangeDateTime(currentInfo.getChangeDateTime());
				info.setCreationDateTime(currentInfo.getCreationDateTime());
				info.setFileName(
					_m_fileList.get(_m_currentFileIndex).getFileName());
				info.setFileAttributes(FileAttribute.Directory);
			}
			else {
				info.setFileName(
					_m_fileList.get(_m_currentFileIndex).getFileName());
				info.setSize(currentInfo.getSize());
				info.setModifyDateTime(currentInfo.getModifyDateTime());
				info.setCreationDateTime(currentInfo.getCreationDateTime());
				info.setFileAttributes(0);
			}
			info.setFileId(_m_currentFileIndex);
			infoValid = true;
			_m_currentFileIndex++;
		}
		return infoValid;
	}

	/**
	 * Move to the next file in the search list and return its name. Invoked by
	 * JLAN (in some cases of FTP access)
	 * 
	 * @return Name of the next file in the search list of null if the list is
	 *         over
	 */
	@Override
	public String nextFileName() {

		// try{
		// FileWriter fstream = new FileWriter("nextFileName.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }

		if (_m_isSingleFileSearch) {
			if (_m_currentFileIndex == 0) {
				_m_currentFileIndex++;
				return _m_singleFileInfo.getFileName();
			}
			else {
				return null;
			}
		} 
		
		// Return the next file name from the list
		
		else if (_m_fileList != null && _m_currentFileIndex <
						_m_fileList.size()) {
			while (_m_currentFileIndex < _m_fileList.size()) {
				
				// Check if the current file name matches the search pattern
				
				FileInfo info = _m_fileList.get(_m_currentFileIndex++);
				String fname = info.getPath() + info.getFileName();
				if (_m_wildcard.matchesPattern(fname)) {
					return fname;
				}
			}
		}
		return null;
	}

	/**
	 * Move the search position to the provided one. Invoked by JLAN
	 * 
	 * @param resumeId The position of file in the search list
	 * @return True if it was possible to position the search pointer; false
	 *         otherwise
	 */
	@Override
	public boolean restartAt(int resumeId) {

		// try{
		// FileWriter fstream = new FileWriter("restartAt.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("resumeId:"+resumeId);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }

		if (resumeId >= _m_fileList.size()) {
			return false;
		}
		else {
			_m_currentFileIndex = resumeId;
			return true;
		}
	}

	/**
	 * Move the search position back to the provided FileInfo. Invoked by JLAN
	 * 
	 * @param info FileInfo to position to
	 * @return True if it was possible to find the given FileInfo in the search
	 *         list while stepping backwards; false otherwise
	 */
	@Override
	public boolean restartAt(FileInfo info) {

		// try{
		// FileWriter fstream = new FileWriter("restartAt.txt",true);
		// BufferedWriter out = new BufferedWriter(fstream);
		// out.newLine();
		// out.write("________");
		// out.newLine();
		// out.write("m_currentFileIndex:"+m_currentFileIndex);
		// out.close();
		// } catch (Exception e) {
		// System.err.println("Error: " + e.getMessage());
		// }

		boolean restartOK = false;
		if (_m_currentFileIndex >= _m_fileList.size()) {
			return restartOK;
		}
		while (_m_currentFileIndex > 0 && restartOK == false) {

			// Check if we found the restart file

			if ((_m_fileList.get(_m_currentFileIndex).getFileName())
							.equals(info.getFileName())) {
				restartOK = true;
			}
			else {
				_m_currentFileIndex--;
			}
		}
		return restartOK;
	}

	/**
	 * The search list. Populated during search initialization in case a
	 * wildcard search is done
	 */
	private ArrayList<FileInfo> _m_fileList;
	/**
	 * Current file index in search list
	 */
	private int _m_currentFileIndex;
	/**
	 * True if a single file search is done, false if wildcard search is done
	 */
	private boolean _m_isSingleFileSearch;
	/**
	 * Wildcard matcher if wildcard search is done
	 */
	private WildCard _m_wildcard;
	/**
	 * FileInfo for the sinlge file search
	 */
	private FileInfo _m_singleFileInfo;
	/**
	 * Liferay parent folder ID for wildcard search
	 */
	private long _m_rootId;
	
}
