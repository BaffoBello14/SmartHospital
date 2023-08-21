#!/bin/bash

# Remove all containers
docker rm -f $(docker ps -aq)

# Run commands in each terminal
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/rpl-border-router && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM0 connect-router; bash"'
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/actuators/mask && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM1 login; bash"'
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/actuators/defibrillator && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM2 login; bash"'
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/actuators/medicine && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM3 login; bash"'
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/sensors/oxygen && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM4 login; bash"'
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/sensors/troponin && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM5 login; bash"'
gnome-terminal -- bash -c 'docker run --privileged --network host --mount type=bind,source=/home/osboxes/contiki-ng,destination=/home/user/contiki-ng -e DISPLAY=:0 -v /tmp/.X11-unix:/tmp/.X11-unix -v /dev/bus/usb:/dev/bus/usb -ti contiker/contiki-ng bash -c "cd ~/contiki-ng/IOT/sensors/cardio && make TARGET=nrf52840 BOARD=dongle PORT=/dev/ttyACM6 login; bash"'

# Run mvn install and java jar
cd ~/contiki-ng/IOT/
mvn install
mysql -u root -p iot22-23 iot < database.sql
gnome-terminal -- bash -c 'java -jar target/iot-1.0-SNAPSHOT.jar'