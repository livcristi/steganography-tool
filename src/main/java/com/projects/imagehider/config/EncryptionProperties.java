package com.projects.imagehider.config;

import com.projects.imagehider.utils.EncryptionConstants;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "encryption")
public class EncryptionProperties
{
    private String seed = EncryptionConstants.BASIC_SEED;

    public String getSeed()
    {
        return seed;
    }

    public void setSeed(String seed)
    {
        this.seed = seed;
    }
}
