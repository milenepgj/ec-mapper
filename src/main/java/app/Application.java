package app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class Application implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private List<String> resultContaisInBoth = new ArrayList<String>();
    private List<String> resultOnlyInC = new ArrayList<String>();
    private List<String> resultOnlyInF = new ArrayList<String>();
    private String fileF = null;
    private String fileC = null;
    private String pattern = null;
    private String outputPath = null;
    private List<String> listF = null;
    private List<String> listC = null;
    private String fileName;

    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {

        log.info("Validating ec-mapper arguments...");

        for(int l=0; l<args.length;l++) {
            String argument = args[l];

            switch (argument){
                case "-f":
                    fileF = args[l+1];
                    break;
                case "-c":
                    fileC = args[l+1];
                    break;
                case "-p":
                    pattern = args[l+1];
                    break;
                case "-o":
                    outputPath = args[l+1];
                    break;
            }
        }

        log.info("fileF: " + fileF);
        log.info("fileC: " + fileC);
        log.info("pattern: " + pattern);

        if (pattern == null){
            System.out.println("Please inform the pattern of comparation (put the -p argument).");
        }else if (fileF == null) {
            System.out.println("Please inform the first file to compare (put the -f argument).");
        }else if (fileC == null) {
            System.out.println("Please inform a file to compare (put the -c argument).");
        }else if (!pattern.equalsIgnoreCase("KASS") && !pattern.equalsIgnoreCase("AEPI") && !pattern.equalsIgnoreCase("KAAN")) {
            System.out.println("Please inform a valid pattern to compare.");
        }else {

            log.info("Starting ec-mapper process...");

            fileName = "ec-mapper_" + pattern + "-Result.txt";

            if (pattern.equalsIgnoreCase("KASS")) {

                pattern = "[EC:";

                log.info(">>>> Creating array of file -f");

                listF = createList(fileF, pattern, true);

                log.info(">>>> Array of file -f created");

                log.info(">>>> Creating array of file -f");

                listC = createList(fileC, pattern, true);

                log.info(">>>> Array of file -c created");

                log.info(">>>> Comparing files...");

            } else if (pattern.equalsIgnoreCase("AEPI")) {

                log.info(">>>> Creating array of file -f");

                listF = createList(fileF, pattern, false);

                log.info(">>>> Array of file -f created");

                log.info(">>>> Creating array of file -f");

                listC = createList(fileC, pattern, false);

                log.info(">>>> Array of file -c created");

                log.info(">>>> Comparing files...");

            } else if (pattern.equalsIgnoreCase("KAAN")) {

                log.info(">>>> Creating array of file -f");

                pattern = "[EC:";

                listF = createList(fileF, pattern, true);

                log.info(">>>> Array of file -f created");

                log.info(">>>> Creating array of file -f");

                listC = createList(fileC, pattern, false);

                log.info(">>>> Array of file -c created");

                log.info(">>>> Comparing files...");

            }

            if ((listF.size() > 0) && (listC.size() > 0)) {

                resultOnlyInC.addAll(listC);
                resultOnlyInF.addAll(listF);

                for (String keyF : listF) {
                    if (listC.contains(keyF)) {
                        resultContaisInBoth.add(keyF);
                        resultOnlyInC.remove(keyF);
                        resultOnlyInF.remove(keyF);
                    }
                }

            } else if (listF.size() == 0) {
                System.out.println(">>>>> Result: File -f do not contains EC numbers");
            } else if (listC.size() == 0) {
                System.out.println(">>>>> Result: File -c do not contains EC numbers");
            }

            printFiles(fileName);
        }

        if (resultContaisInBoth.size()>0) {
            log.info("Ec-mapper process finished! See the file '" + fileName + "' created.");
        }else{
            log.info("Ec-mapper process finished! See the file '" + fileName + "' created.");
        }

    }

    private List<String> createList(String file, String pattern, boolean withPattern){
        List<String> list = new ArrayList<String>();
        try {
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                for(String line; (line = br.readLine()) != null; ) {
                    if (line.contains(pattern) && withPattern){
                        //log.info(line);
                        int start = line.indexOf(pattern)+4;
                        int end = line.lastIndexOf("]");
                        String data = line.substring(start,end);

                        if (data.contains(" ")){
                            data = data.replaceAll("EC:","");
                            String[] breaking = data.split(" ");
                            for (String ecNumber : breaking) {
                                if (!list.contains(ecNumber)) list.add(ecNumber);
                            }
                        }else {
                            if (!list.contains(data)) list.add(data);
                        }
                    }else if (!withPattern){
                        if (!list.contains(line)) list.add(line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private void printFiles(String fileName){
        try {

            if (outputPath != null){
                fileName = outputPath + File.pathSeparator + fileName;
            }

            BufferedWriter writer = null;

            if (resultContaisInBoth.size()>0){

                writer = new BufferedWriter (new FileWriter(fileName, false));

                writer.append("EC numbers found in both files:");
                writer.newLine();
                writer.newLine();
                for(String str: resultContaisInBoth) {
                    writer.append(str);
                    writer.newLine();
                }

                if (resultOnlyInF.size() > 0) {
                    writer.append("EC numbers found only in file: " + fileF);
                    writer.newLine();
                    writer.newLine();
                    for (String str : resultOnlyInF) {
                        writer.append(str);
                        writer.newLine();
                    }
                }

                if (resultOnlyInC.size() > 0) {
                    writer.append("EC numbers found only in file: " + fileC);
                    writer.newLine();
                    writer.newLine();
                    for (String str : resultOnlyInC) {
                        writer.append(str);
                        writer.newLine();
                    }
                }
            }

            if (writer != null) writer.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}