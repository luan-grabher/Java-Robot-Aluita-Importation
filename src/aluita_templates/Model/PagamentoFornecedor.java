package aluita_templates.Model;

import java.io.File;
import java.io.FileInputStream;
import JExcel.JExcel;
import TemplateContabil.Model.Entity.LctoTemplate;
import aluita_templates.Aluita_Templates;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.ini4j.Profile.Section;

public class PagamentoFornecedor {

    /*Cfg data*/
    private String dataAtual = "";
    private final String regexDate = "\\d{1,2}\\/\\d{1,2}\\/\\d{4}";

    /*cfg execução*/
    private final List<LctoTemplate> lctos = new ArrayList<>();
    private final File localArquivos;
    private final String filtro;
    private final String filtroArquivos = ""; // Anteriormente era 'contas a pagar' e o filtro recebido era o nome da filial

    /*cfg lançamentos*/
    private final Section pfor = Aluita_Templates.ini.get("pagamentos_fornecedor");
    private final String prefixoCompl = pfor.fetch("prefixo");
    private final int col_Doc = Integer.valueOf(pfor.fetch("col_Doc"));
    private final int col_Data = Integer.valueOf(pfor.fetch("col_Data"));
    private final int col_Fornecedor = Integer.valueOf(pfor.fetch("col_Fornecedor"));
    private final int col_Banco = Integer.valueOf(pfor.fetch("col_Banco"));
    private final int col_Valor = Integer.valueOf(pfor.fetch("col_Valor"));

    public PagamentoFornecedor(File localArquivos) {
        this.localArquivos = localArquivos;
        this.filtro = "";

        if (localArquivos.exists()) {
            montarListaLctos();
        }
    }

    public PagamentoFornecedor(File localArquivos, String filtro) {
        this.localArquivos = localArquivos;
        this.filtro = filtroArquivos + filtro;

        if (localArquivos.exists()) {
            montarListaLctos();
        }
    }

    /**
     * Monta a lista de lançamentos a partir dos arquivos de pagamentos
     * @return Void
     */
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
                            //var dataAtual = celA only numbers and . and / and : and - and  ,
                            dataAtual = celA.split(": ")[1];
                        }

                        if (row.getLastCellNum() >= col_Valor + 1) {

                            String doc = JExcel.getStringCell(row.getCell(col_Doc)).trim(); //-------- A = doc
                            String data = JExcel.getStringDate(
                                        Integer.valueOf(
                                            String.valueOf(
                                                    row.getCell(col_Data).getNumericCellValue()
                                            ).replaceAll("\\.0", "")
                                        )
                            );//-------- G = data
                            String fornecedor = row.getCell(col_Fornecedor).getStringCellValue().replaceAll("[0-9;.,-]", "").trim();//-------- H = fornecedor
                            //String banco = row.getCell(col_Banco).getStringCellValue().trim();//-------- J = Banco (PAGFOR)
                            BigDecimal valor = new BigDecimal(String.valueOf(row.getCell(col_Valor).getNumericCellValue()));//-------- P = valor
                            //multiplica por -1 para que o valor seja negativo
                            valor = valor.multiply(new BigDecimal(-1));

                            if (!doc.equals("")
                                    && data.matches(regexDate)
                                    && dataAtual.matches(regexDate)
                                    && !fornecedor.equals("")
                                    //&& banco.equals("PAGFOR")
                                    && valor.compareTo(BigDecimal.ZERO) != 0) {
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

    /**
     * Retorna a lista de lançamentos
     * @return List<LctoTemplate>
     */
    public List<LctoTemplate> getLctos() {
        return lctos;
    }

}
