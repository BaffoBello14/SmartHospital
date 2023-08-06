#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res-type"
#define LOG_LEVEL LOG_LEVEL_DBG

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_type,
         "title=\"Medicine's type\";rt=\"Control\"",
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);

int medicine_type = 0; // 0: off, 1: type 1, 2: type 2

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    // Convert medicine type to a string
    char type_str[2];
    snprintf(type_str, sizeof(type_str), "%d", medicine_type);

    // Set payload of the response to the current medicine type
    coap_set_payload(response, type_str, strlen(type_str));
    coap_set_status_code(response, CONTENT_2_05);
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *type = NULL;

    len = coap_get_payload(request, (const uint8_t **)&type);
    if(len <= 0 || len >= 20)
        goto error;

    char type_buffer[21];
    memcpy(type_buffer, type, len);
    type_buffer[len] = '\0';

    const char *type_key = "level:";
    char *type_start = strstr(type_buffer, type_key);

    if(type_start == NULL) {
        goto error;
    } else {
        type_start += strlen(type_key);
        medicine_type = atoi(type_start);
    }

    if(medicine_type == 0) {
        leds_set(LEDS_OFF);
        LOG_INFO("MEDICINE OFF\n");
    } else if(medicine_type == 1) {
        leds_set(LEDS_BLUE);
        LOG_INFO("MEDICINE TYPE 1\n");
    } else if(medicine_type == 2) {
        leds_set(LEDS_RED);
        LOG_INFO("MEDICINE TYPE 2\n");
    } else {
        goto error;
    }

    coap_set_status_code(response, CHANGED_2_04);
    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
