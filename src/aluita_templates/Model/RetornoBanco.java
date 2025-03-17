package aluita_templates.Model;

import java.io.File;
import aluita_templates.Aluita_Templates;
import fileManager.FileManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.ini4j.Profile.Section;

import TemplateContabil.Model.Entity.LctoTemplate;

public class RetornoBanco {

    private File pasta;
    private String nomeBanco;
    private List<LctoTemplate> lctos = new ArrayList<>();
    private List<LctoTemplate> juros = new ArrayList<>();

    public RetornoBanco(File pasta, String nomeBanco) {
        this.pasta = pasta;
        this.nomeBanco = nomeBanco;

        getLctosFromFiles();
        salvarJuros();
    }

    public List<LctoTemplate> getLctos() {
        return lctos;
    }

    private void getLctosFromFiles() {
        //Lista arquivos TXT da pasta
        File[] arquivos = pasta.listFiles((pasta, name) -> name.toLowerCase().endsWith(".txt"));

        for (File arquivo : arquivos) {
            //Para cada um, adiciona seus lctos
            adicionarRetornos(arquivo);
        }
    }

    /**
     * Adiciona os lctos de retorno do arquivo especificado
     */
    private void adicionarRetornos(File arquivo) {
        //Verifica arquivo
        String pathArquivo = arquivo.getAbsolutePath();
        boolean isArquivoSafra = pathArquivo.toLowerCase().contains("safra");

        String textoArquivo = FileManager.getText(pathArquivo);
        String[] linhas = textoArquivo.split("\r\n");

        String prefixo = Aluita_Templates.ini.get("retornos", "prefixo");
        String prefixo_juros = Aluita_Templates.ini.get("retornos", "prefixo_juros");

        //Se tiver no minimo 15 linhas ( baseado em headers e footers)
        if (linhas.length > 15) {
            //Se a linha da data tiver no minimo 29 caracteres
            if (linhas[3].length() > 28) {
                int lenghtLinhaNumeroArquivo = linhas[2].length();
                String numero_do_arquivo = linhas[2].substring(19, lenghtLinhaNumeroArquivo).trim();

                String data = linhas[3].substring(19, 29).trim();
                String regexDate = "\\d{2}\\/\\d{2}\\/\\d{4}";
                String regexDate2 = "\\d{2}\\/\\d{2}\\/\\d{2}";                

                if (data.matches(regexDate)) {
                    if (isArquivoSafra) {
                        data = diaUtilAnterior(data);
                    } else {
                        data = proximoDiaUtil(data);
                    }

                    //Percorre linhas
                    for (int i = 10; i < linhas.length; i++) {
                        String linha = linhas[i];

                        //Se linha tiver tamanho os suficiente
                        if (linha.length() == 80) {
                            try {
                                String doc = linha.substring(0, 10).trim();
                                String cliente = linha.substring(11, 34).trim();
                                String vecto = linha.substring(34, 42).trim();
                                //BigDecimal valorDoc = new BigDecimal(linha.substring(42, 52).trim());
                                BigDecimal valor = new BigDecimal(linha.substring(62, 74).trim().replace(",", "."));
                                BigDecimal valorJuros = new BigDecimal(linha.substring(52, 62).trim().replace(",", "."));
                                Integer m1 = Integer.valueOf(linha.substring(75, 77).trim());

                                boolean isM1Valido = m1 == 17;
                                boolean isValorValido = valor.compareTo(BigDecimal.ZERO) != 0;
                                //boolean isVectoValido = vecto.isEmpty() | vecto.matches(regexDate2);
                                if (isM1Valido && isValorValido) {
                                    String historico = cliente + (vecto.isEmpty() ? "" : " - vecto " + vecto);

                                    lctos.add(new LctoTemplate(data, doc, prefixo, historico, valor));

                                    //Adiciona juros se tiver
                                    if (valorJuros.compareTo(BigDecimal.ZERO) != 0) {
                                        juros.add(new LctoTemplate(data, doc, prefixo_juros, historico, valorJuros));
                                    }
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
        }
    }

    private void salvarJuros() {
        Section retornos = Aluita_Templates.ini.get("retornos");


        File arquivoJuros = new File(pasta.getParentFile().getAbsolutePath() + "\\Retorno JUROS " + nomeBanco + ".csv");
        StringBuilder textoCsvJuros = new StringBuilder();

        //Cria cabeçalho
        textoCsvJuros.append("#empresa;filial;data;deb;cred;hist padrao;complemento;valor");

        for (LctoTemplate juro : juros) {
            textoCsvJuros.append("\r\n");
            textoCsvJuros.append(Aluita_Templates.ini.get("enterprise", "code"));
            textoCsvJuros.append(";");
            textoCsvJuros.append("");
            textoCsvJuros.append(";");
            textoCsvJuros.append(juro.getData());
            textoCsvJuros.append(";");
            textoCsvJuros.append(retornos.get("juros_debito"));//deb
            textoCsvJuros.append(";");
            textoCsvJuros.append(retornos.get("juros_credito"));//cred
            textoCsvJuros.append(";");
            textoCsvJuros.append(retornos.get("juros_hitorico_padrao"));//hist p
            textoCsvJuros.append(";");

            textoCsvJuros.append(juro.getHistorico());

            textoCsvJuros.append(";");
            textoCsvJuros.append(juro.getValor());
        }

        FileManager.save(arquivoJuros.getAbsolutePath(), textoCsvJuros.toString());
    }

    private final String[] feriados = {
        "1/1",
        "2/2",
        "12/2",
        "13/2",
        "14/2",
        "29/3",
        "31/3",
        "21/4",
        "1/5",
        "30/5",
        "7/9",
        "20/9",
        "12/10",
        "2/11",
        "15/11",
        "20/11",
        "25/12"
    };

    private boolean isFeriado(String date) {
        for (String feriado : feriados) {
            if (feriado.equals(date)) {
                return true;
            }
        }
        return false;
    }
    

    private String proximoDiaUtil(String date) {
        try {
            String[] dateParts = date.split("/");
            int dia = Integer.valueOf(dateParts[0]);
            int mes = Integer.valueOf(dateParts[1]);
            int ano = Integer.valueOf(dateParts[2]);
            ano = ano < 100 ? 2000 + ano : ano;

            Calendar dateCal = Calendar.getInstance();
            dateCal.set(ano, mes - 1, dia);
            dateCal.add(Calendar.DAY_OF_MONTH, 1);


            boolean isFinalDeSemana = dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;            
            boolean isFeriado = isFeriado(dateCal.get(Calendar.DAY_OF_MONTH) + "/" + (dateCal.get(Calendar.MONTH) + 1));

            while (isFinalDeSemana || isFeriado) {          
                dateCal.add(Calendar.DAY_OF_MONTH, 1);

                isFinalDeSemana = dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
                isFeriado = isFeriado(dateCal.get(Calendar.DAY_OF_MONTH) + "/" + (dateCal.get(Calendar.MONTH) + 1));  
            }

            return dateCal.get(Calendar.DAY_OF_MONTH) + "/" + (dateCal.get(Calendar.MONTH) + 1) + "/" + dateCal.get(Calendar.YEAR);
        } catch (Exception e) {
            return "01/01/1900";
        }

    }

    private String diaUtilAnterior(String date){
        try {
            String[] dateParts = date.split("/");
            int dia = Integer.valueOf(dateParts[0]);
            int mes = Integer.valueOf(dateParts[1]);
            int ano = Integer.valueOf(dateParts[2]);
            ano = ano < 100 ? 2000 + ano : ano;

            Calendar dateCal = Calendar.getInstance();
            dateCal.set(ano, mes - 1, dia);
            dateCal.add(Calendar.DAY_OF_MONTH, -1);

            boolean isFinalDeSemana = dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
            boolean isFeriado = isFeriado(dateCal.get(Calendar.DAY_OF_MONTH) + "/" + (dateCal.get(Calendar.MONTH) + 1));

            while (isFinalDeSemana || isFeriado) {
                dateCal.add(Calendar.DAY_OF_MONTH, -1);

                isFinalDeSemana = dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || dateCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
                isFeriado = isFeriado(dateCal.get(Calendar.DAY_OF_MONTH) + "/" + (dateCal.get(Calendar.MONTH) + 1));
            }

            return dateCal.get(Calendar.DAY_OF_MONTH) + "/" + (dateCal.get(Calendar.MONTH) + 1) + "/" + dateCal.get(Calendar.YEAR);
        } catch (Exception e) {
            return "01/01/1900";
        }
    }

}
