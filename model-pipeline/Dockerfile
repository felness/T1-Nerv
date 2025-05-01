
FROM pytorch/pytorch:2.5.0-cuda12.1-cudnn9-runtime


RUN apt-get update && apt-get install -y \
    libgl1 \
    libglib2.0-0 \
    libpng-dev \
    libjpeg-dev \
    libtiff-dev \
    && rm -rf /var/lib/apt/lists/*


WORKDIR /app


COPY app/requirements.txt .


RUN pip install --upgrade pip && \
    pip install --extra-index-url https://download.pytorch.org/whl/cu121 -r requirements.txt


COPY app/ ./


EXPOSE 8000


CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]