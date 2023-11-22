package cpw.mods.fml.common;

/*
 * Copied from ConsoleLogFormatter for shared use on the client
 */

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class FMLLogFormatter extends Formatter {
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String format(LogRecord record) {
        StringBuilder msg = new StringBuilder();
        msg.append(this.dateFormat.format(record.getMillis()));
        Level lvl = record.getLevel();

        if (lvl == Level.FINEST) {
            msg.append(" [FINEST] ");
        } else if (lvl == Level.FINER) {
            msg.append(" [FINER] ");
        } else if (lvl == Level.FINE) {
            msg.append(" [FINE] ");
        } else if (lvl == Level.INFO) {
            msg.append(" [INFO] ");
        } else if (lvl == Level.WARNING) {
            msg.append(" [WARNING] ");
        } else if (lvl == Level.SEVERE) {
            msg.append(" [SEVERE] ");
        } else {
            msg.append(" [").append(lvl.getLocalizedName()).append("] ");
        }

        msg.append(record.getMessage());
        msg.append('\n');
        Throwable thr = record.getThrown();

        if (thr != null) {
            StringWriter thrDump = new StringWriter();
            thr.printStackTrace(new PrintWriter(thrDump));
            msg.append(thrDump);
        }

        return msg.toString();
    }
}
