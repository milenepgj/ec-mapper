package app;


import app.util.FileUtil;
import bio.domain.*;
import com.bioinfo.http.KEGGApiRequest;
import com.bioinfo.http.KEGGRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;
import util.UtilMethods;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@SpringBootApplication
@PropertySource("application.properties")
public class BioModulePipeline implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BioModulePipeline.class);
    private boolean help = false;
    private String filesPath = "";
    private String organismName = "";
    private String proteomFile = "";
    private boolean verbose = false;

    public static void main(String args[]) {
        SpringApplication.run(BioModulePipeline.class, args);
    }

    private boolean argumentsValidation(String... args){

        //List<Fasta> list = new FileUtil().getFastaDataFromFile("/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/Parsed/out/EC_1.1.1.1.merged.txt", " ");
        //new KEGGApiRequest().getKeggApiInfo("edi:EDI_149100");
        //new FileUtil().getProteinDataListFromProteomeFile("/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/A.CANTO_PRJEB493.WBPS3.protein_60.fa");

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
                    case "-fa":
                        proteomFile = args[l + 1];
                        break;
                    case "-v":
                        verbose=true;
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
                System.out.println("-fa: File with all organism's proteome data");
                System.out.println("-v: Print log messages during pipeline execution");
                System.out.println("-help: Show the arguments");
            } else if ((filesPath == null && filesPath == null) || (proteomFile == null && proteomFile == null)) {
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

/*            Organism o = new Organism();
            o.setName("TESTESTESTE");
            o.getProteinDataList().add(new ProteinData("PTN1", "HUIAHUIAHUIHAUIHAI"));
            o.getEcList().add(new EnzymeClass("7777"));
            o.getEcList().get(0).getModules().add(new KEGGModule("M1", true));
            o.getEcList().get(0).getModules().get(0).getKosFromEC().add("KOe1");
            o.getEcList().get(0).getModules().get(0).getKos().add("KOe1");
            printResultProteinData(o);
            printResultIdentificatedEnzimesModules(o);*/
            organismAnalises(organismName, filesPath, proteomFile);

        }
    }

    public String organismAnalises(String organismName, String pathToRead, String proteomFile) {

        try {

//            String pathToRead = "/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/Parsed";

            KEGGRequest keggRequest = new KEGGRequest();
            Organism organism = new Organism();
            organism.setName(organismName);

            Object[] files = Files.list(Paths.get(pathToRead))
                    .sorted(Comparator.naturalOrder()).toArray();

            String actualEC = "";
            Stream<String> resultingStream = Stream.empty();

            System.out.println("Getting EC List from parsed file");
            for (int i = 0; i < files.length; i++) {
                Path file = ((Path)files[i]);
                String fileName = file.toAbsolutePath().toString();
                if (fileName.contains("EC")){

                    String ecName = fileName.substring(fileName.indexOf("EC"), fileName.indexOf(".merged"));
                    //start getFastaFromFile
                    List<Fasta> fastaList = new FileUtil().getFastaDataFromFile(fileName, " ");
                    organism.getEcList().add(new EnzymeClass(ecName, fastaList));

                }
            }

            System.out.println("Finish :: Getting EC List from parsed file");

            //Aqui tenho o objeto de Organism com os dados do fasta.

            //Ir ao KEGG buscar os KOs
            System.out.println("Getting all Kegg data from KeggBlast entries by EC");

            for (int j = 0; j < organism.getEcList().size(); j++) {

                EnzymeClass ec = organism.getEcList().get(j);
                String kosEc = "";
                for (int e = 0; e < ec.getFastaList().size(); e++) {
                    String entryKeggBlast = ec.getFastaList().get(e).getEntryKeggBlastHit();

                    if (verbose) System.out.println(">> EC: " + ec.getEcNumber() + " | entryKeggBlast: " + entryKeggBlast);

                    if (entryKeggBlast != null && entryKeggBlast != ""){
                        KEGGData kd = new KEGGApiRequest().getKeggApiInfo(entryKeggBlast);

                        if (kd != null){
                            kd.setEc(ec.getEcNumber());
                            ec.getFastaList().get(e).setKeggData(kd);

                            if (kd.getOrthologyKo() != null){
                                if (kosEc != "")
                                    kosEc = kosEc + "\n" + kd.getOrthologyKo();
                                else
                                    kosEc = kd.getOrthologyKo();
                            }
                        }

                    }
                }

                System.out.println("Finish :: Getting all Kegg data from KeggBlast entries by EC");

                //Buscar módulos e verificar se são completos
                System.out.println("Getting all modules data from Kegg");
                if (kosEc != ""){
                    List<KEGGModule> modules = keggRequest.getModules(kosEc, verbose);
                    ec.setModules(modules);
                }
                System.out.println("Finish :: Getting all modules data from Kegg");
            }

            organism.setProteinDataList(new FileUtil().getProteinDataListFromProteomeFile(proteomFile));

            //Impressão do resultado
            System.out.println("Generating Protein Data result file");
            printResultProteinData(organism);
            System.out.println("Finish :: Starting to generate Protein Data result file");
            System.out.println("Starting to generate Identificated Enzimes Modules result file");
            printResultIdentificatedEnzimesModules(organism);
            System.out.println("Finish :: Generating Identificated Enzimes Modules result file");

        } catch (IOException e) {
            e.printStackTrace();
            return "Ops! There is a problem: " + e.getMessage();
        }

        return "All done";

    }

    private static void printResult(Organism organism){
        String fileName = organism.getName() +"_biomodulepipeline.out.txt";

        try {

            BufferedWriter writer = null;

            if (organism.getEcList().size()>0){

                writer = new BufferedWriter (new FileWriter(fileName, false));

                List<EnzymeClass> completeModulesList = new ArrayList<>();

                writer.append("ORGANISM:" + organism.getName() + "\n");
                writer.newLine();

                EnzymeClass ecComplete = new EnzymeClass();

                for (int i = 0; i < organism.getEcList().size(); i++) {

                    writer.append("-------------------------   PROTEIN DATA");
                    writer.newLine();

                    for (int j = 0; j < organism.getProteinDataList().size(); j++) {
                        ProteinData pd = organism.getProteinDataList().get(j);
                        writer.append("PROTEINID:" + pd.getProteinId() + "\n");
                        writer.append("AMINODATA:" + pd.getAminoacidData() + "\n");
                    }

                    writer.append("-------------------------   IDENTIFICATED ENZIMES");
                    writer.newLine();

                    EnzymeClass ec = organism.getEcList().get(i);
                    ecComplete = new EnzymeClass(ec.getEcNumber());

                    writer.append("EC " + ec.getEcNumber() + "\n");
                    writer.append(":: Organism modules:");
                    writer.newLine();

                    for (int m = 0; m < ec.getModules().size(); m++) {
                        writer.append("MNAME " + ec.getModules().get(m).getModule()+ "\n");
                        writer.append("::    Module's KOs:"+ "\n");
                        writer.append("MOKs " + ec.getModules().get(m).getKos().toString()+ "\n");
                        writer.append("::    EC's KOs:"+ "\n"); //sendKoList
                        writer.append("EKOs " + ec.getModules().get(m).getKosFromEC().toString()+ "\n"); //sendKoList
                        writer.append("COMPLETE " + ec.getModules().get(m).isComplete()+ "\n");
                        if (!ec.getModules().get(m).isComplete()){
                            //Se não estiver completo, mostra quais KOs pertencem apenas a modulo ou à lista de Kos do EC
                            writer.append("    - MKOSONLY " + new UtilMethods().getMinusList(ec.getModules().get(m).getKos(), ec.getModules().get(m).getKosFromEC()).toString()+ "\n");
                            writer.append("    - EKOSONLY " + new UtilMethods().getMinusList(ec.getModules().get(m).getKosFromEC(), ec.getModules().get(m).getKos()).toString()+ "\n");
                        }else{
                            ecComplete.getModules().add(ec.getModules().get(m));
                        }
                    }

                    if (ecComplete.getModules().size() > 0) {

                        completeModulesList.add(ecComplete);
                    }
                }

                writer.newLine();
                writer.append("-------------------------   IDENTIFICATED COMPLETE MODULES");
                writer.newLine();

                //Lista módulos completos
                for (int i = 0; i < completeModulesList.size() ; i++) {
                    EnzymeClass complete = completeModulesList.get(i);
                    writer.append("EC " + complete.getEcNumber()+ "\n");

                    for (int m = 0; m < complete.getModules().size(); m++) {
                        writer.append("COMPLETE MNAME " + complete.getModules().get(m).getModule()+ "\n");
                        writer.append("::    Module's KOs:"+ "\n");
                        writer.append("CMOKs " + complete.getModules().get(m).getKos().toString()+ "\n");
                        writer.append("::    EC's KOs:" + complete.getModules().get(m).getKosFromEC().toString()+ "\n"); //sendKoList
                        writer.append("CEKOs " + complete.getModules().get(m).getKosFromEC().toString()+ "\n"); //sendKoList
                    }

                }

            }

            if (writer != null) writer.close();

            System.out.println("Arquivo de resultados [" + fileName + "] gerado com sucesso");

        }catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static void printResultProteinData(Organism organism){

        String fileName = organism.getName() +"_proteinDataBMPipeline.out.txt";

        try {

            BufferedWriter writer = null;

            if (organism.getEcList().size()>0){

                writer = new BufferedWriter (new FileWriter(fileName, false));

                List<EnzymeClass> completeModulesList = new ArrayList<>();

                writer.append("ORGANISM:" + organism.getName() + "\n");
                writer.newLine();

                EnzymeClass ecComplete = new EnzymeClass();

                for (int i = 0; i < organism.getEcList().size(); i++) {

                    writer.append("-------------------------   PROTEIN DATA");
                    writer.newLine();

                    for (int j = 0; j < organism.getProteinDataList().size(); j++) {
                        ProteinData pd = organism.getProteinDataList().get(j);
                        writer.append("PROTEINID:" + pd.getProteinId() + "\n");
                        writer.append("AMINODATA:" + pd.getAminoacidData() + "\n");
                    }
                }

            }

            if (writer != null) writer.close();

            System.out.println("Arquivo de resultados [" + fileName + "] gerado com sucesso");

        }catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private static void printResultIdentificatedEnzimesModules(Organism organism){
        String fileName = organism.getName() +"_enzymeModulesBMPipeline.out.txt";

        try {

            BufferedWriter writer = null;

            if (organism.getEcList().size()>0){

                writer = new BufferedWriter (new FileWriter(fileName, false));

                List<EnzymeClass> completeModulesList = new ArrayList<>();

                writer.append("ORGANISM:" + organism.getName() + "\n");
                writer.newLine();

                EnzymeClass ecComplete = new EnzymeClass();

                for (int i = 0; i < organism.getEcList().size(); i++) {

                    writer.append("-------------------------   IDENTIFICATED ENZIMES");
                    writer.newLine();

                    EnzymeClass ec = organism.getEcList().get(i);
                    ecComplete = new EnzymeClass(ec.getEcNumber());

                    writer.append("EC " + ec.getEcNumber() + "\n");
                    writer.append(":: Organism modules:");
                    writer.newLine();

                    for (int m = 0; m < ec.getModules().size(); m++) {
                        writer.append("MNAME " + ec.getModules().get(m).getModule()+ "\n");
                        writer.append("::    Module's KOs:"+ "\n");
                        writer.append("MOKs " + ec.getModules().get(m).getKos().toString()+ "\n");
                        writer.append("::    EC's KOs:"+ "\n"); //sendKoList
                        writer.append("EKOs " + ec.getModules().get(m).getKosFromEC().toString()+ "\n"); //sendKoList
                        writer.append("COMPLETE " + ec.getModules().get(m).isComplete()+ "\n");
                        if (!ec.getModules().get(m).isComplete()){
                            //Se não estiver completo, mostra quais KOs pertencem apenas a modulo ou à lista de Kos do EC
                            writer.append("    - MKOSONLY " + new UtilMethods().getMinusList(ec.getModules().get(m).getKos(), ec.getModules().get(m).getKosFromEC()).toString()+ "\n");
                            writer.append("    - EKOSONLY " + new UtilMethods().getMinusList(ec.getModules().get(m).getKosFromEC(), ec.getModules().get(m).getKos()).toString()+ "\n");
                        }else{
                            ecComplete.getModules().add(ec.getModules().get(m));
                        }
                    }

                    if (ecComplete.getModules().size() > 0) {

                        completeModulesList.add(ecComplete);
                    }
                }

                writer.newLine();
                writer.append("-------------------------   IDENTIFICATED COMPLETE MODULES");
                writer.newLine();

                //Lista módulos completos
                for (int i = 0; i < completeModulesList.size() ; i++) {
                    EnzymeClass complete = completeModulesList.get(i);
                    writer.append("EC " + complete.getEcNumber()+ "\n");

                    for (int m = 0; m < complete.getModules().size(); m++) {
                        writer.append("COMPLETE MNAME " + complete.getModules().get(m).getModule()+ "\n");
                        writer.append("::    Module's KOs:"+ "\n");
                        writer.append("CMOKs " + complete.getModules().get(m).getKos().toString()+ "\n");
                        writer.append("::    EC's KOs:" + complete.getModules().get(m).getKosFromEC().toString()+ "\n"); //sendKoList
                        writer.append("CEKOs " + complete.getModules().get(m).getKosFromEC().toString()+ "\n"); //sendKoList
                    }

                }

            }

            if (writer != null) writer.close();

            System.out.println("Arquivo de resultados [" + fileName + "] gerado com sucesso");

        }catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}