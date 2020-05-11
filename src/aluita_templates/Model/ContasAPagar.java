package aluita_templates.Model;

import Auxiliar.LctoTemplate;
import Auxiliar.Valor;
import java.io.File;
import java.io.FileInputStream;
import JExcel.JExcel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ContasAPagar {

    /*Cfg data*/
    private String dataAtual = "";
    private final String regexDate = "\\d{1,2}\\/\\d{1,2}\\/\\d{4}";

    /*cfg execução*/
    private final List<LctoTemplate> lctos = new ArrayList<>();
    private final File localArquivos;
    private final String filtro;
    private final String filtroArquivos = ""; // Anteriormente era 'contas a pagar' e o filtro recebido era o nome da filial

    /*cfg lançamentos*/
    private final String prefixoCompl = "Pgto. Dupl. nro ";
    private final int col_Doc = 0;
    private final int col_Data = 5;
    private final int col_Fornecedor = 6;
    private final int col_Banco = 8;
    private final int col_Valor = 12;

    public ContasAPagar(File localArquivos) {
        this.localArquivos = localArquivos;
        this.filtro = "";

        if (localArquivos.exists()) {
            montarListaLctos();
        }
    }

    public ContasAPagar(File localArquivos, String filtro) {
        this.localArquivos = localArquivos;
        this.filtro = filtroArquivos + filtro;

        if (localArquivos.exists()) {
            montarListaLctos();
        }
    }

    private void montarListaLctos() {
        //Listar arquivos de contas a pagar
        File[] arquivos = localArquivos.listFiles((localArquivos, name) -> name.toLowerCase().contains(filtro.toLowerCase()));

        //Percorrer arquivos
        for (File arquivo : arquivos) {
            if (arquivo.getName().endsWith(".xls")) {
                montarListaLctosArquivo(arquivo);
            }
        }
    }

    private void montarListaLctosArquivo(File arquivo) {
        try {
            //Abre workbook
            HSSFWorkbook wk = new HSSFWorkbook(new FileInputStream(arquivo));

            //Percorre sheets
            for (int i = 0; i < wk.getNumberOfSheets(); i++) {
                Sheet sheet = wk.getSheetAt(i);

                //----Percorre linhas
                for (int j = 0; j < sheet.getLastRowNum(); j++) {
                    try {
                        Row row = sheet.getRow(j);

                        //Atualiza data atual
                        String celA = JExcel.getStringCell(row.getCell(0));
                        if (celA.toLowerCase().contains("liquida") && celA.contains(":")) {
                            dataAtual = new Valor(celA).getNumbersList("/").get(0);
                        }

                        if (row.getLastCellNum() >= col_Valor + 1) {

                            String doc = JExcel.getStringCell(row.getCell(col_Doc)).trim(); //-------- A = doc
                            String data = JExcel.getStringDate(
                                    new Valor(
                                            String.valueOf(
                                                    row.getCell(col_Data).getNumericCellValue()
                                            ).replaceAll("\\.0", "")
                                    ).getInteger()
                            );//-------- G = data
                            String fornecedor = row.getCell(col_Fornecedor).getStringCellValue().replaceAll("[0-9;.,-]", "").trim();//-------- H = fornecedor
                            String banco = row.getCell(col_Banco).getStringCellValue().trim();//-------- J = Banco (PAGFOR)
                            Valor valor = new Valor(String.valueOf(row.getCell(col_Valor).getNumericCellValue()));//-------- P = valor
                            valor = new Valor(valor.getBigDecimal().multiply(new BigDecimal("-1")).toString());

                            if (!doc.equals("")
                                    && data.matches(regexDate)
                                    && dataAtual.matches(regexDate)
                                    && !fornecedor.equals("")
                                    //&& banco.equals("PAGFOR")
                                    && valor.getBigDecimal().compareTo(BigDecimal.ZERO) != 0) {
                                lctos.add(new LctoTemplate(dataAtual, doc, prefixoCompl, fornecedor, valor));
                            }
                        }
                    } catch (Exception e) {
                    }

                }
            }

            wk.close();

        } catch (Exception e) {
            System.out.println("Erro ao montar lista do arquivo: " + arquivo.getAbsolutePath());
        }
    }

    public List<LctoTemplate> getLctos() {
        return lctos;
    }

}
