package me.kptmusztarda.lights;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Logger {

    private static File file;
    private static boolean enabled = false;

    protected static void log(String tag, String string) {
        Log.i(tag, string);
        appendToFile(format(tag, string));
    }

    protected static void log(String tag, Exception e) {
        e.printStackTrace();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        appendToFile(format(tag, sw.toString()));
    }

    private static String format(String tag, String str) {
        return String.format("%s [%s]: %s",
                new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Calendar.getInstance(Locale.getDefault()).getTime()),
                tag,
                str);
    }

    protected static void space() {
        if(file.length() > 0) {
            for (int i = 0; i < 3; i++) {
                appendToFile(format("", ""));
            }
        }
    }

    private static void appendToFile(String string){
        try {
            FileOutputStream f = new FileOutputStream(file, true);
            PrintWriter pw = new PrintWriter(f);
            pw.println(string);
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i("MEDIA", "File not found. No permissions?");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static void enableLogging(boolean b) {
        enabled = b;
    }

    protected static void setDirectory(String path, String name) {
        File root = android.os.Environment.getExternalStorageDirectory();
        File dir = new File(root.getAbsolutePath());
        dir.mkdirs();
        file = new File(root + path, name);
    }

}