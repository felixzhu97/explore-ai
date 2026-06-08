package com.ai.tts.adapter;

import com.ai.tts.domain.model.OutputFormat;
import com.ai.tts.domain.model.ProviderInfo;
import com.ai.tts.domain.model.Voice;
import com.ai.tts.domain.port.TtsProvider;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Map;

public class EdgeTtsProvider implements TtsProvider {

    private static final String EDGE_TTS_URL = "https://edge-tts.googleapis.com/v1/text:synthesize";

    private final HttpClient httpClient;

    public EdgeTtsProvider() {
        this.httpClient = HttpClient.create()
                .compress(true);
    }

    @Override
    public String name() {
        return "edge";
    }

    @Override
    public ProviderInfo getInfo() {
        return ProviderInfo.of(
            "edge",
            "Microsoft Edge TTS (Neural)",
            List.of(
                "zh-CN", "zh-TW", "zh-HK",
                "en-US", "en-GB", "en-AU", "en-CA",
                "ja-JP", "ko-KR",
                "fr-FR", "de-DE", "es-ES", "es-MX",
                "pt-BR", "pt-PT", "it-IT", "ru-RU",
                "hi-IN"
            ),
            List.of(
                "Neural voices (Neural suffix)",
                "No API key required",
                "Multi-language (50+ languages)",
                "Speed/pitch adjustment",
                "Local TTS (no cloud dependency)"
            )
        );
    }

    @Override
    public Mono<byte[]> synthesize(String text, String voice, String language, float speed, float pitch, OutputFormat format) {
        String edgeVoice = resolveVoice(voice, language);
        String rate = speedToEdgeRate(speed);
        String pitchStr = pitchToEdgePitch(pitch);

        String ssml = buildSsml(text, edgeVoice, rate, pitchStr);

        return httpClient.post()
                .uri(EDGE_TTS_URL)
                .send((r, out) -> {
                    r.header("Content-Type", "application/json");
                    Map<String, Object> body = Map.of(
                        "input", Map.of("ssml", ssml),
                        "voice", Map.of(
                            "languageCode", extractLanguageCode(edgeVoice),
                            "name", edgeVoice
                        ),
                        "audioConfig", Map.of(
                            "audioEncoding", "mp3-24kbts"
                        )
                    );
                    return out.sendString(Mono.just(toJson(body)));
                })
                .responseSingle((r, buf) -> buf.asString())
                .map(this::decodeAudio);
    }

    @Override
    public Flux<byte[]> stream(String text, String voice, String language, float speed, OutputFormat format) {
        return synthesize(text, voice, language, speed, 0, format)
                .flatMapMany(audio -> Flux.just(audio));
    }

    @Override
    public List<Voice> listVoices(String language) {
        List<Voice> allVoices = List.of(
            Voice.defaultVoice("zh-CN-XiaoxiaoNeural", "Xiaoxiao (晓晓)", "zh-CN", "edge"),
            Voice.of("zh-CN-YunxiNeural", "Yunxi (云希)", "zh-CN", "edge"),
            Voice.of("zh-CN-YunyangNeural", "Yunyang (云扬)", "zh-CN", "edge"),
            Voice.of("zh-CN-XiaoyiNeural", "Xiaoyi (小艺)", "zh-CN", "edge"),
            Voice.of("zh-TW-HsiaoYuNeural", "HsiaoYu", "zh-TW", "edge"),
            Voice.of("zh-HK-HiuGaaiNeural", "HiuGaai", "zh-HK", "edge"),
            Voice.of("en-US-JennyNeural", "Jenny", "en-US", "edge"),
            Voice.of("en-US-GuyNeural", "Guy", "en-US", "edge"),
            Voice.of("en-US-AriaNeural", "Aria", "en-US", "edge"),
            Voice.of("en-GB-SoniaNeural", "Sonia", "en-GB", "edge"),
            Voice.of("en-GB-RyanNeural", "Ryan", "en-GB", "edge"),
            Voice.of("en-AU-NatashaNeural", "Natasha", "en-AU", "edge"),
            Voice.of("ja-JP-NanamiNeural", "Nanami (七海)", "ja-JP", "edge"),
            Voice.of("ko-KR-SunHiNeural", "SunHi (선희)", "ko-KR", "edge"),
            Voice.of("fr-FR-DeniseNeural", "Denise", "fr-FR", "edge"),
            Voice.of("de-DE-KatjaNeural", "Katja", "de-DE", "edge"),
            Voice.of("es-ES-ElviraNeural", "Elvira", "es-ES", "edge"),
            Voice.of("pt-BR-FranciscaNeural", "Francisca", "pt-BR", "edge"),
            Voice.of("it-IT-ElsaNeural", "Elsa", "it-IT", "edge"),
            Voice.of("ru-RU-SvetlanaNeural", "Svetlana", "ru-RU", "edge"),
            Voice.of("hi-IN-SwaraNeural", "Swara", "hi-IN", "edge")
        );

        if (language == null || language.isBlank()) {
            return allVoices;
        }
        return allVoices.stream()
                .filter(v -> v.language().equalsIgnoreCase(language))
                .toList();
    }

    @Override
    public boolean healthCheck() {
        return true;
    }

    private String resolveVoice(String voice, String language) {
        if (voice != null && !voice.isBlank()) {
            return voice;
        }
        return switch (language != null ? language.toLowerCase() : "zh-cn") {
            case "en-us", "en" -> "en-US-JennyNeural";
            case "en-gb" -> "en-GB-SoniaNeural";
            case "ja", "ja-jp" -> "ja-JP-NanamiNeural";
            case "ko", "ko-kr" -> "ko-KR-SunHiNeural";
            case "fr", "fr-fr" -> "fr-FR-DeniseNeural";
            case "de", "de-de" -> "de-DE-KatjaNeural";
            case "es", "es-es" -> "es-ES-ElviraNeural";
            case "pt", "pt-br" -> "pt-BR-FranciscaNeural";
            default -> "zh-CN-XiaoxiaoNeural";
        };
    }

    private String speedToEdgeRate(float speed) {
        if (speed < 0.5f) speed = 0.5f;
        if (speed > 2.0f) speed = 2.0f;
        float percentage = (speed - 1.0f) * 100;
        return (percentage >= 0 ? "+" : "") + Math.round(percentage) + "%";
    }

    private String pitchToEdgePitch(float pitch) {
        if (pitch == 0) return "";
        return (pitch >= 0 ? "+" : "") + Math.round(pitch) + "Hz";
    }

    private String buildSsml(String text, String voice, String rate, String pitch) {
        StringBuilder ssml = new StringBuilder();
        ssml.append("<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='").append(extractLanguageCode(voice)).append("'>");
        ssml.append("<voice name='").append(voice).append("'>");

        String prosody = "<prosody rate='" + rate + "'";
        if (!pitch.isEmpty()) {
            prosody += " pitch='" + pitch + "'";
        }
        prosody += ">" + escapeXml(text) + "</prosody>";
        ssml.append(prosody);

        ssml.append("</voice></speak>");
        return ssml.toString();
    }

    private String extractLanguageCode(String voiceName) {
        int idx = voiceName.indexOf('-');
        return idx > 0 ? voiceName.substring(0, idx) : voiceName;
    }

    private String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String toJson(Map<String, Object> obj) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : obj.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) value));
            } else if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String mapToJson(Map<?, ?> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        int count = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (count++ > 0) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                sb.append("\"").append(value).append("\"");
            } else if (value instanceof Map) {
                sb.append(mapToJson((Map<?, ?>) value));
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private byte[] decodeAudio(String response) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(response);
            String audioContent = node.path("audioContent").asText();
            return java.util.Base64.getDecoder().decode(audioContent);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decode audio response", e);
        }
    }
}
