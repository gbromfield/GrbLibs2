package com.ciena.logx.output;

import java.util.Date;
import java.util.LinkedList;

/**
 * Created by gbromfie on 7/22/16.
 */
public class OutputRecordSet {
    private LinkedList<OutputRecord> _list;
    private int _lastIndex;
    private int _iteratorIndex;
    private String _bufferStr;
    private Date _fromDate;
    private Date _toDate;

    public OutputRecordSet() {
        this(null, null);
    }

    public OutputRecordSet(Date fromDate, Date toDate) {
        _list = new LinkedList<OutputRecord>();
        _lastIndex = -1;
        _iteratorIndex = -1;
        _bufferStr = null;
        _fromDate = fromDate;
        _toDate = toDate;
    }

    public int size() {
        return _list.size();
    }

    public OutputRecord get(int index) {
        return _list.get(index);
    }

    public void addFirst(String format, Object... args) {
        addFirst(new OutputRecord(null, null, null, format, args));
    }

    public void addFirst(OutputRecord record) {
        Date startDate = new Date(0);
        record.setLogDate(startDate);
        add(record);
    }

    public void add(Date logDate, String format, Object... args) {
        add(new OutputRecord(logDate, null, null, format, args));
    }

    public void add(OutputRecord record) {
        if ((record.getLogDate() != null) && (record.getLogDate().getTime() != 0)) {
            if (_fromDate != null) {
                if (_fromDate.compareTo(record.getLogDate()) > 0) {
                    return;
                }
            }
            if (_toDate != null) {
                if (_toDate.compareTo(record.getLogDate()) < 0) {
                    return;
                }
            }
        }
        if(_lastIndex == -1) {
            _list.addFirst(record);
            _lastIndex = 0;
            return;
        }
        boolean found = false;
        for(int i = _lastIndex; i < _list.size(); i++) {
            if(_list.get(i).getLogDate().compareTo(record.getLogDate()) > 0) {
                if (i == 0) {
                    _list.addFirst(record);
                    found = true;
                    _lastIndex = 0;
                    break;
                } else if (i == _lastIndex) {
                    _lastIndex = 0;
                    add(record);
                    found = true;
                    break;
                } else {
                    _list.add(i, record);
                    found = true;
                    _lastIndex = i;
                    break;
                }
            }
        }
        if (!found) {
            _list.addLast(record);
        }
    }

    /**
     * Usually used only during conflation
     * @param index
     * @param record
     */
    public void add(int index, OutputRecord record) {
        _list.add(index, record);
    }

    public void addLast(String format, Object... args) {
        addLast(new OutputRecord(null, null, null, format, args));
    }

    public void addLast(OutputRecord record) {
        Date endDate = new Date(Long.MAX_VALUE);
        record.setLogDate(endDate);
        _list.addLast(record);
    }

    public void iterate(OutputRecordSetIterator iterator) {
        _iteratorIndex = 0;
        while(_iteratorIndex < _list.size()) {
            iterator.next(_iteratorIndex, _list.get(_iteratorIndex));
            _iteratorIndex++;
        }
    }

    public void remove(int idx) {
        _list.remove(idx);
        _iteratorIndex--;
    }

    public void remove(int startIdx, int endIdx) {
        for(int i = startIdx; i <= endIdx; i++) {
            _list.remove(startIdx);
            _iteratorIndex--;
        }
    }

    public int getSize() { return _list.size(); }
}
