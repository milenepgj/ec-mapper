# ec-mapper
A process to compare data from Kass and AnEnPi annotations platform

# Requisitos:
Java 8 (JDK) Instalado 

Veja as instruções:
https://docs.oracle.com/javase/8/docs/technotes/guides/install/install_overview.html

# Instalação:

Basta fazer download do arquivo ec-mapper[version].jar que está na pasta "versions" para a pasta onde deseja instalar o projeto em sua máquina

# Exemplos de execução via linha de comando:

Dica: Execute esse comando na pasta onde está o arquivo ec-mapper[version].jar para simplificar a linha de comando
 
:: Para comparar os resultados do Kass

java -jar ec-mapper-[version].jar -f c:\ec-mapper\files\files\Brugia_Kaas -c c:\ec-mapper\files\Necator_Kaas -p KASS

:: Para comparar os resultados do AnEnPi

java -jar ec-mapper-[version].jar -f c:\ec-mapper\files\listofECbmy.20.txt -c C:\ec-mapper\files\listofECnea.20.txt -p AEPI

:: Para comparar os resultados do Kass x AnEnPi, informando a pasta onde será salvo o arquivo de resultado

java -jar ec-mapper-[version].jar -f C:\files\Brugia_Kaas -c C:\files\listofECbmy.20.txt -p KAAN  -o C:\data

Nota: argumento -f nesse caso é o arquivo Kass e o -c é o arquivo AnEnPi

# Exemplo de um dos arquivos de resultado
ec-mapper_KASS-Result.txt
