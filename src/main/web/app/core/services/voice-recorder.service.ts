import { Injectable } from '@angular/core';
import { encodeWav16kMonoBase64 } from '@core/utils/wav-encoder.util';

const CHUNK_INTERVAL_MS = 1_500;

@Injectable({ providedIn: 'root' })
export class VoiceRecorderService {
  private mediaStream: MediaStream | null = null;
  private audioContext: AudioContext | null = null;
  private sourceNode: MediaStreamAudioSourceNode | null = null;
  private processorNode: ScriptProcessorNode | null = null;
  private chunkTimer: ReturnType<typeof setInterval> | null = null;
  private pendingSamples: number[] = [];
  private onChunk: ((base64Wav: string) => void) | null = null;

  async start(onChunk: (base64Wav: string) => void): Promise<void> {
    this.stop();
    this.onChunk = onChunk;
    this.pendingSamples = [];

    this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    this.audioContext = new AudioContext();
    this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream);
    this.processorNode = this.audioContext.createScriptProcessor(4096, 1, 1);

    this.processorNode.onaudioprocess = (event) => {
      const input = event.inputBuffer.getChannelData(0);
      for (const sample of input) {
        this.pendingSamples.push(sample);
      }
    };

    this.sourceNode.connect(this.processorNode);
    this.processorNode.connect(this.audioContext.destination);

    this.chunkTimer = setInterval(() => this.flushChunk(), CHUNK_INTERVAL_MS);
  }

  stop(): void {
    if (this.chunkTimer) {
      clearInterval(this.chunkTimer);
      this.chunkTimer = null;
    }
    this.flushChunk();

    this.processorNode?.disconnect();
    this.sourceNode?.disconnect();
    this.processorNode = null;
    this.sourceNode = null;

    this.mediaStream?.getTracks().forEach(track => track.stop());
    this.mediaStream = null;

    if (this.audioContext) {
      void this.audioContext.close();
      this.audioContext = null;
    }
    this.onChunk = null;
    this.pendingSamples = [];
  }

  private flushChunk(): void {
    if (!this.onChunk || this.pendingSamples.length === 0 || !this.audioContext) {
      return;
    }
    const samples = new Float32Array(this.pendingSamples);
    this.pendingSamples = [];
    const base64 = encodeWav16kMonoBase64(samples, this.audioContext.sampleRate);
    this.onChunk(base64);
  }
}
