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
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 100, 100);

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
                // 1. Rastgele bekleme (2-4 sn)
                int waitTime = 2000 + randomGenerator.nextInt(2001);
                System.out.println("Ses gönderilmeden önce bekleniyor... " + waitTime + " ms");
                Thread.sleep(waitTime);

                // KORUMA: Sesi göndermeden HEMEN ÖNCE yanıt dinlemeyi açıyoruz
                isWaitingForResponse = true;

                // 2. Sesi Çal
                String command = "TONE:" + frequency + "," + dbLevel + "\n";
                System.out.println("<- Cihaza Giden Komut: " + command.trim());
                activePort.writeBytes(command.getBytes(), command.getBytes().length);

                // 3. Sinyal 1.5 saniye çalıyor (Hasta bu sırada butona basabilir!)
                Thread.sleep(1500); 

                // 4. Sesi Kapat
                String stopCommand = "STOP\n";
                activePort.writeBytes(stopCommand.getBytes(), stopCommand.getBytes().length);
                
                // 5. Hasta sesten sonra 2 saniye daha basabilir
                int timeoutCounter = 0;
                while (isWaitingForResponse && timeoutCounter < 20) {
                    Thread.sleep(100); // 100ms'lik dilimler halinde 2 saniye bekle
                    timeoutCounter++;
                }
                
                // 6. Eğer hala cevap gelmediyse (isWaitingForResponse true kaldıysa)
                if (isWaitingForResponse) {
                    isWaitingForResponse = false; // Dinlemeyi kapat
                    System.out.println("-> Süre doldu, hasta butona basmadı.");
                    if (eventListener != null) {
                        eventListener.onNoResponseTimeout();
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    public void simulatePatientResponse() {
        if (isWaitingForResponse) {
            System.out.println("-> [SANAL/KLAVYE] Hasta sesi duyduğunu belirtti!");
            isWaitingForResponse = false;
            if (eventListener != null) {
                eventListener.onPatientResponded(); 
            }
        }
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
