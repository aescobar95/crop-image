package com.byte96.crop_image.api.v1.controller;


import com.byte96.crop_image.api.ApiVersion;
import com.byte96.crop_image.service.ProcessFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController("v1 CropImage")
@RequestMapping(ApiVersion.URL_BASE_V1)
public class CropImageController {


    @Autowired
    ProcessFileService service;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file){

        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path(ApiVersion.URL_BASE_V1+"/files/")
                .path(service.processWithOpenCV(file))
                .toUriString();

    }


    @PostMapping("/ocr")
    public String ocr(@RequestParam("file") MultipartFile file){
        return service.applyOCR(file);
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
        Resource file = service.load(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

}
