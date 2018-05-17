package weiEtal;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

import utilitarios.Padrao;

/**
 * Factory
 */
public class Factory extends Padrao{

	public Factory(String caminho) {
		this.setCaminho(caminho);
	}
	
    /**
     * 
     * @return List com Statement de ifs que tem no seu condicional algum parametro do método
     * @param Leitor l - é a classe responsável por ser feito a leitura genérica da classe
     * table 4 - Strategy pattern directed refactoring opportunities identification algorithm
     */
    public Map<MethodDeclaration, List<Statement>> analisador() {
    	Map<MethodDeclaration, List<Statement>> mapaMetodosAnalisados = new HashMap<>(); 
        List<MethodDeclaration> metodos = this.getLeitor().metodosDeclaradosJavaParser();
        for (MethodDeclaration metodo : metodos) {
        	List<Statement> instrucoesIf = new ArrayList<>();
            NodeList<com.github.javaparser.ast.body.Parameter> parametrosMetodo = metodo.getParameters();
            if (parametrosMetodo == null || parametrosMetodo.isEmpty()) {
                System.out.println(" -- Método:" + metodo.getName() + " -- não tem parametros não é usado factory");
                continue;
            }

            /** if abaixo se existe if dentro do método else continue */
            /** se o tipo não for interface ele pode ter métodos */
            if (!this.getLeitor().getTipoClasse().equals("interface")) {
                List<Statement> instrucoesMetodo = this.getLeitor().linhasMetodo(metodo.getName().toString());
                
                if (!instrucoesMetodo.isEmpty()) {
                    /** para cada if faça */
                    for (Statement var : instrucoesMetodo) {
                        if (var instanceof IfStmt) {
                            IfStmt condicional = this.getLeitor().temParametroNoIf2(parametrosMetodo, (IfStmt) var);                   
                            if (condicional != null) {
                            	instrucoesIf.add(condicional);
                            } else {
                                break;
                            }
                        }
                    }
                }
            }
            if(instrucoesIf != null && !instrucoesIf.isEmpty()) {
            	mapaMetodosAnalisados.put(metodo, instrucoesIf);
            }
        }
        return mapaMetodosAnalisados;
    }
    

	/**
	 * le um arquivo .java transforma ele e adiciona coisas ao método e por fim
	 * reescreve ele perante o arquivo
	 */
	public void modificaClasse(MethodDeclaration metodoDeclarado, List<Statement> instrucoesIf) throws IOException {
		DateFormat dat = DateFormat.getDateInstance(DateFormat.LONG, new Locale("pt","BR"));
		String hoje = dat.format(new Date());
		try {
			String classeAvaliada = this.getLeitor().getNomeClasse();
			
			EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
			modifiers.add(Modifier.PUBLIC);			

			for (Statement statement : instrucoesIf) {
				IfStmt ifStmt = (IfStmt) statement;
				if (!this.temElse(ifStmt)) {
					/** criar um arquivo ConcreteStrategy + code com cada if */
					for (IfStmt ifMetodo : this.getCondicoesComIfElse()) {
						String ifThen = ifMetodo.getThenStmt().getChildNodes().toString();
						String separa_then[] = ifThen.split("new ");
						String nomeClasse = separa_then[1].replaceAll("'", "").replace("(", "").replace(");]", "");
						String nomeClasseArquivo = nomeClasse + "Factory";
						
						CompilationUnit cu = new CompilationUnit();
						cu.setPackageDeclaration(this.getLeitor().getPacoteClasse());
						cu.setBlockComment("*\n* Class gerada automaticamente pelo sistema de refatoração - Factory \n* @author - Thyago Henrique Pacher\n *@since "+ hoje +"\n ");
						ClassOrInterfaceDeclaration type = cu.addClass(nomeClasseArquivo);
						
						// create a method coloca o retorno para ser uma instancia da classe
						BlockStmt block = new BlockStmt();
						block.addStatement("return new " + nomeClasse + "();");
						
						MethodDeclaration method = new MethodDeclaration();
						method.setName(metodoDeclarado.getName());
						method.setType(metodoDeclarado.getType());
						method.setModifiers(modifiers);
						method.setBody(block);
						type.addMember(method);
						type.addExtendedType(classeAvaliada);

						this.gravarConteudo(this.getCaminho() + "/" + nomeClasseArquivo + ".java", cu.toString());
					}
				}
			}
			
			//reescreve método da classe refatorada
			CompilationUnit cu = JavaParser.parse(this.getLeitor().getArquivoClasse());
			ClassOrInterfaceDeclaration classeOrigem = (ClassOrInterfaceDeclaration) cu.getType(0);
            NodeList<BodyDeclaration<?>> members = classeOrigem.getMembers();
            for (BodyDeclaration<?> member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    if(method.getName().equals(metodoDeclarado.getName())) {
                    	metodoDeclarado = method;
                    	break;
                    }
                }
            }			
			classeOrigem.setAbstract(true);//seta classe factory pai para abstract
			metodoDeclarado.setBody(new BlockStmt());
			metodoDeclarado.getParameter(0).remove();

			this.gravarConteudo(this.getCaminho() + "/" + classeAvaliada + ".java", cu.toString());
				
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		} 
	}    

 
}