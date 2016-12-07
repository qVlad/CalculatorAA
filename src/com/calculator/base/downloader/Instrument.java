package com.calculator.base.downloader;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

public class Instrument {

    public enum Type {
        ETF,
        FUND,
        INDEX
    }

    public final static Date BEGINNING;
    static {
        Calendar _cal = Calendar.getInstance();
        _cal.set(1900, 0, 1);
        BEGINNING = _cal.getTime();
    }

    private final String ticker;
    private final String fullName;
    private final Type type;
    private final Date fromDate;
    private final Date toDate;
    private final List<InstrumentHistory> history;

    public Instrument(String t, String n, Type y, Date f, Date o) {
        ticker = t;
        fullName = n;
        type = y;
        fromDate = f;
        toDate = o;
        history = new LinkedList<>();
    }

    public String getTicker() {
        return ticker;
    }

    public void download(DataDownloader processor, Consumer<Boolean> after) {
        System.out.print("Downloading ");
        System.out.print(ticker);
        System.out.print(" (");
        System.out.print(fullName);
        System.out.print(")... ");

        processor.init(initDone -> {
            if (initDone) {
                processor.download(this, (downloadDone, s) -> {
                    if (downloadDone) {
                        ReaderCSV reader = new ReaderCSV("\"", ",", s);
                        reader.read()
                                .body()
                                .lines()
                                .forEach(line -> {
                                    history.add(processor.parseLine(line));
                                });

                        history.sort(InstrumentHistory::compareTo);
                        System.out.println("Downloaded " + reader.toList().size() + " lines");

                        fromDate.setTime(history.get(0).getDate().getTime());
                        toDate.setTime(history.get(history.size() - 1).getDate().getTime());
                    }
                    after.accept(downloadDone);
                });
            } else {
                after.accept(false);
            }
        });
    }

    public static void writeHead(PrintWriter os) {
        os.print("\"Ticker\"");
        os.print(";");
        os.print("\"Full name\"");
        os.print(";");
        os.print("\"Type\"");
        os.print(";");
        os.print("\"From date\"");
        os.print(";");
        os.println("\"To date\"");
    }

    public void writeMeta(PrintWriter os) {
        os.print("\"");
        os.print(ticker);
        os.print("\";\"");
        os.print(fullName);
        os.print("\";\"");
        os.print(type.toString());
        os.print("\";\"");
        os.print(printDate(fromDate));
        os.print("\";\"");
        os.print(printDate(toDate));
        os.println("\"");
    }

    public void write(PrintWriter os) {
        history.forEach(instr -> instr.write(os));
    }

    private String printDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("%04d-%02d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
    }
}
