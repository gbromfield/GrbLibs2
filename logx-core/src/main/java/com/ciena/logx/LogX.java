package com.ciena.logx;

import com.ciena.logx.logfile.ra.RALogFileParser;
import com.ciena.logx.logfile.LogFileParser;
import com.ciena.logx.logrecord.LogRecord;
import com.ciena.logx.logrecord.LogRecordParser;
import com.ciena.logx.output.OutputContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by gbromfie on 10/19/16.
 */
public class LogX {
    final Logger logger = LoggerFactory.getLogger(LogX.class);

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

    LogXProperties _props;

    public LogX(LogXProperties props) {
        _props = props;
    }

    public void run() {
        try {
            _props.getOutputContext().init();

            LinkedBlockingQueue<LogRecordProcessor.LogRecordProcessorTask> queue =
                    new LinkedBlockingQueue<LogRecordProcessor.LogRecordProcessorTask>();
            CountDownLatch latch = new CountDownLatch(1);
            LogRecordProcessor proc = new LogRecordProcessor(queue, latch);
            Thread procThread = new Thread(proc);
            procThread.start();

            ArrayList<LogRecordParser> logRecordParsers = _props.getParsers();
            ArrayList<File> inputFileList = _props.getInputFiles();
            boolean legend = true;
            for(int i = 0; i < inputFileList.size(); i++) {
                LogFileParser logFileParser = null;
                try {
                    logFileParser = new RALogFileParser(inputFileList.get(i).getAbsolutePath());
                    if (i > 0) {
                        if (logger.isInfoEnabled()) {
                            logger.info(String.format("Finished Log File %s (%d of %d) with total size now %d",
                                    inputFileList.get(i).getAbsolutePath(), i+1, inputFileList.size(),
                                    _props.getOutputContext().size()));
                        }
                    }
                    if (logger.isInfoEnabled()) {
                        logger.info(String.format("Processing Log File %s (%d of %d)",
                                inputFileList.get(i).getAbsolutePath(), i+1, inputFileList.size()));
                    }
                    for (int j = 0; j < logRecordParsers.size(); j++) {
                        if (legend) {
                            logRecordParsers.get(j).writeLegend();
                        }
                    }
                    legend = false;
                    while(logFileParser.hasNext()) {
                        LogRecord logRecord = logFileParser.next();
                        for (int j = 0; j < logRecordParsers.size(); j++) {
                            proc.submit(logRecordParsers.get(j), logRecord);
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
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Done processing Log Files, (%d size)", _props.getOutputContext().size()));
            }
            proc.submitClose();
            latch.await();
            if (logger.isInfoEnabled()) {
                logger.info("Done processing Log Records");
            }

            // start conflating
            for (int i = 0; i < logRecordParsers.size(); i++) {
                if (i ==0) {
                    if (logger.isInfoEnabled()) {
                        logger.info("Starting conflation of Log Records");
                    }
                }
                logRecordParsers.get(i).conflate();
            }
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Done conflation of Log Records, (%d size)", _props.getOutputContext().size()));
            }

            _props.getOutputContext().close();

            if (logger.isInfoEnabled()) {
                logger.info("Done processing output files");
            }
        } catch(IOException e) {
            e.printStackTrace();
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            if (_props.getOutputContext() != null) {
                _props.getOutputContext().onFinally();
            }
        }
    }
}
