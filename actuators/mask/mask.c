#include "contiki.h"
#include "coap-engine.h"
#include "sys/etimer.h"
#include <string.h>

// Include the resource
#include "res-oxygen.h"

#define SERVER_EP "coap://[aaaa::1]/registration"

PROCESS(mask_process, "Mask Actuator");
AUTOSTART_PROCESSES(&mask_process);

static void response_handler(coap_message_t *response) {
  if(response == NULL) {
    puts("Request timed out");
    return;
  }

  LOG_DBG("Response %i\n", response->code);
}

PROCESS_THREAD(mask_process, ev, data) {
  PROCESS_BEGIN();

  static coap_endpoint_t server_ep;
  static coap_message_t request[1];

  coap_endpoint_parse(SERVER_EP, strlen(SERVER_EP), &server_ep);

  // Resource activation
  res_oxygen_activate();

  // Registration
  coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
  coap_set_header_uri_path(request, "/registration");
  
  const char msg[] = "{\"type\":\"mask\"}";
  coap_set_payload(request, (uint8_t *)msg, sizeof(msg)-1);
  COAP_BLOCKING_REQUEST(&server_ep, request, response_handler);

  PROCESS_END();
}
