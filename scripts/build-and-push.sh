#!/usr/bin/env bash
# barofit-identity 이미지를 빌드해서 Docker Hub(public)에 푸시한다.
# docker login은 미리 1회 수동으로 해둔다(자격증명을 이 스크립트에 넣지 않음).
#
# 사용법: DOCKERHUB_USER=<hub-user> ./scripts/build-and-push.sh
set -euo pipefail
cd "$(dirname "$0")/.."

DOCKERHUB_USER="${DOCKERHUB_USER:?DOCKERHUB_USER 환경변수가 필요합니다}"
TAG="${TAG:-$(git rev-parse --short HEAD)}"
SVC=routine

# barofit-common(barofit-runtime)이 아직 원격 저장소에 없는 동안의 로컬 전용 임시방편.
# 실 퍼블리시가 생기면 이 블록과 Dockerfile의 COPY .m2-cache 줄을 제거한다.
rm -rf .m2-cache && mkdir -p .m2-cache/com
cp -r "$HOME/.m2/repository/com/barofit" .m2-cache/com/
trap 'rm -rf .m2-cache' EXIT

image="docker.io/${DOCKERHUB_USER}/barofit-${SVC}:${TAG}"
echo "==> building ${image}"
docker build -t "${image}" .
echo "==> pushing ${image}"
docker push "${image}"
echo "TAG=${TAG}"
