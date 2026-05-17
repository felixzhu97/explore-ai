from fastapi import APIRouter, UploadFile, File, HTTPException, Depends
from PIL import Image
import io
from ..application.dtos.vision_dtos import (
    TaskType,
    DetectionResponseDTO as DetectionResponse,
    CaptionResponseDTO as CaptionResponse,
    OCRResponseDTO as OCRResponse,
    AnalyzeImageResponseDTO,
    AnalyzeImageRequestDTO,
)
from ..application.use_cases.analyze_image import AnalyzeImageInput, AnalyzeImageUseCase
from ..core.di.container import (
    get_yolo,
    get_blip,
    get_easyocr,
    get_analyze_image_use_case,
)
from ..core.config.settings import get_settings
from ..domain.ports import IObjectDetector, IImageCaptioner, IOCRProcessor

router = APIRouter(prefix="/vision", tags=["vision"])


async def load_image(file: UploadFile) -> Image.Image:
    settings = get_settings()
    contents = await file.read()
    if len(contents) > settings.MAX_IMAGE_SIZE:
        raise HTTPException(400, f"Image too large (max {settings.MAX_IMAGE_SIZE // 1024 // 1024}MB)")
    try:
        return Image.open(io.BytesIO(contents)).convert("RGB")
    except Exception:
        raise HTTPException(400, "Invalid image file")


@router.post("/detect", response_model=DetectionResponse)
async def detect_objects(
    file: UploadFile = File(...),
    conf: float = 0.25,
    detector: IObjectDetector = Depends(get_yolo)
):
    image = await load_image(file)
    return await detector.detect(image, conf_threshold=conf)


@router.post("/caption", response_model=CaptionResponse)
async def caption_image(
    file: UploadFile = File(...),
    captioner: IImageCaptioner = Depends(get_blip)
):
    image = await load_image(file)
    return await captioner.caption(image)


@router.post("/ocr", response_model=OCRResponse)
async def extract_text(
    file: UploadFile = File(...),
    ocr: IOCRProcessor = Depends(get_easyocr),
    engine: str = "easyocr"
):
    image = await load_image(file)
    return await ocr.extract_text(image, engine=engine)


@router.post("/analyze", response_model=AnalyzeImageResponseDTO)
async def analyze_image(
    file: UploadFile = File(...),
    task: TaskType = TaskType.CAPTION_IMAGE,
    use_case: AnalyzeImageUseCase = Depends(get_analyze_image_use_case),
):
    image = await load_image(file)
    input_data = AnalyzeImageInput(image=image, task=task)
    return await use_case.execute(input_data)
