package utilitarios;


import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.Statement;

public class Padrao {

	private List<IfStmt> condicoesComIfElse = new ArrayList<>();

	private Leitor leitor;
	private String caminho;

	public Map<MethodDeclaration, List<Statement>> analisador() {
		return null;
	}

	public void modificaClasse(MethodDeclaration metodoDeclarado, List<Statement> instrucoesIf) throws IOException {

	}

	public boolean temElse(IfStmt ifStmt) {
		this.condicoesComIfElse.add(ifStmt);
		if (ifStmt.getElseStmt().isPresent() && ifStmt.getElseStmt().get().isIfStmt()) {
			IfStmt elseStmt = (IfStmt) ifStmt.getElseStmt().get();
			return this.temElse(elseStmt);
		} else {
			return false;
		}
	}

	/**
	 * grava o conteúdo da classe no arquivo correspondente
	 * @param caminhoArquivo - caminho url aonde vai gravar
	 * @param texto - string que vai ser gravada no arquivo
	 * */
	public void gravarConteudo(String caminhoArquivo, String texto) {

		try {
			FileWriter fileWriter1 = new FileWriter(caminhoArquivo);
			fileWriter1.write(texto);
			fileWriter1.flush();
			fileWriter1.close();
		} catch (IOException ex) {
			throw new IllegalStateException("Erro ao gravar arquivo - causado por: " + ex.getMessage());
		}

	}

	public List<IfStmt> getCondicoesComIfElse() {
		return condicoesComIfElse;
	}

	public void setCondicoesComIfElse(List<IfStmt> condicoesComIfElse) {
		this.condicoesComIfElse = condicoesComIfElse;
	}

	public Leitor getLeitor() {
		return leitor;
	}

	public void setLeitor(Leitor leitor) {
		this.leitor = leitor;
	}

	public String getCaminho() {
		return caminho;
	}

	public void setCaminho(String caminho) {
		this.caminho = caminho;
	}
}
