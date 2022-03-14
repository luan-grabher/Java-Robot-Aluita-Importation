package aluita_templates;

import Robo.AppRobo;
import aluita_templates.Model.CriarExtratoBanco;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Aluita_Templates {

    /**
     * Processo para cada banco:
     *  - Extrair dados do arquivo OFX
     *  - Dentro da pasta do contas a pagar:
     *    - Percorre todos os filtros de arquivos de contas a pagar
     *      - Percorre todos os arquivos de contas a pagar naquele filtro
     *        - Percorre todas as linhas do arquivo
     *          - Se na coluna 'banco' existir o nome do banco, então:
     *            - Cria um objeto do tipo LctoTemplate
     */ 

    public static Integer empresa = 613;

    public static void main(String[] args) {
        AppRobo app = new AppRobo("Aluita Templates");
        
        try {
            app.definirParametros();

            System.out.println(app.getParametro("mes"));
            System.out.println(app.getParametro("ano"));

            
            //Convert 'mes' and 'ano' to integer
            Integer mes = Integer.parseInt(app.getParametro("mes"));
            Integer ano = Integer.parseInt(app.getParametro("ano"));

            app.executar(
                    principal(
                            mes,
                            ano                    
                    )
            );   
        }catch(Exception e){
            e.printStackTrace();
        }

        System.exit(0);
    }

    public static String principal(int mes, int ano) {
        try {
            //Verifica parametros
            if (mes > 0 && mes < 13 && ano > 2017 && ano < 3000) {
                String mesMM = (mes < 9 ? "0" : "") + mes;
                //Define pastas
                String pastaEmpresa = "Aluita Aluminio Porto Alegre Ltda";
                File pastaEscrituraçãoMensal = new File("\\\\HEIMERDINGER\\docs\\Contábil\\Clientes\\" + pastaEmpresa + "\\Escrituração mensal");
                
                //Pasta de Extratos
                File pastaExtratos = new File(pastaEscrituraçãoMensal.getAbsolutePath() + "\\" + ano + "\\Extratos\\" + mesMM + "." + ano);
                //Pasta de Movimentos
                File pastaMovimentos = new File(pastaEscrituraçãoMensal.getAbsolutePath() + "\\" + ano + "\\Movimento\\" + mesMM + "." + ano);

                File pastaBancos = new File(pastaExtratos.getAbsolutePath() + "\\Bancos");
                File pastaRetornos = new File(pastaMovimentos.getAbsolutePath() + "\\Retorno");
                File pastaPagamentos = new File(pastaMovimentos.getAbsolutePath() + "\\Pagamentos\\PAGFOR");

                CriarExtratoBanco.setPastaEscrituraçãoMensal(pastaEscrituraçãoMensal);
                CriarExtratoBanco.setPastaBancos(pastaBancos);
                CriarExtratoBanco.setPastaRetornos(pastaRetornos);
                CriarExtratoBanco.setPastaContasAPagar(pastaPagamentos);

                String verificaçãoPastasArquivos = CriarExtratoBanco.verificarPastasArquivos();
                if (verificaçãoPastasArquivos.equals("")) {
                    StringBuilder retorno = new StringBuilder();
                    List<CriarExtratoBanco> listaExtratos = new ArrayList<>();

                    listaExtratos.add(new CriarExtratoBanco(empresa, mes, ano, "POA", "POA", 9, 1, true));
                    listaExtratos.add(new CriarExtratoBanco(empresa, mes, ano, "CAXIAS", "CAXIAS", 9, 1, false));
                    listaExtratos.add(new CriarExtratoBanco(empresa, mes, ano, "NH", "NH", 9, 1, false));
                    listaExtratos.add(new CriarExtratoBanco(empresa, mes, ano, "PELOTAS", "PELOTAS", 5008, 2, false));
                    listaExtratos.add(new CriarExtratoBanco(empresa, mes, ano, "PASSO FUNDO", "P;FUNDO", 2111, 4, false));
                    listaExtratos.add(new CriarExtratoBanco(empresa, mes, ano, "SANTA MARIA", "S;MARIA", 2315, 5, false));

                    listaExtratos.forEach((listaExtrato) -> {
                        retorno.append(listaExtrato.executar());
                    });

                    return retorno.toString();
                } else {
                    return verificaçãoPastasArquivos;
                }
            } else {
                return "[ERRO] Mês ou ano inválidos.";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "[ERRO] Ocorreu um erro JAVA na aplicação: " + e;
        }
    }

}
