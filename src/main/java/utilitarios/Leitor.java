package utilitarios;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

/**
 * Leitor
 */
public class Leitor {

	private String pacoteClasse = "";
	private String nomeClasse;
	private CompilationUnit cu = null;
	private String tipoClasse;
	private String caminhoClasse;
	private File arquivoClasse;
	public boolean erro = false;

	public Leitor(){
		
	}

	public Leitor(String nomeClasse, String caminho) {
		try (BufferedReader br = new BufferedReader(new FileReader(caminho))) {
			this.caminhoClasse = caminho;
			while (br.ready()) {
				String linha = br.readLine();
				if (linha.contains("package")) {
					String separa_linha[] = linha.split("package ");
					this.pacoteClasse = separa_linha[1].replace(";", "");
				}
				//para identificar o modificar da classe
				if (linha.contains("public") || linha.contains("protected") || linha.contains("private")) {
					String separa_linhaModificador[] = null;
					if (linha.contains("public")) {
						separa_linhaModificador = linha.split("public ");
					}
					if (linha.contains("protected")) {
						separa_linhaModificador = linha.split("protected ");
					}
					if (linha.contains("private")) {
						separa_linhaModificador = linha.split("private ");
					}
					String separadorTipo[] = separa_linhaModificador[1].trim().split(" ");
					this.tipoClasse = separadorTipo[0];//pegando aqui o tipo se é interface, abstract, ou class
					break;
				}else if(linha.contains("interface")){
					this.tipoClasse = "interface";
					break;
				}
			}

			this.nomeClasse = nomeClasse;
			this.arquivoClasse = new File(this.caminhoClasse);
			cu = JavaParser.parse(arquivoClasse);			
		} catch (Exception ex) {
			this.erro = true;
			System.out.println("Erro causado por: " + ex.getMessage() + " no caminho: " + caminho);
		}
	}


	public List<MethodDeclaration> metodosDeclaradosJavaParser() {
		List<MethodDeclaration> metodos = new ArrayList<>();
		try {

			// Go through all the types in the file
			NodeList<TypeDeclaration<?>> types = cu.getTypes();
			for (TypeDeclaration<?> type : types) {
				// Go through all fields, methods, etc. in this type
				NodeList<BodyDeclaration<?>> members = type.getMembers();
				for (BodyDeclaration<?> member : members) {
					if (member instanceof MethodDeclaration) {
						MethodDeclaration method = (MethodDeclaration) member;
						metodos.add(method);
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Erro ao pegar linhas: " + ex.getMessage());
		}
		return metodos;
	}	

	/**
	 * retorna as linhas do método de maneira legivel 
	 */
	public List<Statement> linhasMetodo(String nomeMetodo) {
		try {
			NodeList<TypeDeclaration<?>> types = this.cu.getTypes();
			for (TypeDeclaration<?> type : types) {
				NodeList<BodyDeclaration<?>> members = type.getMembers();
				for (BodyDeclaration<?> member : members) {
					if (member instanceof MethodDeclaration) {
						MethodDeclaration method = (MethodDeclaration) member;
						String nomeMetodoLocal = method.getName().toString();
						if (nomeMetodoLocal.equals(nomeMetodo)) {
							List<Statement> retorno = method.getBody().get().getStatements();
							if(retorno != null && !retorno.isEmpty()){
								return retorno;
							}
						}
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Erro ao pegar linhas: " + ex.getMessage());
		}
		return null;
	}

	public List<FieldDeclaration> camposClasse(){
		return this.cu.getTypes().get(0).getFields();
	}
	

	public IfStmt temNewNoIf(IfStmt ifStmt) {
		for (FieldDeclaration fieldDeclaration : this.camposClasse()) {
			String condicao = ifStmt.getCondition().toString();
			String campo = fieldDeclaration.getVariable(0).getNameAsString();
			if(condicao.contains(campo)){
				/** achou um parametro sendo usado no condicional */
				return ifStmt;
			}	
		}
		return null;
	}		


	/**
	 *  verifica se tem parametros no condicional IF
	 * @param parametrosMetodo - os parametros do método
	 * @param condicional - condicional perante o IF
	 * @return retorna o condicional cujo houver parametro sendo usado nele.
	 */
	public IfStmt temParametroNoIf1(NodeList<com.github.javaparser.ast.body.Parameter> parametrosMetodo, IfStmt ifStmt){
		for (com.github.javaparser.ast.body.Parameter parametro : parametrosMetodo) {
			String condicao = ifStmt.getCondition().toString();
			String thenIf = ifStmt.getThenStmt().toString();
			if(!thenIf.contains("new ") && condicao.contains(parametro.getName().toString())){
				/** achou um parametro sendo usado no condicional */
				return ifStmt;
			}
		}
		return null;		
	}
	
	public IfStmt temParametroNoIf2(NodeList<com.github.javaparser.ast.body.Parameter> parametrosMetodo, IfStmt ifStmt){
		for (com.github.javaparser.ast.body.Parameter parametro : parametrosMetodo) {
			String condicao = ifStmt.getCondition().toString();
			String thenIf = ifStmt.getThenStmt().toString();
			if(thenIf.contains("new ") && condicao.contains(parametro.getName().toString())){
				/** achou um parametro sendo usado no condicional */
				return ifStmt;
			}
		}
		return null;		
	}	
	

	/**
	 * @return the tipoClasse
	 */
	public String getTipoClasse() {
		return tipoClasse;
	}

	/**
	 * @param tipoClasse the tipoClasse to set
	 */
	public void setTipoClasse(String tipoClasse) {
		this.tipoClasse = tipoClasse;
	}
	
	/**
	 * @return the caminhoClasse
	 */
	public String getCaminhoClasse() {
		return caminhoClasse;
	}

	/**
	 * @param caminhoClasse the caminhoClasse to set
	 */
	public void setCaminhoClasse(String caminhoClasse) {
		this.caminhoClasse = caminhoClasse;
	}
	
	
	public CompilationUnit getCu() {
		return cu;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
	}

	
	public String getPacoteClasse() {
		return pacoteClasse;
	}

	public void setPacoteClasse(String pacoteClasse) {
		this.pacoteClasse = pacoteClasse;
	}

	public File getArquivoClasse() {
		return arquivoClasse;
	}

	public void setArquivoClasse(File arquivoClasse) {
		this.arquivoClasse = arquivoClasse;
	}
	
	/**
	 * @return the nomeClasse
	 */
	public String getNomeClasse() {
		return nomeClasse;
	}

	/**
	 * @param nomeClasse the nomeClasse to set
	 */
	public void setNomeClasse(String nomeClasse) {
		this.nomeClasse = nomeClasse;
	}
	
}