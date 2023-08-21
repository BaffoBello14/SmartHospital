#!/bin/bash

docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "
cd ~/contiki-ng/IOT/rpl-border-router && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM0 border-router.dfu-upload || true && 
cd ~/contiki-ng/IOT/actuators/mask && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1 mask.dfu-upload || true && 
cd ~/contiki-ng/IOT/actuators/defibrillator && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM2 defibrillator.dfu-upload || true && 
cd ~/contiki-ng/IOT/actuators/medicine && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM3 medicine.dfu-upload || true && 
cd ~/contiki-ng/IOT/sensors/oxygen && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM4 oxygen.dfu-upload || true && 
cd ~/contiki-ng/IOT/sensors/troponin && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM5 troponin.dfu-upload || true && 
cd ~/contiki-ng/IOT/sensors/cardio && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM6 cardio.dfu-upload || true
"