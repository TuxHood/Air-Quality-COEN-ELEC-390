//https://github.com/labay11/MQ-2-sensor-library/blob/master/src/MQ2.h
# ifndef MQ2_h
# define MQ2_h

//Load resistance on board
#define RL_VALUE 5

//the value of sensor resistance R0 at 1kppm  in clean air, from the graph
#define R0_CLEAN_AIR_FACTOR 9.83

//in calibration, sensor wil read the concentration of particles 10x every 50ms
//and takes the average
#define CALIBRATION_SAMPLE_TIMES 10
#define CALIBRATION_SAMPLE_INTERVAL 50

//sensor wil read the concentration of particles 5x every 50ms
//and takes the avergae
# define READ_SAMPLE_TIMES 5
# define READ_SAMPLE_INTERVAL 50

//10s between readings
#define READ_DELAY 10000

class MQ2{
  public:
    /*constructor which creates sensor instance, 
    the pin is where sensor is connected to the ESP-32*/
    MQ2(int pin);

    /*
    intialises the sensor before use*/
    void begin();

    //stops the sensor
    void close();

    /*Reads data from sensor and returns array with values in this order
    LPG, CO, Smoke, alcohol, methane, H2

    This method reads the READ_SAMPLES_TIMES samples from the sensor 
    at every READ_SAMPLE_INTERVAL and returns average
    */
    float *read(bool print);

    //these methods return the values of specific gases
    float readLPG();
    float readCO();
    float readSmoke();
    float readAlcohol();
    float readMethane();
    float readH2();
    float MQCalibration();
  private:

    int pin;
    //****************{log(x),log(y),slope}
    float LPGcurve[3]={2.3, 0.21,-0.46};
    float COcurve[3]={2.3, 0.71, -0.314};
    float smokeCurve[3]={2.3, 0.55,-0.45};
    float alcoholCurve[3]={2.3,0.47,-0.39};
    float methaneCurve[3]={2.3,0.48,-0.37};
    float H2Curve[3]={2.3,0.04,-0.89};
    float R0=6.35;

    // array of measured values of
    // LPG, CO, Smoke, alcohol, methane, H2
    float values[6];

    float MQRead();
    float MQGetPercentage(float *pcurve);
    
    float MQResistanceCalculation(int adc_values);
    bool checkCalibration();

    int lastReadTime=0;
};

#endif



