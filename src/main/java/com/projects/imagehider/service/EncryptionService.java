package com.projects.imagehider.service;

import com.projects.imagehider.config.EncryptionProperties;
import com.projects.imagehider.exception.DecryptionException;
import com.projects.imagehider.exception.EncryptionException;
import com.projects.imagehider.exception.ImageException;
import com.projects.imagehider.utils.EncryptionConstants;
import com.projects.imagehider.utils.EncryptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;

import static com.projects.imagehider.utils.EncryptionUtils.hashSimpleData;


@Service
public class EncryptionService
{
    private static final Logger LOG = LoggerFactory.getLogger(EncryptionService.class);
    private static final String PNG_FORMAT = "png";

    private final EncryptionProperties encryptionProperties;

    public EncryptionService(EncryptionProperties encryptionProperties)
    {
        this.encryptionProperties = encryptionProperties;
    }

    public byte[] encodeImageSecret(MultipartFile inputImage, String secret, String key)
    {
        LOG.info("Will encodeImageSecret, imageName: {}", hashSimpleData(inputImage.getName()));

        // First, try to encrypt the secret
        String encryption = encryptSecret(secret, key);

        // Now that we have the encryption as a string, we need to convert it to bits
        BitSet binaryEncryptionData = convertToBitSet(encryption);
        LOG.info("Encrypted the secret for imageName: {}", hashSimpleData(inputImage.getName()));

        // Now we can read the image data and hide our secret inside it
        BufferedImage encryptedImage = getRgbImage(inputImage);
        // It is also important to check that we can hide the message inside the image
        if (encryptedImage.getWidth() * encryptedImage.getHeight() * 3 < binaryEncryptionData.size())
        {
            throw new ImageException("Image is too small to hold the message");
        }

        addEncryptionData(encryptedImage, binaryEncryptionData);
        LOG.info("Saved the encryption in the image, imageName: {}", hashSimpleData(inputImage.getName()));

        // Convert the image to its bytes
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try
        {
            ImageIO.write(encryptedImage, PNG_FORMAT, byteStream);
        } catch (IOException e)
        {
            throw new ImageException("Error encoding image data", e);
        }
        LOG.info("Done encodeImageSecret, imageName: {}", hashSimpleData(inputImage.getName()));
        return byteStream.toByteArray();
    }

    public String decodeImageSecret(MultipartFile encodedImage, String key)
    {
        LOG.info("Will decodeImageSecret, imageName: {}", hashSimpleData(encodedImage.getName()));
        SecretKeySpec secretKeySpec;
        try
        {
            secretKeySpec = EncryptionUtils.hashData(key, encryptionProperties.getSeed());
        } catch (NoSuchAlgorithmException e)
        {
            throw new DecryptionException("Decryption operation failed due to input data or processing issues", e);
        }

        BufferedImage imageData = getRgbImage(encodedImage);
        int width = imageData.getWidth();
        int height = imageData.getHeight();

        StringBuilder imageBits = new StringBuilder();
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                int pixel = imageData.getRGB(x, y);
                int red = (pixel >> 16) & 0xFF;
                int green = (pixel >> 8) & 0xFF;
                int blue = (pixel) & 0xFF;

                imageBits.append(red & 1);
                imageBits.append(green & 1);
                imageBits.append(blue & 1);
            }
        }

        int endIndex = imageBits.indexOf(EncryptionConstants.END_SEPARATOR);
        if (endIndex == -1)
        {
            throw new DecryptionException("Decryption failed, cannot find secret in image");
        }

        StringBuilder encodedMessage = new StringBuilder();
        for (int bitIndex = 0; bitIndex < endIndex; bitIndex += 8)
        {
            String binaryCharacter = imageBits.substring(bitIndex, bitIndex + 8);
            int characterCode = Integer.parseInt(binaryCharacter, 2);
            encodedMessage.append((char) characterCode);
        }
        LOG.info("Retrieved bits of message, imageName: {}", hashSimpleData(encodedImage.getName()));

        // Try to decrypt the message
        try
        {
            String message = EncryptionUtils.decrypt(String.valueOf(encodedMessage), secretKeySpec);
            LOG.info("Done decodeImageSecret, imageName: {}", hashSimpleData(encodedImage.getName()));
            return message;
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e)
        {
            throw new DecryptionException("Decryption operation failed due to input data or processing issues", e);
        }
    }

    private String encryptSecret(String secret, String key)
    {
        try
        {
            SecretKeySpec secretKeySpec = EncryptionUtils.hashData(key, encryptionProperties.getSeed());
            return EncryptionUtils.encrypt(secret, secretKeySpec);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e)
        {
            throw new EncryptionException("Encryption operation failed due to input data or processing issues", e);
        }
    }

    private static BitSet convertToBitSet(String message)
    {
        byte[] encryptionBytes = message.getBytes(StandardCharsets.UTF_8);
        int bitSetSize = encryptionBytes.length * 8;
        BitSet binaryEncryptionData = new BitSet(bitSetSize);
        int dataIndex = 0;
        for (byte dataByte : encryptionBytes)
        {
            for (short bitIndex = 7; bitIndex >= 0; bitIndex--)
            {
                binaryEncryptionData.set(dataIndex, (dataByte & (1 << bitIndex)) != 0);
                dataIndex++;
            }
        }
        // Don't forget to add the end separator, so that we know the message ended
        for (char separatorChar : EncryptionConstants.END_SEPARATOR.toCharArray())
        {
            binaryEncryptionData.set(dataIndex, separatorChar == '1');
            dataIndex++;
        }
        return binaryEncryptionData;
    }

    private static BufferedImage getRgbImage(MultipartFile inputImage)
    {
        BufferedImage originalImage;

        try
        {
            originalImage = ImageIO.read(inputImage.getInputStream());
        } catch (IOException | IllegalArgumentException e)
        {
            throw new ImageException("Image cannot be read", e);
        }

        // If the image is already RGB, return it as is
        if (originalImage.getType() == BufferedImage.TYPE_INT_RGB)
        {
            return originalImage;
        }

        // Else, we need to create a new RGB image for it
        BufferedImage rgbImage = new BufferedImage(
                originalImage.getWidth(),
                originalImage.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );

        // Draw the original image onto the new RGB image
        Graphics2D g = rgbImage.createGraphics();
        g.drawImage(originalImage, 0, 0, null);
        g.dispose();

        return rgbImage;
    }

    private static void addEncryptionData(BufferedImage encryptedImage, BitSet binaryEncryptionData)
    {
        // Hide the image in the first bits of each pixel channel
        int encryptionIndex = 0;
        int width = encryptedImage.getWidth();
        int height = encryptedImage.getHeight();
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                if (encryptionIndex >= binaryEncryptionData.size())
                {
                    break;
                }
                int pixel = encryptedImage.getRGB(x, y);
                int red = (pixel >> 16) & 0xFE;
                int green = (pixel >> 8) & 0xFE;
                int blue = (pixel) & 0xFE;

                red = red | (binaryEncryptionData.get(encryptionIndex++) ? 1 : 0);
                if (encryptionIndex < binaryEncryptionData.size())
                {
                    green = green | (binaryEncryptionData.get(encryptionIndex++) ? 1 : 0);
                }
                if (encryptionIndex < binaryEncryptionData.size())
                {
                    blue = blue | (binaryEncryptionData.get(encryptionIndex++) ? 1 : 0);
                }

                int newPixel = (red << 16) | (green << 8) | blue;
                encryptedImage.setRGB(x, y, newPixel);
            }
        }
    }
}
