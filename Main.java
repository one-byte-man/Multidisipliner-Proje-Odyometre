import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        
        // 1. UI'yi Thread-Safe bir şekilde başlatıyoruz
        SwingUtilities.invokeLater(() -> {
            
            // Arayüzcü arkadaşının yazdığı UI sınıfını ayağa kaldır
            AudiometerUI ui = new AudiometerUI();
            ui.setVisible(true);

            // NOT: UI sınıfının içinde "AudiometerCommunication comm = new AudiometerCommunication();"
            // tanımlı. UI arkadaşın "Start Test" butonuna bastığında aslında Algoritmacı arkadaşın
            // test dizisini başlatması lazım. 
            
            /* 
             * AŞAĞIDAKİ YAPI ALGORİTMACI (LOGIC) İÇİN BİR TASLAKTIR:
             * UI üzerinden "Test Başlat" dendiğinde, Algoritmacı bu tarz bir 
             * state-machine (durum makinesi) ile senin modülünü (comm.sendStimulus) çağıracak
             * ve geri dönen onPatientResponded() verisine göre grafiği çizecek.
             * 
             * Şimdilik UI arkadaşının test verileri doğrudan AudiometerUI sınıfında 
             * "startTest()" metodu içinde ui.addThresholdToGraph(...) olarak çiziliyor.
             */
             
            System.out.println("Sistem Hazır. Lütfen arayüzden port seçip Connect'e basınız.");
        });
    }
}