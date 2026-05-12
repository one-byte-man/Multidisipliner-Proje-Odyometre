
import java.util.HashMap;
import java.util.Map;

public class HughsonWestlakeStateMachine implements AudiometerListener {

    private static final int[] FREQUENCY_SEQUENCE = {1000, 2000, 4000, 8000, 500, 250};
    private static final int STARTING_DB = 40;
    private static final int MIN_DB = -10;
    private static final int MAX_DB = 110;

    private AudiometerCommunication comm;
    private AudiometerUI ui;

    private int currentFreqIndex = 0;
    private int currentDbLevel = STARTING_DB;
    private String currentEar = "RIGHT";

    // 2/3 Kuralı için takip değişkenleri
    private boolean isAscendingPhase = false;
    private Map<Integer, Integer> responseCounts; // Hangi desibelde kaç kere duydu

    public HughsonWestlakeStateMachine(AudiometerCommunication comm, AudiometerUI ui) {
        this.comm = comm;
        this.ui = ui;
        this.comm.setListener(this); // Haberleşmeyi dinlemeye başla
        this.responseCounts = new HashMap<>();
    }

    public void startTest() {
        System.out.println("Odyometri testi başlatılıyor...");
        currentEar = "RIGHT";
        currentFreqIndex = 0;
        startOrNextFrequency();
    }

    private void startOrNextFrequency() {
        if (currentFreqIndex < FREQUENCY_SEQUENCE.length) {
            currentDbLevel = STARTING_DB;
            resetCounters();
            System.out.println("--- YENİ FREKANS: " + currentEar + " Kulak, " + FREQUENCY_SEQUENCE[currentFreqIndex] + " Hz ---");
            
            // DİKKAT: Artık kulak bilgisini de gönderiyoruz!
            comm.sendStimulus(currentEar, FREQUENCY_SEQUENCE[currentFreqIndex], currentDbLevel);
        } else {
            switchEar(); // Frekanslar bittiyse diğer kulağa geç
        }
    }

    @Override
    public void onPatientResponded() {
        ui.showPatientResponseFeedback();
        System.out.println("[ALGORİTMA] RESPONSE Alındı! Mevcut dB: " + currentDbLevel);

        if (isAscendingPhase) {
            // Yükseliş evresinde (5 dB artırırken) duyduysa, o desibele bir puan yaz
            int count = responseCounts.getOrDefault(currentDbLevel, 0) + 1;
            responseCounts.put(currentDbLevel, count);

            if (count >= 2) { // 2/3 kuralı sağlandı!
                recordThreshold(currentDbLevel);
                return;
            }
        }

        // Kural: Duyarsa 10 dB düşür
        isAscendingPhase = false;
        currentDbLevel = Math.max(MIN_DB, currentDbLevel - 10);
        comm.sendStimulus(currentEar, FREQUENCY_SEQUENCE[currentFreqIndex], currentDbLevel);
    }

    @Override
    public void onNoResponseTimeout() {
        System.out.println("[ALGORİTMA] Response GELMEDİ! Mevcut dB: " + currentDbLevel);
        
        // Kural: Duymazsa 5 dB artır
        isAscendingPhase = true;
        currentDbLevel = Math.min(MAX_DB, currentDbLevel + 5);
        comm.sendStimulus(currentEar, FREQUENCY_SEQUENCE[currentFreqIndex], currentDbLevel);
    }

    @Override
    public void onConnectionLost() {
        System.out.println("Bağlantı koptu. Test durduruldu.");
    }

    private void recordThreshold(int thresholdDb) {
        int freq = FREQUENCY_SEQUENCE[currentFreqIndex];
        System.out.println(">>> EŞİK BULUNDU: " + currentEar + " Kulak, " + freq + " Hz -> " + thresholdDb + " dB <<<");
        
        // UI grafiğine çizdir
        ui.addThresholdToGraph(currentEar, freq, thresholdDb);
        
        // Sonraki frekansa geç
        currentFreqIndex++;
        startOrNextFrequency();
    }

    private void switchEar() {
        if (currentEar.equals("RIGHT")) {
            System.out.println("Sağ kulak testi bitti. SOL kulağa geçiliyor...");
            currentEar = "LEFT";
            currentFreqIndex = 0; // Frekansları baştan başlat
            startOrNextFrequency();
        } else {
            System.out.println("TÜM TEST KUSURSUZ TAMAMLANDI!");
            ui.updateStatus("Status: Test Completed successfully!");
        }
    }

    private void resetCounters() {
        isAscendingPhase = false;
        responseCounts.clear();
    }
}
