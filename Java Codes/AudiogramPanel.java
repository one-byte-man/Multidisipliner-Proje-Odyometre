import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * AudiogramPanel
 *
 * This panel draws a standard audiogram graph for an audiometer project.
 *
 * Requirements implemented:
 * - X-axis contains only tested frequencies: 250, 500, 1000, 2000, 4000, 8000 Hz
 * - Y-axis is inverted: -10 dB HL at the top, 110 dB HL at the bottom
 * - Right ear results are displayed as red "O"
 * - Left ear results are displayed as blue "X"
 * - Hearing loss zones are shown as background reference regions
 */
public class AudiogramPanel extends JPanel {

    private final int[] frequencies = {250, 500, 1000, 2000, 4000, 8000};

    private final Map<Integer, Integer> rightEarResults = new HashMap<>();
    private final Map<Integer, Integer> leftEarResults = new HashMap<>();

    private static final int MIN_DB = -10;
    private static final int MAX_DB = 110;

    public AudiogramPanel() {
        setPreferredSize(new Dimension(850, 550));
        setBackground(Color.WHITE);
    }

    /**
     * Adds or updates a hearing threshold result on the audiogram.
     *
     * @param ear       "RIGHT" or "LEFT"
     * @param frequency frequency in Hz
     * @param dbHL      hearing level in dB HL
     */
    public void addResult(String ear, int frequency, int dbHL) {
        if (!isValidFrequency(frequency)) {
            throw new IllegalArgumentException("Invalid frequency: " + frequency);
        }

        if (dbHL < MIN_DB || dbHL > MAX_DB) {
            throw new IllegalArgumentException("dB HL value must be between -10 and 110.");
        }

        if (ear.equalsIgnoreCase("RIGHT")) {
            rightEarResults.put(frequency, dbHL);
        } else if (ear.equalsIgnoreCase("LEFT")) {
            leftEarResults.put(frequency, dbHL);
        } else {
            throw new IllegalArgumentException("Ear must be RIGHT or LEFT.");
        }

        repaint();
    }

    public void clearResults() {
        rightEarResults.clear();
        leftEarResults.clear();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int left = 80;
        int right = getWidth() - 40;
        int top = 55;
        int bottom = getHeight() - 75;

        drawHearingLossZones(g2, left, right, top, bottom);
        drawGrid(g2, left, right, top, bottom);
        drawAxisLabels(g2, left, right, top, bottom);
        drawLegend(g2, right, top);
        drawResults(g2, rightEarResults, Color.RED, "O", left, right, top, bottom);
        drawResults(g2, leftEarResults, Color.BLUE, "X", left, right, top, bottom);
    }

    private void drawHearingLossZones(Graphics2D g2, int left, int right, int top, int bottom) {
        drawZone(g2, left, right, top, bottom, -10, 25, new Color(225, 255, 225), "Normal");
        drawZone(g2, left, right, top, bottom, 26, 40, new Color(255, 255, 210), "Mild");
        drawZone(g2, left, right, top, bottom, 41, 55, new Color(255, 230, 200), "Moderate");
        drawZone(g2, left, right, top, bottom, 90, 110, new Color(255, 210, 210), "Severe");
    }

    private void drawZone(Graphics2D g2, int left, int right, int top, int bottom,
                          int dbStart, int dbEnd, Color color, String label) {
        int y1 = yForDb(dbStart, top, bottom);
        int y2 = yForDb(dbEnd, top, bottom);

        g2.setColor(color);
        g2.fillRect(left, y1, right - left, y2 - y1);

        g2.setColor(Color.DARK_GRAY);
        g2.setFont(new Font("Arial", Font.PLAIN, 11));
        g2.drawString(label, right - 95, y1 + 15);
    }

    private void drawGrid(Graphics2D g2, int left, int right, int top, int bottom) {
        g2.setStroke(new BasicStroke(1));

        for (int db = MIN_DB; db <= MAX_DB; db += 10) {
            int y = yForDb(db, top, bottom);

            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(left, y, right, y);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString(db + " dB", 25, y + 4);
        }

        for (int i = 0; i < frequencies.length; i++) {
            int x = xForFrequencyIndex(i, left, right);

            g2.setColor(Color.LIGHT_GRAY);
            g2.drawLine(x, top, x, bottom);

            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString(frequencies[i] + " Hz", x - 25, bottom + 25);
        }

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(2));
        g2.drawRect(left, top, right - left, bottom - top);
    }

    private void drawAxisLabels(Graphics2D g2, int left, int right, int top, int bottom) {
        g2.setColor(Color.BLACK);

        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("Audiogram Graph", getWidth() / 2 - 80, 30);

        g2.setFont(new Font("Arial", Font.PLAIN, 13));
        g2.drawString("Frequency (Hz)", getWidth() / 2 - 45, getHeight() - 25);

        Graphics2D copy = (Graphics2D) g2.create();
        copy.rotate(-Math.PI / 2);
        copy.drawString("Hearing Level (dB HL)", -getHeight() / 2 - 60, 18);
        copy.dispose();
    }

    private void drawLegend(Graphics2D g2, int right, int top) {
        g2.setFont(new Font("Arial", Font.BOLD, 14));

        g2.setColor(Color.RED);
        g2.drawString("O", right - 120, top - 35);
        g2.setColor(Color.BLACK);
        g2.drawString("Right Ear", right - 100, top - 35);

        g2.setColor(Color.BLUE);
        g2.drawString("X", right - 120, top - 15);
        g2.setColor(Color.BLACK);
        g2.drawString("Left Ear", right - 100, top - 15);
    }

    private void drawResults(Graphics2D g2, Map<Integer, Integer> data, Color color, String symbol,
                             int left, int right, int top, int bottom) {
        g2.setColor(color);
        g2.setFont(new Font("Arial", Font.BOLD, 24));

        for (Map.Entry<Integer, Integer> entry : data.entrySet()) {
            int frequency = entry.getKey();
            int dbHL = entry.getValue();

            int frequencyIndex = getFrequencyIndex(frequency);
            if (frequencyIndex == -1) {
                continue;
            }

            int x = xForFrequencyIndex(frequencyIndex, left, right);
            int y = yForDb(dbHL, top, bottom);

            g2.drawString(symbol, x - 8, y + 8);
        }
    }

    private int xForFrequencyIndex(int index, int left, int right) {
        return left + index * (right - left) / (frequencies.length - 1);
    }

    /**
     * Converts dB HL value into Y coordinate.
     * Since audiogram Y-axis is inverted, larger dB values are lower on the screen.
     */
    private int yForDb(int dbHL, int top, int bottom) {
        return top + (dbHL - MIN_DB) * (bottom - top) / (MAX_DB - MIN_DB);
    }

    private int getFrequencyIndex(int frequency) {
        for (int i = 0; i < frequencies.length; i++) {
            if (frequencies[i] == frequency) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidFrequency(int frequency) {
        return getFrequencyIndex(frequency) != -1;
    }
}
