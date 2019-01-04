import java.io.IOException;
import java.util.HashMap;

import com.google.gson.Gson;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.column.page.PageReadStore;
import org.apache.parquet.example.data.Group;
import org.apache.parquet.example.data.simple.convert.GroupRecordConverter;
import org.apache.parquet.format.converter.ParquetMetadataConverter;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.io.ColumnIOFactory;
import org.apache.parquet.io.MessageColumnIO;
import org.apache.parquet.io.RecordReader;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

public class Main {

    private static Path path = new Path("apc-parquet.parquet");

    private static void printGroup(Group g) throws Exception{
        Gson gson=new Gson();
        String jsonOutput="";
        HashMap<String,String> jsonObject=new HashMap<>();

        PublisherExample publisherExample=new PublisherExample();
        int fieldCount = g.getType().getFieldCount();
        for (int field = 0; field < fieldCount; field++) {
            int valueCount = g.getFieldRepetitionCount(field);

            Type fieldType = g.getType().getType(field);
            String fieldName = fieldType.getName();

            for (int index = 0; index < valueCount; index++) {
                if (fieldType.isPrimitive()) {
                    System.out.println(fieldName + " " + g.getValueToString(field, index));
                    jsonObject.put(fieldName,g.getValueToString(field, index));
                }
            }
        }
        jsonOutput=gson.toJson(jsonObject);
        System.out.println(jsonOutput);
        publisherExample.publishMessages(jsonOutput);
        System.out.println("");
    }

    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();

        try {
            ParquetMetadata readFooter = ParquetFileReader.readFooter(conf, path, ParquetMetadataConverter.NO_FILTER);
            MessageType schema = readFooter.getFileMetaData().getSchema();
            ParquetFileReader r = new ParquetFileReader(conf, path, readFooter);

            PageReadStore pages = null;
            try {
                while (null != (pages = r.readNextRowGroup())) {
                    final long rows = pages.getRowCount();
                    System.out.println("Number of rows: " + rows);

                    final MessageColumnIO columnIO = new ColumnIOFactory().getColumnIO(schema);
                    final RecordReader recordReader = columnIO.getRecordReader(pages, new GroupRecordConverter(schema));
                    for (int i = 0; i < rows; i++) {
                        final Group g = (Group) recordReader.read();
                        printGroup(g);

                        // TODO Compare to System.out.println(g);
                    }
                }
            } finally {
                r.close();
            }
        } catch (IOException e) {
            System.out.println("Error reading parquet file.");
            e.printStackTrace();
        }
    }
}
