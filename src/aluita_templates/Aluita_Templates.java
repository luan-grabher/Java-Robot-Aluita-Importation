package aluita_templates;

import Entity.Executavel;
import JExcel.XLSX;
import Robo.AppRobo;
import TemplateContabil.Control.ControleTemplates;
import TemplateContabil.Model.ComparacaoTemplates;
import TemplateContabil.Model.Entity.Importation;
import fileManager.FileManager;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class Aluita_Templates {

    // map of strings to informations
    public static Map<String, StringBuilder> informations = new HashMap<>();

    // public mes and year
    public static Integer mes;
    public static Integer ano;

    private static String nomeApp = "";
    public static Ini ini = null;

    public static String testParameters = "[mes:5][ano:2023][ini:robot-aluita]";

    public static void main(String[] args) {
        try {
            AppRobo robo = new AppRobo(nomeApp);

            if (args.length > 0 && args[0].equals("test")) {
                robo.definirParametros(testParameters);
            } else {
                robo.definirParametros();
            }

            try {

                String scriptIniFileName = "robot-aluita-importation.ini";
                File scriptIniFile = FileManager.getFile(scriptIniFileName);
                Ini scriptIni = new Ini(scriptIniFile);

                /* Pega os dados do arquivo ini */
                String iniPath = scriptIni.fetch("folders", "templateConfig");
                String iniName = robo.getParametro("ini");

                String iniFullPath = iniPath + iniName + ".ini";
                File iniFile = FileManager.getFile(iniFullPath);
                ini = new Ini(iniFile);

                mes = Integer.valueOf(robo.getParametro("mes"));
                mes = mes >= 1 && mes <= 12 ? mes : 1;
                ano = Integer.valueOf(robo.getParametro("ano"));

                // Set mes e ano na comparacao
                ComparacaoTemplates.setMonth(mes);
                ComparacaoTemplates.setYear(ano);

                // with ini4J on section date set month = mes
                ini.put("date", "month", String.valueOf(mes < 10 ? "0" + mes : mes));
                ini.put("date", "year", String.valueOf(ano));
                // save ini
                ini.store();

                String pastaEmpresa = ini.fetch("Pastas", "empresa");
                String pastaAnual = ini.fetch("Pastas", "anual");
                String pastaMensal = ini.fetch("Pastas", "mensal");

                nomeApp = "Importação " + pastaEmpresa + " - " + ini.get("Config", "nome") + " " + mes + "/" + ano;

                StringBuilder returnExecutions = new StringBuilder();

                String[] templates = ini.get("Config", "templates").split(";");
                // Para cada template pega as informações
                for (String template : templates) {
                    template = !template.equals("") ? " " + template : "";

                    String comparar = template + (template.equals("") ? "" : " ") + "Comparar";

                    Map<String, Object> templateConfig = getTemplateConfig(template);
                    Map<String, Object> compararConfig = getTemplateConfig(comparar);

                    returnExecutions.append("\n").append(
                            start(mes, ano, pastaEmpresa, pastaAnual, pastaMensal, templateConfig, compararConfig));
                }

                robo.setNome(nomeApp);

                System.out.println("FIM DA EXECUÇÃO!");
                robo.executar(returnExecutions.toString());
            } catch (Exception e) {
                e.printStackTrace();
                FileManager.save(new File(System.getProperty("user.home")) + "\\Desktop\\JavaError.txt",
                        getStackTrace(e));
                System.out.println("Ocorreu um erro na aplicação: " + e);

                robo.executar("Ocorreu um erro na aplicação: " + e);
            }
        } catch (Exception e) {
            e.printStackTrace();
            FileManager.save(new File(System.getProperty("user.home")) + "\\Desktop\\JavaError.txt", getStackTrace(e));
            System.out.println("Ocorreu um erro na aplicação: " + e);
        }

        System.exit(0);
    }

    private static String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);

        return sw.toString();
    }

    /**
     * Retorna as configurações do template selecionado
     *
     * @param template Nome do template na seção ini
     */
    private static Map<String, Object> getTemplateConfig(String template) {
        // define section name
        String section = "Template" + template;

        // Se não encontrar a seção do template, retorna null
        if (ini.get(section, "nome") == null) {
            return null;
        }

        Map<String, Object> templateConfig = new HashMap<>();
        // section
        templateConfig.put("section", section);
        templateConfig.put("nome", ini.get(section, "nome"));
        templateConfig.put("id", ini.get(section, "id"));
        templateConfig.put("filtroArquivo", ini.get(section, "filtroArquivo"));
        templateConfig.put("tipo", ini.get(section, "tipo"));
        templateConfig.put("colunas", getTemplateColsConfig((String) templateConfig.get("tipo"), template));
        // unir arquivos
        templateConfig.put("unirArquivos", ini.get(section, "unirArquivos"));
        // pforFiltros
        templateConfig.put("pforFiltros", ini.get(section, "pforFiltros"));
        // pasta_retorno
        templateConfig.put("pasta_retorno", ini.get(section, "pasta_retorno"));

        return templateConfig;
    }

    /**
     * Retorna a configuração de colunas da seção "Colunas NOME-TEMPLATE" no
     * arquivo ini
     *
     * @param template Nome do template na seção ini
     * @param tipo     Tipo do arquivo, para este metodo funcionar deve ser "excel"
     * @return configuração de colunas da seção "Colunas NOME-TEMPLATE" no
     *         arquivo ini
     */
    private static Map<String, Map<String, String>> getTemplateColsConfig(String tipo, String template) {
        Map<String, Map<String, String>> colunas = new HashMap<>();
        if (tipo.equals("excel")) {
            colunas.put("data", getCollumnConfig("data", template));
            colunas.put("documento", getCollumnConfig("documento", template));
            colunas.put("pretexto", getCollumnConfig("pretexto", template));
            colunas.put("historico", getCollumnConfig("historico", template));
            colunas.put("entrada", getCollumnConfig("entrada", template));
            colunas.put("saida", getCollumnConfig("saida", template));
            colunas.put("valor", getCollumnConfig("valor", template));
            colunas.put("startGet", getCollumnConfig("startGet", template));
            colunas.put("endGet", getCollumnConfig("endGet", template));
        }

        return colunas;
    }

    private static Map<String, String> getCollumnConfig(String collumnName, String template) {
        return XLSX.getCollumnConfigFromString(collumnName, ini.get("Colunas" + template, collumnName));
    }

    public static String start(int mes, int ano, String pastaEmpresa, String pastaAnual, String pastaMensal,
            Map<String, Object> templateConfig, Map<String, Object> compararConfig) throws Exception {
        Importation importation = new Importation();
        importation.setTIPO(templateConfig.get("tipo").equals("excel") ? Importation.TIPO_EXCEL : Importation.TIPO_OFX);
        importation.setIdTemplateConfig((String) templateConfig.get("id"));
        importation.setNome((String) templateConfig.get("nome"));
        importation.getXlsxCols().putAll((Map<String, Map<String, String>>) templateConfig.get("colunas"));

        // create informations
        informations.put(importation.getNome(), new StringBuilder());

        Importation importationC = null;
        if (compararConfig != null) {
            importationC = new Importation();
            importationC.setTIPO(
                    compararConfig.get("tipo").equals("excel") ? Importation.TIPO_EXCEL : Importation.TIPO_OFX);
            importationC.setIdTemplateConfig((String) compararConfig.get("id"));
            importationC.setNome((String) compararConfig.get("nome"));
            importationC.getXlsxCols().putAll((Map<String, Map<String, String>>) compararConfig.get("colunas"));
        }

        ControleTemplates controle = new ControleTemplates(mes, ano);
        controle.setPastaEscMensal(pastaEmpresa);
        controle.setPasta(pastaAnual, pastaMensal);

        Section templateSection = ini.get(templateConfig.get("section"));

        // unir arquivos se necessário
        if (templateConfig.get("unirArquivos") != null && templateConfig.get("unirArquivos").equals("true")) {
            // from ini get 'bancos' on section 'folders'
            String bancosPath = ini.fetch("folders", "bancos");

            // from trmplateconfig get 'filtroArquivo' and replace ';unify' with ''
            String filtroArquivo = (String) templateConfig.get("filtroArquivo");
            filtroArquivo = filtroArquivo.replace(";unify", "");
            filtroArquivo = filtroArquivo + "#unify";

            Control.unirArquivos(bancosPath, filtroArquivo);
        }

        Map<String, Executavel> execs = new LinkedHashMap<>();
        execs.put("Procurando arquivo " + templateConfig.get("filtroArquivo"),
                controle.new defineArquivoNaImportacao((String) templateConfig.get("filtroArquivo"), importation));

        if (compararConfig != null) {
            execs.put("Procurando arquivo " + compararConfig.get("filtroArquivo"),
                    controle.new defineArquivoNaImportacao((String) compararConfig.get("filtroArquivo"), importation));
        }

        // call para importar os lctos do arquivo
        execs.put("Importando Lctos do arquivo " + templateConfig.get("filtroArquivo"),
                (new Control()).new importImportationLctos(importation, mes, ano));

        // if has 'pforFiltros', put Control.pagamentosFornecedor with importation and
        // config.pforFiltros splited by '|'
        if (templateConfig.get("pforFiltros") != null) {
            String[] pforFiltros = ((String) templateConfig.get("pforFiltros")).split("\\|");
            execs.put("Pagamentos Fornecedor " + templateConfig.get("nome"),
                    (new Control()).new pagamentosFornecedor(importation, pforFiltros));
        }

        // if has 'pasta_retorno', put Control.pastaRetorno with importation and
        // config.pasta_retorno
        if (templateConfig.get("pasta_retorno") != null) {
            execs.put("Retornos " + templateConfig.get("nome"),
                    (new Control()).new pastaRetorno(importation, templateSection));
        }

        // if templateconfig section on ini has 'contas_a_pagar_coluna' and
        // 'contas_a_pagar_filtro', put Control.contasapagar
        if (templateSection.containsKey("contas_a_pagar_coluna")
                && templateSection.containsKey("contas_a_pagar_filtro")) {
            execs.put("Contas a Pagar " + templateConfig.get("nome"),
                    (new Control()).new contasAPagar(importation, templateSection));
        }

        execs.put("Criando template " + templateConfig.get("nome"),
                (new Control()).new convertImportationToTemplate(importation, mes, ano));

        // show information
        execs.put("Informações ", (new Control()).new showInformation(importation));

        return AppRobo.rodarExecutaveis(nomeApp, execs);
    }

}
