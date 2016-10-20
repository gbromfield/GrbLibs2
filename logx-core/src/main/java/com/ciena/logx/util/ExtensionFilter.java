package com.ciena.logx.util;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by gbromfie on 10/7/16.
 */
public class ExtensionFilter implements FileFilter {
    private String _extension;

    public ExtensionFilter(String extension) {
        _extension = extension;
    }

    @Override
    public boolean accept(File pathname) {
        int extIndex = pathname.getName().lastIndexOf('.');
        if (extIndex == -1) {
            return false;
        }
        return _extension.equalsIgnoreCase(pathname.getName().substring(extIndex+1));
    }
}
