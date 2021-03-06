package app;

/**

 Created by Milene Pereira Guimarães de Jezuz on 03/08/2016.

 1) comparar a listas de anotação do resultado de anotação do kass vs kass (diferentes organismos) e ver os ECs compartilhados, ou ecs unicos (presente em apenas um dos organismos):

 anexo 3: Brugia_Kass_Anotação vs  anexo4: Necator_kass_anotacao.

 Por exemplo na lista Necator_kass_anotacao abaixo, a primeira linha aparece o  seguinte EC: [EC:3.1.3.3]

 nct_013290387.1	K01079	phosphoserine phosphatase [EC:3.1.3.3]

 Este EC também está na lista de Brugia?

 Depois, resultado usando outro pipeline, comparar resultado do AnEnPi: listofECbmy.20.txt vs listofECnea.20.txt

 Comparar  as listas para encontrar os ecs compartilhados (anexo1: listofECbmy.20.txt vs anexo2: listofECnea.20.txt) ou  ecs presente em apenas uma lista( organismo).

 Também precisamos comparar as duas listas

 Exemplo: Brugia_Kass_Anotação (metodo de kass)  vs listofECbmy.20.txt  (metodo do AnEnPi)
 Necator_kass_anotacao vs  listofECnea.20.txt

 Neste caso são dois pipelines de anotação diferentes que utilizamos. Gostaria de ver se os ecs que mapearam em um metodo também foram mapeados no outro metodo.

 */

import app.util.FileUtil;
import bio.domain.Expasy;
import com.bioinfo.http.ExpasyRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import java.io.*;
import java.util.*;

@SpringBootApplication
@PropertySource("application.properties")
public class Application implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Application.class);
    private List<String> resultContaisInBoth = new ArrayList<String>();
    private List<String> resultOnlyInC = new ArrayList<String>();
    private List<String> resultOnlyInF = new ArrayList<String>();
    private HashMap<String, Integer> resultBothWithTotal = new HashMap<String, Integer>();
    private String filePaths = "";
    private String fileF = null;
    private String fileC = null;
    private String pattern = null;
    private String outputPath = null;
    private List<String> listF = null;
    private List<String> listC = null;
    private String fileName = "";
    private boolean help = false;
    private boolean isOnlyECBoth = false;
    private boolean isPrintCsv = false;
    private boolean isGetECDescription = false;
    private boolean isGetUniProtAnnotation = false;
    private boolean isMergeFiles = false;

/*
    public static void main(String args[]) {
        SpringApplication.run(Application.class, args);
    }
*/

    private boolean argumentsValidation(String... args){

        if (args == null || args.length == 0){
            System.out.println("Please inform the arguments. For more explanation, put -help argument.");
        }else {
            for (int l = 0; l < args.length; l++) {
                String argument = args[l];

                switch (argument) {
                    case "-m":
                        filePaths = args[l + 1];
                        isMergeFiles = true;
                        break;
                    case "-fp":
                        filePaths = args[l + 1];
                        break;
                    case "-f":
                        fileF = args[l + 1];
                        break;
                    case "-c":
                        fileC = args[l + 1];
                        break;
                    case "-p":
                        pattern = args[l + 1];
                        break;
                    case "-o":
                        outputPath = args[l + 1];
                        break;
                    case "-fn":
                        fileName = args[l + 1];
                        break;
                    case "-oe":
                        isOnlyECBoth = true;
                        break;
                    case "-csv":
                        isPrintCsv = true;
                        break;
                    case "-enz":
                        isGetECDescription = true;
                        break;
                    case "-uniprot":
                        isGetUniProtAnnotation = true;
                        break;
                    case "-help":
                        help = true;
                        break;
                }
            }

            if (help) {
                System.out.println("\nec-mapper-0.5.0: A process to compare data from Kass and AnEnPi annotations platform\n");
                System.out.println("Example: java -jar ec-mapper-0.5.0.jar -f C:\\files\\Brugia_Kaas -c C:\\files\\listofECbmy.20.txt -p KAAN  -o C:\\data \n");
                System.out.println("Arguments:\n");
                System.out.println("-m: Path of EC files to merge");
                System.out.println("-f: File of Kass or AnEnPi result type");
                System.out.println("-c: File of Kass or AnEnPi result type");
                System.out.println("-o: Path to put the result file");
                System.out.println("-fn: File output name");
                System.out.println("-oe: Return only the ECs in both files");
                System.out.println("-fp: File with all the filenames to compare, available only to AnEnPi annotation result type");
                System.out.println("-p KASS: Compare two files of Kass annotation result type");
                System.out.println("-p AEPI: Compare two files of AnEnPi annotation result type");
                System.out.println("-p KAAN: Compare a Kass file (-f argument) and AnEnPi file (-c argument)");
                System.out.println("-csv: Create CSV file with all to all AnEnPi annotation results");
                System.out.println("-enz: Get Enzyme description on Expasy");
                System.out.println("-uniprot: Get protein data on UniProtxSwissProt");
                System.out.println("-help: Show the arguments");
            } else if (pattern == null && !isMergeFiles) {
                System.out.println("Please inform the pattern of comparation (put the -p argument).");
            } else if (fileF == null && filePaths == null) {
                System.out.println("Please inform the first file to compare (put the -f argument).");
            } else if (fileC == null && filePaths == null) {
                System.out.println("Please inform a file to compare (put the -c argument).");
            } else if (!isMergeFiles && !pattern.equalsIgnoreCase("KASS") && !pattern.equalsIgnoreCase("AEPI") && !pattern.equalsIgnoreCase("KAAN")) {
                System.out.println("Please inform a valid pattern to compare.");
            } else {
                return true;
            }
        }
        return false;
    }

    private String getFileName (String nameListaF, String nameListaC){
        fileF = getFileNameFromList(nameListaF);
        fileC = getFileNameFromList(nameListaC);
        String fileName = fileF + "X" + fileC;
        return fileName;
    }

    private String getFileNameFromList(String nameLista) {
        String[] firstName = nameLista.split("/");
        String fileName = firstName[firstName.length-1];
        String[] splitFileName = fileName.split("[.]");
        return splitFileName[0];
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Validating ec-mapper-0.1.0 arguments...");

        if (argumentsValidation(args) && isMergeFiles && !"".equals(filePaths)){

            String returnFunction = FileUtil.mergeECFastaFiles(filePaths);

            System.out.println(returnFunction);

        }else if (argumentsValidation(args) && !"".equals(filePaths)){

            compareAllAnEnPiFiles(isPrintCsv);

        }else if (argumentsValidation(args)) {

            System.out.println("Starting ec-mapper-0.1.0 process...");
            System.out.println("fileF: " + fileF);
            System.out.println("fileC: " + fileC);
            System.out.println("pattern: " + pattern);

            if (fileName.equals(""))
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

                callAEPI(fileF, fileC);

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

            compareFiles(listF, listC, fileName);
        }

    }

    private void compareAllAnEnPiFiles(boolean isPrintCsv) {
        List<String> list = createFileList(filePaths, "", false);

        for (int f = 0; f < list.size(); f++) {
            int indC = f+1;
            if (indC != f){
                for (int c = indC; c < list.size(); c++) {
                    restartObjects();
                    if (f!=c){
                        doAnEnPiComparedItens(list, f, c);
                    }
                }
            }
        }
        if (isPrintCsv)
            printCSV("resultAllAnEnPiFiles.csv");
    }

    private void printCSV(String fileName) {

        try {
            BufferedWriter writer = new BufferedWriter (new FileWriter(fileName, false));
            printResultOnCsv(writer);
            if (writer != null) writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doAnEnPiComparedItens(List<String> list, int f, int c) {
        isOnlyECBoth = false;
        callAEPI(list.get(f), list.get(c));
        String fileName = getFileName(list.get(f), list.get(c));
        compareFiles(listF, listC, fileName);
        fileName = "OnlyCommomECs_" + fileName;
        isOnlyECBoth = true;
        printFiles(fileName);
        resultBothWithTotal.put(fileName, resultContaisInBoth.size());
    }

    private void restartObjects() {
        listF = null;
        listC = null;
        resultContaisInBoth = new ArrayList<String>();
        resultOnlyInC = new ArrayList<String>();
        resultOnlyInF = new ArrayList<String>();
    }

    private void compareFiles(List<String> listF, List<String> listC, String fileName) {
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
            log.info(">>>>> Result: File -f do not contains EC numbers");
        } else if (listC.size() == 0) {
            log.info(">>>>> Result: File -c do not contains EC numbers");
        }

        fileName = printFiles(fileName);

        if (resultContaisInBoth.size()>0) {
            System.out.println("Ec-mapper process finished. See the file '" + fileName + "' created.");
        }else{
            System.out.println("Ec-mapper process finished. There was no similar data found at the files..");
        }
    }

    private void callAEPI(String fileF, String fileC) {
        log.info(">>>> Creating array of file -f");

        listF = createList(fileF, pattern, false);

        log.info(">>>> Array of file -f created");

        log.info(">>>> Creating array of file -f");

        listC = createList(fileC, pattern, false);

        log.info(">>>> Array of file -c created");

        log.info(">>>> Comparing files...");
    }

    private List<String> createFileList(String file, String pattern, boolean withPattern){
        List<String> list = new ArrayList<String>();
        try {
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                for(String line; (line = br.readLine()) != null; ) {
                    if (line.contains(pattern) && withPattern){
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
            log.error(e.getMessage());
        }
        return list;
    }

    private List<String> createList(String file, String pattern, boolean withPattern){
        List<String> list = new ArrayList<String>();
        try {
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {
                for(String line; (line = br.readLine()) != null; ) {
                    if (line.contains(pattern) && withPattern){
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
            log.error(e.getMessage());
        }
        return list;
    }

    private String printFiles(String fileName){
        try {

            if (outputPath != null){
                fileName = outputPath + File.separator + fileName;
            }

            BufferedWriter writer = null;

            if (resultContaisInBoth.size()>0){

                writer = new BufferedWriter (new FileWriter(fileName, false));

                if (isOnlyECBoth){

                    printResultContainsInBoth(writer, isGetECDescription, isGetUniProtAnnotation);

                }else {

                    writer.append("::::::::::::::::::::::::::::\n");
                    writer.append(":::: EC-MAPPER RESULTS  ::::\n");
                    writer.append("::::::::::::::::::::::::::::\n\n");
                    writer.append("1) EC numbers found in both files");
                    writer.append("\n\nTotal mapped EC: " + resultContaisInBoth.size());
                    writer.newLine();
                    writer.newLine();
                    printResultContainsInBoth(writer, isGetECDescription, isGetUniProtAnnotation);

                    if (resultOnlyInF.size() > 0) {
                        writer.append("\n\n2) EC numbers found only in the file " + fileF);
                        writer.append("\n\nTotal mapped EC: " + resultOnlyInF.size());
                        writer.newLine();
                        writer.newLine();
                        Collections.sort(resultOnlyInF);
                        for (String str : resultOnlyInF) {
                            writer.append(str);
                            writer.newLine();
                        }
                    }

                    if (resultOnlyInC.size() > 0) {
                        writer.append("\n\n3)EC numbers found only in the file " + fileC);
                        writer.append("\n\nTotal mapped EC: " + resultOnlyInC.size());
                        writer.newLine();
                        writer.newLine();
                        Collections.sort(resultOnlyInC);
                        for (String str : resultOnlyInC) {
                            writer.append(str);
                            writer.newLine();
                        }
                    }
                }
            }

            if (writer != null) writer.close();

        }catch (IOException e) {
            log.error(e.getMessage());
        }

        return fileName;
    }

    private void printResultContainsInBoth(BufferedWriter writer, boolean isGetECDescription, boolean isGetUniProtAnnotation) throws IOException {
        Collections.sort(resultContaisInBoth);
        for (String str : resultContaisInBoth) {
            writer.append(str);

            if (isGetECDescription){
                Expasy expasy = new ExpasyRequest().getExpasyECData(str, isGetUniProtAnnotation);
                if (expasy != null)
                    writer.append(expasy.getDescription());
            }
            writer.newLine();
        }
    }

    private void printResultOnCsv(BufferedWriter writer) throws IOException {
        Set<String> keys = resultBothWithTotal.keySet();
        writer.append("Filename" + ";" + "Compared Organisms" + ";" + "# Common ECs");
        writer.newLine();
        for (String key : keys){
            String keyToPrint = key.replaceAll("OnlyCommomECs_","").replaceAll("X", " x ").replaceAll("listofEC","").toUpperCase();
            if(key != null){
                writer.append(key + ";");
                writer.append(keyToPrint + ";");
                writer.append(resultBothWithTotal.get(key).toString());
                writer.newLine();
            }
        }
    }
}