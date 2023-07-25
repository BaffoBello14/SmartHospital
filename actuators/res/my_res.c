#include "contiki.h"
#include "coap-engine.h"
#include "dev/leds.h"
#include "os/dev/button-hal.h"
#include "json_util.h"

#include <string.h>

// #if PLATFORM_HAS_LEDS || LEDS_COUNT

/* Log configuration */
#include "sys/log.h"
#define LOG_MODULE "App"
#define LOG_LEVEL LOG_LEVEL_APP

static uint8_t mask_status =0;  //0 off, 1 on

static void res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset);

RESOURCE(my_res,
         "title=\"res:?value=0|1 \" PUT action=<action> ;rt=\"Control\"",
         NULL,
         NULL,
         res_put_handler,
         NULL);

static void
res_put_handler(coap_message_t *request, coap_message_t *response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset)
{
  size_t len = 0;
  const char *threshold = NULL;
  const char *action = NULL;
  int success = 1;
  const uint8_t *chunk;
  // Checking payload lenght
  len = coap_get_payload(request,&chunk);

  LOG_DBG("HANDLER STARTED\n");

  if(len>0)
  {
        action = get_json_value_string((char *)chunk, "action");
        LOG_INFO("received command: action=%s\n", action);

        threshold = get_json_value_string((char *)chunk, "threshold");
        LOG_INFO("received command: threshold=%s\n", threshold);
	} 

  if(threshold!=NULL && strlen(threshold)!=0) 
  {
    LOG_DBG("value %.*s\n", (int)len, threshold);
    
    int value_int = atoi(threshold);
    if(value_int==1)
    {
          //critic value of radiation, and the action on led and shielding are obliged
          leds_on(LEDS_BLUE);
          
          LOG_INFO("start shielding because gas value critic\n");

          //printf("status :%d\n",shielding_status);

          if(mask_status==0)
          {
              LOG_INFO("status is changing\n");
              coap_set_status_code(response, CHANGED_2_04);
              
          }
          mask_status = 1;

        
    }
    else
    {
          //critic value of radiation are not critic, the user can also turn on or turn off the shielding
          if ((action!=NULL && strlen(action)!=0))
          {
              LOG_DBG("action: %s\n", action);
              // action off 
              if (strncmp(action, "OFF", len) == 0 && mask_status==1)
              {
                LOG_INFO("stop shielding because user request\n");
                coap_set_status_code(response,CHANGED_2_04);
                leds_off(LEDS_GREEN);
                
                mask_status=0;
              }
              // action on
              else if (strncmp(action, "ON", len) == 0 && mask_status==0)
              {
                LOG_INFO("start shielding because user request\n");
                coap_set_status_code(response,CHANGED_2_04);

                leds_on(LEDS_GREEN);
                
                mask_status=1;
                
              }
              else
              {
                // action is a string different from off e on, or the action doesn't change the status of the shielding
                  coap_set_status_code(response,BAD_OPTION_4_02);
              }
          }

        
    }
    success = 1;
   
  } 
  else 
  {
    success = 0;
  } 
  if (!success)
  {
    coap_set_status_code(response, BAD_REQUEST_4_00);
  }
}
// #endif /* PLATFORM_HAS_LEDS */