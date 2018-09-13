package app;


import app.util.FileUtil;
import bio.domain.Expasy;
import bio.domain.Fasta;
import bio.domain.Organism;
import com.bioinfo.http.ExpasyRequest;
import com.bioinfo.http.KEGGApiRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

@SpringBootApplication
@PropertySource("application.properties")
public class BioModulePipeline implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BioModulePipeline.class);
    private boolean help = false;
    private String filesPath = "";
    private String organismName = "";

    public static void main(String args[]) {
        SpringApplication.run(BioModulePipeline.class, args);
    }

    private boolean argumentsValidation(String... args){

        List<Fasta> list = new FileUtil().getFastaDataFromFile("/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/Parsed/out/EC_1.1.1.1.merged.txt", " ");
        new KEGGApiRequest().getKeggApiInfo("edi:EDI_149100");

        if (args == null || args.length == 0){
            System.out.println("Please inform the arguments. For more explanation, put -help argument.");
        }else {
            for (int l = 0; l < args.length; l++) {
                String argument = args[l];

                switch (argument) {
                    case "-o":
                        organismName = args[l + 1];
                        break;
                    case "-fp":
                        filesPath = args[l + 1];
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
                System.out.println("-fp: File path with all the filenames to analise");
                System.out.println("-help: Show the arguments");
            } else if (filesPath == null && filesPath == null) {
                System.out.println("Please inform the files path to analise.");
            } else if (organismName == null){
                System.out.println("Please inform the organism name");
            } else {
                return true;
            }
        }
        return false;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Validating ec-mapper-0.1.0 arguments...");

        if (argumentsValidation(args) && !"".equals(filesPath)){
            organismAnalises(organismName, filesPath);

        }
    }

    public static String organismAnalises(String organismName, String pathToRead) {

        try {

//            String pathToRead = "/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/Parsed";

            Organism organism = new Organism();
            organism.setName(organismName);

            Object[] files = Files.list(Paths.get(pathToRead))
                    .sorted(Comparator.naturalOrder()).toArray();

            String actualEC = "";
            Stream<String> resultingStream = Stream.empty();

            for (int i = 0; i < files.length; i++) {
                Path file = ((Path)files[i]);
                String fileName = file.getFileName().toString();
                if (fileName.contains("EC")){

                    String fileECName = fileName.substring(fileName.indexOf("EC"), fileName.indexOf(".merged"));
                    //start getFastaFromFile
                    List<Fasta> fastaList = new FileUtil().getFastaDataFromFile(fileName, " ");

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "Ops! There is a problem: " + e.getMessage();
        }

        return "All done";

    }


}