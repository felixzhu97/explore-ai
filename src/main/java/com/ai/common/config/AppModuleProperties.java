package com.ai.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.modules")
public class AppModuleProperties {

    private boolean vision = true;
    private boolean audioAsr = true;
    private boolean mcp = true;
    private boolean eval = true;

    public boolean isVision() {
        return vision;
    }

    public void setVision(boolean vision) {
        this.vision = vision;
    }

    public boolean isAudioAsr() {
        return audioAsr;
    }

    public void setAudioAsr(boolean audioAsr) {
        this.audioAsr = audioAsr;
    }

    public boolean isMcp() {
        return mcp;
    }

    public void setMcp(boolean mcp) {
        this.mcp = mcp;
    }

    public boolean isEval() {
        return eval;
    }

    public void setEval(boolean eval) {
        this.eval = eval;
    }
}
