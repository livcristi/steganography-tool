package com.projects.imagehider;


import com.projects.imagehider.config.EncryptionProperties;
import com.projects.imagehider.service.EncryptionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class EncryptionServiceTest
{
    private final EncryptionProperties encryptionProperties = new EncryptionProperties();
    private final EncryptionService encryptionService = new EncryptionService(encryptionProperties);

    @Test
    public void shouldEncryptAndDecryptMessage() throws IOException
    {
        // given
        String imagePath = "src/test/resources/testimage.jpg";
        String secret = "bongoCat";
        String key = "notakey";

        File imageFile = new File(imagePath);
        MockMultipartFile image = new MockMultipartFile("image", imageFile.getName(),
                "image/jpeg", Files.readAllBytes(imageFile.toPath()));

        // when
        byte[] encryptedImageData = encryptionService.encodeImageSecret(image, secret, key);
        MockMultipartFile encryptedImageFile = new MockMultipartFile("image", imageFile.getName(),
                "image/png", encryptedImageData);
        String decryptedMessage = encryptionService.decodeImageSecret(encryptedImageFile, key);

        // then
        Assertions.assertEquals(secret, decryptedMessage);
    }
}
