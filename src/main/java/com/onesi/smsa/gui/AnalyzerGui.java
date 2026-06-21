package com.onesi.smsa.gui;

import com.onesi.smsa.app.AnalysisExecutionResult;
import com.onesi.smsa.app.AnalysisRequest;
import com.onesi.smsa.app.AnalysisRunner;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AnalyzerGui {
    private static final DateTimeFormatter LOG_TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JFrame frame;
    private final JTextField targetPathField = new JTextField();
    private final JTextField outputPathField = new JTextField();
    private final JTextArea logArea = new JTextArea();
    private final JButton analyzeButton = new JButton("Analyze");
    private final JButton chooseTargetButton = new JButton("Browse...");
    private final JButton chooseOutputButton = new JButton("Save as...");

    public AnalyzerGui() {
        frame = new JFrame("Spring MVC Static Analyzer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 480));
        frame.setLayout(new BorderLayout(8, 8));
        frame.add(formPanel(), BorderLayout.NORTH);
        frame.add(logPanel(), BorderLayout.CENTER);
        frame.add(actionPanel(), BorderLayout.SOUTH);
        frame.setLocationRelativeTo(null);
    }

    public static void show() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // The default look and feel is fine if the system one is unavailable.
            }
            AnalyzerGui gui = new AnalyzerGui();
            gui.frame.setVisible(true);
        });
    }

    private JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 4, 12));

        targetPathField.setEditable(false);
        outputPathField.setEditable(false);
        chooseTargetButton.addActionListener(event -> chooseTargetDirectory());
        chooseOutputButton.addActionListener(event -> chooseOutputFile());

        addRow(panel, 0, "Target project", targetPathField, chooseTargetButton);
        addRow(panel, 1, "Result file", outputPathField, chooseOutputButton);
        return panel;
    }

    private void addRow(JPanel panel, int row, String label, JTextField field, JButton button) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(4, 0, 4, 8);
        panel.add(new JLabel(label), labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1.0;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.insets = new Insets(4, 0, 4, 8);
        panel.add(field, fieldConstraints);

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        buttonConstraints.gridx = 2;
        buttonConstraints.gridy = row;
        buttonConstraints.insets = new Insets(4, 0, 4, 0);
        panel.add(button, buttonConstraints);
    }

    private JScrollPane logPanel() {
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        appendLog("Ready.");
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder("Execution log"));
        return scrollPane;
    }

    private JPanel actionPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 12, 12, 12));
        analyzeButton.addActionListener(event -> runAnalysis());
        panel.add(analyzeButton);
        return panel;
    }

    private void chooseTargetDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select target Spring MVC project");
        if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
            Path targetPath = chooser.getSelectedFile().toPath();
            targetPathField.setText(targetPath.toString());
            if (outputPathField.getText().isBlank()) {
                outputPathField.setText(targetPath.resolve("smsa-result.txt").toString());
            }
            appendLog("Target project selected: " + targetPath);
        }
    }

    private void chooseOutputFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select result file");
        chooser.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        chooser.setSelectedFile(Path.of("result.txt").toFile());
        if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            Path outputPath = chooser.getSelectedFile().toPath();
            outputPathField.setText(outputPath.toString());
            appendLog("Result file selected: " + outputPath);
        }
    }

    private void runAnalysis() {
        Path targetPath = pathFromField(targetPathField);
        Path outputPath = pathFromField(outputPathField);
        if (targetPath == null || outputPath == null) {
            showError("Select both a target project folder and a result file.");
            return;
        }
        setControlsEnabled(false);
        appendLog("Analysis started.");
        appendLog("Target project: " + targetPath);
        appendLog("Result file: " + outputPath);

        new SwingWorker<AnalysisExecutionResult, String>() {
            @Override
            protected AnalysisExecutionResult doInBackground() {
                try {
                    AnalysisExecutionResult result =
                            new AnalysisRunner().run(new AnalysisRequest(targetPath, outputPath));
                    for (var warning : result.warnings()) {
                        publish("WARN " + warning.format().replace("\n", " "));
                    }
                    if (result.reportWritten()) {
                        publish("Analysis completed.");
                        publish("Report written: " + result.outputPath());
                    } else if (result.exitCode() == 1) {
                        publish("Analysis stopped because the selected input cannot be analyzed.");
                        publish(result.message());
                    } else {
                        publish("ERROR " + result.message());
                    }
                    return result;
                } catch (Exception ex) {
                    publish("ERROR Analysis failed: " + ex.getMessage());
                    return new AnalysisExecutionResult(2, targetPath, outputPath, false, List.of(), ex.getMessage());
                }
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                chunks.forEach(AnalyzerGui.this::appendLog);
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
            }
        }.execute();
    }

    private Path pathFromField(JTextField field) {
        if (field.getText().isBlank()) {
            return null;
        }
        return Path.of(field.getText());
    }

    private void setControlsEnabled(boolean enabled) {
        analyzeButton.setEnabled(enabled);
        chooseTargetButton.setEnabled(enabled);
        chooseOutputButton.setEnabled(enabled);
    }

    private void appendLog(String message) {
        logArea.append("[" + LocalTime.now().format(LOG_TIME) + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void showError(String message) {
        appendLog("ERROR " + message);
        JOptionPane.showMessageDialog(frame, message, "Spring MVC Static Analyzer", JOptionPane.ERROR_MESSAGE);
    }
}
