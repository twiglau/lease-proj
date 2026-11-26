# 基本使用

```shell
curl -XPOST 'http://127.0.0.1:8080/admin/label/saveOrUpdate' \
     -H 'Content-Type: application/json' \
     -d '{"type":1, "name":"近商圈"}'

#参数多是写到文件里更清楚
curl -X POST 'http://127.0.0.1:8080/admin/label/saveOrUpdate' \
     -H 'Content-Type: application/json' \
     --data @body.json

# 可以放到临时目录
cat > /tmp/body.json << 'JSON'
{
   "type": 1,
   "name": "近商圈“
}
JSON

# -i  查看状态码和响应体
curl -i -X POST 'http://127.0.0.1:8080/admin/label/saveOrUpdate' \
     -H 'Content-Type: application/json' \
     --data @/tmp/body.json
```