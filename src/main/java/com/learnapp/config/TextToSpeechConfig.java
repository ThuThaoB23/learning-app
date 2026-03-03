package com.learnapp.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TextToSpeechProperties.class)
public class TextToSpeechConfig {
}
