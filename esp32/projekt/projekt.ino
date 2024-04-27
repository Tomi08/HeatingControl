#include <Arduino.h>
#include <ArduinoJson.h>
#include <WiFi.h>
#include <WebServer.h>
#include "DHT.h"
#include <EEPROM.h>
#define DHTTYPE DHT11
#define LED_BUILTIN 2

//Control
const int DHT11PIN = 13;  // D13 (GPIO 13) where your sensor is connected
//const int transistorPin = 12; // D12 (GPIO 12)
float tp = 18.0;
struct SensorData{
float t;
float h;
float setTemperature;
};
int address = 0;

SensorData data = {18.0, 0.0, 18.0};
unsigned long lastDHT11ReadTime = 0;
const unsigned long DHT11_READ_INTERVAL = 2000;


//Web Server
const char* ssid = "your_ssid";  // Enter SSID here
const char* password = "your_pw";  //Enter Password here

IPAddress local_ip(192,168,1,1);
IPAddress gateway(192,168,1,1);
IPAddress subnet(255,255,255,0);
/**/
WebServer server(8083);

uint8_t transistorPin = 12;
bool transistorStatus = LOW;

uint8_t LED1Pin = 14;
bool LED1Status = LOW;

bool modeStatus = HIGH;
bool sourceStatus = HIGH;

DHT dht(DHT11PIN, DHTTYPE);


void setup() {
  Serial.begin(115200);
  pinMode(transistorPin, OUTPUT);
  pinMode(LED1Pin, OUTPUT);
  pinMode(LED_BUILTIN, OUTPUT);
  
  EEPROM.begin(sizeof(float));
  EEPROM.get(address, data.setTemperature);
  Serial.print("Stored Temperature: ");
  Serial.println(data.setTemperature);

  Serial.println(F("DHT11 start!"));
  dht.begin();

  Serial.println("Connecting to ");
  Serial.println(ssid);

  //connect to your local wi-fi network
  WiFi.begin(ssid, password);

  //check wi-fi is connected to wi-fi network
  while (WiFi.status() != WL_CONNECTED) {
  delay(1000);
  Serial.print(".");
  }
  Serial.println("");
  Serial.println("WiFi connected..!");
  Serial.print("Got IP: ");  Serial.println(WiFi.localIP());
  /**/
  /*
  WiFi.softAP(ssid, password);
  WiFi.softAPConfig(local_ip, gateway, subnet);
  delay(100);
  */
  
  server.on("/", handle_OnConnect);
  server.on("/led1on", handle_LedOn);
  server.on("/led1off", handle_LedOff);
  server.on("/heatingon", handle_HeatingOn);
  server.on("/heatingoff", handle_HeatingOff);
  server.on("/settemperature", HTTP_GET, handle_SetTemperature);
  server.on("/gettemperature", HTTP_GET, handle_GetTemperature);
  server.on("/setmode",HTTP_GET, handle_SetMode);
  server.on("/setsource",HTTP_GET, handle_SetSource);
  server.on("/sendambienttemperature",HTTP_GET, handle_GetAmbientTemperature);
  server.onNotFound(handle_NotFound);
  
  server.begin();
  Serial.println("HTTP server started");
  
  
}
void loop() {
  digitalWrite(LED_BUILTIN, LOW);
  
  server.handleClient();
  if(transistorStatus)
  {digitalWrite(transistorPin, HIGH);}
  else
  {digitalWrite(transistorPin, LOW);}  
  if(LED1Status)
  {digitalWrite(LED1Pin, HIGH);}
  else
  {digitalWrite(LED1Pin, LOW);}



  if(sourceStatus){
    if (millis() - lastDHT11ReadTime >= DHT11_READ_INTERVAL) {
    readDHT11Values();
    lastDHT11ReadTime = millis();

      if (!isnan(data.h)){
        if (data.h > 50.0){
        handle_HumidityPercentageWarning();
        }
      }
    }
  }
  else{
    data.t = tp;
  }

  if (!isnan(data.t) && modeStatus) {
    if (data.t < data.setTemperature && transistorStatus == LOW) {
      Serial.println(F("Heating is getting switched on!"));
      handle_HeatingOn();
    } else if(data.t >= data.setTemperature && transistorStatus == HIGH){
      Serial.println(F("Heating is getting switched off!"));
      handle_HeatingOff();
    }
  }
 
   
}

void handle_OnConnect() {
  LED1Status = LOW;
  transistorStatus = LOW;
  Serial.println("GPIO2 Status: OFF | GPIO12 Status: OFF");
  server.send(200, "text/html", SendHTML(LED1Status,transistorStatus, data.t)); 
}

void handle_LedOn() {
  LED1Status = HIGH;
  Serial.println("GPIO14 Status: ON");
  server.send(200, "text/html", SendHTML(true,transistorStatus, data.t)); 
}

void handle_LedOff() {
  LED1Status = LOW;
  Serial.println("GPIO14 Status: OFF");
  server.send(200, "text/html", SendHTML(false,transistorStatus, data.t)); 
}

void handle_HeatingOn() {
  transistorStatus = HIGH;
  Serial.println("GPIO12 Status: ON");
  server.send(200, "text/html", SendHTML(LED1Status,true, data.t)); 
}

void handle_HeatingOff() {
  transistorStatus = LOW;
  Serial.println("GPIO12 Status: OFF");
  server.send(200, "text/html", SendHTML(LED1Status,false, data.t)); 
}

void handle_SetTemperature() {
  if (server.hasArg("temperature")) {
    String temperatureStr = server.arg("temperature");
    data.setTemperature = temperatureStr.toFloat();

    EEPROM.put(address, data.setTemperature);
    EEPROM.commit();
    
    Serial.print("Set Temperature: ");
    Serial.println(data.setTemperature);
  }
  server.send(200, "text/html", SendHTML(LED1Status, transistorStatus, data.t));
}
void handle_GetAmbientTemperature(){
   if (server.hasArg("ambienttemperature")) {
     String temperatureStr = server.arg("ambienttemperature");
     tp = temperatureStr.toFloat();
    
    Serial.print("Ambient Temperature: ");
    Serial.println(tp);
  }
  server.send(200, "text/html", SendHTML(LED1Status, transistorStatus, tp));
}
void handle_GetTemperature(){
  
  const size_t capacity = JSON_OBJECT_SIZE(3);
  DynamicJsonDocument jsonDoc(capacity);
  jsonDoc["temperature"] = data.t;
  jsonDoc["humidity"] = data.h;
  jsonDoc["reftemp"] = data.setTemperature;

  String message;
  serializeJson(jsonDoc, message);

  server.send(200,"application/json", message);
}

void handle_SetMode() { 
  if (server.hasArg("mode")){
    String modeStr = server.arg("mode");
    Serial.println(modeStr);
    if(modeStr == "automatic"){
      modeStatus = HIGH;
    }
    else{
      modeStatus = LOW;
    }
  }

  Serial.print("Mode Status: ");
  Serial.println(modeStatus);
  server.send(200, "text/plain", "Mode changed"); 
}
void handle_SetSource(){
   if (server.hasArg("mode")){
    String modeStr = server.arg("mode");
    Serial.println(modeStr);
    if(modeStr == "room"){
      sourceStatus = HIGH;
    }
    else{
      sourceStatus = LOW;
    }
   }
     server.send(200, "text/plain", "Source changed"); 
}
void handle_HumidityPercentageWarning() {
  digitalWrite(LED1Pin, HIGH);
  delay(100);
  digitalWrite(LED1Pin, LOW);
  Serial.println("High Humidity Percentage");
  server.send(200, "text/html", SendHTML(LED1Status,true, data.t)); 
}
void handle_NotFound(){
  server.send(404, "text/plain", "Not found");
}

SensorData readDHT11Values() {
  data.h = dht.readHumidity();
  data.t = dht.readTemperature();  
  if (!isnan(data.h) && !isnan(data.t)) {
    Serial.print("Humidity: ");
    Serial.print(data.h);
    Serial.print("%  Temperature: ");
    Serial.print(data.t);
    Serial.println("°C");
  } else {
    Serial.println("Failed to read from DHT sensor!");
  }
  
  return data;
}

String SendHTML(uint8_t led1stat,uint8_t transistorstat, float currentTemperature){
  String ptr = "<!DOCTYPE html> <html>\n";
  ptr +="<head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, user-scalable=no\">\n";
  ptr +="<title>LED Control</title>\n";
  ptr +="<style>html { font-family: Helvetica; display: inline-block; margin: 0px auto; text-align: center;}\n";
  ptr +="body{margin-top: 50px;} h1 {color: #444444;margin: 50px auto 30px;} h3 {color: #444444;margin-bottom: 50px;}\n";
  ptr +=".button {display: block;width: 80px;background-color: #3498db;border: none;color: white;padding: 13px 30px;text-decoration: none;font-size: 25px;margin: 0px auto 35px;cursor: pointer;border-radius: 4px;}\n";
  ptr +=".button-on {background-color: #3498db;}\n";
  ptr +=".button-on:active {background-color: #2980b9;}\n";
  ptr +=".button-off {background-color: #34495e;}\n";
  ptr +=".button-off:active {background-color: #2c3e50;}\n";
  ptr +="p {font-size: 14px;color: #888;margin-bottom: 10px;}\n";
  ptr +="</style>\n";
  ptr +="<script>\n";
  ptr +="function refreshPage() {\n";
  ptr +="  location.reload();\n";
  ptr +="}\n";
  ptr +="</script>\n";
  ptr +="</head>\n";
  ptr +="<body>\n";
  ptr +="<h1>ESP32 Web Server</h1>\n";
  ptr +="<h3>Using Station(STA) Mode</h3>\n";
  
   if(led1stat)
  {ptr +="<p>LED1 Status: ON</p><a class=\"button button-off\" href=\"/led1off\">OFF</a>\n";}
  else
  {ptr +="<p>LED1 Status: OFF</p><a class=\"button button-on\" href=\"/led1on\">ON</a>\n";}

  if(transistorstat)
  {ptr +="<p>Heating Status: ON</p><a class=\"button button-off\" href=\"/heatingoff\">OFF</a>\n";}
  else
  {ptr +="<p>Heating Status: OFF</p><a class=\"button button-on\" href=\"/heatingon\">ON</a>\n";}
  
  ptr += "<p>Current Temperature: ";
  ptr += currentTemperature;
  ptr += " °C</p>";


  ptr += "<form action=\"/settemperature\" method=\"get\">";
  ptr += "<label for=\"temperature\">Set Temperature:</label>";
  ptr += "<input type=\"number\" id=\"temperature\" name=\"temperature\" placeholder=\"Enter temperature\">";
  ptr += "<input type=\"submit\" value=\"Submit\">";
  ptr += "</form>";

  ptr +="</body>\n";
  ptr +="</html>\n";
  return ptr;
}
