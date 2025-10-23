#include <MQ2.h>
#include <Arduino.h>
//https://github.com/labay11/MQ-2-sensor-library/blob/master/

MQ2::MQ2(int pin1){
  pin=pin1;
  R0=6.35;
}
void MQ2::begin(){
  Serial.println("Starting MQ2 calibration...");
  R0=MQ2::MQCalibration();
  Serial.print("Ro: ");
  Serial.print(R0);
  Serial.println(" kohms");
}

void MQ2::close(){
  R0=6.35;
  values[0]=0;
  values[1]=0;
  values[2]=0;
  values[3]=0;
  values[4]=0;
  values[5]=0;
}

bool MQ2::checkCalibration(){
  if(R0<0){
    Serial.println("Sensor not calibrated.");
    return false ;
  }
  return true;
}

float* MQ2::read(bool print){
  if(!checkCalibration()){
    return NULL;
  }else{
  values[0] = MQGetPercentage(LPGcurve);
	values[1] = MQGetPercentage(COcurve);
	values[2] = MQGetPercentage(smokeCurve);
  values[3] = MQGetPercentage(alcoholCurve);
	values[4] = MQGetPercentage(methaneCurve);
	values[5] = MQGetPercentage(H2Curve);
 
  lastReadTime=millis();

  if(print){
    Serial.print(lastReadTime);
    Serial.print("ms - LPG:");
		Serial.print(values[0], 5);
		Serial.print("ppm\t");
		Serial.print("CO:");
		Serial.print(values[1], 5);
		Serial.print("ppm\t");
		Serial.print("SMOKE:");
		Serial.print(values[2], 5);
		Serial.print("ppm\t");
    Serial.print("Alcohol:");
    Serial.print(values[3], 5);
		Serial.print("ppm\t");
    Serial.print("Methane:");
    Serial.print(values[4], 5);
		Serial.print("ppm\t");
     Serial.print("H2:");
    Serial.print(values[5], 5);
		Serial.print("ppm\n");
  }
  return values;
  }
}
float MQ2::readLPG(){
  if (!checkCalibration()){
    return 0;
    }
  if (millis()< (lastReadTime+READ_DELAY)&& values[0]>0)
    return values[0];
  else
      return (values[0]=MQGetPercentage(LPGcurve));
  
}

float MQ2::readCO(){
  if (!checkCalibration()){
    return 0;}
  if (millis()< (lastReadTime+READ_DELAY)&& values[1]>0)
    return values[1];
  else
      return (values[1]=MQGetPercentage(COcurve));
  
}

float MQ2::readSmoke(){
  if (!checkCalibration()){
    return 0;}
  if (millis()< (lastReadTime+READ_DELAY)&& values[2]>0)
    return values[2];
  else
      return (values[2]=MQGetPercentage(smokeCurve));
  
}

float MQ2::readAlcohol(){
  if (!checkCalibration()){
    return 0;}
  if (millis()< (lastReadTime+READ_DELAY)&& values[3]>0)
    return values[3];
  else
      return (values[3]=MQGetPercentage(alcoholCurve));
  
}

float MQ2::readMethane(){
  if (!checkCalibration()){
    return 0;}
  if (millis()< (lastReadTime+READ_DELAY)&& values[4]>0)
    return values[4];
  else
      return (values[4]=MQGetPercentage(methaneCurve));
  
}

float MQ2::readH2(){
  if (!checkCalibration()){
    return 0;}
  if (millis()< (lastReadTime+READ_DELAY)&& values[5]>0)
    return values[5];
  else
      return (values[5]=MQGetPercentage(H2Curve));
  
}

float MQ2::MQRead(){
  float rs=0;
  for (int i =0; i<READ_SAMPLE_TIMES;i++){
    rs+=MQResistanceCalculation(analogRead(pin));
    delay(READ_SAMPLE_INTERVAL);
  }
  return rs/((float)READ_SAMPLE_TIMES);
}

float MQ2::MQGetPercentage(float *pcurve){
  float Rs_R0_ratio=MQRead()/R0;
  return pow(10,((log(Rs_R0_ratio)-pcurve[1])/pcurve[2]+pcurve[0]));
}
float MQ2::MQCalibration(){
  float val=0;

  for (int i=0; i<CALIBRATION_SAMPLE_TIMES; i++){
    val+=MQResistanceCalculation(analogRead(pin));
    delay(CALIBRATION_SAMPLE_INTERVAL);
  }

  val=val/((float)CALIBRATION_SAMPLE_TIMES);

  val=val/R0_CLEAN_AIR_FACTOR;

  return val;

}

float MQ2::MQResistanceCalculation(int adc){
  float adc_fl=adc;
  return RL_VALUE*(4095.0-adc_fl)/adc_fl;


}
