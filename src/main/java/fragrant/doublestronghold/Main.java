package fragrant.doublestronghold;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import fragrant.doublestronghold.core.search.SearchEngine;
import fragrant.doublestronghold.core.search.StrongholdSearcher;
import fragrant.doublestronghold.domain.SearchParameters;
import fragrant.doublestronghold.io.ResultProcessor;
import fragrant.doublestronghold.ui.SearchParametersPanel;

public class Main extends JFrame {
    private SearchParametersPanel parametersPanel;
    private JButton searchButton;
    private JButton stopButton;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel progressLabel;
    private JLabel statusLabel;
    private ExecutorService executor;
    private ScheduledExecutorService uiUpdateExecutor;
    private ScheduledExecutorService startSeedUpdateExecutor;
    private final AtomicBoolean isSearching = new AtomicBoolean(false);
    private final AtomicLong currentStartSeed = new AtomicLong(0);
    private final AtomicLong seedsProcessed = new AtomicLong(0);
    private final AtomicInteger resultsFound = new AtomicInteger(0);
    private final StrongholdSearcher searcher;
    private final SearchEngine searchEngine;
    private static final int UI_UPDATE_INTERVAL_MS = 500;
    private static final int SEED_UPDATE = 1000;

    public Main() {
        setTitle("Double Stronghold Finder");
        setSize(600, 620);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        searcher = new StrongholdSearcher(isSearching);
        BlockingQueue<String> resultQueue = new LinkedBlockingQueue<>();
        searchEngine = new SearchEngine(isSearching, currentStartSeed, seedsProcessed,
                resultsFound, resultQueue, searcher);

        initComponents();
        ResultProcessor resultProcessor = new ResultProcessor(resultQueue, tableModel);
        resultProcessor.start();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        parametersPanel = new SearchParametersPanel();
        add(parametersPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel();
        searchButton = new JButton("Start");
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        searchButton.addActionListener(e -> startSearch());
        stopButton.addActionListener(e -> stopSearch());

        buttonPanel.add(searchButton);
        buttonPanel.add(stopButton);

        String[] columnNames = {"Seed", "Mid portal xz"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        resultsTable = new JTable(tableModel);
        resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        resultsTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        resultsTable.getColumnModel().getColumn(1).setPreferredWidth(300);

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem copySeedsItem = new JMenuItem("Copy Seeds");
        JMenuItem copyCommandsItem = new JMenuItem("Copy Commands");
        JMenuItem copyAllItem = new JMenuItem("Copy All");
        JMenuItem deleteItem = new JMenuItem("Delete");

        copySeedsItem.addActionListener(e -> copySelectedSeeds());
        copyCommandsItem.addActionListener(e -> copySelectedCommands());
        copyAllItem.addActionListener(e -> copySelectedRows());
        deleteItem.addActionListener(e -> deleteSelectedRows());

        popupMenu.add(copySeedsItem);
        popupMenu.add(copyCommandsItem);
        popupMenu.add(copyAllItem);
        popupMenu.addSeparator();
        popupMenu.add(deleteItem);

        resultsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopup(e);
                }
            }

            private void showPopup(MouseEvent e) {
                int row = resultsTable.rowAtPoint(e.getPoint());
                if (row >= 0 && row < resultsTable.getRowCount()) {
                    if (!resultsTable.isRowSelected(row)) {
                        resultsTable.setRowSelectionInterval(row, row);
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        scrollPane.setPreferredSize(new Dimension(550, 300));

        statusLabel = new JLabel();
        progressLabel = new JLabel();

        JPanel progressPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        statusLabel.setHorizontalAlignment(JLabel.CENTER);
        progressLabel.setHorizontalAlignment(JLabel.CENTER);
        progressPanel.add(statusLabel);
        progressPanel.add(progressLabel);

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(scrollPane, BorderLayout.CENTER);
        southPanel.add(progressPanel, BorderLayout.SOUTH);

        add(southPanel, BorderLayout.CENTER);
    }

    private void copySelectedSeeds() {
        int[] selectedRows = resultsTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int row : selectedRows) {
            String seed = (String) tableModel.getValueAt(row, 0);
            sb.append(seed).append("\n");
        }

        StringSelection selection = new StringSelection(sb.toString().trim());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void copySelectedCommands() {
        int[] selectedRows = resultsTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int row : selectedRows) {
            String command = (String) tableModel.getValueAt(row, 1);
            sb.append(command).append("\n");
        }

        StringSelection selection = new StringSelection(sb.toString().trim());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void copySelectedRows() {
        int[] selectedRows = resultsTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        StringBuilder sb = new StringBuilder();
        for (int row : selectedRows) {
            String seed = (String) tableModel.getValueAt(row, 0);
            String command = (String) tableModel.getValueAt(row, 1);
            sb.append(seed).append("\t").append(command).append("\n");
        }

        StringSelection selection = new StringSelection(sb.toString());
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void deleteSelectedRows() {
        int[] selectedRows = resultsTable.getSelectedRows();
        if (selectedRows.length == 0) return;

        for (int i = selectedRows.length - 1; i >= 0; i--) {
            tableModel.removeRow(selectedRows[i]);
        }
    }

    private void startSearch() {
        if (isSearching.get()) return;

        try {
            long startSeed = parametersPanel.getStartSeed();
            long endSeed = parametersPanel.getEndSeed();
            int threadCount = parametersPanel.getThreadCount();
            int centerX = parametersPanel.getCenterX();
            int centerZ = parametersPanel.getCenterZ();
            int searchRadius = parametersPanel.getSearchRadius();
            double maxStrongholdDistance = parametersPanel.getMaxStrongholdDistance();
            double maxPortalDistance = parametersPanel.getMaxPortalDistance();

            if (threadCount <= 0 || searchRadius <= 0 || maxStrongholdDistance <= 0 || maxPortalDistance <= 0) {
                JOptionPane.showMessageDialog(this, "All numeric values must be positive",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (startSeed == endSeed) {
                JOptionPane.showMessageDialog(this, "Start seed and end seed cannot be the same",
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                return;
            }

            isSearching.set(true);
            searchButton.setEnabled(false);
            stopButton.setEnabled(true);
            currentStartSeed.set(startSeed);
            seedsProcessed.set(0);
            searcher.clearCaches();

            final SearchParameters params = new SearchParameters(
                    centerX, centerZ, searchRadius, maxStrongholdDistance, maxPortalDistance
            );

            executor = Executors.newFixedThreadPool(threadCount, r -> {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                return thread;
            });

            uiUpdateExecutor = Executors.newSingleThreadScheduledExecutor();
            startSeedUpdateExecutor = Executors.newSingleThreadScheduledExecutor();

            startProgressUpdates();
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> searchEngine.searchSequential(startSeed, endSeed, params));
            }
            monitorSearchCompletion();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid number format in one of the fields",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startProgressUpdates() {
        final long startTime = System.currentTimeMillis();

        uiUpdateExecutor.scheduleAtFixedRate(() -> {
            long currentTime = System.currentTimeMillis();
            long elapsedMs = currentTime - startTime;

            if (elapsedMs == 0) return;

            long currentProcessed = seedsProcessed.get();
            double seedsPerSec = (currentProcessed * 1000.0) / elapsedMs;

            SwingUtilities.invokeLater(() -> progressLabel.setText(String.format("%.2f seeds/sec", seedsPerSec)));
        }, 0, UI_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS);

        startSeedUpdateExecutor.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() ->
                    parametersPanel.setStartSeed(String.valueOf(currentStartSeed.get())));
        }, 0, SEED_UPDATE, TimeUnit.MILLISECONDS);
    }

    private void monitorSearchCompletion() {
        CompletableFuture.runAsync(() -> {
            try {
                executor.shutdown();
                boolean terminated = executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

                if (terminated || !isSearching.get()) {
                    SwingUtilities.invokeLater(() -> {
                        searchButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        statusLabel.setText(isSearching.get() ? "Completed" : "Stopped");
                        isSearching.set(false);
                    });

                    if (uiUpdateExecutor != null) {
                        uiUpdateExecutor.shutdown();
                    }
                    if (startSeedUpdateExecutor != null) {
                        startSeedUpdateExecutor.shutdown();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    private void stopSearch() {
        if (isSearching.compareAndSet(true, false)) {
            statusLabel.setText("Stopping search...");

            if (executor != null) {
                executor.shutdownNow();
            }
            if (uiUpdateExecutor != null) {
                uiUpdateExecutor.shutdownNow();
            }
            if (startSeedUpdateExecutor != null) {
                startSeedUpdateExecutor.shutdownNow();
            }

            CompletableFuture.runAsync(() -> {
                try {
                    if (executor != null) {
                        executor.awaitTermination(5, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        searchButton.setEnabled(true);
                        stopButton.setEnabled(false);
                        statusLabel.setText("Search stopped");
                    });
                }
            });
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new Main().setVisible(true);
        });
    }

}