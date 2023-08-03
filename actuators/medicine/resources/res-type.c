#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res-type"
#define LOG_LEVEL LOG_LEVEL_DBG


static void type_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_type,
         "title=\"Medicine's type\";rt=\"Control\"",
         NULL,
         NULL,
         type_put_handler,
         NULL);

int medicine_type = 0; // 0: off, 1: type 1, 2: type 2

static void type_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *type = NULL;
    
    len = coap_get_payload(request, (const uint8_t**)&type);
    if(len <= 0 || len >= 20)
        goto error;
    
    if(strncmp(type, "0", len) == 0) {
        medicine_type = 0;
        leds_set(LEDS_OFF);
        LOG_INFO("MEDICINE OFF\n");
    } else if(strncmp(type, "1", len) == 0) {
        medicine_type = 1;
        leds_set(LEDS_BLUE);
        LOG_INFO("MEDICINE TYPE 1\n");
    } else if(strncmp(type, "2", len) == 0) {
        medicine_type = 2;
        leds_set(LEDS_GREEN);
        LOG_INFO("MEDICINE TYPE 2\n");
    }
    else
        goto error;

    return;
error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
