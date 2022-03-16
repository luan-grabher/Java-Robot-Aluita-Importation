package aluita_templates;

import java.io.File;

import Entity.Executavel;
import TemplateContabil.Model.Entity.Importation;
import fileManager.FileManager;
import fileManager.StringFilter;
import aluita_templates.Model.PagamentoFornecedor;

import TemplateContabil.Model.Entity.LctoTemplate;

import java.util.ArrayList;
import java.util.List;

public class Control {

    //static function unirArquivos(String pasta, String filtro)
    public static void unirArquivos(String folder, String filtro){
        File folderFile = new File(folder);

        //get all files on folder
        File[] files = folderFile.listFiles();

        //initiliaze String Filter
        StringFilter filter = new StringFilter(filtro);
        
        StringBuilder sb = new StringBuilder();
        
        //for each file
        for(File file : files){
            //if file is a file and file name is not null
            if(file.isFile() && file.getName() != null){
                if(filter.filterOfString(file.getName())){
                    //append file content to StringBuilder
                    sb.append(fileManager.FileManager.getText(file));
                    sb.append("\n");
                }
            }
        }

        //name of file is filter replacing ";" with " "
        String name = "unify " + filtro.replace(";", " ");
        //save file
        FileManager.save(folderFile, name, sb.toString());
    }
    
    //function to if has pagamentos de fornecedores 
    public class pagamentosFornecedor extends Executavel{
        public Importation imp;
        public String[] filters;

        public pagamentosFornecedor(Importation imp, String[] filters){
            this.imp = imp;
            this.filters = filters;
        }

        @Override
        public void run(){
            //get path of folder pfor with section folders.pagamentos + '\PFOR'
            String path = Aluita_Templates.ini.get("folders").fetch("pagamentos") + "\\PFOR";
            //convert to file
            File folder = new File(path);

            //if folder exists
            if(folder.exists()){
                //call to pagamentosFornecedor
                PagamentoFornecedor pfors = new PagamentoFornecedor(folder);

                //create lctotemplate list to delete of importation after
                List<LctoTemplate> lctos_to_delete = new ArrayList<LctoTemplate>();

                //for each lcto in importation
                for(LctoTemplate lcto : imp.getLctos()){
                    //for each filter
                    for(String filter : filters){
                        //if historico contains filter
                        if(lcto.getHistorico().contains(filter)){
                            //add lcto to list to delete
                            lctos_to_delete.add(lcto);
                        }
                    }
                }

                //for each lcto to delete, remove from importation
                for(LctoTemplate lcto : lctos_to_delete){
                    imp.getLctos().remove(lcto);
                }

                //add pagamentos fornecedor to importation
                imp.getLctos().addAll(pfors.getLctos());
            }//else throw new Error
            else{
                throw new Error("Pasta '" + path + "' não existe");
            }                
        }
    }
}
