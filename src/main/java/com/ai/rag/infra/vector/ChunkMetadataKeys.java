package com.ai.rag.infra.vector;

/** Metadata keys shared by chunk storage, retrieval filters and source citations. */
public final class ChunkMetadataKeys {

  public static final String TITLE = "title";
  public static final String FILE_NAME = "fileName";
  public static final String DOCUMENT_ID = "document_id";

  /** Retrieval filter key only; the owner travels on {@code DocumentChunk#getOwnerKey()}. */
  public static final String OWNER_KEY = "ownerKey";

  private ChunkMetadataKeys() {}
}
