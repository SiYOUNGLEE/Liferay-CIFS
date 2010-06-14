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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.rpc.ServiceException;

import org.alfresco.jlan.server.filesys.FileAttribute;
import org.alfresco.jlan.server.filesys.FileInfo;
import org.alfresco.jlan.server.filesys.FileName;
import org.alfresco.jlan.server.filesys.FileStatus;
import org.alfresco.jlan.server.filesys.FileType;

/**
 * Proxy to Liferay portal document library & utulity operations
 */
public class LiferayStorage {

	/**
	 * Store the connection credentials and detect the "Guest" group ID
	 * 
	 * @param url Liferay root URL
	 * @param userId Liferay user ID
	 * @param password Liferay user password
	 */
	public static void initStorage(
		String url, String user, String userId, String password) {

		_storageRootUrl = url.replaceFirst("http://", "");// +
		
		// "/tunnel-web/secure/axis/";
		
		try {
			URL liferayURL = new URL(url);
			_password = password;
			_userId = Integer.parseInt(userId);
			_user = user;
			_fileListCache = new Hashtable<String, FileInfo>();
			_cookies = "";

			CompanyServiceSoapServiceLocator companyLocator =
				new CompanyServiceSoapServiceLocator();
			CompanyServiceSoap companyService =
				companyLocator.getPortal_CompanyService(
					_getURL("Portal_CompanyService"));
			long companyId =
				companyService.getCompanyByVirtualHost(
					liferayURL.getHost()).getCompanyId();
			GroupServiceSoapServiceLocator groupLocator =
				new GroupServiceSoapServiceLocator();
			GroupServiceSoap groupService =
				groupLocator.getPortal_GroupService(
					_getURL("Portal_GroupService"));
			_guestGroupId =
				groupService.getGroup(companyId, "Guest").getGroupId();
		}
		catch (Exception e) {
		}
	}

	public static FileInfo createFile(String name)
		throws FileNotFoundException {

		long folderId = findParentFolderId(name);
		if (folderId == -1) {
			throw new FileNotFoundException("LiferayCreateFile FolderId=-1:");
		}
		String[] pathComponents = FileName.splitPath(name);
		String fileName = pathComponents[1];
		String filePath = pathComponents[0];

		FileInfo info = null;
		if (fileName == null) {
			fileName = filePath;
			filePath = "";
		}
		com.liferay.client.soap.portal.service.ServiceContext serviceContext;
		serviceContext =
			new com.liferay.client.soap.portal.service.ServiceContext();
		try {
			DLFileEntryServiceSoapServiceLocator documentLocator =
				new DLFileEntryServiceSoapServiceLocator();
			DLFileEntryServiceSoap documentService =
				documentLocator.getPortlet_DL_DLFileEntryService(
					_getURL("Portlet_DL_DLFileEntryService"));
			serviceContext.setGuestPermissions(new String[] {
				"VIEW"
			});
			
			// .setGuestPermissions(new String[]{"qwe"});
			
			DLFileEntrySoap fileEntry = null;
			fileEntry =
				documentService.addFileEntry(
					folderId, fileName, fileName, "test", "test", new byte[10],
					serviceContext);
			info = _getFileInfoFromFileEntry(fileEntry, folderId, filePath);
			_fileListCache.put(name, info);
			return info;
		}
		catch (Exception ex) {
		}
		return null;
	}

	public static void createFolder(String name) {
		long folderId = findParentFolderId(name);
		String[] pathComponents = FileName.splitPath(name);
		String folderName = pathComponents[1];
		String folderPath = pathComponents[0];
		if (folderName == null) {
			folderName = folderPath;
			folderPath = "";
		}
		FileInfo info = null;
		com.liferay.client.soap.portal.service.ServiceContext serviceContext;
		serviceContext =
			new com.liferay.client.soap.portal.service.ServiceContext();
		try {
			DLFolderServiceSoapServiceLocator directoryLocator =
				new DLFolderServiceSoapServiceLocator();
			DLFolderServiceSoap directoryService =
				directoryLocator.getPortlet_DL_DLFolderService(
					_getURL("Portlet_DL_DLFolderService"));
			info =
				_getFileInfoFromFolderEntry(
					directoryService.addFolder(
						_guestGroupId, folderId, folderName, "", 
						serviceContext),
					folderId, folderPath);

			_fileListCache.put(name, info);
		}
		catch (Exception ex) {
		}
	}

	public static void deleteFile(String name) {
		long folderId = findParentFolderId(name);
		String[] pathComponents = FileName.splitPath(name);
		String fileName = pathComponents[1];
		String filePath = pathComponents[0];
		if (fileName == null) {
			fileName = filePath;
			filePath = "";
		}
		try {
			DLFileEntryServiceSoapServiceLocator documentLocator =
				new DLFileEntryServiceSoapServiceLocator();
			DLFileEntryServiceSoap documentService =
				documentLocator.getPortlet_DL_DLFileEntryService(
					_getURL("Portlet_DL_DLFileEntryService"));
			documentService.deleteFileEntryByTitle(folderId, fileName);

			if (name.endsWith("\\")) {
				name = name.substring(0, name.length() - 1);
			}

			if (_fileListCache.containsKey(name)) {
				_fileListCache.remove(name);
			}
		}
		catch (Exception ex) {
		}
	}

	public static void deleteFolder(String name) {
		long folderId = findFolderId(name);
		try {
			DLFolderServiceSoapServiceLocator directoryLocator =
				new DLFolderServiceSoapServiceLocator();
			DLFolderServiceSoap directoryService =
				directoryLocator.getPortlet_DL_DLFolderService(
					_getURL("Portlet_DL_DLFolderService"));
			directoryService.deleteFolder(folderId);
			if (name.endsWith("\\")) {
				name = name.substring(0, name.length() - 1);
			}
			if (_fileListCache.containsKey(name)) {
				_fileListCache.remove(name);
			}
		}
		catch (Exception ex) {
		}
	}

	/**
	 * Check if a file of folder exists in Liferay storage
	 * 
	 * @param path Full file/folder name in Liferay with path starting with
	 *            topmost folder name without leading backslash or period
	 * @return FileStatus.DirectoryExists if file exists and is a Liferay
	 *         folder; FileStatus.FileExists if file exists and is a Liferay
	 *         file; FileStatus.NotExist if file does not exist in Liferay
	 */
	public static int fileExists(String path) {
		if (_fileListCache.containsKey(path)) {
			if (_fileListCache.get(path).isDirectory()) {
				return FileStatus.DirectoryExists;
			}
			else {
				return FileStatus.FileExists;
			}
		}
		else {
			long parentFolderId = findFolderId(FileName.removeFileName(path));
			String fileName =
				path.substring(path.lastIndexOf(FileName.DOS_SEPERATOR) + 1);
			try {
				DLFileEntryServiceSoapServiceLocator documentLocator =
					new DLFileEntryServiceSoapServiceLocator();
				DLFileEntryServiceSoap documentService =
					documentLocator.getPortlet_DL_DLFileEntryService(
						_getURL("Portlet_DL_DLFileEntryService"));
				DLFileEntrySoap[] fileList =
					documentService.getFileEntries(parentFolderId);
				for (DLFileEntrySoap file : fileList) {
					if ((file.getTitle() + file.getName().substring(
						file.getName().lastIndexOf("."))).equals(fileName)) {
						return FileStatus.FileExists;
					}
				}
			}
			catch (Exception e) {
			}
			try {
				DLFolderServiceSoapServiceLocator directoryLocator =
					new DLFolderServiceSoapServiceLocator();
				DLFolderServiceSoap directoryService =
					directoryLocator.getPortlet_DL_DLFolderService(
						_getURL("Portlet_DL_DLFolderService"));
				DLFolderSoap[] folderList =
					directoryService.getFolders(_guestGroupId, parentFolderId);
				for (DLFolderSoap folder : folderList) {
					if (folder.getName().equals(fileName)) {
						return FileStatus.DirectoryExists;
					}
				}
			}
			catch (Exception e) {
			}
			return FileStatus.NotExist;
		}
	}

	/**
	 * Get Liferay folder ID by folder name
	 * 
	 * @param path Full path to folder in Liferay, no backslashes at head or
	 *            tail
	 * @return Liferay folder ID. 0 for root folder. -1 if not found
	 */
	public static long findFolderId(String path) {

		String pathList[] = FileName.splitAllPaths(path);
		long currentId = 0;
		try {
			DLFolderServiceSoapServiceLocator folderLocator =
				new DLFolderServiceSoapServiceLocator();
			DLFolderServiceSoap folderService =
				folderLocator.getPortlet_DL_DLFolderService(
					_getURL("Portlet_DL_DLFolderService"));
			boolean pathFound = false;
			for (int count = 0; count < pathList.length; count++) {
				for (DLFolderSoap folder : folderService.getFolders(
					_guestGroupId, currentId)) {
					if (pathList[count].equalsIgnoreCase(folder.getName())) {
						currentId = folder.getFolderId();
						pathFound = true;
						break;
					}
					else {
						pathFound = false;
					}
				}
			}
			return pathFound ? currentId : -1;
		}
		catch (Exception e) {
		}

		return currentId;
	}

	/**
	 * Return the Liferay ID of the parent folder of file or folder
	 * 
	 * @param path Full path to file or folder in Liferay, no backslashes at
	 *            head or tail
	 * @return ID of parent Liferay folder. 0 if parent folder is root folder.
	 *         -1 if not found
	 */
	public static long findParentFolderId(String path) {
		if (path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		if (_fileListCache.containsKey(path)) {
			return _fileListCache.get(path).getDirectoryIdLong();
		}
		else {
			return findFolderId(FileName.removeFileName(path));
		}
	}

	/**
	 * Get the FileInfo object by its full Liferay path
	 * 
	 * @param currentPath Full Liferay path to file, no head backslash, possibly
	 *            tail backslash
	 * @return FileInfo object for given path or null if not found
	 */
	public static FileInfo getFileInfo(String currentPath) {
		String path = currentPath;
		if (path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		if (_fileListCache.containsKey(path)) {
			return _fileListCache.get(path);
		}
		else {
			long parentFolderId = findParentFolderId(path);
			String[] pathComponents = FileName.splitPath(path);
			String fileName = pathComponents[1];
			String filePath = pathComponents[0];
			if (fileName == null) {
				fileName = filePath;
				filePath = "";
			}
			try {
				DLFileEntryServiceSoapServiceLocator documentLocator =
					new DLFileEntryServiceSoapServiceLocator();
				DLFileEntryServiceSoap documentService =
					documentLocator.getPortlet_DL_DLFileEntryService(
						_getURL("Portlet_DL_DLFileEntryService"));
				DLFileEntrySoap[] fileList =
					documentService.getFileEntries(parentFolderId);
				for (DLFileEntrySoap file : fileList) {
					if ((file.getTitle() + file.getName().substring(
						file.getName().lastIndexOf("."))).equals(fileName)) {
						return _getFileInfoFromFileEntry(
							file, parentFolderId, filePath);
					}
				}
			}
			catch (Exception e) {
			}
			try {
				DLFolderServiceSoapServiceLocator directoryLocator =
					new DLFolderServiceSoapServiceLocator();
				DLFolderServiceSoap directoryService =
					directoryLocator.getPortlet_DL_DLFolderService(
						_getURL("Portlet_DL_DLFolderService"));
				DLFolderSoap[] folderList =
					directoryService.getFolders(_guestGroupId, parentFolderId);
				for (DLFolderSoap folder : folderList) {
					if (folder.getName().equals(fileName)) {
						return _getFileInfoFromFolderEntry(
							folder, parentFolderId, filePath);
					}
				}
			}
			catch (Exception e) {
			}
			return null;
		}
	}

	/**
	 * Get the list of FileInfo objects for all files in a folder. Populates the
	 * golbal cache to speed up the work of GetFileInfo
	 * 
	 * @param folderId Id of the folder we search files in
	 * @param path Full Liferay path to the folder we search files in, no
	 *            backslashes at head or tail
	 * @return List of FileInfo objects for all files and folders in the parent
	 *         folder
	 */
	public static ArrayList<FileInfo> getFileList(long folderId, String path)
		throws ServiceException, Exception {

		ArrayList<FileInfo> result = new ArrayList<FileInfo>();
		DLFileEntryServiceSoapServiceLocator documentLocator =
			new DLFileEntryServiceSoapServiceLocator();
		DLFileEntryServiceSoap documentService =
			documentLocator.getPortlet_DL_DLFileEntryService(
				_getURL("Portlet_DL_DLFileEntryService"));
		DLFileEntrySoap[] fileList = documentService.getFileEntries(folderId);
		for (DLFileEntrySoap entry : fileList) {
			FileInfo info = _getFileInfoFromFileEntry(entry, folderId, path);
			result.add(info);
			String fileName = info.getFileName();
			if (!path.isEmpty()) {
				fileName = path + "\\" + fileName;
			}
			_fileListCache.put(fileName, info);
		}
		DLFolderServiceSoapServiceLocator directoryLocator =
			new DLFolderServiceSoapServiceLocator();
		DLFolderServiceSoap directoryService =
			directoryLocator.getPortlet_DL_DLFolderService(
				_getURL("Portlet_DL_DLFolderService"));
		DLFolderSoap[] folderList =
			directoryService.getFolders(_guestGroupId, folderId);
		for (DLFolderSoap folder : folderList) {
			FileInfo info = _getFileInfoFromFolderEntry(folder, folderId, path);
			result.add(info);
			String folderName = info.getFileName();
			if (!path.isEmpty()) {
				folderName = path + "\\" + folderName;
			}
			_fileListCache.put(folderName, info);
		}
		return result;
	}

	public static String getFileNameForWrite(String name) {
		long folderId = findParentFolderId(name);

		String[] pathComponents = FileName.splitPath(name);
		String fileName = pathComponents[1];
		String filePath = pathComponents[0];

		if (fileName == null) {
			fileName = filePath;
			filePath = "";
		}
		try {
			DLFileEntryServiceSoapServiceLocator documentLocator =
				new DLFileEntryServiceSoapServiceLocator();
			DLFileEntryServiceSoap documentService =
				documentLocator.getPortlet_DL_DLFileEntryService(
					_getURL("Portlet_DL_DLFileEntryService"));
			DLFileEntrySoap[] fileList =
				documentService.getFileEntries(folderId);
			for (DLFileEntrySoap file : fileList) {
				if ((file.getTitle() + file.getName().substring(
					file.getName().lastIndexOf("."))).equals(fileName)) {
					return "write" + file.getName();
				}
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	public static String getLiferayFileName(String currentPath) {
		String path = currentPath;
		if (path.endsWith("\\")) {
			path = path.substring(0, path.length() - 1);
		}
		if (_fileListCache.containsKey(path)) {
			return _fileListCache.get(path).getShortName();
		}
		else {
			long parentFolderId = findParentFolderId(path);
			String[] pathComponents = FileName.splitPath(path);
			String fileName = pathComponents[1];
			String filePath = pathComponents[0];
			if (fileName == null) {
				fileName = filePath;
				filePath = "";
			}
			try {
				DLFileEntryServiceSoapServiceLocator documentLocator =
					new DLFileEntryServiceSoapServiceLocator();
				DLFileEntryServiceSoap documentService =
					documentLocator.getPortlet_DL_DLFileEntryService(
						_getURL("Portlet_DL_DLFileEntryService"));
				DLFileEntrySoap[] fileList =
					documentService.getFileEntries(parentFolderId);
				for (DLFileEntrySoap file : fileList) {
					if ((file.getTitle() + file.getName().substring(
						file.getName().lastIndexOf("."))).equals(fileName)) {
						return file.getName();
					}
				}
			}
			catch (Exception e) {
			}
			return null;
		}
	}

	public static File readFile(String name) {
		long folderId = findParentFolderId(name);

		String[] pathComponents = FileName.splitPath(name);
		String fileName = pathComponents[1];
		String filePath = pathComponents[0];

		if (fileName == null) {
			fileName = filePath;
			filePath = "";
		}

		File outputFile = null;
		try {
			DLFileEntryServiceSoapServiceLocator documentLocator =
				new DLFileEntryServiceSoapServiceLocator();
			DLFileEntryServiceSoap documentService =
				documentLocator.getPortlet_DL_DLFileEntryService(
					_getURL("Portlet_DL_DLFileEntryService"));
			DLFileEntrySoap[] fileList =
				documentService.getFileEntries(folderId);
			for (DLFileEntrySoap file : fileList) {
				if ((file.getTitle() + file.getName().substring(
					file.getName().lastIndexOf("."))).equals(fileName)) {
					boolean getFileError = false;
					try {
						outputFile = _getFileFromLiferay(
								"http://" + _storageRootUrl +
									"/c/document_library/get_file?uuid=" +
									file.getUuid() + "&groupId=" +
									_guestGroupId, file.getName(),
								file.getSize());
					}
					catch (java.io.IOException ex) {
						getFileError = true;
					}
					if (getFileError) {
						_authorize();
						outputFile = _getFileFromLiferay(
								"http://" + _storageRootUrl +
									"/c/document_library/get_file?uuid=" +
									file.getUuid() + "&groupId=" +
									_guestGroupId, file.getName(),
								file.getSize());
					}
					return outputFile;
				}
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	public static void renameFile(String oldName, String newName)
		throws IOException {
		long oldFolderId = findParentFolderId(oldName);
		long newFolderId = findParentFolderId(newName);
		String[] oldPathComponents = FileName.splitPath(oldName);
		String oldFileName = oldPathComponents[1];
		String oldFilePath = oldPathComponents[0];

		String[] newPathComponents = FileName.splitPath(newName);
		String newFileName = newPathComponents[1];
		String newFilePath = newPathComponents[0];

		if (newFileName == null) {
			newFileName = newFilePath;
			newFilePath = "";
		}
		if (oldFileName == null) {
			oldFileName = oldFilePath;
			oldFilePath = "";
		}
		String sourceFileName = "";
		com.liferay.client.soap.portal.service.ServiceContext serviceContext;
		serviceContext =
			new com.liferay.client.soap.portal.service.ServiceContext();
		try {
			DLFileEntryServiceSoapServiceLocator documentLocator =
				new DLFileEntryServiceSoapServiceLocator();
			DLFileEntryServiceSoap documentService =
				documentLocator.getPortlet_DL_DLFileEntryService(
					_getURL("Portlet_DL_DLFileEntryService"));
			DLFileEntrySoap[] fileList =
				documentService.getFileEntries(oldFolderId);
			for (DLFileEntrySoap file : fileList) {
				if ((file.getTitle() + file.getName().substring(
					file.getName().lastIndexOf("."))).equals(oldFileName)) {
					sourceFileName = file.getName();
					if (oldFolderId != newFolderId) {
						boolean renameSuccess = false;
						renameSuccess = _moveFile(
								file.getName(), file.getTitle(),
								Long.toString(oldFolderId),
								Long.toString(newFolderId));
						if (!renameSuccess) {
							_authorize();
							renameSuccess = _moveFile(
									file.getName(), file.getTitle(),
									Long.toString(oldFolderId),
									Long.toString(newFolderId));
						}
						if (renameSuccess) {
							if (_fileListCache.containsKey(oldName)) {
								FileInfo newFileInfo =
									_fileListCache.get(oldName);
								newFileInfo.setDirectoryId((int) newFolderId);
								newFileInfo.setPath(newFilePath);
								_fileListCache.remove(oldName);
							}
							else {
								DLFileEntrySoap fileEntry =
									documentService.getFileEntry(
										newFolderId, file.getName());
								_fileListCache.put(
									newName, _getFileInfoFromFileEntry(
										fileEntry, newFolderId, newName));
							}
						}

					}
					else {
						DLFileEntrySoap fileEntry =
							documentService.updateFileEntry(
								oldFolderId, newFolderId, sourceFileName, null,
								newFileName, null, null, null, serviceContext);
						_fileListCache.put(newName, _getFileInfoFromFileEntry(
							fileEntry, newFolderId, newName));
						if (_fileListCache.containsKey(oldName)) {
							_fileListCache.remove(oldName);
						}
					}
					break;
				}
			}
		}
		catch (Exception ex) {
			throw new UnsupportedOperationException(
				"oldname:" + oldName +
				" newname:" + newName + " oldFolder" + oldFolderId +
				" newFolder" + newFolderId + " Error:" + ex);
		}
	}

	public static void renameFolder(String oldName, String newName) {
		long oldFolderId = findFolderId(oldName);
		long newFolderId = findParentFolderId(newName);
		String[] oldPathComponents = FileName.splitPath(oldName);
		String oldFileName = oldPathComponents[1];
		String oldFilePath = oldPathComponents[0];

		String[] newPathComponents = FileName.splitPath(newName);
		String newFileName = newPathComponents[1];
		String newFilePath = newPathComponents[0];

		if (newFileName == null) {
			newFileName = newFilePath;
			newFilePath = "";
		}
		if (oldFileName == null) {
			oldFileName = oldFilePath;
			oldFilePath = "";
		}
		com.liferay.client.soap.portal.service.ServiceContext serviceContext;
		serviceContext =
			new com.liferay.client.soap.portal.service.ServiceContext();
		try {
			DLFolderServiceSoapServiceLocator folderLocator =
				new DLFolderServiceSoapServiceLocator();
			DLFolderServiceSoap folderService =
				folderLocator.getPortlet_DL_DLFolderService(
					_getURL("Portlet_DL_DLFolderService"));
			DLFolderSoap folder =
				folderService.updateFolder(
					oldFolderId, newFolderId, newFileName, "", serviceContext);
			if (_fileListCache.containsKey(oldName)) {
				_fileListCache.remove(oldName);
			}
			_fileListCache.put(newName, _getFileInfoFromFolderEntry(
				folder, newFolderId, newName));
		}
		catch (Exception ex) {
			
			// throw new UnsupportedOperationException("Error:" +
			// sourceFileLink);
		}
	}

	public static FileInfo updateFile(String name, File fileForWrite) {
		long folderId = findParentFolderId(name);

		String[] pathComponents = FileName.splitPath(name);
		String fileName = pathComponents[1];
		String filePath = pathComponents[0];

		FileInfo info = null;
		if (fileName == null) {
			fileName = filePath;
			filePath = "";
		}
		String sourceFileName = "";
		String sourceFileTitle = "";
		try {

			DLFileEntryServiceSoapServiceLocator documentLocator =
				new DLFileEntryServiceSoapServiceLocator();
			DLFileEntryServiceSoap documentService =
				documentLocator.getPortlet_DL_DLFileEntryService(
					_getURL("Portlet_DL_DLFileEntryService"));
			DLFileEntrySoap[] fileList =
				documentService.getFileEntries(folderId);
			for (DLFileEntrySoap file : fileList) {
				if ((file.getTitle() + file.getName().substring(
					file.getName().lastIndexOf("."))).equals(fileName)) {
					sourceFileName = file.getName();
					sourceFileTitle = file.getTitle();
					boolean writeSuccess = false;
					writeSuccess = _writeFile(
							sourceFileName, sourceFileTitle,
							Long.toString(folderId), fileForWrite);
					if (!writeSuccess) {
						_authorize();
						_writeFile(
							sourceFileName, sourceFileTitle,
							Long.toString(folderId), fileForWrite);
					}
					break;
				}
			}
		}
		catch (Exception ex) {
		}
		return info;
	}

	private static void _authorize()
		throws MalformedURLException, IOException {
		URL url;
		HttpURLConnection urlConn;
		DataOutputStream printout;
		HttpURLConnection.setFollowRedirects(false);

		url =
			new URL(
				"http://" + _storageRootUrl +
				"/web/guest/home?p_p_id=58&p_p_lifecycle=1&p_p_state=normal" +
				"&p_p_mode=view&p_p_col_id=column-1&p_p_col_pos=1" +
				"&p_p_col_count=2&saveLastPath=0" +
				"&_58_struts_action=%2Flogin%2Flogin");
		urlConn = (HttpURLConnection) url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setDoOutput(true);
		urlConn.setUseCaches(false);
		urlConn.setRequestMethod("POST");
		urlConn.setRequestProperty("Host", _storageRootUrl);
		urlConn.setRequestProperty("Cookie", "COOKIE_SUPPORT=true");
		urlConn.connect();

		printout = new DataOutputStream(urlConn.getOutputStream());
		String content =
			"_58_login=" + URLEncoder.encode(_user, _UTF8) + "&_58_password=" +
				URLEncoder.encode(_password, _UTF8) + "&_58_redirect" +
				URLEncoder.encode("", _UTF8) + "&_58_rememberMe" +
				URLEncoder.encode("false", _UTF8);
		printout.writeBytes(content);
		printout.flush();
		printout.close();

		int headerKeyIndex = 0;
		String headerKey = urlConn.getHeaderFieldKey(headerKeyIndex);
		Map<String, List<String>> headers = urlConn.getHeaderFields();
		String cookies = "COOKIE_SUPPORT=true; ";
		_cookiesMap = new HashMap<String, String>();
		while (headerKey != null || headerKeyIndex == 0) {
			for (String headerValue : headers.get(headerKey)) {
				if (headerKey != null && headerKey.equals("Set-Cookie")) {
					cookies = cookies +
							headerValue.substring(0, headerValue.indexOf(";")) +
							"; ";
					try {
						_cookiesMap.put(
							headerValue.substring(0, headerValue.indexOf("=")),
							headerValue.substring(
								headerValue.indexOf("=") + 1,
								headerValue.indexOf(";")));
					}
					catch (Exception ex) {
						throw new UnsupportedOperationException(headerValue);
					}
					finally {
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

	private static File _getFileFromLiferay(
		String fileUrl, String name, int size)
		throws MalformedURLException, IOException {
		FileOutputStream str = new FileOutputStream("read" + name);
		DataInputStream input;
		URL url;
		HttpURLConnection urlConn;
		url = new URL(fileUrl);
		urlConn = (HttpURLConnection) url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setDoOutput(true);
		urlConn.setUseCaches(false);
		urlConn.setRequestMethod("GET");
		urlConn.setRequestProperty("Host", _storageRootUrl);
		urlConn.setRequestProperty("Cookie", _cookies);
		urlConn.connect();
		urlConn.disconnect();
		input = new DataInputStream(urlConn.getInputStream());
		str = new FileOutputStream("read" + name);
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
	 * Get the FileInfo record from a DLFileEntrySoap record
	 * 
	 * @param file The DLFileEntrySoap record
	 * @return The FileInfo record
	 */
	private static FileInfo _getFileInfoFromFileEntry(
		DLFileEntrySoap file, long parentFolderId, String path) {
		
		FileInfo fileInfo = new FileInfo();
		fileInfo.setFileName(
			file.getTitle() +
			file.getName().substring(file.getName().lastIndexOf(".")));
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
	 * 
	 * @param folder The DLFolderSoap record
	 * @return The FileInfo record
	 */
	private static FileInfo _getFileInfoFromFolderEntry(
		DLFolderSoap folder, long parentFolderId, String path) {
		
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
	 * Get the web service URL for given service relative path
	 * 
	 * @param serviceName Service name (relative URL)
	 * @return Particular web service URL
	 * @throws Exception
	 */
	private static URL _getURL(String serviceName)
		throws Exception {
		return new URL(
			"http://" + _userId + ":" + _password + "@" +
			_storageRootUrl + "/tunnel-web/secure/axis/" + serviceName);
	}

	private static boolean _moveFile(
		String name, String title, String folderId, String newFolderId)
		throws FileNotFoundException, MalformedURLException, IOException {

		URL url;
		HttpURLConnection urlConn;
		HttpURLConnection.setFollowRedirects(false);

		_prepareFile(folderId, name);

		url =
			new URL(
				"http://" + _storageRootUrl +
				"/web/guest/home?p_p_id=20&p_p_lifecycle=1" +
				"&p_p_state=maximized&p_p_mode=view" +
				"&_20_struts_action=%2Fdocument_library%2Fedit_file_entry");
		ClientHttpRequest client =
			new ClientHttpRequest(
				url.openConnection(),
				"Referer",
				"http://" +
					_storageRootUrl +
					"/web/guest/home?p_p_id=20&p_p_lifecycle=0" +
					"&p_p_state=maximized&p_p_mode=view" +
					"&_20_struts_action=%2Fdocument_library%2Fedit_file_entry" +
					"&_20_redirect=%2Fweb%2Fguest%2Fhome%3Fp_p_id%3D20%26" +
					"p_p_lifecycle%3D0%26p_p_state%3Dmaximized%26" +
					"p_p_mode%3Dview%26_20_struts_action%3D%252F" +
					"document_library%252Fview%26_20_folderId%3D" +
					folderId + "&_20_folderId=" + folderId + "&_20_name=" +
					name);

		client.setCookies(_cookiesMap);
		client.postCookies();
		client.setParameter("_20_cmd", URLEncoder.encode("update", _UTF8));
		client.setParameter(
			"_20_redirect",
			"/web/guest/home?p_p_id=20&p_p_lifecycle=0&p_p_state=maximized" +
				"&p_p_mode=view&_20_struts_action=%2Fdocument_library%2Fview" +
				"&_20_folderId=" + folderId);
		client.setParameter(
			"_20_referringPortletResource", URLEncoder.encode("", _UTF8));
		client.setParameter(
			"_20_uploadProgressId",
			URLEncoder.encode("dlFileEntryUploadProgress", _UTF8));
		client.setParameter("_20_folderId", URLEncoder.encode(folderId, _UTF8));
		client.setParameter("_20_newFolderId", URLEncoder.encode(
			newFolderId, _UTF8));
		client.setEmptyFileParameter("_20_file");// .setParameter("_20_file",
												 // URLEncoder.encode(""));
		client.setParameter("_20_title", title);
		client.setParameter("_20_description", "");
		client.setParameter("_20_tagsEntries", "");
		client.setParameter("_20_name", URLEncoder.encode(name, _UTF8));

		urlConn = (HttpURLConnection) client.post();

		return urlConn.getResponseCode() == 302;
	}

	private static void _prepareFile(String folderId, String fileName)
		throws MalformedURLException, IOException {
		URL url;
		HttpURLConnection urlConn;
		DataOutputStream printout;
		HttpURLConnection.setFollowRedirects(false);

		url =
			new URL(
				"http://" +
					_storageRootUrl +
					"/web/guest/home?p_p_id=20&p_p_lifecycle=0" +
					"&p_p_state=maximized&p_p_mode=view" +
					"&_20_struts_action=%2Fdocument_library%2Fedit_file_entry" +
					"&_20_redirect=%2Fweb%2Fguest%2Fhome%3F" +
					"_20_struts_action%3D%252Fdocument_library%252Fview%26" +
					"p_p_lifecycle%3D0%26_20_folderId%3D10322%26" +
					"p_p_mode%3Dview%26p_p_id%3D20%26" +
					"p_p_state%3Dmaximized&_20_folderId=" +
					folderId + "&_20_name=" + fileName);
		urlConn = (HttpURLConnection) url.openConnection();
		urlConn.setDoInput(true);
		urlConn.setDoOutput(true);
		urlConn.setUseCaches(false);
		urlConn.setRequestMethod("POST");
		urlConn.setRequestProperty("Host", _storageRootUrl);
		urlConn.setRequestProperty("Cookie", _cookies);
		urlConn.setRequestProperty(
			"CONTENT-TYPE", "application/x-www-form-urlencoded");
		urlConn.connect();
		printout = new DataOutputStream(urlConn.getOutputStream());
		String content = " ";
		printout.writeBytes(content);
		printout.flush();
		printout.close();

		urlConn.disconnect();
	}

	private static boolean _writeFile(
		String name, String title, String folderId, File fileForWrite)
		throws FileNotFoundException, MalformedURLException, IOException {
		URL url;
		HttpURLConnection urlConn;
		HttpURLConnection.setFollowRedirects(false);

		url =
			new URL(
				"http://" + _storageRootUrl +
				"/web/guest/home?p_p_id=20&p_p_lifecycle=1" +
				"&p_p_state=maximized&p_p_mode=view" +
				"&_20_struts_action=%2Fdocument_library%2Fedit_file_entry");
		ClientHttpRequest client = new ClientHttpRequest(url);
		client.setCookies(_cookiesMap);

		String fileName;
		fileName =
			title +
				(name.lastIndexOf(".") > 0
					? name.substring(name.lastIndexOf(".")) : "");
		client.postCookies();

		client.setParameter("_20_cmd", URLEncoder.encode("update", _UTF8));
		client.setParameter(
			"_20_redirect",
			"/web/guest/home?p_p_id=20&p_p_lifecycle=0&p_p_state=maximized" +
				"&p_p_mode=view" +
				"&_20_struts_action=%2Fdocument_library%2Fview&_20_folderId=" +
				folderId);
		client.setParameter(
			"_20_referringPortletResource", URLEncoder.encode("", _UTF8));
		client.setParameter(
			"_20_uploadProgressId",
			URLEncoder.encode("dlFileEntryUploadProgress", _UTF8));
		client.setParameter("_20_folderId", URLEncoder.encode(folderId, _UTF8));
		client.setParameter("_20_newFolderId", URLEncoder.encode("", _UTF8));
		client.setParameter("_20_file", fileName, new FileInputStream(
			fileForWrite));
		client.setParameter("_20_title", title);
		client.setParameter("_20_description", "");
		client.setParameter("_20_tagsEntries", "");
		client.setParameter("_20_name", /* URLEncoder.encode( */name/* ) */);

		urlConn = (HttpURLConnection) client.post();
		if (fileForWrite.exists()) {
			fileForWrite.delete();
		}
		return urlConn.getResponseCode() == 302;

	}

	private final static String _UTF8 = "UTF-8";
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
	private static String _cookies;
	private static Map<String, String> _cookiesMap;

}
