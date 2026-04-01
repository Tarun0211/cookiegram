FROM mongo:7.0

COPY dump /dump
COPY restore.sh /docker-entrypoint-initdb.d/restore.sh

RUN sed -i 's/\r$//' /docker-entrypoint-initdb.d/restore.sh && \
    chmod +x /docker-entrypoint-initdb.d/restore.sh