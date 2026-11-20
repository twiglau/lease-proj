#!/bin/bash
export MINIO_ROOT_USER=minioadmin
export MINIO_ROOT_PASSWORD=minioadmin123
echo "MinIO API:      http://127.0.0.1:9000"
echo "MinIO Console:  http://127.0.0.1:9001"
echo "账号: minioadmin / minioadmin123"
echo "按 Ctrl+C 停止服务"
exec /Users/feiya/minio/bin/minio server /Users/feiya/minio/data \
  --address :9000 --console-address :9001