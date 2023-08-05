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
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_oxygen,
         "title=\"Oxygen mask actuator\";rt=\"Control\"",
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);

static int oxygen_level = 0;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    // Convert oxygen level to a string
    char level_str[3];
    snprintf(level_str, sizeof(level_str), "%d", oxygen_level);

    // Set payload of the response to the current oxygen level
    coap_set_payload(response, level_str, strlen(level_str));
    coap_set_status_code(response, CONTENT_2_05);
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *level = NULL;

    // Utilizza coap_get_payload per ottenere il payload della richiesta
    len = coap_get_payload(request, (const uint8_t **)&level);
    if(len <= 0 || len >= 20)
        goto error;

    oxygen_level = atoi(level);
    
    if(oxygen_level == 0) {
        leds_set(LEDS_COLOUR_NONE);
        LOG_INFO("Oxygen mask OFF\n");
    } else if(oxygen_level == 1) {
        leds_set(LEDS_BLUE);
        LOG_INFO("Oxygen mask ON LOW\n");
    } else if(oxygen_level == 2) {
        leds_set(LEDS_RED);
        LOG_INFO("Oxygen mask ON HIGH\n");
    } else {
        goto error;
    }

    coap_set_status_code(response, CHANGED_2_04);
    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
