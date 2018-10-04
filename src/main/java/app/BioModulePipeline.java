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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            organismAnalises(organismName, filesPath, proteomFile);

        }
    }

    public static String organismAnalises(String organismName, String pathToRead, String proteomFile) {

        try {

//            String pathToRead = "/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/Parsed";

            KEGGRequest keggRequest = new KEGGRequest();
            Organism organism = new Organism();
            organism.setName(organismName);

            Object[] files = Files.list(Paths.get(pathToRead))
                    .sorted(Comparator.naturalOrder()).toArray();

            String actualEC = "";
            Stream<String> resultingStream = Stream.empty();

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

            //Aqui tenho o objeto de Organism com os dados do fasta.

            //Ir ao KEGG buscar os KOs
            for (int j = 0; j < organism.getEcList().size(); j++) {

                EnzymeClass ec = organism.getEcList().get(j);
                String kosEc = "";
                for (int e = 0; e < ec.getFastaList().size(); e++) {
                    String entryKeggBlast = ec.getFastaList().get(e).getEntryKeggBlastHit();

                    System.out.println(">> EC: " + ec.getEcNumber() + " | entryKeggBlast: " + entryKeggBlast);

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

                //Buscar módulos e verificar se são completos
                if (kosEc != ""){
                    List<KEGGModule> modules = keggRequest.getModules(kosEc);
                    ec.setModules(modules);
                }
            }

            organism.setProteinDataList(new FileUtil().getProteinDataListFromProteomeFile(proteomFile));

            System.out.println("::::: Organism name:" + organism.getName());
            System.out.println(":::: Organism modules:");
            for (int p = 0; p < organism.getEcList().size(); p++) {
                EnzymeClass ec = organism.getEcList().get(p);

                for (int m = 0; m < ec.getModules().size(); m++) {
                    System.out.println(":::Module name:" + ec.getModules().get(m).getModule());
                    System.out.println("Module's KOs:" + ec.getModules().get(m).getKos().toString());
                    System.out.println("EC's KOs:" + ec.getModules().get(m).getKosFromEC().toString()); //sendKoList
                    System.out.println("Is complete?" + ec.getModules().get(m).isComplete());
                    if (!ec.getModules().get(m).isComplete()){
                        //Se não estiver completo, mostra quais KOs pertencem apenas a modulo ou à lista de Kos do EC
                        System.out.println("   - KOs only on Module Kos' list:" + new UtilMethods().getMinusList(ec.getModules().get(m).getKos(), ec.getModules().get(m).getKosFromEC()).toString());
                        System.out.println("   - KOs only on EC Kos' list:" + new UtilMethods().getMinusList(ec.getModules().get(m).getKosFromEC(), ec.getModules().get(m).getKos()).toString());
                    }
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
            return "Ops! There is a problem: " + e.getMessage();
        }

        return "All done";

    }


}