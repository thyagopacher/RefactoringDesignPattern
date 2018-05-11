package utilitarios;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.util.ByteSequence;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
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
	private Class<?> caller;
	private JavaClass classeInstanciada = null;
	private CompilationUnit cu = null;
	private ClassOrInterfaceDeclaration classeJavaParser;
	private String tipoClasse;
	private String caminhoClasse;
	private File arquivoClasse;
	public boolean erro = false;

	public Leitor(){
		
	}

	public Leitor(String nomeClasse, String caminho) {
		try {
			this.caminhoClasse = caminho;
			/**le e nós define o nome do pacote - essencial para Class.forName  */
			BufferedReader br = new BufferedReader(new FileReader(caminho));
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

			br.close();
			this.nomeClasse = nomeClasse;
			this.carregaClasseOutraURL(caminho);
		} catch (Exception ex) {
			this.erro = true;
			System.out.println("Erro causado por: " + ex.getMessage() + " no caminho: " + caminho);
		}
	}

	/**
	 *  carrega classes apontados em outro diretório
	 * @param caminho onde o arquivo se encontra em .java
	 */
	public void carregaClasseOutraURL(String caminho) {
		try {
			String separa_caminho[] = caminho.split("src");
			//começando a procura na pasta target padrão para IDE Visual Studio Code
			String caminhoClasse = separa_caminho[0] + "target\\classes\\";
			arquivoClasse = new File(caminhoClasse);
			if (!arquivoClasse.exists()) {//caso a pasta não exista então procura na build  - padrão para IDE netbeans e eclipse
				arquivoClasse = new File(separa_caminho[0] + "build\\classes\\");
				if (!arquivoClasse.exists()) {//caso a pasta não exista então procura na bin
					arquivoClasse = new File(separa_caminho[0] + "bin\\");
				}
			}
			//convert the file to URL format
			URL url = arquivoClasse.toURI().toURL();
			URL[] urls = new URL[] { url };
			//load this folder into Class loader
			ClassLoader cl = new URLClassLoader(urls);
			//load the Address class in 'c:\\other_classes\\'
			String classeCarregada = this.pacoteClasse + "." + this.nomeClasse;
			this.caller = cl.loadClass(classeCarregada);
			if (this.classeInstanciada == null) {
				this.classeInstanciada = Repository.lookupClass(caller);
			}

			//print the location from where this class was loaded
			ProtectionDomain pDomain = this.caller.getProtectionDomain();
			CodeSource cSource = pDomain.getCodeSource();
			URL urlfrom = cSource.getLocation();

			System.out.println(" -- Nome da classe carregada: " + this.caller.getSimpleName());
		} catch (Exception ex) {
			throw new IllegalStateException(
					"Erro ao carregar .class causado por: " + ex.getMessage() + " no caminho: " + caminho);
		}
	}

	public Class<?> classe() {
		return caller;
	}

	/** 
	 * retorna os campos da classe
	 */
	public Field[] campos() {
		try {
			return caller.getDeclaredFields();
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

	/** 
	 * retorna os construtores da classe
	 */
	public Constructor<?>[] construtor() {
		try {
			Constructor<?>[] constructors = caller.getConstructors();
			return constructors;
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

	/** 
	 * retorna os construtores da classe
	 */
	public Constructor<?>[] construtoresDeclarados() {
		try {
			Constructor<?>[] constructors = caller.getDeclaredConstructors();
			return constructors;
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

	/** retorna os métodos da classe */
	public Method[] metodos() {
		try {
			return caller.getMethods();
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

	public List<MethodDeclaration> metodosDeclaradosJavaParser() {
		List<MethodDeclaration> metodos = new ArrayList<>();
		try {
			this.arquivoClasse = new File(this.caminhoClasse);
			cu = JavaParser.parse(arquivoClasse);
			// Go through all the types in the file
			NodeList<TypeDeclaration<?>> types = cu.getTypes();
			this.setClasseJavaParser((ClassOrInterfaceDeclaration) types.get(0));
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
	
	/** retorna os métodos da classe */
	public Method[] metodosDeclarados() {
		try {
			return caller.getDeclaredMethods();
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

	
	
	public TypeVariable<?>[] parametros() {
		try {
			return caller.getTypeParameters();
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
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

	public String modificador(int qualModificador) {
		String res = "";
		if (qualModificador == Modifier.PUBLIC) {
			res = "public";
		} else if (qualModificador == Modifier.PRIVATE) {
			res = "private";
		} else if (qualModificador == Modifier.PROTECTED) {
			res = "protected";
		} else if (qualModificador == Modifier.STATIC) {
			res = "static";
		} else if (qualModificador == Modifier.VOLATILE) {
			res = "volatile";
		} else if (qualModificador == Modifier.FINAL) {
			res = "final";
		} else if (qualModificador == Modifier.NATIVE) {
			res = "native";
		}
		return res;
	}

	public org.apache.bcel.classfile.Method metodoBcel(Method metodo) {
		try {
			return classeInstanciada.getMethod(metodo);
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

	/**
	 * retorna as instruções perante o método
	 */
	public List<String> instrucoesMetodo(Method metodo) {
		return this.instrucoesMetodo(this.metodoBcel(metodo));
	}

	public List<String> instrucoesMetodo(org.apache.bcel.classfile.Method metodoBcel) {
		try {
			List<String> instrucoes = new ArrayList<String>();
			if (metodoBcel.getCode() != null) {
				byte[] code = metodoBcel.getCode().getCode();
				ByteSequence stream = new ByteSequence(code);
				while (stream.available() > 0) {
					instrucoes.add(Utility.codeToString(stream, metodoBcel.getConstantPool()));
				}
			}
			return instrucoes;
		} catch (Exception ex) {
			throw new IllegalStateException(
					"Erro causado por: " + ex.getMessage() + " - método: " + metodoBcel.getName());
		}
	}

	public List<String> retornaIf(Method metodo) {
		org.apache.bcel.classfile.Method metodoBcel = this.metodoBcel(metodo);
		List<String> instrucoes = this.instrucoesMetodo(metodoBcel).stream()
				.filter(line -> line.equals("ifnonnull") || line.equals("ifeq") || line.equals("ifnull"))
				.collect(Collectors.toList());
		return instrucoes;
	}

	/**
	 * pesquisa se tem algum if nas instruções
	 */
	public List<String> retornaIf(List<String> instrucoes) {
		List<String> resultado = instrucoes.stream()
				.filter(line -> line.equals("ifnonnull") || line.equals("ifeq") || line.equals("ifnull") || line.equals("ifle"))
				.collect(Collectors.toList());
		return resultado;
	}

	public void checkTable(LineNumberTable table) {
		System.out.println("line number table has length " + table.getTableLength());
		LineNumber[] entries = table.getLineNumberTable();
		int lastBytecode = -1;
		for (int i = 0; i < entries.length; ++i) {
			LineNumber ln = entries[i];
			System.out.println("Entry " + i + ": pc=" + ln.getStartPC() + ", line=" + ln.getLineNumber());
			int pc = ln.getStartPC();
			if (pc <= lastBytecode) {
				throw new IllegalStateException("LineNumberTable is not sorted");
			}
		}
	}

	public void analyze(org.apache.bcel.classfile.Method method) {
		Code code = method.getCode();
		if (code != null) {
			byte[] instructionList = code.getCode();
			System.out.println(" - Método de análise");
			final InstructionList list = new InstructionList(instructionList);
			System.out.println(list.getInstructionPositions());
		}
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
	 * retorna as linhas do método de maneira legivel 
	 */
	public List<Statement> linhasMetodo(String nomeMetodo) {
		try {
			this.arquivoClasse = new File(this.caminhoClasse);
			CompilationUnit cu = JavaParser.parse(arquivoClasse);
			// Go through all the types in the file
			NodeList<TypeDeclaration<?>> types = cu.getTypes();
			for (TypeDeclaration<?> type : types) {
				// Go through all fields, methods, etc. in this type
				NodeList<BodyDeclaration<?>> members = type.getMembers();
				for (BodyDeclaration<?> member : members) {
					if (member instanceof MethodDeclaration) {
						MethodDeclaration method = (MethodDeclaration) member;
						String nomeMetodoLocal = method.getName().toString();
						if (nomeMetodoLocal.equals(nomeMetodo)) {
						
							List<Statement> retorno = method.getBody().get().getStatements();
							if(retorno != null && !retorno.isEmpty()){
								return method.getBody().get().getStatements();
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
	
	public JavaClass getClasseInstanciada() {
		return classeInstanciada;
	}

	public void setClasseInstanciada(JavaClass classeInstanciada) {
		this.classeInstanciada = classeInstanciada;
	}

	public CompilationUnit getCu() {
		return cu;
	}

	public void setCu(CompilationUnit cu) {
		this.cu = cu;
	}

	public ClassOrInterfaceDeclaration getClasseJavaParser() {
		return classeJavaParser;
	}

	public void setClasseJavaParser(ClassOrInterfaceDeclaration classeJavaParser) {
		this.classeJavaParser = classeJavaParser;
	}	
	
	
	public String getPacoteClasse() {
		return pacoteClasse;
	}

	public void setPacoteClasse(String pacoteClasse) {
		this.pacoteClasse = pacoteClasse;
	}

	public Class<?> getCaller() {
		return caller;
	}

	public void setCaller(Class<?> caller) {
		this.caller = caller;
	}

	public File getArquivoClasse() {
		return arquivoClasse;
	}

	public void setArquivoClasse(File arquivoClasse) {
		this.arquivoClasse = arquivoClasse;
	}	
}
