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

static void quantity_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_quantity,
         "title=\"Medicine's quantity\";rt=\"Control\"",
         NULL,
         NULL,
         quantity_put_handler,
         NULL);

int medicine_quantity = 0; // 0: off, 1: low, 2: high

static void quantity_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *quantity = NULL;
    
    len = coap_get_payload(request, (const uint8_t**)&quantity);
    if(len <= 0 || len >= 20)
        goto error;
    
    if(strncmp(quantity, "0", len) == 0) {
        medicine_quantity = 0;
        leds_set(LEDS_OFF);
        LOG_INFO("MEDICINE OFF\n");
    } else if(strncmp(quantity, "1", len) == 0 && medicine_type != 0) {
        medicine_quantity = 1;
        leds_set(LEDS_BLUE);
        LOG_INFO("MEDICINE QUANTITY LOW\n");
    } else if(strncmp(quantity, "2", len) == 0 && medicine_type != 0) {
        medicine_quantity = 2;
        leds_set(LEDS_GREEN);
        LOG_INFO("MEDICINE QUANTITY HIGH\n");
    }
    else
        goto error;

    return;
error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
