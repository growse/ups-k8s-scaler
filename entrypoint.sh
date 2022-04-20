#!/bin/sh

echo "MONITOR ${UPSMON_NAME:-ups}@${UPSMON_HOST:-localhost} ${UPSMON_MINONLINE:-1} ${UPSMON_USERNAME:-username} ${UPSMON_PASSWORD:-password} slave" >> /etc/nut/upsmon.conf

/usr/sbin/upsmon -D