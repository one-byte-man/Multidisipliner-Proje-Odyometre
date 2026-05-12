
public interface AudiometerListener {
    // Cihazdan "RESPONSE" geldiğinde tetiklenir
    void onPatientResponded();
    
    // Hasta belirli bir süre butona basmazsa tetiklenir (5 dB artırmak için)
    void onNoResponseTimeout(); 
    
    // Bağlantı koptuğunda tetiklenir
    void onConnectionLost(); 
}