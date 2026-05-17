"""Utility functions for TTS Service."""

import io
import struct
import wave
from typing import Optional


def get_wav_info(wav_data: bytes) -> dict:
    """Get information about WAV audio data.
    
    Args:
        wav_data: Raw WAV bytes.
        
    Returns:
        Dictionary with audio info.
    """
    with io.BytesIO(wav_data) as f:
        with wave.open(f, 'rb') as wav:
            return {
                "channels": wav.getnchannels(),
                "sample_width": wav.getsampwidth(),
                "sample_rate": wav.getframerate(),
                "num_frames": wav.getnframes(),
                "duration_seconds": wav.getnframes() / wav.getframerate()
            }


def convert_bytes_to_wav(audio_bytes: bytes, sample_rate: int = 24000, channels: int = 1) -> bytes:
    """Convert raw audio bytes to WAV format.
    
    Args:
        audio_bytes: Raw audio bytes (16-bit PCM).
        sample_rate: Sample rate in Hz.
        channels: Number of channels.
        
    Returns:
        WAV formatted bytes.
    """
    buffer = io.BytesIO()
    with wave.open(buffer, 'wb') as wav:
        wav.setnchannels(channels)
        wav.setsampwidth(2)  # 16-bit
        wav.setframerate(sample_rate)
        wav.writeframes(audio_bytes)
    
    return buffer.getvalue()


def calculate_audio_duration(bytes_size: int, sample_rate: int = 24000, channels: int = 1, bits_per_sample: int = 16) -> float:
    """Calculate audio duration from file size.
    
    Args:
        bytes_size: Audio file size in bytes.
        sample_rate: Sample rate in Hz.
        channels: Number of channels.
        bits_per_sample: Bits per sample.
        
    Returns:
        Duration in seconds.
    """
    bytes_per_sample = bits_per_sample // 8
    bytes_per_frame = bytes_per_sample * channels
    num_frames = bytes_size / bytes_per_frame
    return num_frames / sample_rate


def generate_silence(duration_seconds: float, sample_rate: int = 24000, channels: int = 1) -> bytes:
    """Generate silent audio.
    
    Args:
        duration_seconds: Duration in seconds.
        sample_rate: Sample rate in Hz.
        channels: Number of channels.
        
    Returns:
        WAV formatted silence bytes.
    """
    num_frames = int(duration_seconds * sample_rate)
    silence = bytes(num_frames * channels * 2)  # 16-bit silence
    
    return convert_bytes_to_wav(silence, sample_rate, channels)


def normalize_text_for_tts(text: str) -> str:
    """Normalize text for better TTS output.
    
    Args:
        text: Input text.
        
    Returns:
        Normalized text.
    """
    import re
    
    # Replace common abbreviations
    replacements = {
        r'\bDr\.': 'Doctor',
        r'\bMr\.': 'Mister',
        r'\bMrs\.': 'Missus',
        r'\bMs\.': 'Miss',
        r'\bProf\.': 'Professor',
        r'\bU\.S\.': 'U S',
        r'\bU\.K\.': 'U K',
        r'\bE\.g\.': 'for example',
        r'\bI\.e\.': 'that is',
    }
    
    result = text
    for pattern, replacement in replacements.items():
        result = re.sub(pattern, replacement, result)
    
    # Remove extra whitespace
    result = re.sub(r'\s+', ' ', result).strip()
    
    # Ensure sentence ends with punctuation
    if result and result[-1] not in '.!?':
        result += '.'
    
    return result
