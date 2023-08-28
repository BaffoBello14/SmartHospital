#include <stdlib.h>
#include <string.h>
#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "res-type"
#define LOG_LEVEL LOG_LEVEL_DBG

void leds_blink_current_color(void) {
    leds_mask_t current_leds = leds_get();  // Ottieni lo stato corrente dei LED
    for(int i = 0; i < 3; i++) {            // Lampeggia 3 volte, puoi cambiare questo valore
        leds_off(LEDS_ALL);                // Spegne tutti i LED
        clock_delay(400);                  // Attendi
        leds_on(current_leds);             // Accende i LED nello stato originale
        clock_delay(400);                  // Attendi
    }
}

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(res_type,
         "title=\"Medicine's type\";rt=\"Control\"",
         res_get_handler,
         NULL,
         res_put_handler,
         NULL);

int medicine_type = 0;
static struct ctimer timer;
static int ignore_zero_time_requests = 0;

static void reset_request_ignore(void *ptr) {
    ignore_zero_time_requests = 0;
    medicine_type = 0;
    leds_set(LEDS_COLOUR_NONE);
}

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
        medicine_type = atoi(level_start);
        int time = atoi(time_start);

        if(time == 0 && ignore_zero_time_requests) {
            coap_set_status_code(response, FORBIDDEN_4_03);  // Ignore the request
            return;
        } else if(time > 0) {
            ctimer_set(&timer, time*CLOCK_SECOND, reset_request_ignore, NULL);
            ignore_zero_time_requests = 1;
        }
    }

    if(medicine_type == 0) {
        LOG_INFO("MEDICINE OFF\n");
    } else if(medicine_type == 1 || medicine_type == 2) {
        LOG_INFO("MEDICINE TYPE 1\n");
    } else if(medicine_type == 3 || medicine_type == 4) {
        leds_blink_current_color();
        LOG_INFO("MEDICINE TYPE 2\n");
    } else {
        goto error;
    }

    coap_set_status_code(response, CHANGED_2_04);
    return;

error:
    coap_set_status_code(response, BAD_REQUEST_4_00);
}
