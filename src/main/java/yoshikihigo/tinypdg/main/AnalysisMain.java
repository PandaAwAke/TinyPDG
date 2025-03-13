package yoshikihigo.tinypdg.main;

import cn.hutool.cache.Cache;
import cn.hutool.cache.CacheUtil;
import cn.hutool.core.io.FileUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.cli.*;
import org.eclipse.jdt.core.dom.CompilationUnit;
import yoshikihigo.tinypdg.ast.PEASTVisitor;
import yoshikihigo.tinypdg.cfg.CFG;
import yoshikihigo.tinypdg.cfg.node.CFGNodeFactory;
import yoshikihigo.tinypdg.main.model.DefUseJson;
import yoshikihigo.tinypdg.pdg.PDG;
import yoshikihigo.tinypdg.pdg.node.PDGNodeFactory;
import yoshikihigo.tinypdg.pe.MethodInfo;

import java.util.Map;
import java.util.TreeMap;

public class AnalysisMain {

    private final static int CACHE_CAPACITY = 64;

    /**
     * The cache of the mapping: source code -> PEASTVisitor
     */
    private final static Cache<String, PEASTVisitor> sourceASTVisitorCache = CacheUtil.newFIFOCache(CACHE_CAPACITY);

    /**
     * Get the PEASTVisitor of the source code. Used for generating CFG, DFG and PDG.
     * @param source The source code (CompilationUnit)
     * @return The ASTVisitor for the source
     */
    private static PEASTVisitor getASTVisitorBySource(String source) {
        if (!sourceASTVisitorCache.containsKey(source)) {
            CompilationUnit ast = PEASTVisitor.createAST(source);
            PEASTVisitor visitor = new PEASTVisitor(ast);
            ast.accept(visitor);
            sourceASTVisitorCache.put(source, visitor);
        }
        return sourceASTVisitorCache.get(source);
    }

    /**
     * Get the CFG by the CompilationUnit AST node.
     * @param source The source code
     * @return CFGs (Control Flow Graphs) of methods in the ast
     */
    public static Map<MethodInfo, CFG> getCFG(String source) {
        PEASTVisitor astVisitor = getASTVisitorBySource(source);

        Map<MethodInfo, CFG> result = new TreeMap<>();
        for (final MethodInfo method : astVisitor.getMethods()) {
            final CFG cfg = new CFG(method, new CFGNodeFactory());
            cfg.build();
//            cfg.removeSwitchCases();
//            cfg.removeJumpStatements();

            result.put(method, cfg);
        }
        return result;
    }

    /**
     * Get the DDG by the CompilationUnit AST node. The returned PDG only contains data dependency edges.
     * @param source The source code
     * @return DDGs (Data Dependency Graphs) of methods in the ast
     */
    public static Map<MethodInfo, PDG> getDDG(String source) {
        PEASTVisitor astVisitor = getASTVisitorBySource(source);

        Map<MethodInfo, PDG> result = new TreeMap<>();
        for (final MethodInfo method : astVisitor.getMethods()) {
            // Only build data dependency here
            final PDG pdg = new PDG(method,
                    new PDGNodeFactory(),
                    new CFGNodeFactory(),
                    false, true, false);
            pdg.build();

            result.put(method, pdg);
        }
        return result;
    }

    /**
     * Get the PDG by the CompilationUnit AST node.
     * @param source The source code
     * @return PDGs (Program Dependency Graphs) of methods in the ast,
     *         a PDG usually consists of DFG (Data Dependency Graph) and CFG
     */
    public static Map<MethodInfo, PDG> getPDG(String source) {
        PEASTVisitor astVisitor = getASTVisitorBySource(source);

        Map<MethodInfo, PDG> result = new TreeMap<>();
        for (final MethodInfo method : astVisitor.getMethods()) {
            final PDG pdg = new PDG(method,
                    new PDGNodeFactory(),
                    new CFGNodeFactory(),
                    true, true, true);
            pdg.build();

            result.put(method, pdg);
        }
        return result;
    }

    public static void main(String[] args) {
        // Options
        Options options = new Options();
        options.addOption("t", "type", true, "The analysis type, currently only \"ddg\" was supported");
        options.addOption("f", "filePath", true, "The java source file (compilation unit) to parse");

        // Parse the options
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);

            // Get values of the options
            String type = cmd.getOptionValue("type");
            String sourceFilePath = cmd.getOptionValue("filePath");
            String source = FileUtil.readUtf8String(sourceFilePath);

            assert type.equals("ddg");

            Map<MethodInfo, PDG> ddgs = getDDG(source);

            // Transfer to json
            Map<String, DefUseJson> resultMap = new TreeMap<>();
            for (Map.Entry<MethodInfo, PDG> entry : ddgs.entrySet()) {
                MethodInfo method = entry.getKey();
                resultMap.put(method.getName() + "#" + method.getStartLine(), DefUseJson.createFromPDG(entry.getValue()));
            }

            // Print json
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(resultMap);
            System.out.println(jsonString);

        } catch (ParseException e) {
            System.err.println("Error parsing command line arguments: " + e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("Main", options);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }


}
