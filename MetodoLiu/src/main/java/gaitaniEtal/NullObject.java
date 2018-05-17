package gaitaniEtal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import utilitarios.Padrao;

public class NullObject extends Padrao {

	/**
	 * nome avaliado perante o if onde vai ser criado a classe Null + nomeObjeto
	 */
	private String nomeObjeto;

	public NullObject(String caminho) {
		this.setCaminho(caminho);
	}

	@Override
	/**
	 * deve verificar se tem if comparando em null perante a um objeto sendo
	 * verificado
	 */
	public Map<MethodDeclaration, List<Statement>> analisador() {
		Map<MethodDeclaration, List<Statement>> mapaMetodosAnalisados = new HashMap<>();
		List<MethodDeclaration> metodos = this.getLeitor().metodosDeclaradosJavaParser();
		for (MethodDeclaration metodo : metodos) {
			List<Statement> instrucoesIf = new ArrayList<>();

			/** if abaixo se existe if dentro do método else continue */
			/** se o tipo não for interface ele pode ter métodos */
			if (!this.getLeitor().getTipoClasse().equals("interface")
					&& !this.getLeitor().getTipoClasse().equals("abstract")) {
				List<Statement> instrucoesMetodo = this.getLeitor().linhasMetodo(metodo.getName().toString());

				if (!instrucoesMetodo.isEmpty()) {
					/** para cada if faça */
					for (Statement var : instrucoesMetodo) {
						if (var instanceof IfStmt) {
							IfStmt condicional = this.getLeitor().temNewNoIf((IfStmt) var);
							if (condicional != null) {
								instrucoesIf.add(condicional);
							} else {
								break;
							}
						}
					}
				}
			}
			if (instrucoesIf != null && !instrucoesIf.isEmpty()) {
				mapaMetodosAnalisados.put(metodo, instrucoesIf);
			}
		}
		return mapaMetodosAnalisados;
	}

	/**
	 * cria o objeto Null + nome objeto e uma classe abstrata para servir como pai
	 * da classe nula e da classe padrão
	 */
	public void modificaClasse(MethodDeclaration metodoDeclarado, List<Statement> instrucoesIf) throws IOException {
		/** precisa criar antes a classe abstract - com Abstract + nome Objeto */
		IfStmt ifStmt1 = instrucoesIf.get(0).asIfStmt();
		String campoIf = ifStmt1.getCondition().getChildNodes().get(0).toString();
		FieldDeclaration campoClasse = this.getLeitor().camposClasse().stream()
				.filter(l -> l.getVariable(0).getNameAsString().equals(campoIf)).findAny().orElse(null);
		this.nomeObjeto = campoClasse.getVariable(0).getTypeAsString();

		/**
		 * buscando métodos da classe encontrada no if para voltar list e usar no
		 * abstract e na classe Null...
		 */
		CompilationUnit cu = JavaParser.parse(new File(this.getCaminho() + "\\" + nomeObjeto + ".java"));
		ClassOrInterfaceDeclaration type = (ClassOrInterfaceDeclaration) cu.getType(0);

		NodeList<BodyDeclaration<?>> members = cu.getType(0).getMembers();

		// modificadores em public
		EnumSet<Modifier> modifiers = EnumSet.of(Modifier.PUBLIC);
		modifiers.add(Modifier.PUBLIC);

		/** criando classe abstrata */

		String nomeClasse = "Abstract" + nomeObjeto;
		boolean arquivoAbstratoExiste = new File(this.getCaminho() + "\\" + nomeClasse).exists();
		CompilationUnit cu1 = null;
		ClassOrInterfaceDeclaration type1 = null;
		if (!arquivoAbstratoExiste) {
			cu1 = new CompilationUnit();
			type1 = cu1.addClass(nomeClasse);
			/** passa pelos metodos da classe objeto */
			for (BodyDeclaration<?> member : members) {
				if (member instanceof MethodDeclaration) {
					MethodDeclaration method0 = (MethodDeclaration) member;// método original

					MethodDeclaration method1 = new MethodDeclaration();
					method1.setName(method0.getNameAsString());
					method1.setType(method0.getType());
					method1.setModifiers(method0.getModifiers());
					method1.setAbstract(true);
					method1.setBody(new BlockStmt());// deixa o corpo do método vazio
					type1.addMember(method1);
				}
			}
			type1.setAbstract(true);
		}

		/** métodos extras criados especificamente pela instrução do Gaitani */
		MethodDeclaration methodExtra1 = new MethodDeclaration();
		methodExtra1.setName("isNull");
		methodExtra1.setType("boolean");
		methodExtra1.setModifiers(modifiers);
		methodExtra1.setAbstract(true);

		MethodDeclaration methodExtra2 = new MethodDeclaration();
		methodExtra2.setName("getReference");
		methodExtra2.setType(nomeObjeto);
		methodExtra2.setModifiers(modifiers);
		methodExtra2.setAbstract(true);

		MethodDeclaration methodExtra3 = new MethodDeclaration();
		methodExtra3.setName("assertNotNull");
		methodExtra3.setType("void");
		methodExtra3.setModifiers(modifiers);
		methodExtra3.setAbstract(true);
		methodExtra3.setThrownExceptions(
				NodeList.nodeList(new ClassOrInterfaceType(NullPointerException.class.getSimpleName())));
		if (!arquivoAbstratoExiste) {
			type1.addMember(methodExtra1);
			type1.addMember(methodExtra2);
			type1.addMember(methodExtra3);
			this.gravarConteudo(this.getCaminho() + "/" + nomeClasse + ".java", cu1.toString());
		}
		/**
		 * criar o objeto null + nome com mesmos atributos e métodos da classe analisada
		 * no null
		 */
		String nomeClasse2 = "Null" + nomeObjeto;
		CompilationUnit cu2 = null;
		ClassOrInterfaceDeclaration type2 = null;
		boolean arquivoNullExiste = new File(this.getCaminho() + "\\" + nomeClasse2).exists();
		if (!arquivoNullExiste) {
			cu2 = new CompilationUnit();
			type2 = cu2.addClass(nomeClasse2);
			/** passa pelos metodos da classe objeto */
			for (BodyDeclaration<?> member : members) {
				if (member instanceof MethodDeclaration) {
					MethodDeclaration method0 = (MethodDeclaration) member;// método original

					MethodDeclaration method1 = new MethodDeclaration();
					method1.setName(method0.getNameAsString());
					method1.setType(method0.getType());
					method1.setModifiers(method0.getModifiers());
					BlockStmt body = new BlockStmt();
					if (!method0.getType().toString().equals("void")) {
						if (method0.getType().toString().equals("double") || method0.getType().toString().equals("int")
								|| method0.getType().toString().equals("float")) {
							body.addStatement("return 0;");
						} else if (method0.getType().toString().equals("boolean")) {
							body.addStatement("return true;");
						} else {
							body.addStatement("return null;");
						}
					} else {
						body.addStatement("throw new UnsupportedOperationException();");
					}
					method1.setBody(body);
					type2.addMember(method1);
				}
			}
			type2.addExtendedType(nomeClasse);
		}

		/**
		 * métodos extras criados especificamente pela instrução do Gaitani -
		 * modificações para NullObject
		 */
		methodExtra1.setAbstract(false);
		methodExtra1.setBody(new BlockStmt().addStatement("return true;"));

		methodExtra2.setAbstract(false);
		methodExtra2.setBody(new BlockStmt().addStatement("return null;"));

		methodExtra3.setAbstract(false);
		methodExtra3.setBody(new BlockStmt().addStatement("throw new UnsupportedOperationException();"));
		methodExtra3.setThrownExceptions(
				NodeList.nodeList(new ClassOrInterfaceType(NullPointerException.class.getSimpleName())));

		if (!arquivoNullExiste) {
			type2.addMember(methodExtra1);
			type2.addMember(methodExtra2);
			type2.addMember(methodExtra3);
			this.gravarConteudo(this.getCaminho() + "/" + nomeClasse2 + ".java", cu2.toString());
		}

		/** modificar a classe objeto adicionando métodos extra */
		methodExtra1.setBody(new BlockStmt().addStatement("return false;"));
		type.addMember(methodExtra1);

		methodExtra2.setBody(new BlockStmt().addStatement("return this;"));
		type.addMember(methodExtra2);

		methodExtra3.setBody(new BlockStmt());
		type.addMember(methodExtra3);

		/**
		 * finalmente voltando a classe analisada, e retirando todos os if null e usando
		 * AbstractObject
		 */
		ClassOrInterfaceDeclaration classeAnalisada = (ClassOrInterfaceDeclaration) this.getLeitor().getCu().getType(0);

		/**
		 * modifica o campo da classe para ao invés de instanciar em nulo pegar new da
		 * classe NullObject
		 */
		FieldDeclaration campoClasseAnalisada = classeAnalisada.getFieldByName(campoIf).get();
		campoClasseAnalisada.getVariable(0).setType(nomeClasse);
		campoClasseAnalisada.getVariable(0).setInitializer("new " + nomeClasse2 + "()");

		// modificadores em private
		EnumSet<Modifier> modifiers2 = EnumSet.of(Modifier.PRIVATE);
		modifiers2.add(Modifier.PRIVATE);

		/** método com if para decidir se usa o NullObject ou o Object */
		String campoIfMaiusculo = campoIf.substring(0, 1).toUpperCase() + campoIf.substring(1);
		MethodDeclaration method1 = new MethodDeclaration();
		method1.setName("assignTo" + campoIfMaiusculo);
		method1.setType(nomeClasse);
		method1.setModifiers(modifiers2);
		method1.addParameter(nomeClasse, campoIf);
		BlockStmt body = new BlockStmt();
		body.addStatement(
				"if(" + campoIf + " == null){return new " + nomeClasse2 + "();}else{return " + campoIf + ";}");
		method1.setBody(body);
		classeAnalisada.addMember(method1);

		/** retirar os ifs e jogar direto ao thenif nos métodos */
		for (Statement instrucaoif : instrucoesIf) {
			IfStmt ifStmt = (IfStmt) instrucaoif;
			/*** renova a  referencia  a memória  de  metodoDeclarado  para  poder alterado*/
			metodoDeclarado = classeAnalisada.getMethodsByName(metodoDeclarado.getNameAsString()).get(0);
			BlockStmt corpo = metodoDeclarado.getBody().get();
			int i = 0;
			int qtd = corpo.getStatements().size();
			for (Statement statementMetodo : corpo.getStatements()) {
				if (statementMetodo instanceof IfStmt) {// a instrução é um if aqui tiro ela do método
					String oqueFicoudoif = ifStmt.getThenStmt().toString();
					corpo.setStatement(i, JavaParser.parseStatement(oqueFicoudoif));
					break;
				}
				++i;
			}
			System.out.println(corpo.getStatements());
			metodoDeclarado.setBody(corpo);
		}
		this.gravarConteudo(this.getCaminho() + "/" + this.getLeitor().getNomeClasse() + ".java",
				this.getLeitor().getCu().toString());
	}
}
