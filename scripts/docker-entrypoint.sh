#!/bin/sh
set -eu

install -d -m 700 -o 1001 -g 1001 /app/secrets

if [ -f /run/secrets/jwt/jwt-private.pem ]; then
  install -m 600 -o 1001 -g 1001 /run/secrets/jwt/jwt-private.pem /app/secrets/jwt-private.pem
fi

install -m 600 -o 1001 -g 1001 /run/secrets/jwt/jwt-jwks.json /app/secrets/jwt-jwks.json

exec setpriv --reuid=1001 --regid=1001 --clear-groups java -jar /app/app.jar
