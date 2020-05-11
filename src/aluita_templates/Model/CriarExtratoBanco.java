package aluita_templates.Model;

import Auxiliar.LctoTemplate;
import Auxiliar.Valor;
import java.io.File;
import OFX.OFX;
import TemplateContabil.ComparacaoTemplates;
import TemplateContabil.Template;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

public class CriarExtratoBanco {

    private static File pastaEscrituraçãoMensal;
    private static File pastaBancos;
    private static File pastaRetornos;
    private static File arquivoTemplatePadrao;
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

    public static void setArquivoTemplatePadrao(File arquivoTemplatePadrao) {
        CriarExtratoBanco.arquivoTemplatePadrao = arquivoTemplatePadrao;
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
        File pastaRetorno = Selector.Pasta.procura_arquivo(pastaRetornos, arquivoBanco, ".");
        if (pastaRetorno != null) {
            RetornoBanco retornos = new RetornoBanco(pastaRetorno, nomeBanco, filial);

            //Contas a pagar
            ContasAPagar contasAPagar = new ContasAPagar(new File("C:/Lugar inexistente")); //Pré inicializa, para ter uma lista zerada
            if (usarContasAPagar) {
                contasAPagar = new ContasAPagar(pastaContasAPagar);
            }

            //OFX do banco
            String filtroOFX = arquivoBanco + ";.ofx";
            File arquivoOFX = Selector.Pasta.procura_arquivo(pastaBancos, filtroOFX);
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
                    File arquivoGeradoTemplate = new File(pastaBancos.getAbsolutePath() + "/" + arquivoTemplatePadrao.getName().replaceAll(".xlsm", " ") + nomeBanco + ".xlsm");
                    Template template = new Template(mes, ano, arquivoTemplatePadrao, arquivoGeradoTemplate, nroEmpresa, filial, 51, 61, nroBanco, todosLctos);

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
            Valor valorData = new Valor(data);
            if(valorData.éUmaDataValida()){
                Calendar cal = valorData.getCalendar();
                return (cal.get(Calendar.MONTH) + 1) == mes && cal.get(Calendar.YEAR) == ano;
            }else{
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean containsAny(String str, String[] listStr) {
        for (String string : listStr) {
            if (str.toUpperCase().contains(string.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private static String link(String nome, String link) {
        return "<a href='" + link + "'>" + nome + "</a>";
    }
}
