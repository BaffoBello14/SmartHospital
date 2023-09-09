#include "contiki.h"
#include "net/routing/routing.h"
#include "mqtt.h"
#include "net/ipv6/uip.h"
#include "net/ipv6/uip-icmp6.h"
#include "net/ipv6/sicslowpan.h"
#include "sys/etimer.h"
#include "sys/ctimer.h"
#include "lib/sensors.h"
#include "dev/button-hal.h"
#include "dev/etc/rgb-led/rgb-led.h"
#include "os/sys/log.h"
#include <sys/node-id.h>

#include <time.h>
#include <string.h>
#include <strings.h>
#include <stdbool.h>

/*---------------------------------------------------------------------------*/
#define LOG_MODULE "oxygen"
#ifdef MQTT_CLIENT_CONF_LOG_LEVEL
#define LOG_LEVEL MQTT_CLIENT_CONF_LOG_LEVEL
#else
#define LOG_LEVEL LOG_LEVEL_DBG
#endif

/*---------------------------------------------------------------------------*/
/* MQTT broker address. */
#define MQTT_CLIENT_BROKER_IP_ADDR "fd00::1"

static const char *broker_ip = MQTT_CLIENT_BROKER_IP_ADDR;

// Default config values
#define DEFAULT_BROKER_PORT         1883
#define DEFAULT_PUBLISH_INTERVAL    (30 * CLOCK_SECOND)
#define PUBLISH_INTERVAL            (4 * CLOCK_SECOND)


// We assume that the broker does not require authentication


/*---------------------------------------------------------------------------*/
/* Various states */
static uint8_t state;

#define STATE_INIT                0
#define STATE_NET_OK              1
#define STATE_CONNECTING          2
#define STATE_CONNECTED           3
#define STATE_SUBSCRIBED          4
#define STATE_DISCONNECTED        5

/*---------------------------------------------------------------------------*/
PROCESS_NAME(oxygen_process);
AUTOSTART_PROCESSES(&oxygen_process);

/*---------------------------------------------------------------------------*/
/* Maximum TCP segment size for outgoing segments of our socket */
#define MAX_TCP_SEGMENT_SIZE    32
#define CONFIG_IP_ADDR_STR_LEN   64
/*---------------------------------------------------------------------------*/
/*
 * Buffers for Client ID and Topics.
 * Make sure they are large enough to hold the entire respective string
 */
#define BUFFER_SIZE 64

static char client_id[BUFFER_SIZE];
static char pub_topic[BUFFER_SIZE];
static char sub_topic[BUFFER_SIZE];
// Periodic timer to check the state of the MQTT client
#define STATE_MACHINE_PERIODIC     (CLOCK_SECOND >> 1)
static struct etimer periodic_timer;
static struct etimer reset_timer;

/*---------------------------------------------------------------------------*/
/*
 * The main MQTT buffers.
 * We will need to increase if we start publishing more data.
 */
#define APP_BUFFER_SIZE 512
static char app_buffer[APP_BUFFER_SIZE];
/*---------------------------------------------------------------------------*/
//static struct mqtt_message *msg_ptr = 0;

static struct mqtt_connection conn;

/*---------------------------------------------------------------------------*/
PROCESS(oxygen_process, "Oxygen process");

static int oxygen = 99;  // Initial oxygen level value

static char new_id[6] = "o001";

int generateRandomOxygen(int input) {
    // Calculate the minimum and maximum oxygen level values
    int min_oxygen = input - 5;
    int max_oxygen = input + 1;
    
    // Generate a random oxygen level within the range
    int output = (rand() % (max_oxygen - min_oxygen + 1)) + min_oxygen;

    if(output > 100) output = 100;
    if(output < 0) output = 0;
    
    return output;
}

static void mqtt_event(struct mqtt_connection *m, mqtt_event_t event, void *data)
{
  switch(event) {
  case MQTT_EVENT_CONNECTED: {
    printf("Application has a MQTT connection\n");
    state = STATE_CONNECTED;
    break;
  }
  case MQTT_EVENT_DISCONNECTED: {
    printf("MQTT Disconnect. Reason %u\n", *((mqtt_event_t *)data));
    state = STATE_DISCONNECTED;
    process_poll(&oxygen_process);
    break;
  }
  case MQTT_EVENT_SUBACK: {
#if MQTT_311
    mqtt_suback_event_t *suback_event = (mqtt_suback_event_t *)data;
    if(suback_event->success) {
      printf("Application is subscribed to topic successfully\n");
    } else {
      printf("Application failed to subscribe to topic (ret code %x)\n", suback_event->return_code);
    }
    
#else
    printf("Application is subscribed to topic successfully\n");
#endif
    break;
  }
  case MQTT_EVENT_UNSUBACK: {
    printf("Application is unsubscribed to topic successfully\n");
    break;
  }
  case MQTT_EVENT_PUBACK: {
    printf("Publishing complete.\n");
    break;
  }
  case 4: {
    break;
  }

  default:
    printf("Application got an unhandled MQTT event: %i\n", event);
    break;
  }
}

static bool have_connectivity(void)
{
  if(uip_ds6_get_global(ADDR_PREFERRED) == NULL ||
     uip_ds6_defrt_choose() == NULL) {
    return false;
  }
  return true;
}

/*---------------------------------------------------------------------------*/
PROCESS_THREAD(oxygen_process, ev, data)
{

  PROCESS_BEGIN();
  mqtt_status_t status;
  char broker_address[CONFIG_IP_ADDR_STR_LEN];
  printf("MQTT Client Process\n");

  // Initialize the ClientID as MAC address
  snprintf(client_id, BUFFER_SIZE, "%02x%02x%02x%02x%02x%02x",
                     linkaddr_node_addr.u8[0], linkaddr_node_addr.u8[1],
                     linkaddr_node_addr.u8[2], linkaddr_node_addr.u8[5],
                     linkaddr_node_addr.u8[6], linkaddr_node_addr.u8[7]);

  // Broker registration                     
  mqtt_register(&conn, &oxygen_process, client_id, mqtt_event,
                  MAX_TCP_SEGMENT_SIZE);
  state=STATE_INIT;
                    
  // Initialize periodic timer to check the status 
  etimer_set(&periodic_timer, PUBLISH_INTERVAL);
  etimer_set(&reset_timer, CLOCK_SECOND);
  // rgb_led_set(RGB_LED_RED);
  /* Main loop */
  while(1) {

    PROCESS_YIELD();

    if((ev == PROCESS_EVENT_TIMER && data == &periodic_timer) || ev == PROCESS_EVENT_POLL){
                          
      if(state==STATE_INIT && have_connectivity()){
         state = STATE_NET_OK;
      } 
      
      if(state == STATE_NET_OK){
        // Connect to MQTT server
        printf("Connecting!\n");
        memcpy(broker_address, broker_ip, strlen(broker_ip));
        mqtt_connect(&conn, broker_address, DEFAULT_BROKER_PORT, (PUBLISH_INTERVAL * 3) / CLOCK_SECOND, MQTT_CLEAN_SESSION_ON);
        state = STATE_CONNECTING;
      }
      
      if(state == STATE_CONNECTED){
        // Subscribe to a topic
        strcpy(sub_topic,"oxygen");
        status = mqtt_subscribe(&conn, NULL, sub_topic, MQTT_QOS_LEVEL_0);
        printf("Subscribing!\n");
        if(status == MQTT_STATUS_OUT_QUEUE_FULL) {
          LOG_ERR("Tried to subscribe but command queue was full!\n");
          PROCESS_EXIT();
        }

        state = STATE_SUBSCRIBED;
      }

      if(state == STATE_SUBSCRIBED){
        // Publish something
        sprintf(pub_topic, "%s", "oxygen");

        oxygen = generateRandomOxygen(oxygen);

        sprintf(app_buffer, "{\"id\": \"%s\", \"value\": %d}", new_id, oxygen);
        printf("Hello, here are the info: %s \n", app_buffer);

        mqtt_publish(&conn, NULL, pub_topic, (uint8_t *)app_buffer, strlen(app_buffer), MQTT_QOS_LEVEL_0, MQTT_RETAIN_OFF);
      } else if (state == STATE_DISCONNECTED){
        LOG_ERR("Disconnected from MQTT broker\n");  
        //on_off = false;
        // Recover from error
        state = STATE_INIT;
      }
      
      etimer_set(&periodic_timer, PUBLISH_INTERVAL);
    }

    if(ev == PROCESS_EVENT_TIMER && data == &reset_timer) {
      etimer_set(&reset_timer, CLOCK_SECOND);
    }

  }

  PROCESS_END();
}
