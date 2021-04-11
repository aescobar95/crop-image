package com.byte96.crop_image.service;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.Resource;

public interface ProcessFileService {


    String processWithOpenCV(MultipartFile file);

    String applyOCR(MultipartFile file);

    Resource load(String filename);

}
