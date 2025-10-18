#include <MQ2.h>
#include <Arduino.h>
#include <SparkFunCCS811.h>
#define CCS811_ADDR 0x5A//CCS811 address
#define MQ2_PIN 34//can be modified to pins 36,35,39

CCS811 mySensor(CCS811_ADDR);//creating a CCS811 instance
MQ2 gasTest(MQ2_PIN);//creating a MQ-2 instance 

void setup() {
  Wire.begin(21,22);//CCS811 sensor turn on Wire.begin(SCA,SCL)
  delay(1000);//wait for CCS811 to turn on
  Serial.begin(115200);
  
  
  gasTest.begin();//MQ-2 sensor begins
  delay(1000);//wait for MQ-2 to turn on
  if (mySensor.begin() == false)
  {
    Serial.print("CCS811 error. Please check wiring. Freezing...");
    while (1)
      ;
  }
Serial.println("-----------------------------------------------");
}



void loop() {
    Serial.print("LPG:");
		Serial.print(gasTest.readLPG(), 5);//reading liquid propane concentration gas-MQ2
		Serial.print("ppm\t");
		Serial.print("CO:");
		Serial.print(gasTest.readCO(), 5);//reading CO concentration-MQ2
		Serial.print("ppm\t");
		Serial.print("SMOKE:");
		Serial.print(gasTest.readSmoke(), 5);//reading smoke concentration-MQ2
		Serial.print("ppm\t");
    Serial.print("Alcohol:");
    Serial.print(gasTest.readAlcohol(), 5);//reading alcohol gas concentration-MQ2
		Serial.print("ppm\t");
    Serial.print("Methane:");
    Serial.print(gasTest.readMethane(), 5);//reading methane gas concentration-MQ2
		Serial.print("ppm\t");
     Serial.print("H2:");
    Serial.print(gasTest.readH2(), 5);//reading H2 gas concentration-MQ2
		Serial.print("ppm\n");
  if (mySensor.dataAvailable())//Check to see if data is ready with .dataAvailable()
  {
    //If so, have the sensor read and calculate the results.
    //Get them later
    mySensor.readAlgorithmResults();

    Serial.print("CO2:  ");
    //Returns calculated CO2 reading
    Serial.print(mySensor.getCO2());
    Serial.print("ppm tVOC ");
    //Returns calculated TVOC reading
    Serial.print(mySensor.getTVOC());
    Serial.print("ppb Time Elapsed: ");
    //Display the time since program start
    Serial.print(millis());
    Serial.println(" ms");
  }


  delay(1000);
}
