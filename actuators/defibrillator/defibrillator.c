#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "coap-blocking-api.h"

#include "node-id.h"
#include "net/ipv6/simple-udp.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-ds6.h"
#include "net/ipv6/uip-debug.h"
#include "routing/routing.h"

#define SERVER_EP "coap://[fd00::1]:5683"
#define CONNECTION_TRY_INTERVAL 1
#define REGISTRATION_TRY_INTERVAL 1
#define SIMULATION_INTERVAL 1

#define DO_REGISTER 1

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "defibrillator"
#define LOG_LEVEL LOG_LEVEL_DBG

#define INTERVAL_BETWEEN_CONNECTION_TESTS 1

extern coap_resource_t res_shock;

#define SENSOR_TYPE "{\"deviceType\": \"defibrillator\", \"sensorId\": %u}"

#ifdef DO_REGISTER
char *service_url = "/registration";
static bool registered = false;

#endif

static struct etimer connectivity_timer;
static struct etimer wait_registration;

...
// Il resto del codice segue la stessa logica del file "water_pump.c"
...
