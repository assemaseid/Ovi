# import paho.mqtt.client as mqttclient
# import time
#
#
# def on_connect(client, userdata, flags, rc):
#     if rc==0:
#         print("client is connected")
#         global connected
#         connected = True
#     else:
#         print("connection failed")
#
# connected = False
# broker_address = "test.mosquitto.org"
# port=1883
#
# clientID = "abc"
# client = mqttclient.Client(callback_api_version=mqttclient.CallbackAPIVersion.VERSION1,
#                            client_id=clientID,
#                            )
# client.on_connect=on_connect
# client.connect(broker_address, port=port)
# client.loop_start()
# while connected != True:
#     time.sleep(0.2)
# client.publish("mqtt/firstcode", "hello abzal !")
# client.loop_stop()

import paho.mqtt.client as mqttclient
import time

def on_connect(client, userdata, flags, rc, properties):
    if rc == 0:
        print("client is connected")
    else:
        print("connection failed")

def on_message(client, userdata, msg):
    print("Message received: " + str(msg.payload))
    print("Topic: " + str(msg.topic))


connected = False
MessageReceived = False

broker_address = "test.mosquitto.org"
port = 1883

clientID = "mqtt"
client = mqttclient.Client(
    callback_api_version=mqttclient.CallbackAPIVersion.VERSION2,
    client_id=clientID,
    )
client.on_connect=on_connect
client.on_message=on_message
client.connect(broker_address, port)
client.loop_start()
client.subscribe("abzal")

while connected != True:
    time.sleep(0.2)

while MessageReceived != True:
    time.sleep(0.2)

client.loop_stop()
