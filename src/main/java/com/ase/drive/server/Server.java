package com.ase.drive.server;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ase.drive.common.FileMetaData;

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
public class Server {
    @GetMapping("/{id}/directoryinfo/existing")
    public FileMetaData[] getExistingFilesData(@PathVariable("id") String userID) {
        return DirectoryData.shared.readExisitngFileInfoFor(userID);
    }

    @GetMapping("/{id}/directoryinfo/deleted")
    public FileMetaData[] getDeletedFilesData(@PathVariable("id") String userID) {
        return DirectoryData.shared.readDeletedFileInfoFor(userID);
    }

    @GetMapping("/{id}/file/{filepathBase64}")
    public Resource downloadFile(@PathVariable("id") String userID, @PathVariable String filepathBase64, HttpServletResponse response) throws IOException {
        String filepathString = new String(Base64.getDecoder().decode(filepathBase64));
        Path filepath = DirectoryData.shared.fullPathForUser(userID, filepathString);
        response.setHeader("Content-Disposition", "attachment; filename=" + filepath.getFileName());
        return new FileSystemResource(filepath);
    }

    @DeleteMapping("/{id}/file/{filepathBase64}")
    public ResponseEntity<?> deleteFile(@PathVariable("id") String userID, @PathVariable String filepathBase64) {
        String filepath = new String(Base64.getDecoder().decode(filepathBase64));
        if (DirectoryData.shared.deleteUserFile(userID, filepath)) {
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
        String filepath = new String(Base64.getDecoder().decode(filepathBase64));
        if (DirectoryData.shared.saveUserFile(userID, file, filepath)) {
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        }
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }
}