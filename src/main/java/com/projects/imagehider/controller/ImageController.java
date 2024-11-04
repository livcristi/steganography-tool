package com.projects.imagehider.controller;

import com.projects.imagehider.service.EncryptionService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping(ImageController.IMAGE_ENDPOINT)
public class ImageController
{
    public static final String IMAGE_ENDPOINT = "/images";

    private final EncryptionService encryptionService;

    public ImageController(EncryptionService encryptionService)
    {
        this.encryptionService = encryptionService;
    }

    @PostMapping("/encryption")
    public ResponseEntity<Resource> encryptImage(@RequestParam("image") MultipartFile image,
                                                 @RequestParam("secret") String secret,
                                                 @RequestParam("key") String key)
    {
        byte[] encryptedData = encryptionService.encodeImageSecret(image, secret, key);
        ByteArrayResource resource = new ByteArrayResource(encryptedData);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @PostMapping("/decryption")
    public ResponseEntity<String> decryptImage(@RequestParam("image") MultipartFile image,
                                               @RequestParam("key") String key)
    {
        String decodedMessage = encryptionService.decodeImageSecret(image, key);
        return new ResponseEntity<>(decodedMessage, HttpStatus.OK);
    }

    // Handle MultipartException specifically for missing multipart data
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<String> handleMultipartException(MultipartException e)
    {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Request must contain a file (multipart/form-data).");
    }
}
