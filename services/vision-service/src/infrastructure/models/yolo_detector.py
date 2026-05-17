"""YOLO object detector."""
import time
from PIL import Image
from ultralytics import YOLO
from loguru import logger


class YOLODetector:
    """Object detector using YOLOv8."""

    def __init__(self, model_name: str = "yolov8n"):
        self.model_name = model_name
        self._model = None

    @property
    def model(self) -> YOLO:
        if self._model is None:
            logger.info(f"Loading YOLO model: {self.model_name}")
            self._model = YOLO(self.model_name)
            logger.info("YOLO model loaded successfully")
        return self._model

    async def detect(
        self, image: Image.Image, conf_threshold: float = 0.25
    ) -> dict:
        """Detect objects in image using YOLO."""
        start = time.time()

        # Convert PIL to numpy for YOLO
        import numpy as np
        img_array = np.array(image)

        # Run inference
        results = self.model(img_array, conf=conf_threshold, verbose=False)

        detections = []
        if len(results) > 0 and results[0].boxes is not None:
            boxes = results[0].boxes
            for box in boxes:
                xyxy = box.xyxy[0].cpu().numpy()
                conf = float(box.conf[0].cpu().numpy())
                cls_id = int(box.cls[0].cpu().numpy())
                cls_name = self.model.names[cls_id]

                detections.append({
                    "class_name": cls_name,
                    "confidence": round(conf, 4),
                    "bbox": [int(x) for x in xyxy]
                })

        width, height = image.size
        return {
            "task": "detect_objects",
            "model": self.model_name,
            "detections": detections,
            "image_width": width,
            "image_height": height,
            "processing_time_ms": (time.time() - start) * 1000,
        }
