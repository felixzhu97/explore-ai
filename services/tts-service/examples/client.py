"""TTS Client examples and usage demonstrations."""

import httpx
from typing import Optional


class TTSClient:
    """HTTP client for TTS Service."""
    
    def __init__(self, base_url: str = "http://localhost:8013", timeout: float = 60.0):
        """Initialize TTS client.
        
        Args:
            base_url: TTS service base URL.
            timeout: Request timeout in seconds.
        """
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
    
    def synthesize(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        pitch: float = 0,
        output_format: str = "mp3"
    ) -> bytes:
        """Synthesize text to speech.
        
        Args:
            text: Text to synthesize.
            voice: Voice identifier.
            language: Language code.
            speed: Speech speed multiplier.
            pitch: Pitch adjustment in Hz.
            output_format: Output format (mp3, wav, ogg).
            
        Returns:
            Audio bytes.
        """
        payload = {
            "text": text,
            "voice": voice,
            "language": language,
            "speed": speed,
            "pitch": pitch,
            "output_format": output_format
        }
        
        with httpx.Client(timeout=self.timeout) as client:
            response = client.post(
                f"{self.base_url}/tts/synthesize",
                json=payload
            )
            response.raise_for_status()
            return response.content
    
    def synthesize_to_file(
        self,
        text: str,
        output_path: str,
        **kwargs
    ) -> None:
        """Synthesize text and save to file.
        
        Args:
            text: Text to synthesize.
            output_path: Output file path.
            **kwargs: Additional synthesis parameters.
        """
        audio_data = self.synthesize(text, **kwargs)
        with open(output_path, "wb") as f:
            f.write(audio_data)
    
    def stream_synthesize(
        self,
        text: str,
        voice: Optional[str] = None,
        language: Optional[str] = None,
        speed: float = 1.0,
        output_format: str = "mp3",
        chunk_size: int = 8192
    ) -> bytes:
        """Stream synthesize text and return full audio.
        
        Args:
            text: Text to synthesize.
            voice: Voice identifier.
            language: Language code.
            speed: Speech speed multiplier.
            output_format: Output format.
            chunk_size: Chunk size for streaming.
            
        Returns:
            Complete audio bytes.
        """
        payload = {
            "text": text,
            "voice": voice,
            "language": language,
            "speed": speed,
            "output_format": output_format
        }
        
        audio_chunks = []
        with httpx.Client(timeout=self.timeout * 2) as client:
            with client.stream(
                "POST",
                f"{self.base_url}/tts/stream",
                json=payload
            ) as response:
                response.raise_for_status()
                for chunk in response.iter_bytes(chunk_size=chunk_size):
                    audio_chunks.append(chunk)
        
        return b"".join(audio_chunks)
    
    def list_voices(self, language: Optional[str] = None) -> list:
        """List available voices.
        
        Args:
            language: Optional language filter.
            
        Returns:
            List of voice dictionaries.
        """
        params = {}
        if language:
            params["language"] = language
        
        with httpx.Client(timeout=30.0) as client:
            response = client.get(
                f"{self.base_url}/tts/voices",
                params=params
            )
            response.raise_for_status()
            return response.json()
    
    def list_providers(self) -> list:
        """List available TTS providers.
        
        Returns:
            List of provider dictionaries.
        """
        with httpx.Client(timeout=30.0) as client:
            response = client.get(f"{self.base_url}/tts/providers")
            response.raise_for_status()
            return response.json()
    
    def health_check(self) -> dict:
        """Check service health.
        
        Returns:
            Health status dictionary.
        """
        with httpx.Client(timeout=10.0) as client:
            response = client.get(f"{self.base_url}/tts/health")
            response.raise_for_status()
            return response.json()


# ============================================================================
# Usage Examples
# ============================================================================

if __name__ == "__main__":
    # Initialize client
    client = TTSClient(base_url="http://localhost:8013")
    
    # Check health
    print("=== Health Check ===")
    health = client.health_check()
    print(f"Status: {health['status']}")
    print(f"Provider: {health['provider']}")
    print()
    
    # List available providers
    print("=== Available Providers ===")
    providers = client.list_providers()
    for p in providers:
        print(f"- {p['display_name']}: {len(p['supported_languages'])} languages")
    print()
    
    # List voices
    print("=== Available English Voices ===")
    voices = client.list_voices(language="en")
    for v in voices[:5]:
        default = " (default)" if v.get("is_default") else ""
        gender = f" ({v.get('gender', 'unknown')})" if v.get("gender") else ""
        print(f"- {v['name']}{gender}{default}: {v['id']}")
    print()
    
    # Synthesize speech
    print("=== Synthesize Speech ===")
    print("Synthesizing: 'Hello, world! This is a test of the text-to-speech system.'")
    
    audio = client.synthesize(
        text="Hello, world! This is a test of the text-to-speech system.",
        voice="en-US-JennyNeural",
        speed=1.0
    )
    
    print(f"Generated audio: {len(audio):,} bytes")
    
    # Save to file
    with open("hello_world.mp3", "wb") as f:
        f.write(audio)
    print("Saved to: hello_world.mp3")
    print()
    
    # Stream synthesis
    print("=== Stream Synthesis ===")
    audio_stream = client.stream_synthesize(
        text="Streaming synthesis example with real-time audio generation.",
        voice="en-US-JennyNeural",
        speed=1.1
    )
    print(f"Streamed audio: {len(audio_stream):,} bytes")
