package com.ase.drive.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

import com.ase.drive.utility.FileMetaData;

/**
 * DirectoryInfoManager
 */
public class FolderData {
    private Path directory;
    
    FolderData(String userDataDirectoryPath) {
        File dir = Paths.get(".", userDataDirectoryPath).toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }
        directory = dir.toPath();
    }

    private static class MetaDataManager {
        static private String getInfofromUser() {
            return ".directoryInfo";
        }
    
        static private Path getExistingFileMetadatafromUser(Path clientLocation) {
            return Paths.get(clientLocation.toString(), getInfofromUser(), "existing.json");
        }
    
        static private Path getDeletedFilesMetadatafromUser(Path clientLocation) {
            return Paths.get(clientLocation.toString(), getInfofromUser(), "deleted.json");
        }

        static private FileMetaData[] getFilemetadata(Path metadata) {
            FileMetaData[] fileInfoArray = new FileMetaData[0];
            try {
                String content = FileUtils.readFileToString(metadata.toFile());
                JSONArray fileInfos = new JSONArray(content);
                fileInfoArray = new FileMetaData[fileInfos.length()];
                for (int i = 0; i < fileInfoArray.length; i++) {
                    fileInfoArray[i] = new FileMetaData(fileInfos.getJSONObject(i));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return fileInfoArray;
        }
    
        static FileMetaData[] readExisitngFileInfoFor(Path clientLocation) {
            return getFilemetadata(getExistingFileMetadatafromUser(clientLocation));
        }
    
        static FileMetaData[] readDeletedFileInfoFor(Path clientLocation) {
            return getFilemetadata(getDeletedFilesMetadatafromUser(clientLocation));
        }

        private static int indexOf(FileMetaData fileMetaData, JSONArray array) {
            for (int i = 0; i < array.length(); i++) {
                FileMetaData curr = new FileMetaData(array.getJSONObject(i));
                if (curr.hasSamePathAs(fileMetaData)) {
                    return i;
                }
            }
            return -1;
        }

        static private boolean clientAddFilesData(FileMetaData fileMetaData, Path infoFile) {
            try {
                infoFile.toFile().getParentFile().mkdirs();
                infoFile.toFile().createNewFile();
                String content = FileUtils.readFileToString(infoFile.toFile());
                if (content.equals("")) content = "[]";
                JSONArray fileInfos = new JSONArray(content);
                int fileInfoIndex = indexOf(fileMetaData, fileInfos);
                if (fileInfoIndex < 0) {
                    fileInfos.put(fileMetaData.json());
                    FileUtils.writeStringToFile(infoFile.toFile(), fileInfos.toString());
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    
        static private boolean clientRemoveFilesData(FileMetaData fileMetaData, Path infoFile) {
            try {
                String content = FileUtils.readFileToString(infoFile.toFile());
                JSONArray fileInfos = new JSONArray(content);
                int fileInfoIndex = indexOf(fileMetaData, fileInfos);
                if (fileInfoIndex > -1) {
                    fileInfos.remove(fileInfoIndex);
                    FileUtils.writeStringToFile(infoFile.toFile(), fileInfos.toString());
                    return true;
                }
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
    
        static void saveFileInfoToExisting(Path clientLocation, Path filePath) {
            FileMetaData fileMetaData = new FileMetaData(filePath, clientLocation);
            clientAddFilesData(fileMetaData, getExistingFileMetadatafromUser(clientLocation));
            clientRemoveFilesData(fileMetaData, getDeletedFilesMetadatafromUser(clientLocation));
        }
    
        static void saveFileInfoToDeleted(Path clientLocation, Path filePath) {
            FileMetaData fileMetaData = new FileMetaData(filePath, clientLocation);
            clientAddFilesData(fileMetaData, getDeletedFilesMetadatafromUser(clientLocation));
            clientRemoveFilesData(fileMetaData, getExistingFileMetadatafromUser(clientLocation));
        }
    
    }

    FileMetaData[] readAddFileInfo(String userID) {
        Path clientLocation = directory.resolve(userID);
        return MetaDataManager.readExisitngFileInfoFor(clientLocation);
    }

    FileMetaData[] readDeletedFileInfo(String userID) {
        Path clientLocation = directory.resolve(userID);
        return MetaDataManager.readDeletedFileInfoFor(clientLocation);
    }

    boolean uploadClientFile(String userID, MultipartFile file, String filepathString) {
        try {
            byte[] bytes = file.getBytes();
            Path clientLocation = directory.resolve(userID);
            Path filePath = clientLocation.resolve(filepathString);
            filePath.getParent().toFile().mkdirs();
            Files.write(filePath, bytes);
            MetaDataManager.saveFileInfoToExisting(clientLocation, filePath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    Path pathForUser(String userID, String filepathString) {
        return directory.resolve(userID).resolve(filepathString);
    }

    boolean deleteClientFile(String userID, String filepathString) {
        Path filePath = directory.resolve(userID).resolve(filepathString);
        Path clientLocation = directory.resolve(userID);
        File file = filePath.toFile();
        if (file.exists()) {
            MetaDataManager.saveFileInfoToDeleted(clientLocation, filePath);
            return file.delete();
        }
        return false;
    }

    static FolderData shared = new FolderData("userdata");
}