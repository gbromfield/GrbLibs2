package com.ciena.logx;

import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.logrecord.LogRecordParser;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by gbromfie on 5/10/16.
 */
public class LogRecordProcessor implements Runnable {

    public class LogRecordProcessorTask {
        LogRecordParser logRecordParser;
        LogRecord logRecord;
    }

    private LinkedBlockingQueue<LogRecordProcessorTask> _queue;
    private CountDownLatch _latch;

    public LogRecordProcessor(LinkedBlockingQueue<LogRecordProcessorTask> queue, CountDownLatch latch) {
        _queue = queue;
        _latch = latch;
    }

    public void submit(LogRecordParser logRecordParser, LogRecord logRecord) throws InterruptedException {
        LogRecordProcessorTask task = new LogRecordProcessorTask();
        task.logRecordParser = logRecordParser;
        task.logRecord = logRecord;
        _queue.put(task);
    }

    public void submitClose() throws InterruptedException {
        submit(null, null);
    }

    @Override
    public void run() {
        while(true) {
            try {
                LogRecordProcessorTask task = _queue.take();
                if (task.logRecordParser == null) {
                    // exit signal
                    _latch.countDown();
                    return;
                }
                task.logRecordParser.parse(task.logRecord);
            } catch(Exception e) {
                e.printStackTrace();
                _latch.countDown();
                return;
            }
        }
    }
}
