#!/usr/bin/env python3
"""
Stable Diffusion inference script for Java Gateway.
This script is called by DiffusionLocalAdapter via ProcessBuilder.

Usage:
    python3 run_diffusion.py <params.json>
    
Expected params.json structure:
{
    "model_id": "stabilityai/stable-diffusion-2-1",
    "model_path": "/path/to/model",
    "prompt": "a beautiful landscape",
    "negative_prompt": "blurry, low quality",
    "width": 512,
    "height": 512,
    "num_inference_steps": 25,
    "guidance_scale": 7.5,
    "seed": 42,
    "output_path": "/tmp/output.png",
    "device": "cuda",
    "enable_xformers": false
}
"""

import argparse
import json
import os
import sys
import time
from pathlib import Path

# Try to import diffusers, install if not available
try:
    from diffusers import StableDiffusionPipeline, DPMSolverMultistepScheduler
    import torch
except ImportError as e:
    print(f"ERROR: Missing required packages. Please install: pip install diffusers torch", file=sys.stderr)
    print(f"Import error: {e}", file=sys.stderr)
    sys.exit(1)


class StableDiffusionRunner:
    """Manages Stable Diffusion model loading and inference."""
    
    _instance = None
    _pipeline = None
    _current_model_id = None
    _device = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
        return cls._instance
    
    def load_model(self, model_path: str, model_id: str, device: str, enable_xformers: bool = False) -> bool:
        """
        Load or reuse cached model pipeline.
        Returns True if model was loaded successfully.
        """
        # Check if we can reuse the existing pipeline
        if self._pipeline is not None and self._current_model_id == model_id:
            print(f"Reusing cached model: {model_id}")
            return True
        
        print(f"Loading model: {model_id} on device: {device}")
        start_time = time.time()
        
        try:
            # Resolve model path
            if os.path.exists(model_path):
                model_to_load = model_path
            else:
                model_to_load = model_id
            
            # Create pipeline
            if device == "cuda" and torch.cuda.is_available():
                self._pipeline = StableDiffusionPipeline.from_pretrained(
                    model_to_load,
                    torch_dtype=torch.float16,
                    safety_checker=None,
                    requires_safety_checker=False
                )
                self._pipeline = self._pipeline.to("cuda")
                
                # Enable memory optimizations
                if enable_xformers:
                    try:
                        self._pipeline.enable_xformers_memory_efficient_attention()
                        print("Enabled xformers memory efficient attention")
                    except Exception as e:
                        print(f"Warning: Could not enable xformers: {e}")
                
                # Enable attention slicing for lower memory usage
                self._pipeline.enable_attention_slicing()
                
            elif device == "mps" and torch.backends.mps.is_available():
                self._pipeline = StableDiffusionPipeline.from_pretrained(
                    model_to_load,
                    torch_dtype=torch.float32,
                    safety_checker=None,
                    requires_safety_checker=False
                )
                self._pipeline = self._pipeline.to("mps")
                
            else:
                # CPU fallback
                self._pipeline = StableDiffusionPipeline.from_pretrained(
                    model_to_load,
                    torch_dtype=torch.float32,
                    safety_checker=None,
                    requires_safety_checker=False
                )
                self._pipeline = self._pipeline.to("cpu")
            
            # Use DPM++ solver for faster inference
            self._pipeline.scheduler = DPMSolverMultistepScheduler.from_config(
                self._pipeline.scheduler.config
            )
            
            self._current_model_id = model_id
            self._device = device
            
            elapsed = time.time() - start_time
            print(f"Model loaded successfully in {elapsed:.2f}s")
            
            # Print memory usage if CUDA
            if device == "cuda":
                memory_allocated = torch.cuda.memory_allocated() / 1024**3
                memory_reserved = torch.cuda.memory_reserved() / 1024**3
                print(f"GPU Memory: {memory_allocated:.2f}GB allocated, {memory_reserved:.2f}GB reserved")
            
            return True
            
        except Exception as e:
            print(f"ERROR: Failed to load model: {e}", file=sys.stderr)
            import traceback
            traceback.print_exc()
            return False
    
    def generate(self, params: dict) -> bool:
        """
        Generate image from parameters.
        Returns True if generation was successful.
        """
        if self._pipeline is None:
            print("ERROR: No model loaded", file=sys.stderr)
            return False
        
        try:
            prompt = params["prompt"]
            negative_prompt = params.get("negative_prompt", "")
            width = params.get("width", 512)
            height = params.get("height", 512)
            num_inference_steps = params.get("num_inference_steps", 25)
            guidance_scale = params.get("guidance_scale", 7.5)
            seed = params.get("seed", None)
            output_path = params["output_path"]
            
            print(f"Generating image: {width}x{height}, steps={num_inference_steps}, guidance={guidance_scale}")
            print(f"Prompt: {prompt[:100]}{'...' if len(prompt) > 100 else ''}")
            
            # Set random seed for reproducibility
            generator = None
            if seed is not None:
                generator = torch.Generator(device=self._device or "cpu").manual_seed(int(seed))
            
            start_time = time.time()
            
            # Generate
            with torch.inference_mode():
                image = self._pipeline(
                    prompt=prompt,
                    negative_prompt=negative_prompt,
                    width=width,
                    height=height,
                    num_inference_steps=num_inference_steps,
                    guidance_scale=guidance_scale,
                    generator=generator
                ).images[0]
            
            elapsed = time.time() - start_time
            print(f"Generation completed in {elapsed:.2f}s ({elapsed/num_inference_steps*1000:.1f}ms/step)")
            
            # Save image
            os.makedirs(os.path.dirname(output_path), exist_ok=True)
            image.save(output_path)
            print(f"Image saved to: {output_path}")
            
            return True
            
        except Exception as e:
            print(f"ERROR: Generation failed: {e}", file=sys.stderr)
            import traceback
            traceback.print_exc()
            return False


def main():
    parser = argparse.ArgumentParser(description="Stable Diffusion inference script")
    parser.add_argument("params_file", help="Path to JSON parameters file")
    args = parser.parse_args()
    
    # Load parameters
    try:
        with open(args.params_file, "r") as f:
            params = json.load(f)
    except Exception as e:
        print(f"ERROR: Failed to load parameters: {e}", file=sys.stderr)
        sys.exit(1)
    
    # Create runner and load model
    runner = StableDiffusionRunner()
    
    model_path = params.get("model_path", "")
    model_id = params.get("model_id", "stabilityai/stable-diffusion-2-1")
    device = params.get("device", "cpu")
    enable_xformers = params.get("enable_xformers", False)
    
    if not runner.load_model(model_path, model_id, device, enable_xformers):
        sys.exit(1)
    
    # Generate image
    if runner.generate(params):
        sys.exit(0)
    else:
        sys.exit(1)


if __name__ == "__main__":
    main()
