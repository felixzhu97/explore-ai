package com.ai.rag.infrastructure.config;

import com.ai.rag.infrastructure.config.RagProperties;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RagPropertiesTest {

    @Nested
    class DefaultValues {

        @Test
        void should_have_default_chunk_size_when_created() {
            RagProperties properties = new RagProperties();

            assertEquals(500, properties.getChunk().getSize());
        }

        @Test
        void should_have_default_chunk_overlap_when_created() {
            RagProperties properties = new RagProperties();

            assertEquals(50, properties.getChunk().getOverlap());
        }

        @Test
        void should_have_default_retrieval_topK_when_created() {
            RagProperties properties = new RagProperties();

            assertEquals(5, properties.getRetrieval().getTopK());
        }

        @Test
        void should_have_default_retrieval_score_threshold_when_created() {
            RagProperties properties = new RagProperties();

            assertEquals(0.5, properties.getRetrieval().getScoreThreshold());
        }
    }

    @Nested
    class ChunkConfiguration {

        @Test
        void should_update_chunk_size_via_setter() {
            RagProperties properties = new RagProperties();

            properties.getChunk().setSize(1000);

            assertEquals(1000, properties.getChunk().getSize());
        }

        @Test
        void should_update_chunk_overlap_via_setter() {
            RagProperties properties = new RagProperties();

            properties.getChunk().setOverlap(100);

            assertEquals(100, properties.getChunk().getOverlap());
        }

        @Test
        void should_update_entire_chunk_object() {
            RagProperties properties = new RagProperties();
            RagProperties.Chunk newChunk = new RagProperties.Chunk();
            newChunk.setSize(800);
            newChunk.setOverlap(80);

            properties.setChunk(newChunk);

            assertEquals(800, properties.getChunk().getSize());
            assertEquals(80, properties.getChunk().getOverlap());
        }
    }

    @Nested
    class RetrievalConfiguration {

        @Test
        void should_update_retrieval_topK_via_setter() {
            RagProperties properties = new RagProperties();

            properties.getRetrieval().setTopK(10);

            assertEquals(10, properties.getRetrieval().getTopK());
        }

        @Test
        void should_update_retrieval_score_threshold_via_setter() {
            RagProperties properties = new RagProperties();

            properties.getRetrieval().setScoreThreshold(0.8);

            assertEquals(0.8, properties.getRetrieval().getScoreThreshold());
        }

        @Test
        void should_update_entire_retrieval_object() {
            RagProperties properties = new RagProperties();
            RagProperties.Retrieval newRetrieval = new RagProperties.Retrieval();
            newRetrieval.setTopK(15);
            newRetrieval.setScoreThreshold(0.75);

            properties.setRetrieval(newRetrieval);

            assertEquals(15, properties.getRetrieval().getTopK());
            assertEquals(0.75, properties.getRetrieval().getScoreThreshold());
        }
    }

    @Nested
    class ChunkNestedClass {

        @Test
        void should_have_default_values_when_chunk_created() {
            RagProperties.Chunk chunk = new RagProperties.Chunk();

            assertEquals(500, chunk.getSize());
            assertEquals(50, chunk.getOverlap());
        }

        @Test
        void should_accept_zero_size() {
            RagProperties.Chunk chunk = new RagProperties.Chunk();

            chunk.setSize(0);

            assertEquals(0, chunk.getSize());
        }

        @Test
        void should_accept_zero_overlap() {
            RagProperties.Chunk chunk = new RagProperties.Chunk();

            chunk.setOverlap(0);

            assertEquals(0, chunk.getOverlap());
        }
    }

    @Nested
    class RetrievalNestedClass {

        @Test
        void should_have_default_values_when_retrieval_created() {
            RagProperties.Retrieval retrieval = new RagProperties.Retrieval();

            assertEquals(5, retrieval.getTopK());
            assertEquals(0.5, retrieval.getScoreThreshold());
        }

        @Test
        void should_accept_zero_topK() {
            RagProperties.Retrieval retrieval = new RagProperties.Retrieval();

            retrieval.setTopK(0);

            assertEquals(0, retrieval.getTopK());
        }

        @Test
        void should_accept_zero_score_threshold() {
            RagProperties.Retrieval retrieval = new RagProperties.Retrieval();

            retrieval.setScoreThreshold(0.0);

            assertEquals(0.0, retrieval.getScoreThreshold());
        }

        @Test
        void should_accept_one_as_score_threshold() {
            RagProperties.Retrieval retrieval = new RagProperties.Retrieval();

            retrieval.setScoreThreshold(1.0);

            assertEquals(1.0, retrieval.getScoreThreshold());
        }
    }
}
