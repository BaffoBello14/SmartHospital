#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res-quantity"
#define LOG_LEVEL LOG_LEVEL_DBG

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_quantity,
         "title=\"Medicine's quantity\";rt=\"Control\"",
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);

int medicine_quantity = 0;
static struct ctimer timer;
static int ignore_zero_time_requests = 0;

static void reset_request_ignore(void *ptr) {
    ignore_zero_time_requests = 0;
}

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
    const char *payload = NULL;

    len = coap_get_payload(request, (const uint8_t **)&payload);
    if(len <= 0 || len >= 40)
        goto error;

    char payload_buffer[41];
    memcpy(payload_buffer, payload, len);
    payload_buffer[len] = '\0';

    const char *level_key = "level:";
    const char *time_key = "time:";
    char *level_start = strstr(payload_buffer, level_key);
    char *time_start = strstr(payload_buffer, time_key);

    if(level_start == NULL || time_start == NULL) {
        goto error;  // "level:" or "time:" not found in the payload
    } else {
        level_start += strlen(level_key);  // Move the pointer to the start of the number
        time_start += strlen(time_key);  // Move the pointer to the start of the number
        medicine_quantity = atoi(level_start);
        int time = atoi(time_start);

        if(time == 0 && ignore_zero_time_requests) {
            goto error;  // Ignore the request
        } else if(time > 0) {
            ctimer_set(&timer, time*CLOCK_SECOND, reset_request_ignore, NULL);
            ignore_zero_time_requests = 1;
        }
    }

    if(medicine_quantity == 0) {
        leds_set(LEDS_OFF);
        LOG_INFO("MEDICINE OFF\n");
    } else if(medicine_quantity == 1 || medicine_quantity == 3) {
        leds_set(LEDS_BLUE);
        LOG_INFO("MEDICINE QUANTITY LOW\n");
    } else if(medicine_quantity == 2 || medicine_quantity == 4) {
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