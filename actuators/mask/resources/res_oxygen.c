#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res_oxygen"
#define LOG_LEVEL LOG_LEVEL_DBG


static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_oxygen,
         "title=\"Oxygen mask actuator\";rt=\"Control\"",
         NULL,
         NULL,
         res_put_handler,
         NULL);

static int oxygen_level = 0;

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *level = NULL;

    // Utilizza coap_get_payload per ottenere il payload della richiesta
    len = coap_get_payload(request, (const uint8_t **)&level);
    if(len <= 0 || len >= 20)
        goto error;

    oxygen_level = atoi(level);
    
    if(oxygen_level == 0) {
        leds_set(LEDS_RED);
        LOG_INFO("Oxygen mask OFF\n");
    } else if(oxygen_level == 1) {
        leds_set(LEDS_BLUE);
        LOG_INFO("Oxygen mask ON LOW\n");
    } else if(oxygen_level == 2) {
        leds_set(LEDS_BLUE);
        LOG_INFO("Oxygen mask ON HIGH\n");
    } else {
        goto error;
    }

    coap_set_status_code(response, COAP_CHANGED_2_04);
    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
