import os
import cv2
import numpy as np


def remove_shadows_and_noise(image):
    """
    Удаляет тени и шум, мягко снижает контраст для лучшей читаемости текста.
    """
    gray = cv2.cvtColor(image, cv2.COLOR_BGR2GRAY)

    # Удаление теней — делим изображение на фон
    dilated = cv2.morphologyEx(gray, cv2.MORPH_CLOSE, np.ones((15, 15), np.uint8))
    no_shadows = cv2.divide(gray, dilated, scale=255)

    # Сглаживаем шумы
    blurred = cv2.GaussianBlur(no_shadows, (5, 5), 0)

    # 🔽 Понижение контрастности вручную (alpha < 1)
    alpha = 1  # коэффициент контрастности (1.0 — без изменений)
    beta = 10  # добавим немного яркости
    result = cv2.convertScaleAbs(blurred, alpha=alpha, beta=beta)

    return result


def correct_rotation_to_vertical(image):
    """
    Делает изображение вертикальным: если оно горизонтальное — поворачивает на 90°.
    """
    h, w = image.shape[:2]
    if w > h:
        image = cv2.rotate(image, cv2.ROTATE_90_CLOCKWISE)
    return image


def process_jpg_images(input_folder, output_folder):
    """
    Обрабатывает .jpg: чистка + поворот в вертикальную ориентацию.
    """
    os.makedirs(output_folder, exist_ok=True)

    for filename in os.listdir(input_folder):
        if filename.lower().endswith(".jpg"):
            input_path = os.path.join(input_folder, filename)
            image = cv2.imread(input_path)

            if image is None:
                print(f"❌ Не удалось открыть: {filename}")
                continue

            cleaned = remove_shadows_and_noise(image)
            tmp = smooth_contrast(cleaned)
            tmp = apply_clahe(tmp)
            rotated = correct_rotation_to_vertical(tmp)

            # preprocessed = preprocess_document(image)

            output_path = os.path.join(output_folder, filename)
            cv2.imwrite(output_path, rotated)
            print(f"✅ Обработан: {filename}")


def smooth_contrast(image_gray):
    blurred = cv2.GaussianBlur(image_gray, (3, 3), 0)
    adjusted = cv2.convertScaleAbs(blurred, alpha=0.9, beta=10)
    return adjusted


def apply_clahe(image_gray):
    clahe = cv2.createCLAHE(clipLimit=2.0, tileGridSize=(8, 8))
    return clahe.apply(image_gray)


input_dir = "../data/PRIVATE/jpg"
output_dir = "../data/PRIVATE/output_jpg"
process_jpg_images(input_dir, output_dir)



