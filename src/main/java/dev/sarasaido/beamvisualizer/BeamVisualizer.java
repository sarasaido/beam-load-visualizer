package dev.sarasaido.beamvisualizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;

/**
 * Interactive visualizer for a simply supported beam.
 *
 * @author Sara Saido
 */
public final class BeamVisualizer {
    private static final Color GREEN = new Color(190, 24, 93);
    private static final Color DEEP_GREEN = new Color(131, 24, 67);
    private static final Color SAGE = new Color(252, 231, 243);
    private static final Color IVORY = new Color(255, 247, 251);
    private static final Color CHARCOAL = new Color(32, 37, 33);
    private static final Color BLUE = new Color(147, 51, 234);
    private static final Color ORANGE = new Color(244, 63, 94);

    private BeamVisualizer() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 13));
            UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 13));
            UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 13));
            UIManager.put("Spinner.font", new Font("SansSerif", Font.PLAIN, 13));
            createAndShowUi();
        });
    }

    private static void createAndShowUi() {
        JFrame frame = new JFrame("Beam Load Visualizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        BeamCanvas canvas = new BeamCanvas();
        JPanel controls = createControls(canvas);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(IVORY);
        root.add(createHeader(), BorderLayout.NORTH);
        root.add(canvas, BorderLayout.CENTER);
        root.add(controls, BorderLayout.WEST);

        frame.setContentPane(root);
        frame.setMinimumSize(new Dimension(980, 720));
        frame.setSize(1180, 820);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JPanel createHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(DEEP_GREEN);
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("Beam Load Visualizer");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 30));

        JLabel subtitle = new JLabel("Simply supported beam · shear force · bending moment");
        subtitle.setForeground(SAGE);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 13));

        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(subtitle);
        return header;
    }

    private static JPanel createControls(BeamCanvas canvas) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 0, 1, SAGE),
                BorderFactory.createEmptyBorder(24, 20, 24, 20)));
        panel.setPreferredSize(new Dimension(285, 0));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);

        JLabel heading = new JLabel("Load case");
        heading.setFont(new Font("Serif", Font.BOLD, 23));
        heading.setForeground(DEEP_GREEN);
        c.gridy = 0;
        panel.add(heading, c);

        c.gridy++;
        panel.add(new JLabel("Load type"), c);
        JComboBox<LoadType> loadType = new JComboBox<>(LoadType.values());
        c.gridy++;
        c.insets = new Insets(0, 0, 18, 0);
        panel.add(loadType, c);

        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(new JLabel("Beam span L [m]"), c);
        JSpinner span = new JSpinner(new SpinnerNumberModel(8.0, 1.0, 50.0, 0.5));
        c.gridy++;
        c.insets = new Insets(0, 0, 18, 0);
        panel.add(span, c);

        JLabel loadLabel = new JLabel("Point load P [kN]");
        c.gridy++;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(loadLabel, c);
        JSpinner magnitude = new JSpinner(new SpinnerNumberModel(40.0, 1.0, 1000.0, 5.0));
        c.gridy++;
        c.insets = new Insets(0, 0, 20, 0);
        panel.add(magnitude, c);

        JButton update = new JButton("Update diagrams");
        update.setBackground(GREEN);
        update.setForeground(Color.WHITE);
        update.setFocusPainted(false);
        update.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        c.gridy++;
        panel.add(update, c);

        JLabel note = new JLabel("<html><body style='width:210px'>Educational visualization only. Results must be verified before engineering use.</body></html>");
        note.setForeground(new Color(90, 100, 92));
        c.gridy++;
        c.weighty = 1;
        c.anchor = GridBagConstraints.SOUTH;
        c.insets = new Insets(28, 0, 0, 0);
        panel.add(note, c);

        loadType.addActionListener(event -> {
            LoadType selected = (LoadType) loadType.getSelectedItem();
            loadLabel.setText(selected == LoadType.POINT
                    ? "Point load P [kN]"
                    : "Maximum load w_max [kN/m]");
        });

        update.addActionListener(event -> {
            try {
                double length = ((Number) span.getValue()).doubleValue();
                double load = ((Number) magnitude.getValue()).doubleValue();
                if (length <= 0 || load <= 0) {
                    throw new IllegalArgumentException("Span and load must be greater than zero.");
                }
                canvas.setModel(new BeamModel(length, load, (LoadType) loadType.getSelectedItem()));
            } catch (RuntimeException exception) {
                JOptionPane.showMessageDialog(panel, exception.getMessage(), "Invalid input",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        return panel;
    }

    private enum LoadType {
        POINT("Centered point load"),
        TRIANGULAR("Triangular distributed load");

        private final String label;

        LoadType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private record BeamModel(double length, double load, LoadType type) {
        double reactionA() {
            return type == LoadType.POINT ? load / 2.0 : load * length / 6.0;
        }

        double reactionB() {
            return type == LoadType.POINT ? load / 2.0 : load * length / 3.0;
        }

        double shear(double x) {
            if (type == LoadType.POINT) {
                return x < length / 2.0 ? load / 2.0 : -load / 2.0;
            }
            return reactionA() - load * x * x / (2.0 * length);
        }

        double moment(double x) {
            if (type == LoadType.POINT) {
                return x <= length / 2.0
                        ? load * x / 2.0
                        : load * (length - x) / 2.0;
            }
            return reactionA() * x - load * x * x * x / (6.0 * length);
        }

        double maxAbsShear() {
            return Math.max(Math.abs(reactionA()), Math.abs(reactionB()));
        }

        double maxMoment() {
            if (type == LoadType.POINT) {
                return load * length / 4.0;
            }
            double zeroShearX = length / Math.sqrt(3.0);
            return moment(zeroShearX);
        }
    }

    @SuppressWarnings("serial")
    private static final class BeamCanvas extends JPanel {
        private BeamModel model = new BeamModel(8.0, 40.0, LoadType.POINT);

        BeamCanvas() {
            setBackground(IVORY);
            setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        }

        void setModel(BeamModel model) {
            this.model = model;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int left = 90;
            int right = getWidth() - 55;
            int width = Math.max(200, right - left);
            int beamY = 155;

            drawSectionTitle(g, "Structural system", left, 36);
            drawBeam(g, left, beamY, width);
            drawLoad(g, left, beamY, width);
            drawReactions(g, left, beamY, width);

            int shearTop = 290;
            int chartHeight = 95;
            drawDiagram(g, "Shear force V(x) [kN]", left, shearTop, width, chartHeight,
                    model::shear, model.maxAbsShear(), BLUE);

            int momentTop = 465;
            drawDiagram(g, "Bending moment M(x) [kNm]", left, momentTop, width, chartHeight,
                    model::moment, model.maxMoment(), ORANGE);

            g.dispose();
        }

        private void drawSectionTitle(Graphics2D g, String text, int x, int y) {
            g.setColor(DEEP_GREEN);
            g.setFont(new Font("Serif", Font.BOLD, 24));
            g.drawString(text, x, y);
        }

        private void drawBeam(Graphics2D g, int left, int y, int width) {
            g.setColor(CHARCOAL);
            g.fillRoundRect(left, y, width, 14, 6, 6);
            drawPinSupport(g, left, y + 14);
            drawRollerSupport(g, left + width, y + 14);

            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString("A", left - 5, y + 70);
            g.drawString("B", left + width - 4, y + 70);
            drawDimension(g, left, left + width, y + 88,
                    String.format(Locale.US, "L = %.2f m", model.length()));
        }

        private void drawPinSupport(Graphics2D g, int x, int y) {
            Path2D triangle = new Path2D.Double();
            triangle.moveTo(x, y);
            triangle.lineTo(x - 14, y + 24);
            triangle.lineTo(x + 14, y + 24);
            triangle.closePath();
            g.setColor(GREEN);
            g.fill(triangle);
            g.drawLine(x - 22, y + 27, x + 22, y + 27);
        }

        private void drawRollerSupport(Graphics2D g, int x, int y) {
            drawPinSupport(g, x, y);
            g.fillOval(x - 11, y + 27, 7, 7);
            g.fillOval(x + 4, y + 27, 7, 7);
            g.drawLine(x - 22, y + 37, x + 22, y + 37);
        }

        private void drawDimension(Graphics2D g, int start, int end, int y, String text) {
            g.setColor(new Color(90, 100, 92));
            g.drawLine(start, y, end, y);
            g.drawLine(start, y - 5, start, y + 5);
            g.drawLine(end, y - 5, end, y + 5);
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, (start + end - metrics.stringWidth(text)) / 2, y + 18);
        }

        private void drawLoad(Graphics2D g, int left, int beamY, int width) {
            g.setColor(DEEP_GREEN);
            g.setStroke(new BasicStroke(2.2f));
            if (model.type() == LoadType.POINT) {
                int x = left + width / 2;
                drawDownArrow(g, x, beamY - 76, beamY - 4);
                drawCenteredLabel(g, String.format(Locale.US, "P = %.1f kN", model.load()),
                        x, beamY - 86);
            } else {
                int top = beamY - 74;
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(left, beamY - 5);
                triangle.lineTo(left + width, top);
                triangle.lineTo(left + width, beamY - 5);
                triangle.closePath();
                g.setColor(SAGE);
                g.fill(triangle);
                g.setColor(DEEP_GREEN);
                g.draw(triangle);
                int arrows = 8;
                for (int i = 1; i <= arrows; i++) {
                    int x = left + i * width / arrows;
                    int arrowTop = beamY - 5 - (beamY - 5 - top) * i / arrows;
                    drawDownArrow(g, x, arrowTop, beamY - 4);
                }
                drawCenteredLabel(g,
                        String.format(Locale.US, "w_max = %.1f kN/m", model.load()),
                        left + width / 2, top - 12);
            }
            g.setStroke(new BasicStroke(1f));
        }

        private void drawReactions(Graphics2D g, int left, int beamY, int width) {
            g.setColor(GREEN);
            drawUpArrow(g, left, beamY + 58, beamY + 15);
            drawUpArrow(g, left + width, beamY + 58, beamY + 15);
            g.setFont(new Font("SansSerif", Font.PLAIN, 12));
            g.drawString(String.format(Locale.US, "R_A = %.2f kN", model.reactionA()),
                    left + 20, beamY + 48);
            String rightText = String.format(Locale.US, "R_B = %.2f kN", model.reactionB());
            int textWidth = g.getFontMetrics().stringWidth(rightText);
            g.drawString(rightText, left + width - textWidth - 20, beamY + 48);
        }

        private void drawDiagram(Graphics2D g, String title, int left, int top, int width,
                                 int height, DoubleUnaryOperator function, double maxAbs,
                                 Color color) {
            drawSectionTitle(g, title, left, top - 22);
            int axisY = top + height / 2;
            g.setColor(new Color(180, 188, 181));
            g.drawLine(left, axisY, left + width, axisY);
            g.drawLine(left, top, left, top + height);

            double scale = maxAbs > 0 ? (height * 0.42) / maxAbs : 1.0;
            Path2D area = new Path2D.Double();
            area.moveTo(left, axisY);
            Path2D curve = new Path2D.Double();

            int samples = 240;
            for (int i = 0; i <= samples; i++) {
                double x = model.length() * i / samples;
                double value = function.applyAsDouble(x);
                int px = left + width * i / samples;
                int py = (int) Math.round(axisY - value * scale);
                if (i == 0) {
                    curve.moveTo(px, py);
                } else {
                    curve.lineTo(px, py);
                }
                area.lineTo(px, py);
            }
            area.lineTo(left + width, axisY);
            area.closePath();

            Color fill = new Color(color.getRed(), color.getGreen(), color.getBlue(), 42);
            g.setColor(fill);
            g.fill(area);
            g.setColor(color);
            g.setStroke(new BasicStroke(2.4f));
            g.draw(curve);
            g.setStroke(new BasicStroke(1f));

            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.setColor(new Color(90, 100, 92));
            g.drawString("0", left - 15, axisY + 4);
            g.drawString(String.format(Locale.US, "+%.2f", maxAbs), left - 48, top + 8);
            g.drawString(String.format(Locale.US, "-%.2f", maxAbs), left - 48, top + height);
            g.drawString("x = 0", left, top + height + 18);
            String end = String.format(Locale.US, "x = %.2f m", model.length());
            g.drawString(end, left + width - g.getFontMetrics().stringWidth(end), top + height + 18);
        }

        private void drawDownArrow(Graphics2D g, int x, int top, int bottom) {
            g.drawLine(x, top, x, bottom);
            g.drawLine(x, bottom, x - 6, bottom - 10);
            g.drawLine(x, bottom, x + 6, bottom - 10);
        }

        private void drawUpArrow(Graphics2D g, int x, int bottom, int top) {
            g.drawLine(x, bottom, x, top);
            g.drawLine(x, top, x - 6, top + 10);
            g.drawLine(x, top, x + 6, top + 10);
        }

        private void drawCenteredLabel(Graphics2D g, String text, int centerX, int baselineY) {
            g.setFont(new Font("SansSerif", Font.BOLD, 12));
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(text, centerX - metrics.stringWidth(text) / 2, baselineY);
        }
    }
}


