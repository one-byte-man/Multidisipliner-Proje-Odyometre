import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import java.awt.*;

/**
 * AudiometerUI
 *
 * Main graphical user interface for the audiometer system.
 *
 * Responsibilities:
 * - List available serial ports
 * - Connect to selected serial port using AudiometerCommunication
 * - Provide test start button
 * - Display patient RESPONSE feedback
 * - Update audiogram graph when threshold results are received from algorithm module
 *
 * Note:
 * This class assumes that AudiometerCommunication and AudiometerListener
 * are provided by the communication module.
 */
public class AudiometerUI extends JFrame {

    private AudiometerCommunication comm;

    private JComboBox<String> portComboBox;
    private JButton refreshPortsButton;
    private JButton connectButton;
    private JButton startTestButton;
    private JButton clearGraphButton;

    private JLabel connectionStatusLabel;
    private JLabel responseStatusLabel;

    private AudiogramPanel audiogramPanel;

    public AudiometerUI() {
        comm = new AudiometerCommunication();

        setTitle("Audiometer Test System - UI and Audiogram");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeComponents();
        registerCommunicationListener();
        loadAvailablePorts();
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

    private void registerCommunicationListener() {
        comm.setListener(new AudiometerListener() {
            @Override
            public void onPatientResponded() {
                showPatientResponseFeedback();

                /*
                 * Important:
                 * The UI does not calculate the Hughson-Westlake algorithm.
                 * The algorithm module should receive this RESPONSE event,
                 * calculate the threshold, and then call:
                 *
                 * addThresholdToGraph("RIGHT", frequency, dbHL);
                 * or
                 * addThresholdToGraph("LEFT", frequency, dbHL);
                 */
            }

            @Override
            public void onConnectionLost() {
                SwingUtilities.invokeLater(() -> {
                    connectionStatusLabel.setText("Status: Connection lost");
                    startTestButton.setEnabled(false);
                });
            }
        });
    }

    private void loadAvailablePorts() {
        portComboBox.removeAllItems();

        SerialPort[] ports = comm.getAvailablePorts();

        for (SerialPort port : ports) {
            portComboBox.addItem(port.getSystemPortName());
        }

        if (ports.length == 0) {
            connectionStatusLabel.setText("Status: No serial ports found");
        } else {
            connectionStatusLabel.setText("Status: Ports loaded");
        }
    }

    private void connectToSelectedPort() {
        String selectedPort = (String) portComboBox.getSelectedItem();

        if (selectedPort == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "No serial port selected.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        boolean success = comm.connectToPort(selectedPort);

        if (success) {
            connectionStatusLabel.setText("Status: Connected to " + selectedPort);
            startTestButton.setEnabled(true);
        } else {
            connectionStatusLabel.setText("Status: Connection failed");
            startTestButton.setEnabled(false);
        }
    }

    private void startTest() {
        connectionStatusLabel.setText("Status: Test started");
    }

    /**
     * Called when the patient presses the response button.
     * Shows temporary visual feedback on the UI.
     */
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

    /**
     * Public method for the algorithm module.
     * After the Hughson-Westlake algorithm finds a threshold,
     * it should call this method to update the audiogram.
     *
     * @param ear       "RIGHT" or "LEFT"
     * @param frequency frequency in Hz
     * @param dbHL      hearing threshold in dB HL
     */
    public void addThresholdToGraph(String ear, int frequency, int dbHL) {
        SwingUtilities.invokeLater(() -> {
            try {
                audiogramPanel.addResult(ear, frequency, dbHL);
            } catch (IllegalArgumentException ex) {
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Invalid Audiogram Data",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            AudiometerUI ui = new AudiometerUI();
            ui.setVisible(true);
        });
    }
}
