
public interface AudiometerListener {
    // Cihazdan "RESPONSE" geldiğinde tetiklenir
    void onPatientResponded();
    
    // Hasta belirli bir süre butona basmazsa tetiklenir
    void onNoResponseTimeout(); 
    
    // Bağlantı koptuğunda tetiklenir
    void onConnectionLost(); 
}