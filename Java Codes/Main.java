import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            
            // 1. Haberleşme Modülü Yaratılır
            AudiometerCommunication comm = new AudiometerCommunication();

            // 2. Arayüz Modülü Yaratılır
            AudiometerUI ui = new AudiometerUI(comm);

            // 3. Algoritma Modülü Yaratılı
            HughsonWestlakeStateMachine logic = new HughsonWestlakeStateMachine(comm, ui);

            // 4. Parçaları Birbirine Bağla
            ui.setLogic(logic);
            
            ui.setVisible(true);
            System.out.println("Sistem Hazır. Port seçip Connect'e basınız.");
        });
    }
}