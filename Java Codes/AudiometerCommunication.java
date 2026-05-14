import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import java.util.Random;

public class AudiometerCommunication {

    private SerialPort activePort;
    private AudiometerListener eventListener;
    private Random randomGenerator;
    private volatile boolean isWaitingForResponse = false;

    public AudiometerCommunication() {
        randomGenerator = new Random();
    }

    public SerialPort[] getAvailablePorts() {
        return SerialPort.getCommPorts();
    }

    public boolean connectToPort(String portName) {
        activePort = SerialPort.getCommPort(portName);
        
        // Arduino'daki Serial.begin(115200); hızı ile aynı!
        activePort.setComPortParameters(115200, 8, 1, 0); 
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);

        if (activePort.openPort()) {
            System.out.println(portName + " portu başarıyla açıldı.");
            setupListener();
            return true;
        } else {
            System.out.println("Port açılamadı!");
            return false;
        }
    }

    private void setupListener() {
        activePort.addDataListener(new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE) return;

                byte[] newData = new byte[activePort.bytesAvailable()];
                activePort.readBytes(newData, newData.length);
                String receivedMessage = new String(newData).trim();

                // 1. Arduino ilk bağlandığında hazır olduğunu söylerse
                if (receivedMessage.equals("AUDIOMETER_READY")) {
                    System.out.println("-> Cihaz Hazır!");
                }
                
                // 2. Arduino komutumuzu aldığını onaylarsa (Örn: "OK FREQ=1000 DB=40")
                else if (receivedMessage.startsWith("OK")) {
                    // Cihaz komutu aldı, beklemeye geçiyoruz.
                }
                
                // 3. Hastadan Proteus (veya donanım) üzerinden BUTON YANITI gelirse
                else if (receivedMessage.equals("RESPONSE")) {
                    System.out.println("-> HASTA BUTONA BASTI (RESPONSE)!");
                    isWaitingForResponse = false; // Timeout sayacını durdur
                    if (eventListener != null) {
                        eventListener.onPatientResponded(); 
                    }
                }
            }
        });
    }

    /**
     * Sesi bilgisayarda üretmek yerine, doğrudan Arduino'ya "TONE" komutu yolluyoruz.
     */
    public void sendStimulus(String ear, int frequency, int dbLevel) {
        if (activePort == null || !activePort.isOpen()) return;

        new Thread(() -> {
            try {
                // 1. Odyometri kuralı: Testler arası tahmin edilemez bekleme (2 - 4 sn)
                int waitTime = 2000 + randomGenerator.nextInt(2001);
                System.out.println("Ses gönderilmeden önce bekleniyor... " + waitTime + " ms");
                Thread.sleep(waitTime);

                // 2. Arduino'nun beklediği format: TONE:frekans,desibel\n
                String command = "TONE:" + frequency + "," + dbLevel + "\n";
                System.out.println("<- Cihaza Giden Komut: " + command.trim());
                activePort.writeBytes(command.getBytes(), command.getBytes().length);

                // 3. Sesi 1.5 saniye boyunca çalmasını bekle
                Thread.sleep(1500); 

                // 4. Arduino'nun beklediği susturma komutu: STOP\n
                String stopCommand = "STOP\n";
                System.out.println("<- Cihaza Giden Komut: STOP");
                activePort.writeBytes(stopCommand.getBytes(), stopCommand.getBytes().length);
                
                // 5. Hasta butona basacak mı? (Timeout kontrolü)
                isWaitingForResponse = true;
                Thread.sleep(2000); // 2 saniye cevap bekle
                
                if (isWaitingForResponse && eventListener != null) {
                    System.out.println("-> Süre doldu, hasta butona basmadı.");
                    eventListener.onNoResponseTimeout();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void setListener(AudiometerListener listener) {
        this.eventListener = listener;
    }

    public void closePort() {
        if (activePort != null && activePort.isOpen()) {
            activePort.closePort();
            System.out.println("Port kapatıldı.");
        }
    }
}
