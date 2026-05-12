
import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;

public class AudiometerUI extends JFrame {

    private AudiometerCommunication comm;
    private HughsonWestlakeStateMachine logic; // Algoritma referansı eklendi

    private JComboBox<String> portComboBox;
    private JButton refreshPortsButton;
    private JButton connectButton;
    private JButton startTestButton;
    private JButton clearGraphButton;

    private JLabel connectionStatusLabel;
    private JLabel responseStatusLabel;
    private AudiogramPanel audiogramPanel;

    // Constructor artık comm parametresi alıyor
    public AudiometerUI(AudiometerCommunication comm) {
        this.comm = comm;

        setTitle("Audiometer Test System - UI and Audiogram");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeComponents();
        loadAvailablePorts();
    }

    // Main'den algoritma referansını buraya veriyoruz
    public void setLogic(HughsonWestlakeStateMachine logic) {
        this.logic = logic;
    }

    private void initializeComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        portComboBox = new JComboBox<>();
        refreshPortsButton = new JButton("Refresh Ports");
        connectButton = new JButton("Connect");
        startTestButton = new JButton("Start Test");
        clearGraphButton = new JButton("Clear Graph");

        startTestButton.setEnabled(false);

        connectionStatusLabel = new JLabel("Status: Not connected");
        responseStatusLabel = new JLabel("RESPONSE: No");

        audiogramPanel = new AudiogramPanel();

        topPanel.add(new JLabel("Serial Port:"));
        topPanel.add(portComboBox);
        topPanel.add(refreshPortsButton);
        topPanel.add(connectButton);
        topPanel.add(startTestButton);
        topPanel.add(clearGraphButton);
        topPanel.add(connectionStatusLabel);
        topPanel.add(responseStatusLabel);

        add(topPanel, BorderLayout.NORTH);
        add(audiogramPanel, BorderLayout.CENTER);

        refreshPortsButton.addActionListener(e -> loadAvailablePorts());
        connectButton.addActionListener(e -> connectToSelectedPort());
        startTestButton.addActionListener(e -> startTest());
        clearGraphButton.addActionListener(e -> audiogramPanel.clearResults());
    }

    private void loadAvailablePorts() {
        portComboBox.removeAllItems();
        SerialPort[] ports = comm.getAvailablePorts();
        for (SerialPort port : ports) {
            portComboBox.addItem(port.getSystemPortName());
        }
    }

    private void connectToSelectedPort() {
        String selectedPort = (String) portComboBox.getSelectedItem();
        if (selectedPort != null && comm.connectToPort(selectedPort)) {
            connectionStatusLabel.setText("Status: Connected to " + selectedPort);
            startTestButton.setEnabled(true);
        } else {
            connectionStatusLabel.setText("Status: Connection failed");
        }
    }

    private void startTest() {
        connectionStatusLabel.setText("Status: Test started");
        if (logic != null) {
            logic.startTest(); // Arayüz sadece algoritmayı tetikler!
        }
    }

    public void showPatientResponseFeedback() {
        SwingUtilities.invokeLater(() -> {
            responseStatusLabel.setText("RESPONSE: Yes");
            responseStatusLabel.setForeground(Color.RED);

            Timer timer = new Timer(700, e -> {
                responseStatusLabel.setText("RESPONSE: No");
                responseStatusLabel.setForeground(Color.BLACK);
            });
            timer.setRepeats(false);
            timer.start();
        });
    }

    public void addThresholdToGraph(String ear, int frequency, int dbHL) {
        SwingUtilities.invokeLater(() -> audiogramPanel.addResult(ear, frequency, dbHL));
    }
    
    public void updateStatus(String msg) {
        SwingUtilities.invokeLater(() -> connectionStatusLabel.setText(msg));
    }
}
