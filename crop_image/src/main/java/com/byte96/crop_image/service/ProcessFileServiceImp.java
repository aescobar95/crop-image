package com.byte96.crop_image.service;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.byte96.crop_image.utils.exception.LoadFileException;
import com.byte96.crop_image.utils.exception.StorageFileException;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProcessFileServiceImp implements ProcessFileService {


    @Value("${file.upload-dir}")
    private String uploadDir;

    @Value("${file.process-dir}")
    private String proecesDir;


    @Override
    public String processWithOpenCV(MultipartFile file) {


        double startTime = System.currentTimeMillis();

        OpenCV.loadShared();

        String destinationName = String.valueOf(Math.random()) + "-" + file.getOriginalFilename();

        Mat img = Imgcodecs.imread(store(file, destinationName));
//        Imgcodecs.imwrite(destinationFile(proecesDir,"1-original-"+destinationName).toString(), img);

//        Mat imgGray = new Mat();
//        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY);
//        Imgcodecs.imwrite(destinationFile(proecesDir,"2-gray-"+destinationName).toString(), imgGray);
//
//        Mat imgGaussianBlur = new Mat();
//        Imgproc.GaussianBlur(imgGray, imgGaussianBlur, new Size(3, 3), 0);
//        Imgcodecs.imwrite(destinationFile(proecesDir,"3-gaussian_blur-"+destinationName).toString(), imgGaussianBlur);

        // Detectar los bordes de la imagen
        Mat cannyOutput = new Mat();
        Imgproc.Canny(img, cannyOutput, 15, 15 * 4);
//        Imgcodecs.imwrite(destinationFile(proecesDir,"4-canny-"+destinationName).toString(), cannyOutput);

        // Dilatación de la imagen para eliminar el ruido
        Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.dilate(cannyOutput, cannyOutput, element);
//        Imgcodecs.imwrite(destinationFile(proecesDir,"5-dilate-"+destinationName).toString(), cannyOutput);

        // Erosión de la imagen para eliminar el ruido
        element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
        Imgproc.erode(cannyOutput, cannyOutput, element);
//        Imgcodecs.imwrite(destinationFile(proecesDir,"6-erode-"+destinationName).toString(), cannyOutput);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(cannyOutput, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

//        Double maxArea = Imgproc.contourArea(contours.get(0));
//        int indexMaxArea = 0;
//        for (int i = 0; i < contours.size(); i++) {
//            if (Imgproc.contourArea(contours.get(i)) > maxArea) {
//                indexMaxArea = i;
//                maxArea = Imgproc.contourArea(contours.get(i));
//            }
////            if (Imgproc.contourArea(contours.get(i)) > 50) {
////                Rect rect = Imgproc.boundingRect(contours.get(i));
////                if (rect.height > 28) {
////                    Mat roi = img.submat(rect);
////                    Imgcodecs.imwrite(destinationFile(proecesDir,"rectangule-"+i+"-"+destinationName).toString(), roi);
////
////                }
////            }
//        }
//        Rect rect = Imgproc.boundingRect(contours.get(indexMaxArea));

        MatOfPoint max = contours.stream().max(Comparator.comparing(i -> Imgproc.contourArea(i))).get();
        Rect rect = Imgproc.boundingRect(max);

        Mat roi = img.submat(rect);
        String maxImgName = "maxrect-" + destinationName ;
        Imgcodecs.imwrite(destinationFile(proecesDir, maxImgName).toString(), roi);

        double endTime = System.currentTimeMillis();
        System.out.println("Tiempo en procesar imagen with con OpenCV = " + (endTime - startTime) / 1000 + " Segundos");


        return maxImgName;
    }

    @Override
    public String applyOCR(MultipartFile multipartFile) {

        double startTime = System.currentTimeMillis();

        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata");
        tesseract.setLanguage("spa");

        File file = new File(processWithOpenCV(multipartFile));

//        BufferedImage bi = null;
//        try {
//            bi = ImageIO.read(multipartFile.getInputStream());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        String ocr = "";
        try {
            ocr = tesseract.doOCR(file);
//            ocr = tesseract.doOCR(bi);
        } catch (TesseractException e) {
            e.printStackTrace();
        }

        double endTime = System.currentTimeMillis();
        System.out.println("Tiempo en procesar imagen con OpenCV y aplicar OCR con Tesseract = " + (endTime - startTime) / 1000 + " Segundos");

        return ocr;
    }

    @Override
    public Resource load(String filename) {
        try {
            Path file = destinationFile(proecesDir, filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new LoadFileException("Could not read the file!");
            }
        } catch (MalformedURLException e) {
            throw new LoadFileException("Error: " + e.getMessage());
        }
    }

    private String store(MultipartFile file, String destinationName) {

        if (file.isEmpty()) {
            throw new StorageFileException("Failed to store empty file.");
        }
        Path destinationFile = destinationFile(uploadDir, destinationName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new StorageFileException("Failed to store file.", e);
        }
        return destinationFile.toString();
    }

    private Path destinationFile(String uploadDir, String filename) {
        return Paths.get(uploadDir).resolve(Paths.get(filename))
                .normalize().toAbsolutePath();
    }
}
