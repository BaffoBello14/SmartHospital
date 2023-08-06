#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res-quantity"
#define LOG_LEVEL LOG_LEVEL_DBG

extern int medicine_type;

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_quantity,
         "title=\"Medicine's quantity\";rt=\"Control\"",
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);

int medicine_quantity = 0; // 0: off, 1: low, 2: high

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    // Convert medicine quantity to a string
    char quantity_str[2];
    snprintf(quantity_str, sizeof(quantity_str), "%d", medicine_quantity);

    // Set payload of the response to the current medicine quantity
    coap_set_payload(response, quantity_str, strlen(quantity_str));
    coap_set_status_code(response, CONTENT_2_05);
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *quantity = NULL;

    len = coap_get_payload(request, (const uint8_t **)&quantity);
    if(len <= 0 || len >= 20)
        goto error;

    char quantity_buffer[21];
    memcpy(quantity_buffer, quantity, len);
    quantity_buffer[len] = '\0';

    const char *quantity_key = "level:";
    char *quantity_start = strstr(quantity_buffer, quantity_key);

    if(quantity_start == NULL) {
        goto error;
    } else {
        quantity_start += strlen(quantity_key);
        medicine_quantity = atoi(quantity_start);
    }

    if(medicine_quantity == 0) {
        leds_set(LEDS_COLOUR_NONE);
        LOG_INFO("MEDICINE OFF\n");
    } else if(medicine_quantity == 1 && medicine_type != 0) {
        leds_set(LEDS_BLUE);
        LOG_INFO("MEDICINE QUANTITY LOW\n");
    } else if(medicine_quantity == 2 && medicine_type != 0) {
        leds_set(LEDS_RED);
        LOG_INFO("MEDICINE QUANTITY HIGH\n");
    } else {
        goto error;
    }

    coap_set_status_code(response, CHANGED_2_04);
    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
