import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.Random;

public class AudiometerCommunication {

    private SerialPort activePort;
    private AudiometerListener eventListener; // Diğer sınıflara haber verecek elçimiz
    private Random randomGenerator;

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

                if (receivedMessage.equals("RESPONSE")) {
                    System.out.println("-> Cihazdan sinyal geldi: HASTA DUYDU (RESPONSE)");

                    // Eğer biri (UI veya Algoritma) bizi dinliyorsa, ona haber ver!
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
    public void sendStimulus(int frequency, int dbLevel) {
        if (activePort == null || !activePort.isOpen()) {
            System.out.println("Hata: Port acik degil, sinyal gonderilemez.");
            return;
        }

        // Yeni bir Thread başlatıyoruz ki arayüz kilitlenmesin
        new Thread(() -> {
            try {
                // 1. Rastgele Bekleme (Anti-Tahmin): 2 ile 4 saniye arası
                int waitTime = 2000 + randomGenerator.nextInt(2001); // 2000ms - 4000ms
                System.out.println("Sinyal oncesi bekleniyor... " + waitTime + " ms");
                Thread.sleep(waitTime);

                // 2. Komutu Hazırla (Örn: F1000D40\n) - Arduino'nun beklediği format
                String command = "F" + frequency + "D" + dbLevel + "\n";
                byte[] commandBytes = command.getBytes();

                // 3. Sinyali Gönder (Fade-in işlemi donanımda yapılacak, biz sadece komut veriyoruz)
                System.out.println("<- Cihaza Gonderiliyor: " + frequency + "Hz, " + dbLevel + "dB");
                activePort.writeBytes(commandBytes, commandBytes.length);

                // 4. Sinyal Sunum Süresi: 1.5 Saniye
                Thread.sleep(1500); 

                // 5. Sinyali Kapat Komutu (Frekans 0, dB 0 diyebiliriz)
                String stopCommand = "F0D0\n";
                activePort.writeBytes(stopCommand.getBytes(), stopCommand.length());
                System.out.println("<- Sinyal durduruldu.");

            } catch (InterruptedException e) {
                System.out.println("Sinyal gonderimi sirasinda hata olustu.");
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
