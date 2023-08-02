#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include <string.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* A simple actuator example. Toggles the red led */
RESOURCE(res_oxygen,
         "title=\"Oxygen actuator\";methods=\"PUT\";rt=\"Control\"",
         NULL,
         res_put_handler,
         NULL,
         NULL);

static void
res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  const uint8_t *payload;
  char *action;
  size_t len = 0;
  int success = 1;

  len = coap_get_payload(request, &payload);
  action = (char *)payload;

  if(strcmp(action, "ON") == 0) {
    // Activate the actuator at low level
    leds_on(LEDS_GREEN); 
    leds_off(LEDS_RED);
  }
  else if(strcmp(action, "OFF") == 0) {
    // Deactivate the actuator
    leds_off(LEDS_GREEN);
    leds_off(LEDS_RED);
  }
  else if(strcmp(action, "POWER") == 0) {
    // Activate the actuator at high level
    leds_on(LEDS_GREEN);
    leds_on(LEDS_RED);
  }
  else {
    success = 0;
  }

  if (!success)
    coap_set_status_code(response, BAD_REQUEST_4_00);
  else
    coap_set_status_code(response, CHANGED_2_04);
}
