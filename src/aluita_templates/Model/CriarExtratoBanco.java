package aluita_templates.Model;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import TemplateContabil.Model.ComparacaoTemplates;
import TemplateContabil.Model.Template;
import TemplateContabil.Model.Entity.LctoTemplate;
import TemplateContabil.Model.Entity.OFX;
import fileManager.Selector;

public class CriarExtratoBanco {

    private static File pastaEscrituraçãoMensal;
    private static File pastaBancos;
    private static File pastaRetornos;
    private static File pastaContasAPagar;

    private final int nroEmpresa;
    private final int mes;
    private final int ano;
    private final String nomeBanco;
    private final String arquivoBanco;
    private final int nroBanco;
    private final int filial;

    private final boolean usarContasAPagar;

    public static void setPastaEscrituraçãoMensal(File pastaEscrituraçãoMensal) {
        CriarExtratoBanco.pastaEscrituraçãoMensal = pastaEscrituraçãoMensal;
    }

    public static void setPastaBancos(File pastaBancos) {
        CriarExtratoBanco.pastaBancos = pastaBancos;
    }

    public static void setPastaRetornos(File pastaRetornos) {
        CriarExtratoBanco.pastaRetornos = pastaRetornos;
    }

    public static void setPastaContasAPagar(File pastaContasAPagar) {
        CriarExtratoBanco.pastaContasAPagar = pastaContasAPagar;
    }

    public static String verificarPastasArquivos() {
        String r = "";
        if (!pastaEscrituraçãoMensal.exists()) {
            r += "\n[ERRO] A pasta " + link("Escrituração Mensal", pastaEscrituraçãoMensal.getAbsolutePath()) + " não existe!";
        }

        if (!pastaBancos.exists()) {
            r += "\n[ERRO] A pasta " + link("Bancos", pastaBancos.getAbsolutePath()) + " não existe!";
        }

        if (!pastaRetornos.exists()) {
            r += "\n[ERRO] A pasta " + link("Retornos", pastaRetornos.getAbsolutePath()) + " não existe!";
        }

        if (!pastaBancos.exists()) {
            r += "\n[ERRO] O arquivo Template Padrão não existe na pasta "
                    + link("Escrituração Mensal", pastaEscrituraçãoMensal.getAbsolutePath());
        }

        if (!pastaContasAPagar.exists()) {
            r += "\n[ERRO] A Pasta Contas a pagar não existe em " + link("Extratos", pastaContasAPagar.getParentFile().getAbsolutePath());
        }

        return r;
    }

    public CriarExtratoBanco(int nroEmpresa, int mes, int ano, String nomeBanco, String arquivoBanco, int nroBanco, int filial, boolean usarContasAPagar) {
        this.nroEmpresa = nroEmpresa;
        this.mes = mes;
        this.ano = ano;
        this.nomeBanco = nomeBanco;
        this.arquivoBanco = arquivoBanco;
        this.nroBanco = nroBanco;
        this.filial = filial;
        this.usarContasAPagar = usarContasAPagar;
    }

    public String executar() {
        //Pega Retornos
        File pastaRetorno = Selector.getFileOnFolder(pastaRetornos, arquivoBanco, ".");
        if (pastaRetorno != null) {
            RetornoBanco retornos = new RetornoBanco(pastaRetorno, nomeBanco, filial);

            //Contas a pagar
            ContasAPagar contasAPagar = new ContasAPagar(new File("C:/Lugar inexistente")); //Pré inicializa, para ter uma lista zerada
            if (usarContasAPagar) {
                contasAPagar = new ContasAPagar(pastaContasAPagar);
            }

            //OFX do banco
            String filtroOFX = arquivoBanco + ";.ofx";
            File arquivoOFX = Selector.getFileOnFolder(pastaBancos, filtroOFX);
            if (arquivoOFX != null) {
                List<LctoTemplate> lctosOFX = OFX.getListaLctos(arquivoOFX);

                if (lctosOFX.size() > 0) {
                    //Filtrar OFX do mês
                    lctosOFX = lctosOFX.stream().filter(l -> dataEstaNoMes(l.getData())).collect(Collectors.toList());

                    String[] filtroFornecedores = new String[]{"PFOR TIT", "PAGTO ELETRON COB"};
                    String[] filtroLiquidacoes = new String[]{"LIQUID"};

                    //Pega soma Fornecedores e depois elimina
                    List<LctoTemplate> ofxFornecedores = lctosOFX.stream().filter(l -> containsAny(l.getComplementoHistorico(), filtroFornecedores)).collect(Collectors.toList());

                    //Pega soma Liquidação e depois elimina
                    List<LctoTemplate> ofxLiquidacoes = lctosOFX.stream().filter(l -> containsAny(l.getComplementoHistorico(), filtroLiquidacoes)).collect(Collectors.toList());

                    //Verifica diferenças ofx com retornos e contas a pagar
                    String comparacaoContasComFornecedores = ComparacaoTemplates.getComparacaoString("Contas a pagar " + nomeBanco, "Fornecedores " + nomeBanco, contasAPagar.getLctos(), ofxFornecedores);
                    String comparacaoLiquidacoesComRetornos = ComparacaoTemplates.getComparacaoString("Liquidações " + nomeBanco, "Retornos " + nomeBanco, ofxLiquidacoes, retornos.getLctos());

                    //Exclui Liquidações e Fornecedores do original
                    lctosOFX = lctosOFX.stream().filter(l -> !containsAny(l.getComplementoHistorico(), filtroFornecedores)).collect(Collectors.toList());
                    lctosOFX = lctosOFX.stream().filter(l -> !containsAny(l.getComplementoHistorico(), filtroLiquidacoes)).collect(Collectors.toList());

                    //----Une colunas OFX com retornos e contas a pagar
                    List<LctoTemplate> todosLctos = new ArrayList<>();
                    todosLctos.addAll(lctosOFX);
                    todosLctos.addAll(contasAPagar.getLctos());
                    todosLctos.addAll(retornos.getLctos());

                    //----Coloca no template
                    //New file in pastaBancos with name 'Template Aluita ' + nomeBanco + ' ' + mes + ' ' + ano + '.xlsm'
                    File new_template = new File(pastaBancos, "Template Aluita " + nomeBanco + " " + mes + " " + ano + ".xlsm");
                    Template template = new Template(mes, ano, new_template, "aluita" + nroBanco, todosLctos);

                    return comparacaoContasComFornecedores + "<br>" + comparacaoLiquidacoesComRetornos;
                } else {
                    return "[ERRO] Não foi possivel ler o arquivo OFX do banco " + nomeBanco + ": " + link(arquivoBanco, arquivoOFX.getAbsolutePath());
                }
            } else {
                return "[ERRO] O arquivo OFX do banco " + nomeBanco + "(" + filtroOFX + ") não existe na pasta " + link("Bancos", pastaBancos.getAbsolutePath());
            }
        } else {
            return "[ERRO] A pasta de retornos de " + nomeBanco + " não existe em " + link("Retornos", pastaRetornos.getAbsolutePath());
        }
    }

    private boolean dataEstaNoMes(String data) {
        try {
            //validade the date string
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date date = sdf.parse(data);
            
            //get the calendar instance
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            return (cal.get(Calendar.MONTH) + 1) == mes && cal.get(Calendar.YEAR) == ano;
        } catch (Exception e) {
            return false;
        }
    }

    //function containsAny to return if any of the strings in the array are contained in the string
    private boolean containsAny(String string, String[] array) {
        for (String s : array) {
            if (string.contains(s)) {
                return true;
            }
        }
        return false;
    }

    private static String link(String nome, String link) {
        return "<a href='" + link + "'>" + nome + "</a>";
    }
}
