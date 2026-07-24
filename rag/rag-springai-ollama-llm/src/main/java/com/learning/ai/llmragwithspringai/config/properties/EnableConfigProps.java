package com.learning.ai.llmragwithspringai.config.properties;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan(basePackageClasses = {RagQueryProperties.class, AgentProperties.class})
class EnableConfigProps {}
