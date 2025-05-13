package fragrant.doublestronghold.ui;

import javax.swing.*;
import java.awt.*;

public class SearchParametersPanel extends JPanel {
    private final JTextField startSeedField;
    private final JSpinner threadCountSpinner;
    private final JTextField centerXField;
    private final JTextField centerZField;
    private final JTextField searchRadiusField;
    private final JTextField maxStrongholdDistanceField;
    private final JTextField maxPortalDistanceField;

    public SearchParametersPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("Seed"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        startSeedField = new JTextField("0");
        add(startSeedField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        add(new JLabel("Thread"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        int defaultThreads = Math.max(1, availableProcessors / 2);

        SpinnerModel spinnerModel = new SpinnerNumberModel(defaultThreads, 1, availableProcessors, 1);
        threadCountSpinner = new JSpinner(spinnerModel);
        JComponent editor = threadCountSpinner.getEditor();
        JFormattedTextField textField = ((JSpinner.DefaultEditor) editor).getTextField();
        textField.setColumns(3);
        add(threadCountSpinner, gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0;
        add(new JLabel("Center X"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        centerXField = createTextFieldWithUnit("0", "blocks");
        add(centerXField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        add(new JLabel("Center Z"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        centerZField = createTextFieldWithUnit("0", "blocks");
        add(centerZField, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0;
        add(new JLabel("Radius"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        searchRadiusField = createTextFieldWithUnit("1500", "blocks");
        add(searchRadiusField, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        add(new JLabel("Stronghold Distance"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        maxStrongholdDistanceField = createTextFieldWithUnit("32", "blocks");
        add(maxStrongholdDistanceField, gbc);

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0;
        add(new JLabel("Portal Distance"), gbc);

        gbc.gridx = 1; gbc.weightx = 1.0;
        maxPortalDistanceField = createTextFieldWithUnit("64", "blocks");
        add(maxPortalDistanceField, gbc);
    }

    private JTextField createTextFieldWithUnit(String defaultValue, String unit) {
        JTextField field = new JTextField(defaultValue) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (!hasFocus() && isEnabled()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                            RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(Color.GRAY);

                    FontMetrics fm = g2.getFontMetrics();
                    int textWidth = fm.stringWidth(unit);
                    int x = getWidth() - textWidth - 5;
                    int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();

                    g2.drawString(unit, x, y);
                    g2.dispose();
                }
            }
        };

        field.setBorder(BorderFactory.createCompoundBorder(
                field.getBorder(),
                BorderFactory.createEmptyBorder(0, 0, 0, 40)
        ));

        return field;
    }



    public long getStartSeed() {
        return Long.parseLong(startSeedField.getText());
    }

    public int getThreadCount() {
        return (Integer) threadCountSpinner.getValue();
    }

    public int getCenterX() {
        return Integer.parseInt(centerXField.getText());
    }

    public int getCenterZ() {
        return Integer.parseInt(centerZField.getText());
    }

    public int getSearchRadius() {
        return Integer.parseInt(searchRadiusField.getText());
    }

    public double getMaxStrongholdDistance() {
        return Double.parseDouble(maxStrongholdDistanceField.getText());
    }

    public double getMaxPortalDistance() {
        return Double.parseDouble(maxPortalDistanceField.getText());
    }

    public void setStartSeed(String value) {
        startSeedField.setText(value);
    }

}