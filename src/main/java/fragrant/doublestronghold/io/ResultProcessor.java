package fragrant.doublestronghold.io;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

public class ResultProcessor {
    private static final int RESULT_PROCESSING_INTERVAL_MS = 100;
    private final BlockingQueue<String> resultQueue;
    private final DefaultTableModel tableModel;
    private final Thread processorThread;

    public ResultProcessor(BlockingQueue<String> resultQueue, DefaultTableModel tableModel) {
        this.resultQueue = resultQueue;
        this.tableModel = tableModel;
        this.processorThread = createProcessorThread();
    }

    public void start() {
        processorThread.start();
    }

    private Thread createProcessorThread() {
        Thread thread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String result = resultQueue.poll(RESULT_PROCESSING_INTERVAL_MS, TimeUnit.MILLISECONDS);
                    if (result != null) {
                        updateUI(result);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        thread.setDaemon(true);
        return thread;
    }

    private void updateUI(String result) {
        String[] parts = result.split(" ", 2);
        if (parts.length >= 2) {
            String seed = parts[0];
            String command = parts[1];

            SwingUtilities.invokeLater(() -> {
                tableModel.addRow(new Object[]{seed, command});
            });
        }
    }
}