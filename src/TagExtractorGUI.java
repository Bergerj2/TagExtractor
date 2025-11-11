import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

public class TagExtractorGUI extends JFrame {
    private Set<String> stopWords = new HashSet<>();
    private Map<String, Integer> tagFrequencies = new TreeMap<>();
    private JTextArea outputTextArea;
    private JLabel statusLabel;
    private JLabel fileLabel;
    private File currentTextFile;
    private File currentStopWordsFile;

    public TagExtractorGUI() {
        super("Tag Extractor");
        setupGUI();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void setupGUI() {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton loadStopWordsButton = new JButton("1. Load Stop Words File");
        loadStopWordsButton.addActionListener(e -> selectAndLoadStopWords());

        JButton processFileButton = new JButton("2. Select Text File & Extract Tags");
        processFileButton.addActionListener(e -> selectAndProcessTextFile());

        JButton saveTagsButton = new JButton("3. Save Tags to File");
        saveTagsButton.addActionListener(e -> saveTagsToFile());

        fileLabel = new JLabel("File Status: No text file processed yet.");
        fileLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        topPanel.add(loadStopWordsButton);
        topPanel.add(processFileButton);
        topPanel.add(saveTagsButton);
        topPanel.add(fileLabel);

        contentPane.add(topPanel, BorderLayout.NORTH);

        outputTextArea = new JTextArea();
        outputTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        outputTextArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputTextArea);

        contentPane.add(scrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Ready. Load stop words first.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        contentPane.add(statusLabel, BorderLayout.SOUTH);
    }

    private void selectAndLoadStopWords() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Stop Words File");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentStopWordsFile = fileChooser.getSelectedFile();
            loadStopWords(currentStopWordsFile);
        }
    }

    private void loadStopWords(File file) {
        stopWords.clear();
        int wordCount = 0;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String word = scanner.nextLine().trim().toLowerCase();
                if (!word.isEmpty()) {
                    stopWords.add(word);
                    wordCount++;
                }
            }
            statusLabel.setText("Successfully Loaded " + wordCount + " stop words from: " + file.getName());
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error: Stop words file not found: " + e.getMessage());
            statusLabel.setText("Error loading stop words.");
        }
    }

    private void selectAndProcessTextFile() {
        if (stopWords.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load the stop words file first (Step 1).",
                    "Missing Dependency", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select Text File for Tag Extraction");

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentTextFile = fileChooser.getSelectedFile();
            fileLabel.setText("Processing File: " + currentTextFile.getName());
            processTextFile(currentTextFile);
        }
    }

    private void processTextFile(File file) {
        tagFrequencies.clear();
        int totalWordsProcessed = 0;

        try (Scanner fileScanner = new Scanner(file)) {
            fileScanner.useDelimiter("[^a-zA-Z']+");

            while (fileScanner.hasNext()) {
                String word = fileScanner.next();

                String normalizedWord = word.toLowerCase().replaceAll("[^a-z']", "");

                if (normalizedWord.isEmpty() || normalizedWord.equals("'")) {
                    continue;
                }

                totalWordsProcessed++;

                if (!stopWords.contains(normalizedWord)) {
                    tagFrequencies.put(normalizedWord,
                            tagFrequencies.getOrDefault(normalizedWord, 0) + 1);
                }
            }
            displayTags();
            statusLabel.setText(String.format("Extraction Complete. %d unique tags found from %d words processed.",
                    tagFrequencies.size(), totalWordsProcessed));
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error: Text file not found: " + e.getMessage());
            statusLabel.setText("Error processing text file.");
        }
    }

    private void displayTags() {
        if (tagFrequencies.isEmpty()) {
            outputTextArea.setText("No tags extracted (or file was empty/only contained stop words).");
            return;
        }

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(tagFrequencies.entrySet());
        sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        StringBuilder sb = new StringBuilder("TAGS (Sorted by Frequency):\n");
        sb.append("---------------------------------------\n");
        sb.append(String.format("%-20s %s%n", "WORD", "FREQUENCY"));
        sb.append("---------------------------------------\n");

        for (Map.Entry<String, Integer> entry : sortedEntries) {
            sb.append(String.format("%-20s %d%n", entry.getKey(), entry.getValue()));
        }

        outputTextArea.setText(sb.toString());
        outputTextArea.setCaretPosition(0);
    }

    private void saveTagsToFile() {
        if (tagFrequencies.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please extract tags first (Step 2).",
                    "No Data to Save", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Extracted Tags As...");

        if (currentTextFile != null) {
            String defaultName = currentTextFile.getName().replaceFirst("[.][^.]+$", "") + "_tags.txt";
            fileChooser.setSelectedFile(new File(defaultName));
        }

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(saveFile)) {
                writer.println("Extracted Tags and Frequencies from: " + currentTextFile.getName());
                writer.println("---------------------------------------\n");

                List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(tagFrequencies.entrySet());
                sortedEntries.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

                for (Map.Entry<String, Integer> entry : sortedEntries) {
                    writer.println(String.format("%s: %d", entry.getKey(), entry.getValue()));
                }

                statusLabel.setText("Tags successfully saved to: " + saveFile.getName());
            } catch (FileNotFoundException e) {
                JOptionPane.showMessageDialog(this, "Error: Could not save file.",
                        "Save Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TagExtractorGUI());
    }
}