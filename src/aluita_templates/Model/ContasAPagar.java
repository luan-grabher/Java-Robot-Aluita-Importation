package aluita_templates.Model;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import org.ini4j.Profile.Section;
import TemplateContabil.Model.Entity.LctoTemplate;
import aluita_templates.Aluita_Templates;
import fileManager.StringFilter;
import JExcel.XLSX;
import java.util.Map;

public class ContasAPagar {
    private Section templateSection;
    private List<LctoTemplate> lctos = new ArrayList<>();

    public ContasAPagar(Section templateSection) {
        this.templateSection = templateSection;
    }

    //function to get lctos from files
    public List<LctoTemplate> getLctos() {
        //section 'contas a pagar' of ini in Aluita_Templates.ini
        Section cap = Aluita_Templates.ini.get("contas a pagar");

        //get string path in ini file in section 'folders' and data 'pagamentos'
        String pagamentosPath = Aluita_Templates.ini.get("folders").fetch("pagamentos");
        //create file object
        File pagamentosFile = new File(pagamentosPath);
        //get list of files in folder
        File[] pagamentosFiles = pagamentosFile.listFiles();

        //of template section get string contas_a_pagar_filtro and create string filter
        StringFilter contas_a_pagar_filtro = new StringFilter(templateSection.fetch("contas_a_pagar_filtro"));
        //of template section get string contas_a_pagar_coluna
        StringFilter contas_a_pagar_coluna = new StringFilter(templateSection.fetch("contas_a_pagar_coluna"));

        Map<String, Map<String, String>> cols = new HashMap<>();
        //coluna data
        cols.put("data", new HashMap<String, String>() {{
            put("name", "data");
            put("type", "date");
            put("collumn", cap.get("col_data"));
            put("required", "true");
        }});

        //coluna doc
        cols.put("doc", new HashMap<String, String>() {{
            put("name", "doc");
            put("type", "string");
            put("collumn", cap.get("col_doc"));
            put("required", "true");
        }});

        //coluna fornecedor
        cols.put("fornecedor", new HashMap<String, String>() {{
            put("name", "fornecedor");
            put("type", "string");
            put("collumn", cap.get("col_fornecedor"));
            put("required", "true");
        }});

        //coluna valor
        cols.put("valor", new HashMap<String, String>() {{
            put("name", "valor");
            put("type", "value");
            put("collumn", cap.get("col_valor"));
            put("required", "true");
        }});

        //coluna banco
        cols.put("banco", new HashMap<String, String>() {{
            put("name", "banco");
            put("type", "string");
            put("collumn", cap.get("col_banco"));
            put("required", "true");
        }});


        //for each file in list of files
        for (File f : pagamentosFiles){
            //if contas_a_pagar_filtro is filter of file name
            if (contas_a_pagar_filtro.filterOfString(f.getName())){
                //get list of rows in file
                List<Map<String, Object>> rows = XLSX.get(f, cols);

                //for each row in list of rows
                for (Map<String, Object> row : rows){
                    //if contas_a_pagar_coluna is filter of row collumn banco
                    if (contas_a_pagar_coluna.filterOfString(row.get("banco").toString())){
                        /**
                         * New LctoTemplate object
                         * - data
                         * - doc
                         * - prefixo = ""
                         * - historico = fornecedor
                         * - valor (Bigdecimal) = valor * -1
                         */
                        
                        LctoTemplate lcto = new LctoTemplate(
                                Dates.Dates.getCalendarInThisStringFormat((Calendar) row.get("data"), "dd/MM/yyyy"),
                                row.get("doc").toString(),
                                "",
                                row.get("fornecedor").toString(),
                                ((BigDecimal) row.get("valor")).multiply(new java.math.BigDecimal("-1"))
                        );
                        //add lcto to list of lctos
                        lctos.add(lcto);
                    }
                }
            }
        }

        return lctos;
    }
}
