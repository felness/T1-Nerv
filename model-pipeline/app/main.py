from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from io import BytesIO
from PIL import Image
import os
import torch
from transformers import CLIPProcessor, CLIPModel, pipeline
import easyocr
import aiofiles
import aiofiles.os as aio_os
from app.service import get_relevance_extracted_text

app = FastAPI()

UPLOAD_FOLDER = "images"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
print(f"[Init] Используемое устройство: {device}")

print("[Init] Загрузка EasyOCR...")
global_reader = easyocr.Reader(['ru', 'en'], gpu=torch.cuda.is_available())

print("[Init] Загрузка CLIP...")
global_clip_model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32").to(device)
global_clip_processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

print("[Init] Загрузка TrOCR pipeline...")
global_text_pipeline = pipeline(
    "image-to-text",
    model="raxtemur/trocr-base-ru",
    device=0 if torch.cuda.is_available() else -1
)

@app.post("/extract_text/")
async def extract_text_from_image(file: UploadFile = File(...)):
    try:
        image_bytes = await file.read()
        temp_path = os.path.join(UPLOAD_FOLDER, file.filename)

        async with aiofiles.open(temp_path, 'wb') as out_file:
            await out_file.write(image_bytes)

        extracted_text = get_relevance_extracted_text(
            temp_path,
            global_reader,
            global_clip_model,
            global_clip_processor,
            global_text_pipeline
        )

        await aio_os.remove(temp_path)
        return JSONResponse(content=extracted_text)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
