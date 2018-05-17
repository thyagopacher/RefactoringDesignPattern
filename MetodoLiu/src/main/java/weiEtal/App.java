package weiEtal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Utility;
import org.apache.bcel.generic.Type;
import org.apache.bcel.util.ByteSequence;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.TypeParameter;

import utilitarios.Leitor;

/**
 * Classe para testar a refatoração com métodos reflexivos
 * @author Thyyago Henrique Pacher
 */
public class App {

    private App instance;
    private String valor;

    public App() {

    }

    private App(String valor) {

    }

    private void metodo1() {
        boolean res = false;
        System.out.println(" -- verificação do método 01 --");
        instance = new App();
        if (res == true) {
            res = false;
        }
        if (instance != null) {
            instance = new App();
        }
        if (instance == null) {
            instance = new App();
        }
    }

    private boolean metodo2(boolean res) {
        return true;
    }

    /** exemplo para carregar classes de outro diretório em java ele pega só arquivos .class */
    public void carregaClasseOutraURL() {
        try {
            File file = new File("c:\\other_classes\\");
            //convert the file to URL format
            URL url = file.toURI().toURL();
            URL[] urls = new URL[] { url };
            //load this folder into Class loader
            ClassLoader cl = new URLClassLoader(urls);
            //load the Address class in 'c:\\other_classes\\'
            Class cls = cl.loadClass("com.mkyong.io.Address");
            //print the location from where this class was loaded
            ProtectionDomain pDomain = cls.getProtectionDomain();
            CodeSource cSource = pDomain.getCodeSource();
            URL urlfrom = cSource.getLocation();
            System.out.println(urlfrom.getFile());

            System.out.println("Nome da classe carregada: " + cls.getSimpleName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void verificarProjetoExemplo() {
//        Refatoracao rf = new Refatoracao();
//        rf.lerProjeto("C:\\programa-java\\exemplo-cavado-Liu-factory");
        
    	gaitaniEtal.Refatoracao rf = new gaitaniEtal.Refatoracao();
        rf.lerProjeto("C:\\programa-java\\exemplo-cavada-gaitani-nullobject");        
    }

    /**
     *  le um arquivo .java transforma ele e adiciona coisas ao método e por fim reescreve ele perante o arquivo
     * @exception ex não ache o arquivo
     */
    public void modificaClasse() {
        try {
            String camArquivo = "C:\\other_classes\\com\\mkyong\\io\\Address.java";
            FileInputStream file = new FileInputStream(camArquivo);
            CompilationUnit cu = JavaParser.parse(file);
            // change the methods names and parameters
            changeMethods(cu);
            
            // prints the changed compilation unit
            System.out.println(cu.toString());
            FileWriter fileWriter = new FileWriter(camArquivo);
            fileWriter.write(cu.toString());
            fileWriter.flush();
            fileWriter.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void changeMethods(CompilationUnit cu) {
        // Go through all the types in the file
        NodeList<TypeDeclaration<?>> types = cu.getTypes();
        for (TypeDeclaration<?> type : types) {
            // Go through all fields, methods, etc. in this type
            NodeList<BodyDeclaration<?>> members = type.getMembers();
            for (BodyDeclaration<?> member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    changeMethod(method);
                }
            }
        }
    }

    private static void changeMethod(MethodDeclaration n) {
        // change the name of the method to upper case
        n.setName(n.getNameAsString().toLowerCase());

        /** cria comentários */
        n.setBlockComment("Método refatorado automaticamente");

        BlockStmt block = n.getBody().get();
        System.out.println("Tem corpo: " + block.toBlockStmt().isPresent());
        n.setBody(block);

        //só pode setar um parametro caso ele exista, senão ignora aqui.
        if (n.getParameters().size() > 0) {
            n.setParameter(0, new com.github.javaparser.ast.body.Parameter(new TypeParameter("int"), "valor1"));
        }else{
            //caso não tenha parametros ele adiciona um do tipo int 
            n.addParameter(int.class, "valor1");
        }

        // troca tipo para final
        n.setFinal(true);
    }

    public static void main(String[] args) {
        App app = new App();
        app.verificarProjetoExemplo();        
    }
}
