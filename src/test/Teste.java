package test;

import java.io.File;

import org.ini4j.Ini;

import aluita_templates.Aluita_Templates;

public class Teste {

    public static void main(String[] args) {
        System.out.println("INICIANDO TESTE!");

        Aluita_Templates.main("test".split(" "));
        
        System.exit(0);
    }

    //function to test ini
    public static void testIni() {                
        try {
            Ini ini = new Ini(new File("robot-aluita.ini"));
            
            String path = ini.fetch("folders", "bancos");

            //printando o path
            System.out.println("PATH: " + path);
        } catch (Exception e) {
            System.out.println("Erro ao ler arquivo de configuração!");
            e.printStackTrace();
        }
    }
    
}
