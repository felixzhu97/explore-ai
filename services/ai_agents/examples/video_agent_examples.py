"""Usage examples for the Video Agent.

This module demonstrates how to use the Video Agent for various video generation tasks.
"""

from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage
from services.ai_agents.agents import VideoAgent
from services.ai_agents.tools import get_all_video_tools


def example_basic_video_generation():
    """Basic video generation example."""
    print("=" * 60)
    print("Example 1: Basic Video Generation")
    print("=" * 60)
    
    # Initialize LLM
    llm = ChatOpenAI(model="gpt-4")
    
    # Initialize agent with default tools
    agent = VideoAgent(llm=llm)
    
    # Generate a simple video
    result = agent.invoke({
        "messages": [HumanMessage(
            content="Generate a video of a sunset over the ocean with waves crashing on the beach"
        )]
    })
    
    print("Result:", result)
    print()


def example_vertical_video_for_social_media():
    """Generate vertical video for social media platforms."""
    print("=" * 60)
    print("Example 2: Vertical Video for Social Media")
    print("=" * 60)
    
    llm = ChatOpenAI(model="gpt-4")
    agent = VideoAgent(llm=llm)
    
    # Generate vertical video (9:16 aspect ratio) for TikTok/Reels
    result = agent.generate(
        prompt="A person doing yoga on a beach at sunrise, peaceful meditation pose",
        duration=5,
        aspect_ratio="9:16",
        quality="high",
        negative_prompt="people, text, logo"
    )
    
    print("Result:", result)
    print()


def example_check_video_status():
    """Check the status of a video generation task."""
    print("=" * 60)
    print("Example 3: Check Video Status")
    print("=" * 60)
    
    llm = ChatOpenAI(model="gpt-4")
    agent = VideoAgent(llm=llm)
    
    # Check status of a specific task
    task_id = "abc123xyz"
    result = agent.check_status(task_id)
    
    print("Result:", result)
    print()


def example_advanced_video_generation():
    """Generate video with advanced parameters."""
    print("=" * 60)
    print("Example 4: Advanced Video Generation")
    print("=" * 60)
    
    llm = ChatOpenAI(model="gpt-4")
    agent = VideoAgent(llm=llm)
    
    # Generate with advanced parameters
    result = agent.generate_advanced(
        prompt="A futuristic city at night with flying cars and holographic advertisements",
        duration=10,
        aspect_ratio="16:9",
        fps=30,
        quality="high",
        style="cinematic",
        cfg_scale=10.0,
        motion_intensity=1.2
    )
    
    print("Result:", result)
    print()


def example_get_providers():
    """Get information about available video providers."""
    print("=" * 60)
    print("Example 5: Get Provider Information")
    print("=" * 60)
    
    llm = ChatOpenAI(model="gpt-4")
    agent = VideoAgent(llm=llm)
    
    result = agent.get_providers()
    
    print("Result:", result)
    print()


def example_with_custom_tools():
    """Use agent with a subset of tools."""
    print("=" * 60)
    print("Example 6: Custom Tool Selection")
    print("=" * 60)
    
    llm = ChatOpenAI(model="gpt-4")
    
    # Only use generation tools, not status checking
    tools = get_all_video_tools()[:2]  # video_generate and video_generate_advanced
    
    agent = VideoAgent(llm=llm, tools=tools)
    
    result = agent.invoke({
        "messages": [HumanMessage(
            content="Show me what video generation tools are available"
        )]
    })
    
    print("Result:", result)
    print()


def example_natural_language_to_video():
    """Convert natural language requests to video generation."""
    print("=" * 60)
    print("Example 7: Natural Language to Video")
    print("=" * 60)
    
    llm = ChatOpenAI(model="gpt-4")
    agent = VideoAgent(llm=llm)
    
    # Natural language request
    natural_requests = [
        "Create a 10-second cinematic video of a forest in autumn with falling leaves",
        "Make a vertical video of a coffee shop interior for Instagram",
        "Generate an abstract art video with flowing colors",
    ]
    
    for request in natural_requests:
        print(f"\nProcessing: {request}")
        result = agent.invoke({
            "messages": [HumanMessage(content=request)]
        })
        print("Result:", result)
    print()


if __name__ == "__main__":
    # Run all examples
    example_basic_video_generation()
    example_vertical_video_for_social_media()
    example_check_video_status()
    example_advanced_video_generation()
    example_get_providers()
    example_with_custom_tools()
    example_natural_language_to_video()
