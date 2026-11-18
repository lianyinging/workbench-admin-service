# 云效「Docker 部署」执行脚本（部署组每台机器执行）
# $image 由流水线上游「镜像构建」步骤注入，勿改名

CONTAINER_NAME="cm-opc-workbench-admin-service"
SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-prod}"
JAVA_OPTS="${JAVA_OPTS:-}"

if [ "$(docker ps -aq -f name=^/${CONTAINER_NAME}$)" ]; then
  echo "停止并删除旧容器: $CONTAINER_NAME"
  docker stop "$CONTAINER_NAME" || true
  docker rm "$CONTAINER_NAME" || true
fi

for img in $(docker images --format '{{.Repository}}:{{.Tag}} {{.ID}}' | grep -i 'opc-workbench-admin-service' | awk '{print $2}'); do
  echo "尝试删除旧镜像: $img"
  docker rmi "$img" || true
done

docker pull "$image" || true

mkdir -p /data/opc-workbench/admin-service/logs \
         /data/opc-workbench/admin-service/temp

docker run -d \
  --restart=always \
  --name="$CONTAINER_NAME" \
  -p 9080:9080 \
  -p 29080:29080 \
  -e SPRING_PROFILES_ACTIVE="$SPRING_PROFILES_ACTIVE" \
  -e JAVA_OPTS="$JAVA_OPTS" \
  -e TZ=Asia/Shanghai \
  -v /data/opc-workbench/admin-service/logs:/ruoyi/server/logs \
  -v /data/opc-workbench/admin-service/temp:/ruoyi/server/temp \
  "$image"

echo "已启动 $CONTAINER_NAME -> http://0.0.0.0:9080  (profile=$SPRING_PROFILES_ACTIVE)"
docker ps -f name=^/${CONTAINER_NAME}$
