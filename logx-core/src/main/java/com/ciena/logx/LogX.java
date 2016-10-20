package com.ciena.logx;

import com.ciena.logx.logfile.ra.RALogFileParser;
import com.ciena.logx.logfile.LogFileParser;
import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.logrecord.LogRecordParser;
import com.ciena.logx.output.OutputContext;

import java.io.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by gbromfie on 10/19/16.
 */
public class LogX {
    public static String getHostname() {
        InputStream is = null;
        try {
            Process p = Runtime.getRuntime().exec("hostname");
            p.waitFor();
            is = p.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            return reader.readLine();
        } catch(Exception e) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                }
            }
        }
        return null;
    }

    public static ArrayList<File> processFilenames(ArrayList<String> inputFiles, FileFilter filter) {
        ArrayList<File> inputFileList = new ArrayList<File>();
        for (int i = 0; i < inputFiles.size(); i++) {
            File f = new File(inputFiles.get(i));
            if (!f.exists()) {
                throw new IllegalArgumentException(String.format("Input File \"%s\" does not exist", inputFiles.get(i)));
            }
            if (f.isDirectory()) {
                processDirectory(f, filter, inputFileList);
            } else if (!f.isHidden()) {
                inputFileList.add(f);
            }
        }
        return inputFileList;
    }

    public static void processDirectory(File f, FileFilter filter, ArrayList<File> inputFileList) {
        File[] dirFiles = null;
        if (filter == null) {
            dirFiles = f.listFiles();
        } else {
            dirFiles = f.listFiles(filter);
        }
        if (dirFiles != null) {
            for(int i = 0; i < dirFiles.length; i++) {
                if (dirFiles[i].isDirectory()) {
                    processDirectory(dirFiles[i], filter, inputFileList);
                } else if (!dirFiles[i].isHidden()) {
                    inputFileList.add(dirFiles[i]);
                }
            }
        }
    }

    private ArrayList<File> _inputFileList;
    private OutputContext _ctx;

    public LogX(ArrayList<File> inputFileList, OutputContext ctx) {
        _inputFileList = inputFileList;
        _ctx = ctx;
    }

    public void run() {
        try {
            _ctx.init();

            LinkedBlockingQueue<LogRecordProcessor.LogRecordProcessorTask> queue =
                    new LinkedBlockingQueue<LogRecordProcessor.LogRecordProcessorTask>();
            CountDownLatch latch = new CountDownLatch(1);
            LogRecordProcessor proc = new LogRecordProcessor(queue, latch);
            Thread procThread = new Thread(proc);
            procThread.start();

            LogRecordParser[] logRecordParsers = _ctx.getLogRecordParsers();
            boolean legend = true;
            for(int i = 0; i < _inputFileList.size(); i++) {
                LogFileParser logFileParser = null;
                try {
                    logFileParser = new RALogFileParser(_inputFileList.get(i).getAbsolutePath());
                    if (i > 0) {
                        System.out.println(String.format("Finished Log File %s (%d of %d) with total size now %d",
                                _inputFileList.get(i).getAbsolutePath(), i+1, _inputFileList.size(), _ctx.size()));
                    }
                    System.out.println(String.format("Processing Log File %s (%d of %d)",
                            _inputFileList.get(i).getAbsolutePath(), i+1, _inputFileList.size()));
                    for (int j = 0; j < logRecordParsers.length; j++) {
                        if (legend) {
                            logRecordParsers[j].writeLegend();
                        }
                    }
                    legend = false;
                    while(logFileParser.hasNext()) {
                        LogRecord logRecord = logFileParser.next();
                        for (int j = 0; j < logRecordParsers.length; j++) {
                            proc.submit(logRecordParsers[j], logRecord);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (logFileParser != null) {
                        logFileParser.dispose();
                    }
                }
            }

            System.out.println(String.format("Done processing Log Files, (%d size)", _ctx.size()));
            proc.submitClose();
            latch.await();
            System.out.println("Done processing Log Records");

            // start conflating
            for (int i = 0; i < logRecordParsers.length; i++) {
                if (i ==0) {
                    System.out.println("Starting conflation of Log Records");
                }
                logRecordParsers[i].conflate();
            }
            System.out.println(String.format("Done conflation of Log Records, (%d size)", _ctx.size()));

            _ctx.close();

            System.out.println("Done processing output files");
        } catch(IOException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (_ctx != null) {
                _ctx.onFinally();
            }
        }
    }
}
