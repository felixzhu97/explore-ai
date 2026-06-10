package com.ai.media.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * Configuration properties for Media/Gateway service.
 */
@ConfigurationProperties(prefix = "media")
public record MediaProperties(
        StableDiffusion stableDiffusion,
        LocalDiffusion localDiffusion
) {
    public record StableDiffusion(
            String apiUrl,
            String defaultModel,
            List<String> availableModels,
            GenerationConfig defaultGeneration,
            HttpConfig http
    ) {
    }

    public record LocalDiffusion(
            String modelPath,
            String device,
            String defaultModel,
            List<String> availableModels,
            String pythonPath,
            String scriptPath,
            Integer maxMemoryGb,
            Integer numThreads,
            Boolean enableXformers
    ) {
        public LocalDiffusion {
            if (modelPath == null) modelPath = System.getProperty("user.home") + "/.cache/huggingface/hub";
            if (device == null) device = detectDevice();
            if (defaultModel == null) defaultModel = "stabilityai/stable-diffusion-2-1";
            if (availableModels == null) availableModels = List.of(
                    "stabilityai/stable-diffusion-2-1",
                    "stabilityai/stable-diffusion-xl-base-1.0"
            );
            if (pythonPath == null) pythonPath = "python3";
            if (scriptPath == null) scriptPath = "scripts/run_diffusion.py";
            if (maxMemoryGb == null) maxMemoryGb = 4;
            if (numThreads == null) numThreads = 4;
            if (enableXformers == null) enableXformers = false;
        }

        private static String detectDevice() {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("mac")) {
                return "mps"; // Apple Silicon GPU
            } else if (os.contains("linux") || os.contains("windows")) {
                // Try to detect CUDA availability via environment variable
                String cudaVisible = System.getenv("CUDA_VISIBLE_DEVICES");
                if (cudaVisible != null && !cudaVisible.isEmpty() && !"None".equals(cudaVisible)) {
                    return "cuda";
                }
            }
            return "cpu";
        }
    }

    public record GenerationConfig(
            Integer width,
            Integer height,
            Integer steps,
            Float guidanceScale,
            Integer numImages
    ) {
        public GenerationConfig {
            if (width == null) width = 512;
            if (height == null) height = 512;
            if (steps == null) steps = 25;
            if (guidanceScale == null) guidanceScale = 7.5f;
            if (numImages == null) numImages = 1;
        }
    }

    public record HttpConfig(
            Duration connectTimeout,
            Duration readTimeout,
            Duration writeTimeout
    ) {
        public HttpConfig {
            if (connectTimeout == null) connectTimeout = Duration.ofSeconds(30);
            if (readTimeout == null) readTimeout = Duration.ofMinutes(5);
            if (writeTimeout == null) writeTimeout = Duration.ofMinutes(5);
        }
    }
}
