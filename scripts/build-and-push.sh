#!/usr/bin/env bash
# barofit-routine 이미지를 빌드해서 AWS ECR 또는 Docker Hub에 푸시한다.
# docker login은 미리 1회 수동으로 해둔다(자격증명을 이 스크립트에 넣지 않음).
#
# 사용법 (ECR):       ECR_REGISTRY=<account-id>.dkr.ecr.<region>.amazonaws.com ./scripts/build-and-push.sh
# 사용법 (DockerHub): DOCKERHUB_USER=<hub-user> ./scripts/build-and-push.sh
set -euo pipefail
cd "$(dirname "$0")/.."

SVC=routine
TAG="${TAG:-$(git rev-parse --short HEAD)}"

if [ -n "${ECR_REGISTRY:-}" ]; then
  image="${ECR_REGISTRY%/}/barofit-${SVC}:${TAG}"
  PLATFORM="${PLATFORM:-linux/amd64}"
elif [ -n "${DOCKERHUB_USER:-}" ]; then
  image="docker.io/${DOCKERHUB_USER}/barofit-${SVC}:${TAG}"
  PLATFORM="${PLATFORM:-}"
else
  echo "오류: ECR_REGISTRY 또는 DOCKERHUB_USER 환경변수 중 하나가 필요합니다." >&2
  exit 1
fi

# barofit-common(barofit-runtime)이 아직 원격 저장소에 없는 동안의 로컬 전용 임시방편.
# 실 퍼블리시가 생기면 이 블록과 Dockerfile의 COPY .m2-cache 줄을 제거한다.
rm -rf .m2-cache && mkdir -p .m2-cache/com
cp -r "$HOME/.m2/repository/com/barofit" .m2-cache/com/
trap 'rm -rf .m2-cache' EXIT

echo "==> building ${image}"
if [ -n "${PLATFORM}" ]; then
  docker build --platform "${PLATFORM}" -t "${image}" .
else
  docker build -t "${image}" .
fi
echo "==> pushing ${image}"
docker push "${image}"
echo "TAG=${TAG}"
