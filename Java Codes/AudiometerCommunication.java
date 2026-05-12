import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.Random;

public class AudiometerCommunication {

    private SerialPort activePort;
    private AudiometerListener eventListener; // Diğer sınıflara haber verecek elçimiz
    private Random randomGenerator;
    private volatile boolean isWaitingForResponse = false;
    // Constructor (Kurucu Metod)
    public AudiometerCommunication() {
        randomGenerator = new Random();
    }

    /**
     * Adım 1: Bilgisayara bağlı portları bulur (Arayüzcü arkadaşın bunu combobox'a dolduracak)
     */
    public SerialPort[] getAvailablePorts() {
        return SerialPort.getCommPorts();
    }

    /**
     * Adım 2: Seçilen porta bağlanır ve "Dinleyici" (Listener) ekler
     */
    public boolean connectToPort(String portName) {
        // İsimden portu bul
        activePort = SerialPort.getCommPort(portName);
        
        // Donanım (Arduino) ile aynı dili konuşmak için standart hız: 9600
        activePort.setComPortParameters(115200, 8, 1, 0); 
        // Okuma yaparken programın takılmasını önlemek için zaman aşımı
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (activePort.openPort()) {
            System.out.println(portName + " portu basariyla acildi.");
            setupListener(); // Port açılırsa dinlemeye başla
            return true;
        } else {
            System.out.println("Port acilamadi!");
            return false;
        }
    }

    /**
     * Adım 3: Asenkron Dinleyici. Cihazdan (Arduino'dan) veri geldiğinde burası tetiklenir.
     */
    private void setupListener() {
        activePort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                // Sadece portta okunacak yeni bir veri olduğunda beni uyar
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
                    return;

                // Gelen verinin boyutunu öğren
                byte[] newData = new byte[activePort.bytesAvailable()];
                // Veriyi oku ve diziye aktar
                int numRead = activePort.readBytes(newData, newData.length);
                
                // Byte dizisini String'e çevir (Örn: "RESPONSE\n" -> "RESPONSE")
                String receivedMessage = new String(newData).trim();

                // setupListener() içindeki RESPONSE yakalama if bloğunu da şöyle güncelle:
                if (receivedMessage.equals("RESPONSE")) {
                    isWaitingForResponse = false; // Süreyi (Timeout'u) iptal et!
                    if (eventListener != null) {
                        eventListener.onPatientResponded(); 
                    }
                }
            }
        });
    }

    /**
     * Adım 4: Cihaza sesi çalması için komut gönderir (Biyomedikal Zamanlama Kuralları ile)
     * Not: Arayüz donmasın diye bunu ayrı bir Thread (İş Parçacığı) içinde yapıyoruz.
     */
 // sendStimulus metodunu şu şekilde güncelle:
    public void sendStimulus(String ear, int frequency, int dbLevel) {
        if (activePort == null || !activePort.isOpen()) return;

        new Thread(() -> {
            try {
                int waitTime = 2000 + randomGenerator.nextInt(2001);
                Thread.sleep(waitTime);

                // Donanıma kulak bilgisi de gönderilebilir (Örn: R_F1000D40\n)
                String command = ear.substring(0,1) + "_F" + frequency + "D" + dbLevel + "\n";
                activePort.writeBytes(command.getBytes(), command.getBytes().length);

                Thread.sleep(1500); 

                String stopCommand = "F0D0\n";
                activePort.writeBytes(stopCommand.getBytes(), stopCommand.getBytes().length);
                
                // DİKKAT: Timeout Mantığı Başlıyor
                isWaitingForResponse = true;
                Thread.sleep(2000); // Hastanın tepki vermesi için 2 saniye bekle
                
                if (isWaitingForResponse && eventListener != null) {
                    // Eğer bu süre içinde 'RESPONSE' gelip boolean false yapılmadıysa, zaman doldu demektir!
                    eventListener.onNoResponseTimeout();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }



    // Portu güvenli bir şekilde kapatmak için
    public void closePort() {
        if (activePort != null && activePort.isOpen()) {
            activePort.closePort();
            System.out.println("Port kapatildi.");
        }
    }
    
    public void setListener(AudiometerListener listener) {
        this.eventListener = listener;
    }
}
