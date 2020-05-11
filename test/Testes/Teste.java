
package Testes;

import aluita_templates.Aluita_Templates;

public class Teste {

    public static void main(String[] args) {
        testePrincipal();
        
        System.exit(0);
    }
    
    private static void testePrincipal(){
        int mes = 01;
        int ano = 2020;
        
        System.out.println(Aluita_Templates.principal(mes, ano).replaceAll("<br>", "\n").replaceAll("</tr>", "\n"));
    }
    
}
