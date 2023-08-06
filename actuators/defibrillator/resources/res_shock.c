#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res_shock"
#define LOG_LEVEL LOG_LEVEL_DBG

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_shock,
         "title=\"Defibrillator's shock level\";rt=\"Control\"",
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);

static int shock_level = 0;  // 0: off, 3: low, 4: high

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    // Convert shock level to a string
    char level_str[2];
    snprintf(level_str, sizeof(level_str), "%d", shock_level);

    // Set payload of the response to the current shock level
    coap_set_payload(response, level_str, strlen(level_str));
    coap_set_status_code(response, CONTENT_2_05);
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *level = NULL;

    len = coap_get_payload(request, (const uint8_t **)&level);
    if(len <= 0 || len >= 20)
        goto error;

    char level_buffer[21];
    memcpy(level_buffer, level, len);
    level_buffer[len] = '\0';

    const char *level_key = "level:";
    char *level_start = strstr(level_buffer, level_key);

    if(level_start == NULL) {
        goto error;  // "level:" not found in the payload
    } else {
        level_start += strlen(level_key);  // Move the pointer to the start of the number
        shock_level = atoi(level_start);
    }

    if(shock_level == 0) {
        leds_set(LEDS_COLOUR_NONE);
        LOG_INFO("DEFIBRILLATOR OFF\n");
    } else if(shock_level == 3) {
        leds_set(LEDS_BLUE);
        LOG_INFO("DEFIBRILLATOR ON LOW\n");
    } else if(shock_level == 4) {
        leds_set(LEDS_RED);
        LOG_INFO("DEFIBRILLATOR ON HIGH\n");
    } else {
        goto error;
    }

    coap_set_status_code(response, CHANGED_2_04);
    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
