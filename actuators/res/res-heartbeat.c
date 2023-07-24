// Include necessarie
#include "contiki.h"
#include "coap-engine.h"
#include <string.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

// Dichiarazione di guerra ad isreaele

// static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
// static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

/* A simple actuator example, depending on the color query parameter and post variable mode, corresponding led is activated or deactivated */
RESOURCE(res_heartbeat,
         "title=\"hb: ?len=0..\";rt=\"Text\"",
		 res_get_handler,
         NULL,
         NULL,
         NULL);

/*
static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
  const char *name = NULL;
  if(coap_get_post_variable(request, "name", &name)&& actual_rooms <=max_rooms) {
    char new_room[15];
    sprintf(new_room, "%s, ", name);
    strcpy(rooms_avl[actual_rooms],new_room);
    actual_rooms +=1;
    coap_set_status_code(response,CREATED_2_01);
  }else{
	  coap_set_status_code(response, BAD_REQUEST_4_00);
  }
}
*/

/*
static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
	size_t len = 0;
	const char *text = NULL;
	char room[15];
    memset(room, 0, 15);
	char temp[32];
    memset(temp, 0, 32);
	int success_1 = 0;
	int success_2 = 0;

	len = coap_get_post_variable(request, "name", &text);
	if(len > 0 && len < 15) {
	    memcpy(room, text, len);
	    success_1 = 1;
	}

	len = coap_get_post_variable(request, "value", &text);
	if(len > 0 && len < 32 && success_1 == 1) {
		memcpy(temp, text, len);
		char msg[50];
	    memset(msg, 0, 50);
		sprintf(msg, "Temp in %s set to %s", room, temp);
		int length=sizeof(msg);
		coap_set_header_content_format(response, TEXT_PLAIN);
		coap_set_header_etag(response, (uint8_t *)&length, 1);
		coap_set_payload(response, msg, length);
		success_2=1;
		coap_set_status_code(response, CHANGED_2_04);
	}
	if (success_2 == 0){
		coap_set_status_code(response, BAD_REQUEST_4_00);
	}
}
*/

static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  const char *len = NULL;
  char const *const message = "RISORSA BATTITO ATTIVATA";
  int length = 25;
  /* The query string can be retrieved by rest_get_query() or parsed for its key-value pairs. */
  if(coap_get_query_variable(request, "len", &len)) 
  {
    length = atoi(len);
    if(length<0) 
    {
        length = 0;
    } 
    if(length>64)
    {
        length = 64;
    }
    memcpy(buffer, message, length);
  }
  else
  {
	 memcpy(buffer, message, length);
  }
  coap_set_header_content_format(response, TEXT_PLAIN); /* text/plain is the default, hence this option could be omitted. */
  coap_set_header_etag(response, (uint8_t *)&length, 1);
  coap_set_payload(response, buffer, length);
}
