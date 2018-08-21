package app.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtil {

    private static String PATH_OUT = "/out/";

    public static String mergeECFastaFiles(String pathToRead) {

        try {

//            String pathToRead = "/home/milene.guimaraes/Documents/Pessoal/amaranta/DADOS_AMAR_ARTIGO/ANENPI/dados_amaranta/Canto/Parsed";

            Object[] files = Files.list(Paths.get(pathToRead))
                    .sorted(Comparator.naturalOrder()).toArray();

            String actualEC = "";
            Stream<String> resultingStream = Stream.empty();

            for (int i = 0; i < files.length; i++) {
                Path path = ((Path)files[i]);
                String fileName = path.getFileName().toString();
                if (!fileName.contains(".-.pep") && fileName.contains("EC") && fileName.contains(".pep")){

                    String fileECName = fileName.substring(fileName.indexOf("EC"), fileName.indexOf(".pep"));
                    if (!actualEC.equals(fileECName) && actualEC != ""){

                        doMerge(pathToRead, actualEC, resultingStream);
                        //Reinicia buffer
                        resultingStream = Stream.empty();
                    }

                    actualEC = fileECName;

                    Stream<String> streamLines = Files.lines(path);

                    resultingStream = Stream.concat(resultingStream, streamLines);

                }
            }
            //Faz merge do último item
            doMerge(pathToRead, actualEC, resultingStream);

        } catch (IOException e) {
            e.printStackTrace();
            return "Ops! There is a problem: " + e.getMessage();
        }

        return "All merged files are on the path" + pathToRead + PATH_OUT;

    }

    private static void doMerge(String pathToRead, String actualEC, Stream<String> resultingStream) {
        //Cria o parsed do EC
        String lines = resultingStream
                .map(e -> e.toString())
                .collect(Collectors.joining("\n"));

        createFile(pathToRead, actualEC, lines);
    }

    public static void createFile(String filePath, String fileName, String lines) {
        try {
            String pathName = filePath + PATH_OUT + fileName + ".merged.txt";

            Path path = Paths.get(filePath + PATH_OUT);

            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }

            Files.write(Paths.get(pathName), Collections.singleton(lines));

            System.out.println("Merged file created to EC number " + fileName + " is: " + fileName + ".merged.txt");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
