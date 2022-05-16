#!/bin/sh

UPS_CONNECTION=${UPSMON_NAME:-ups}@${UPSMON_HOST:-localhost}:${UPSMON_PORT:-3493}
echo "MONITOR ${UPS_CONNECTION}  0 ${UPSMON_USERNAME:-username} ${UPSMON_PASSWORD:-password} slave" >>/etc/nut/upsmon.conf

if /usr/bin/upsc "${UPS_CONNECTION}" ups.status | grep OL; then
    /ups-event-handler online
fi

/usr/sbin/upsmon -D
