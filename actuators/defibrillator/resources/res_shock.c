#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res-shock"
#define LOG_LEVEL LOG_LEVEL_DBG


static void shock_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_shock,
         "title=\"Defibrillator's shock level\";rt=\"Control\"",
         NULL,
         NULL,
         shock_put_handler,
         NULL);

int shock_level = 0; // 0: off, 3: low, 4: high

static void shock_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset) {
    size_t len = 0;
    const char *level = NULL;
    
    len = coap_get_payload(request, (const uint8_t**)&level);
    if(len <= 0 || len >= 20)
        goto error;
    
    if(strncmp(level, "0", len) == 0) {
        shock_level = 0;
        leds_set(LEDS_OFF);
        LOG_INFO("DEFIBRILLATOR OFF\n");
    } else if(strncmp(level, "3", len) == 0) {
        shock_level = 3;
        leds_set(LEDS_BLUE);
        LOG_INFO("DEFIBRILLATOR ON LOW\n");
    } else if(strncmp(level, "4", len) == 0) {
        shock_level = 4;
        leds_set(LEDS_GREEN);
        LOG_INFO("DEFIBRILLATOR ON HIGH\n");
    }
    else
        goto error;

    return;
error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
