package aluita_templates;

import java.io.File;

import Entity.Executavel;
import TemplateContabil.Model.ImportationModel;
import TemplateContabil.Model.Entity.Importation;
import fileManager.FileManager;
import fileManager.StringFilter;
import aluita_templates.Model.ContasAPagar;
import aluita_templates.Model.PagamentoFornecedor;
import aluita_templates.Model.RetornoBanco;
import TemplateContabil.Model.Entity.LctoTemplate;

import java.util.ArrayList;
import java.util.List;

import org.ini4j.Profile.Section;

public class Control {

    //function to import lctos from importation
    public class importImportationLctos extends Executavel {

        private final Importation imp;

        public importImportationLctos(Importation imp) {
            this.imp = imp;
        }

        @Override
        public void run() {
            ImportationModel.getLctosFromFile(imp);
        }
    }

    //function to convert importation to template
    public class convertImportationToTemplate extends Executavel {

        private final Importation imp;
        private final Integer month;
        private final Integer year;

        public convertImportationToTemplate(Importation imp, Integer month, Integer year) {
            this.imp = imp;
            this.month = month;
            this.year = year;
        }

        @Override
        public void run() {
            ImportationModel.createImportationTemplate(imp, month, year);
        }
    }

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
        String name = "unify " + filtro.split("#")[0].replace(";", " ");
        //save file
        FileManager.save(folderFile, name, sb.toString());
    }
    
    //function to if has pagamentos de fornecedores 
    public class pagamentosFornecedor extends Executavel{
        public Importation imp;
        public String[] filters;
        public List<StringFilter> strFilters = new ArrayList<>();

        public pagamentosFornecedor(Importation imp, String[] filters){
            this.imp = imp;
            this.filters = filters;
            
            //for each filter, create string filter and put in array
            for (String filter : filters) {
                strFilters.add(new StringFilter(filter));
            }
        }

        @Override
        public void run(){
            //get path of folder pfor with section folders.pagamentos + '\PFOR'
            String path = Aluita_Templates.ini.get("folders").fetch("pagamentos") + "\\PAGFOR";
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
                    //for each str filter
                    for (StringFilter strFilter : strFilters) {
                        //if lcto has filter
                        if(strFilter.filterOfString(lcto.getHistorico())){
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

    //function pastaRetorno extending Executavel receiving imp and name of folder.
    /**
     * 
     * With Aluita_tenplates.ini.get("folders").fetch("retorno"), access subfolder by name in constructor.
     * Pass to class RetornoBanco and add to importation.
     * 
     * @param imp
     * @param folderName
     * 
     */
    public class pastaRetorno extends Executavel{
        public Importation imp;
        public Section config;

        public pastaRetorno(Importation imp, Section config){
            this.imp = imp;
            this.config = config;
        }

        @Override
        public void run(){
            //remove all original retornos lctos
            //list of lctos to delete
            List<LctoTemplate> lctos_to_delete = new ArrayList<LctoTemplate>();

            //filter string with config.fetch("retornos_filtros")
            StringFilter filter = new StringFilter(config.fetch("retornos_filtros"));

            //for each lcto in importation, if filter is filter of string of lcto.getHistorico(), add to list to delete
            for(LctoTemplate lcto : imp.getLctos()){
                if(filter.filterOfString(lcto.getHistorico())){
                    lctos_to_delete.add(lcto);
                }
            }

            //for each lcto to delete, remove from importation
            for(LctoTemplate lcto : lctos_to_delete){
                imp.getLctos().remove(lcto);
            }

            //split config.fetch("pasta_retornos") by ";"
            String[] folders = config.fetch("pasta_retorno").split(";");

            //for each folder
            for(String folderName : folders){
                //get path of folder pfor with section folders.pagamentos + '\PFOR'
                String path = Aluita_Templates.ini.get("folders").fetch("retornos") + "/" + folderName;
                //convert to file
                File folder = new File(path);

                //if folder exists
                if(folder.exists()){
                    //Pega retornos da pasta
                    RetornoBanco ret = new RetornoBanco(folder, folderName.replace("RETORNO ", ""));

                    //add retornos to importation
                    imp.getLctos().addAll(ret.getLctos());
                }//else throw new Error
                else{
                    throw new Error("Pasta '" + path + "' não existe");
                }
            }
        }
    }

    //function contasAPagar extending Executavel receiving imp and templateSection to call contasAPagar
    public class contasAPagar extends Executavel{
        public Importation imp;
        public Section config;

        public contasAPagar(Importation imp, Section config){
            this.imp = imp;
            this.config = config;
        }

        @Override
        public void run(){
            ContasAPagar contas = new ContasAPagar(config);
            List<LctoTemplate> contas_lctos = contas.getLctos();

            //lctos to delete
            List<LctoTemplate> lctos_to_delete = new ArrayList<LctoTemplate>();
            //lctos to add
            List<LctoTemplate> lctos_to_add = new ArrayList<LctoTemplate>();

            //for each lcto in importation
            for(LctoTemplate lcto : imp.getLctos()){
                //for each conta in contas_lctos
                for(LctoTemplate conta : contas_lctos){
                    //if data of lcto is equal to data of conta
                    if(lcto.getData().equals(conta.getData())){
                        //if valor of lcto is equal to valor of conta
                        if(lcto.getValor().compareTo(conta.getValor()) == 0){
                            //unify complemento historico of lcto and conta on conta complemento historico
                            conta.setComplementoHistorico(conta.getComplementoHistorico() + " " + lcto.getComplementoHistorico());

                            //lcto to add is conta and lcto to delete is lcto
                            lctos_to_add.add(conta);
                            lctos_to_delete.add(lcto);
                        }
                    }                    
                }
            }

            //for each lcto to delete, remove from importation
            for(LctoTemplate lcto : lctos_to_delete){
                imp.getLctos().remove(lcto);
            }

            //add lctos to importation
            imp.getLctos().addAll(lctos_to_add);
        }
    }
}
