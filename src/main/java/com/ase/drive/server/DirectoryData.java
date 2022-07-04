package com.ase.drive.server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.springframework.web.multipart.MultipartFile;

import com.ase.drive.common.FileMetaData;

/**
 * DirectoryInfoManager
 */
public class DirectoryData {
    private Path directory;

    private static class MetaDataManager {
        static private String getDirectoryDatafromUser() {
            return ".directoryInfo";
        }
    
        static private Path getExistingFilesDirectoryDatafromUser(Path userPath) {
            return Paths.get(userPath.toString(), getDirectoryDatafromUser(), "existing.json");
        }
    
        static private Path getDeletedFilesDirectoryDatafromUser(Path userPath) {
            return Paths.get(userPath.toString(), getDirectoryDatafromUser(), "deleted.json");
        }

        static private FileMetaData[] readFileInfo(Path infoFile) {
            FileMetaData[] fileInfoArray = new FileMetaData[0];
            try {
                String content = FileUtils.readFileToString(infoFile.toFile());
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
    
        static FileMetaData[] readExisitngFileInfoFor(Path userPath) {
            return readFileInfo(getExistingFilesDirectoryDatafromUser(userPath));
        }
    
        static FileMetaData[] readDeletedFileInfoFor(Path userPath) {
            return readFileInfo(getDeletedFilesDirectoryDatafromUser(userPath));
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

        static private boolean addFilesData(FileMetaData fileMetaData, Path infoFile) {
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
    
        static private boolean removeFromFilesData(FileMetaData fileMetaData, Path infoFile) {
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
    
        static void saveFileInfoToExisting(Path userPath, Path filePath) {
            FileMetaData fileMetaData = new FileMetaData(filePath, userPath);
            addFilesData(fileMetaData, getExistingFilesDirectoryDatafromUser(userPath));
            removeFromFilesData(fileMetaData, getDeletedFilesDirectoryDatafromUser(userPath));
        }
    
        static void saveFileInfoToDeleted(Path userPath, Path filePath) {
            FileMetaData fileMetaData = new FileMetaData(filePath, userPath);
            addFilesData(fileMetaData, getDeletedFilesDirectoryDatafromUser(userPath));
            removeFromFilesData(fileMetaData, getExistingFilesDirectoryDatafromUser(userPath));
        }
    
    }


    DirectoryData(String userDataDirectoryPath) {
        File dir = Paths.get(".", userDataDirectoryPath).toFile();
        if (!dir.exists()) {
            dir.mkdir();
        }
        directory = dir.toPath();
    }

    FileMetaData[] readExisitngFileInfoFor(String userID) {
        Path userPath = directory.resolve(userID);
        return MetaDataManager.readExisitngFileInfoFor(userPath);
    }

    FileMetaData[] readDeletedFileInfoFor(String userID) {
        Path userPath = directory.resolve(userID);
        return MetaDataManager.readDeletedFileInfoFor(userPath);
    }

    boolean saveUserFile(String userID, MultipartFile file, String filepathString) {
        try {
            byte[] bytes = file.getBytes();
            Path userPath = directory.resolve(userID);
            Path filePath = userPath.resolve(filepathString);
            filePath.getParent().toFile().mkdirs();
            Files.write(filePath, bytes);
            MetaDataManager.saveFileInfoToExisting(userPath, filePath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    Path fullPathForUser(String userID, String filepathString) {
        return directory.resolve(userID).resolve(filepathString);
    }

    boolean deleteUserFile(String userID, String filepathString) {
        Path filePath = directory.resolve(userID).resolve(filepathString);
        Path userPath = directory.resolve(userID);
        File file = filePath.toFile();
        if (file.exists()) {
            MetaDataManager.saveFileInfoToDeleted(userPath, filePath);
            return file.delete();
        }
        return false;
    }

    static DirectoryData shared = new DirectoryData("userdata");
}