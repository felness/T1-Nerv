from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import JSONResponse
from io import BytesIO
from PIL import Image
import os
from app.service import get_relevance_extracted_text

app = FastAPI()

UPLOAD_FOLDER = "images"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@app.get("/")
async def root():
    return {"message": "API работает!"}


@app.post("/extract_text/")
async def extract_text_from_image(file: UploadFile = File(...)):
    try:
        image_bytes = await file.read()
        image = Image.open(BytesIO(image_bytes))

        temp_path = os.path.join(UPLOAD_FOLDER, file.filename)
        image.save(temp_path)

        extracted_text = get_relevance_extracted_text(temp_path)

        os.remove(temp_path)

        return JSONResponse(content=extracted_text)

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
