from paho.mqtt import client as mqtt

clientId = "abc"
port = 1883
broker = "localhost"

client = mqtt.Client(clientId)
client.connect(broker, port)

client.publish("testing", "hello")

client.loop_forever()
