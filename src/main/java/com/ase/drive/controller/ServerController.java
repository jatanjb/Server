package com.ase.drive.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ase.drive.utility.FileMetaData;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
public class ServerController {
    @GetMapping("/{id}/directoryinfo/existing")
    public FileMetaData[] getExistingFilesData(@PathVariable("id") String userID) {
        return FolderData.shared.readAddFileInfo(userID);
    }

    @GetMapping("/{id}/directoryinfo/deleted")
    public FileMetaData[] getDeletedFilesData(@PathVariable("id") String userID) {
        return FolderData.shared.readDeletedFileInfo(userID);
    }

    @GetMapping("/{id}/file/{filepathBase64}")
    public Resource downloadFile(@PathVariable("id") String userID, @PathVariable String filepathBase64, HttpServletResponse response) throws IOException {
        String pathString = new String(Base64.getDecoder().decode(filepathBase64));
        Path fileLocation = FolderData.shared.pathForUser(userID, pathString);
        response.setHeader("Content-Disposition", "attachment; filename=" + fileLocation.getFileName());
        return new FileSystemResource(fileLocation);
    }

    @DeleteMapping("/{id}/file/{filepathBase64}")
    public ResponseEntity<?> deleteFile(@PathVariable("id") String userID, @PathVariable String filepathBase64) {
        String fileLocation = new String(Base64.getDecoder().decode(filepathBase64));
        if (FolderData.shared.deleteClientFile(userID, fileLocation)) {
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/{id}/file/{filepathBase64}")
    public ResponseEntity<?> singleFileUpload(
        @PathVariable("id") String userID,
        @PathVariable("filepathBase64") String filepathBase64,
        @RequestParam("file") MultipartFile file,
        RedirectAttributes redirectAttributes
    ) {
        String fileLocation = new String(Base64.getDecoder().decode(filepathBase64));
        if (FolderData.shared.uploadClientFile(userID, file, fileLocation)) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }
}