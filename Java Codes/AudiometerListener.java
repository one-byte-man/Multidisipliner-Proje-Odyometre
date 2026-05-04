
// Bu dosya, senin modülün ile diğer modüller arasındaki "sözleşme"dir.
public interface AudiometerListener {
    // Cihazdan "RESPONSE" geldiğinde bu metod otomatik tetiklenecek
    void onPatientResponded();
    
    // İsteğe bağlı: Port koptuğunda UI'cıya haber vermek için
    void onConnectionLost(); 
}
