// Include necessarie
#include "contiki.h"
#include "coap-engine.h"
#include <string.h>

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

// static void res_post_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
// static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);



/* 
A simple actuator example, depending on the color query parameter and post variable mode, corresponding led is activated or deactivated 
RESOURCE(res_heartbeat,
         "title=\"hb: ?len=0..\";rt=\"Text\"",
		 res_get_handler,
         NULL,
     res_put_handler,
         NULL);
*/

RESOURCE(res_heartbeat,
         "title=\"heartbeat:?level=0|1|2 \" PUT action=<putter> ;rt=\"Control\"",
		 res_get_handler,
         NULL,
     res_put_handler,
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


static void
res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  // Preparing useful variables 
  size_t len = 0;
  // Threshol retrievable
  const char *threshold = NULL;
  // 
  const char *action=NULL;
  int success=1;
  const uint8_t *chunk;
 
  len = coap_get_payload(request,&chunk);

  LOG_DBG("handler\n");

  if(len>0){
        action = get_json_value_string((char *)chunk, "action");
        LOG_INFO("received command: action=%s\n", action);
        threshold = get_json_value_string((char *)chunk, "threshold");
        LOG_INFO("received command: threshold=%s\n", threshold);
	} 

  if(threshold!=NULL && strlen(threshold)!=0) {
    LOG_DBG("value %.*s\n", (int)len, threshold);
    
    int value_int = atoi(threshold);
    if(value_int==1){
          //critic value of radiation, and the action on led and shielding are obliged
          leds_on(LEDS_BLUE);
          
          LOG_INFO("start shielding because gas value critic\n");

          //printf("status :%d\n",shielding_status);

          if(shielding_status==0){
              LOG_INFO("status is changing\n");
              coap_set_status_code(response, CHANGED_2_04);
              
          }
          shielding_status=1;

        
    }
    else{
        
          

          //critic value of radiation are not critic, the user can also turn on or turn off the shielding
          if ((action!=NULL && strlen(action)!=0)){
              LOG_DBG("action: %s\n", action);

              // action off 
              if (strncmp(action, "OFF", len) == 0 && shielding_status==1){
                LOG_INFO("stop shielding because user request\n");
                coap_set_status_code(response,CHANGED_2_04);
                leds_off(LEDS_GREEN);
                
                shielding_status=0;
              }
              // action on
              else if (strncmp(action, "ON", len) == 0 && shielding_status==0){
                LOG_INFO("start shielding because user request\n");
                coap_set_status_code(response,CHANGED_2_04);

                leds_on(LEDS_GREEN);
                
                shielding_status=1;
                
              }
              else{
                // action is a string different from off e on, or the action doesn't change the status of the shielding
                  coap_set_status_code(response,BAD_OPTION_4_02);
              }
          }

        
    }
    success=1;
   
  } else {
    success = 0;
  } 

  

  if (!success){
    coap_set_status_code(response, BAD_REQUEST_4_00);
  }
}

/*
static void res_get_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  // Implementare un get_handler che restituisca un valore


  const char *len = NULL;
  char const *const message = "RISORSA BATTITO ATTIVATA";
  int length = 25;
  // The query string can be retrieved by rest_get_query() or parsed for its key-value pairs. 
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
  coap_set_header_content_format(response, TEXT_PLAIN); // text/plain is the default, hence this option could be omitted
  coap_set_header_etag(response, (uint8_t *)&length, 1);
  coap_set_payload(response, buffer, length);

}
*/