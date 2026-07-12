package com.ai.vision.infrastructure.adapter;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BlipVocabulary {

    private static final int UNK_TOKEN_ID = 100;
    private static final int CLS_TOKEN_ID = 101;
    private static final int SEP_TOKEN_ID = 102;

    private final Map<Integer, String> idToToken = new HashMap<>();
    private final Map<String, Integer> tokenToId = new HashMap<>();

    private BlipVocabulary() {}

    static BlipVocabulary load(Path tokenizerDir) throws IOException {
        Path vocabFile = tokenizerDir.resolve("vocab.txt");
        if (!Files.exists(vocabFile)) {
            throw new IOException("vocab.txt not found in " + tokenizerDir);
        }

        BlipVocabulary vocabulary = new BlipVocabulary();
        try (BufferedReader reader = Files.newBufferedReader(vocabFile, StandardCharsets.UTF_8)) {
            String line;
            int id = 0;
            while ((line = reader.readLine()) != null) {
                vocabulary.idToToken.put(id, line);
                vocabulary.tokenToId.put(line, id);
                id++;
            }
        }
        return vocabulary;
    }

    String decode(List<Long> tokenIds) {
        StringBuilder builder = new StringBuilder();
        for (Long tokenId : tokenIds) {
            if (tokenId == CLS_TOKEN_ID || tokenId == SEP_TOKEN_ID || tokenId == 0) {
                continue;
            }
            String token = idToToken.getOrDefault(tokenId.intValue(), "");
            if (token.startsWith("##")) {
                builder.append(token.substring(2));
            } else if (!builder.isEmpty()) {
                builder.append(' ').append(token);
            } else {
                builder.append(token);
            }
        }
        return builder.toString().trim();
    }

    int decoderStartTokenId() {
        return tokenToId.getOrDefault("[DEC]", CLS_TOKEN_ID);
    }

    int eosTokenId() {
        return SEP_TOKEN_ID;
    }
}
