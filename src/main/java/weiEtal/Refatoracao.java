
package weiEtal;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.Statement;

import utilitarios.Leitor;

/**
 * Refatoracao para ler projetos e aplicar método de refatoração
 */
public class Refatoracao {

	/**
	 * le diretório do projeto ve o que é classe java e separa para ler em
	 * refatoração
	 * 
	 * @param caminho
	 *            - para apontar onde está o projeto perante o sistema operacional
	 */
	public void lerProjeto(String caminho) {
		try {
			File d = new File(caminho);
			File[] files = d.listFiles();
			if (files != null && files.length > 0) {
				Strategy st = new Strategy(caminho);// para só instanciar caso ache algo no diretório
				Factory fa = new Factory(caminho);
				int indiceClasse = 0;
				for (File arquivo : files) {
					if (arquivo.isDirectory()) {
						System.out.println("Diretório: " + arquivo.getName());
						/**
						 * usa recurso para entrar dentro das pastas - isso ajuda com pacotes de código
						 */
						this.lerProjeto(caminho + "/" + arquivo.getName());
					} else if (!arquivo.isDirectory() && arquivo.getName().endsWith(".java")
							&& !arquivo.getName().contains("Test")) {
						if (!arquivo.getName().contains("ConcreteStrategy")
								&& !arquivo.getName().contains("Strategy.java")) {
							/** identificação de possíveis classe aqui */
							System.out.println("Arquivo: " + arquivo.getName());
							String nomeClasse[] = arquivo.getName().split(".java");
							Leitor l = new Leitor(nomeClasse[0], arquivo.toPath().toString());
							if (!l.erro) {
								System.out.println(" -- Começando análise da classe --> " + l.getNomeClasse());

								System.out.println("\n == Começando análise do Strategy ==");
								st.setLeitor(l);
								Map<MethodDeclaration, List<Statement>> mapaMetodos = st.analisador();
								if (mapaMetodos != null && !mapaMetodos.isEmpty()) {
									for (Map.Entry<MethodDeclaration, List<Statement>> entrada : mapaMetodos
											.entrySet()) {
										st.modificaClasse(entrada.getKey(), entrada.getValue());
									}
								}

								System.out.println("\n == Começando análise do Factory Method ==");
								fa.setLeitor(l);
								Map<MethodDeclaration, List<Statement>> mapaMetodos2 = fa.analisador();
								if (mapaMetodos2 != null && !mapaMetodos2.isEmpty()) {
									for (Map.Entry<MethodDeclaration, List<Statement>> entrada : mapaMetodos2
											.entrySet()) {
										fa.modificaClasse(entrada.getKey(), entrada.getValue());
									}
								}

								++indiceClasse;
							}
						}
					} else {
						continue;
					}
				}
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Erro causado por: " + ex.getMessage());
		}
	}

}