from PIL import Image
import torch
from transformers import CLIPProcessor, CLIPModel, pipeline
import easyocr
import cv2
import numpy as np
import torchvision

# Классификация CLIP
def classify_clip(image, model, processor):
    labels = ["handwritten text", "printed text"]
    image = image.convert("RGB")
    inputs = processor(text=labels, images=image, return_tensors="pt", padding=True)

    with torch.no_grad():
        outputs = model(**inputs)
        logits_per_image = outputs.logits_per_image
        probs = logits_per_image.softmax(dim=1).squeeze()

    predicted = labels[probs.argmax()]
    return predicted, probs.max().item()

# Детекция + классификация
def extract_and_classify_text_regions(image_path):
    reader = easyocr.Reader(['ru', 'en'], gpu=True)
    model = CLIPModel.from_pretrained("openai/clip-vit-base-patch32")
    processor = CLIPProcessor.from_pretrained("openai/clip-vit-base-patch32")

    img = cv2.imread(image_path)
    if img is None:
        raise ValueError("Не удалось загрузить изображение")

    results = reader.readtext(image_path, detail=1)
    regions = []

    for i, (bbox, text, prob) in enumerate(results):
        (tl, tr, br, bl) = bbox
        x_min = int(min(tl[0], bl[0]))
        y_min = int(min(tl[1], tr[1]))
        x_max = int(max(tr[0], br[0]))
        y_max = int(max(bl[1], br[1]))

        text_region = img[y_min:y_max, x_min:x_max]
        if text_region.size == 0:
            continue

        text_region_pil = Image.fromarray(cv2.cvtColor(text_region, cv2.COLOR_BGR2RGB))
        predicted_label, confidence = classify_clip(text_region_pil, model, processor)

        regions.append({
            'image': text_region_pil,
            'bbox': (x_min, y_min, x_max, y_max),
            'label': predicted_label.replace(" text", ""),  # "handwritten" / "printed"
            'confidence': confidence
        })

    return regions

# Постобработка: NMS
def apply_nms(regions, iou_threshold=0.3):
    if len(regions) == 0:
        return []

    boxes = [list(region['bbox']) for region in regions]
    scores = [region['confidence'] for region in regions]

    boxes_tensor = torch.tensor(boxes, dtype=torch.float32)
    scores_tensor = torch.tensor(scores)

    keep_indices = torchvision.ops.nms(boxes_tensor, scores_tensor, iou_threshold)
    filtered = [regions[i] for i in keep_indices]
    return filtered

def get_relevance_extracted_text(image_path):
    regions = extract_and_classify_text_regions(image_path)
    pipe = pipeline("image-to-text", model="raxtemur/trocr-base-ru", device=0)

    output_data = []
    for region in regions:
        image = region['image']
        result = pipe(image)

        output_data.append({
            "text": result[0]['generated_text'],
            "bbox": list(region['bbox']),
            "label": region['label'],
            "confidence": round(region['confidence'], 4)
        })

    final_json = {
        "results": output_data
    }
    

    return final_json

# def visualize_text_blocks(image_path, regions, save_path="visualized_output.jpg"):
#     img = cv2.imread(image_path)

#     for region in regions:
#         x_min, y_min, x_max, y_max = region['bbox']
#         cv2.rectangle(img, (x_min, y_min), (x_max, y_max), (0, 255, 0), 2)

#     cv2.imwrite(save_path, img)
#     print(f"Визуализация сохранена в {save_path}")
