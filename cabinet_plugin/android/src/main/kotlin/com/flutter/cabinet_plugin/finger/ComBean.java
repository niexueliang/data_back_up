package com.flutter.cabinet_plugin.finger;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 串口实体类
 */
public class ComBean {
    public byte[] bRec = null;
    public String sRecTime = "";
    public String sComPort = "";

    public ComBean(String sPort, byte[] buffer, int size) {
        sComPort = sPort;
        bRec = new byte[size];
        System.arraycopy(buffer, 0, bRec, 0, size);
        SimpleDateFormat sDateFormat = new SimpleDateFormat("hh:mm:ss", Locale.CHINA);
        sRecTime = sDateFormat.format(new java.util.Date());
    }
}