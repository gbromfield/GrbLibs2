package com.ciena.logx;

import com.ciena.logx.logrecord.LogRecordParser;
import com.ciena.logx.output.OutputContext;
import com.ciena.logx.util.ExtensionFilter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

/**
 * Created by gbromfie on 11/1/16.
 */
public interface LogXProperties {
    public ArrayList<String> getInputFilenames();
    public void addInputFilename(String inputFilename);

    public ArrayList<File> getInputFiles();

    public ExtensionFilter getFilter();
    public void setFilter(ExtensionFilter filter);

    public ArrayList<String> getParserNames();
    public void addParserName(String parserName);

    public ArrayList<LogRecordParser> getParsers() throws ClassNotFoundException, NoSuchMethodException,
            IllegalAccessException, InvocationTargetException, InstantiationException;

    public Date[] getDateRange();
    public void setDateRange(Date[] range);

    public Date getFromDate();
    public Date getToDate();

    public HashSet<String> getIncTids();
    public void setIncTids(HashSet<String> incTids);

    public HashSet<String> getExclTids();
    public void setExclTids(HashSet<String> exclTids);

    public HashSet<String> getIncSids();
    public void setIncSids(HashSet<String> incSids);

    public HashSet<String> getExclSids();
    public void setExclSids(HashSet<String> exclSids);

    public Boolean getInclusive();
    public void setInclusive(boolean inclusive);

    public void setOutputContext(OutputContext outputContext);
    public OutputContext getOutputContext();

    public boolean printHelp();
    public void setPrintHelp(boolean printHelp);

    public String getUnknownArg();
    public void setUnknownArg(String unknownArg);
}
