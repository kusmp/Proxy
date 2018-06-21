import com.opencsv.CSVReader;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Logger {
    static void logValues(String host, long count, long bytesSent, long bytesReceived) throws IOException{
        String fileName = "log.csv";
        String hostName = "";
        List<Long> values = new ArrayList<Long>();
        HashMap<String, List<Long>> stats = new HashMap<String, List<Long>>();
        try (FileInputStream fis = new FileInputStream(fileName);
             InputStreamReader isr = new InputStreamReader(fis,
                     StandardCharsets.UTF_8);
             CSVReader reader = new CSVReader(isr)) {
            String[] nextLine;

            while ((nextLine = reader.readNext()) != null) {
                hostName = nextLine[0];
                long nex1 = Integer.parseInt(nextLine[1]);
                long nex2 = Integer.parseInt(nextLine[2]);
                long nex3 = Integer.parseInt(nextLine[3]);
                values.add(nex1);
                values.add(nex2);
                values.add(nex3);
                stats.put(hostName, values);
                values = new ArrayList<>();
            }

            if (stats.containsKey(host)) {
                List<Long> tempList = new ArrayList<Long>();
                tempList.add(stats.get(host).get(0) + count);
                tempList.add(stats.get(host).get(1) + bytesSent);
                tempList.add(stats.get(host).get(2) + bytesReceived);
                stats.put(host, tempList);
            } else {
                List<Long> tempList = new ArrayList<Long>();
                tempList.add(count);
                tempList.add(bytesSent);
                tempList.add(bytesReceived);
                stats.put(host, tempList);
            }
            String data = "";
            for (HashMap.Entry<String, List<Long>> entry : stats.entrySet()) {
                data += entry.getKey() + ",";
                data += stats.get(entry.getKey()).get(0) + ",";
                data += stats.get(entry.getKey()).get(1) + ",";
                data += stats.get(entry.getKey()).get(2) +"\n";
            }
            FileOutputStream output = new FileOutputStream("log.csv", false);
            IOUtils.write(data, output, "UTF-8");
        }
    }
}
