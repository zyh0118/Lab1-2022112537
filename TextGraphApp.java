import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import javax.swing.*;


//dbasjkdh
？？
// --- Graph Representation ---
class DirectedGraph {
    // Adjacency list: source -> (target -> weight)
    private final Map<String, Map<String, Integer>> adjacencyList;
    private final Set<String> nodes;

    public DirectedGraph() {
        adjacencyList = new HashMap<>();
        nodes = new HashSet<>();
    }

    /**
     * Adds a node to the graph.
     * @param node The word to add as a node.
     */
    public void addNode(String node) {
        nodes.add(node);
        adjacencyList.putIfAbsent(node, new HashMap<>());
    }

    /**
     * Adds or increments the weight of a directed edge.
     * @param source The source node (word).
     * @param target The target node (word).
     */
    public void addEdge(String source, String target) {
        // Ensure nodes exist
        addNode(source);
        addNode(target);

        // Get the neighbors of the source node
        Map<String, Integer> neighbors = adjacencyList.get(source);

        // Increment weight or add edge with weight 1
        neighbors.put(target, neighbors.getOrDefault(target, 0) + 1);
    }

    /**
     * Checks if a node exists in the graph.
     * @param node The word to check.
     * @return true if the node exists, false otherwise.
     */
    public boolean containsNode(String node) {
        return nodes.contains(node);
    }

    /**
     * Gets the weight of an edge.
     * @param source The source node.
     * @param target The target node.
     * @return The weight of the edge, or 0 if the edge doesn't exist.
     */
    public int getWeight(String source, String target) {
        return adjacencyList.getOrDefault(source, Collections.emptyMap()).getOrDefault(target, 0);
    }

    /**
     * Gets the outgoing neighbors of a node.
     * @param node The source node.
     * @return A map of target nodes to edge weights, or an empty map if the node has no neighbors or doesn't exist.
     */
    public Map<String, Integer> getNeighbors(String node) {
        return adjacencyList.getOrDefault(node, Collections.emptyMap());
    }

     /**
     * Gets all nodes that have an edge pointing *to* the given node.
     * @param targetNode The node whose incoming neighbors are sought.
     * @return A set of source nodes pointing to targetNode.
     */
    public Set<String> getInNeighbors(String targetNode) {
        Set<String> inNeighbors = new HashSet<>();
        if (!nodes.contains(targetNode)) {
            return inNeighbors; // Target node doesn't exist
        }
        for (Map.Entry<String, Map<String, Integer>> entry : adjacencyList.entrySet()) {
            String sourceNode = entry.getKey();
            Map<String, Integer> neighbors = entry.getValue();
            if (neighbors.containsKey(targetNode)) {
                inNeighbors.add(sourceNode);
            }
        }
        return inNeighbors;
    }

    /**
     * Gets the set of all nodes in the graph.
     * @return An unmodifiable set of nodes.
     */
    public Set<String> getAllNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    /**
     * Generates a string representation of the graph in Graphviz DOT format.
     * @param highlightedPath Nodes in this path will be highlighted. Can be null.
     * @return The graph structure in DOT format as a string.
     */
    public String toDotFormat(List<String> highlightedPath) {
        StringBuilder dot = new StringBuilder("digraph G {\n");
        dot.append("  node [shape=box, style=rounded];\n"); // Style nodes

        Set<String> pathNodes = highlightedPath == null ? Collections.emptySet() : new HashSet<>(highlightedPath);
        Set<String> pathEdges = new HashSet<>();
        if (highlightedPath != null && highlightedPath.size() > 1) {
            for (int i = 0; i < highlightedPath.size() - 1; i++) {
                pathEdges.add(highlightedPath.get(i) + "->" + highlightedPath.get(i+1));
            }
        }

        // Define nodes first (optional, but good practice, allows styling)
        for (String node : nodes) {
            dot.append("  \"").append(node).append("\"");
            if (pathNodes.contains(node)) {
                 dot.append(" [color=red, penwidth=2.0]");
            }
             dot.append(";\n");
        }


        // Define edges
        for (Map.Entry<String, Map<String, Integer>> entry : adjacencyList.entrySet()) {
            String source = entry.getKey();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                String target = edge.getKey();
                int weight = edge.getValue();
                dot.append("  \"").append(source).append("\" -> \"").append(target).append("\"");
                dot.append(" [label=\"").append(weight).append("\"");
                 if (pathEdges.contains(source + "->" + target)) {
                    dot.append(", color=red, penwidth=2.0");
                }
                dot.append("];\n");
            }
        }

        dot.append("}\n");
        return dot.toString();
    }

     /**
     * Generates a simple text representation for the CLI.
     * @return A string describing the graph.
     */
    public String toCliString() {
        StringBuilder sb = new StringBuilder("Directed Graph (Node -> Target (Weight)):\n");
         if (nodes.isEmpty()) {
            return "Graph is empty.\n";
        }
        List<String> sortedNodes = new ArrayList<>(nodes);
        Collections.sort(sortedNodes); // Sort nodes alphabetically for consistent output

        for (String node : sortedNodes) {
            Map<String, Integer> neighbors = getNeighbors(node);
            if (neighbors.isEmpty()) {
                 sb.append(node).append(" -> (No outgoing edges)\n");
            } else {
                sb.append(node).append(" -> ");
                List<String> neighborDesc = new ArrayList<>();
                 neighbors.entrySet().stream()
                          .sorted(Map.Entry.comparingByKey()) // Sort neighbors alphabetically
                          .forEach(entry -> neighborDesc.add(entry.getKey() + "(" + entry.getValue() + ")"));
                 sb.append(String.join(", ", neighborDesc)).append("\n");
            }
        }
        return sb.toString();
    }
}

// --- Main Application with GUI ---
public class TextGraphApp extends JFrame {

    private DirectedGraph graph = null;
    private File lastOpenedFile = null;
    private String currentGraphDotString = ""; // To store the latest DOT representation
    private Map<String, Double> pageRankScores = null; // Store calculated PageRank scores
    private volatile boolean stopRandomWalk = false; // Flag to stop random walk
    private Map<String, Integer> wordCounts = null;

    // GUI Components
    private JTextArea outputArea;
    private JButton btnLoadFile, btnShowGraph, btnQueryBridge, btnGenerateText, btnShortestPath, btnPageRank, btnRandomWalk, btnStopWalk;
    private JTextField word1Input, word2Input, textInput;
    private JTextField spWord1Input, spWord2Input;
    private JLabel statusLabel;
    private JFileChooser fileChooser;

    public TextGraphApp() {
        super("Text to Directed Graph Analyzer");
        initComponents();
        layoutComponents();
        setupActions();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 700); // Adjusted size
        setLocationRelativeTo(null); // Center window
    }

    private void initComponents() {
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        btnLoadFile = new JButton("Load Text File");
        btnShowGraph = new JButton("Show Graph");
        btnQueryBridge = new JButton("Query Bridge Words");
        btnGenerateText = new JButton("Generate New Text");
        btnShortestPath = new JButton("Shortest Path");
        btnPageRank = new JButton("Calc PageRank (All)"); // Changed button text
        btnRandomWalk = new JButton("Random Walk");
        btnStopWalk = new JButton("Stop Walk");
        btnStopWalk.setEnabled(false); // Initially disabled

        word1Input = new JTextField(10);
        word2Input = new JTextField(10);
        textInput = new JTextField(30);

        spWord1Input = new JTextField(10); // <-- 新增：Shortest Path 输入框 1
        spWord2Input = new JTextField(10); // <-- 新增：Shortest Path 输入框 2


        statusLabel = new JLabel("Status: No file loaded.");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Disable buttons initially until graph is loaded
        setGraphOperationButtonsEnabled(false);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());

        // --- Top Panel: File Loading ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(btnLoadFile);
        topPanel.add(statusLabel);
        add(topPanel, BorderLayout.NORTH);

        // --- Center Panel: Output Area ---
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // --- Bottom Panel: Operations ---
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS)); // Vertical layout

        // Panel for Graph Visualization
        JPanel graphPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        graphPanel.setBorder(BorderFactory.createTitledBorder("Graph Operations"));
        graphPanel.add(btnShowGraph);
        bottomPanel.add(graphPanel);

        // Panel for Bridge Words
        JPanel bridgePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bridgePanel.setBorder(BorderFactory.createTitledBorder("Bridge Words"));
        bridgePanel.add(new JLabel("Word 1:"));
        bridgePanel.add(word1Input);
        bridgePanel.add(new JLabel("Word 2:"));
        bridgePanel.add(word2Input);
        bridgePanel.add(btnQueryBridge);
        bottomPanel.add(bridgePanel);

        // Panel for New Text Generation
        JPanel generateTextPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generateTextPanel.setBorder(BorderFactory.createTitledBorder("Generate New Text"));
       generateTextPanel.add(new JLabel("Input Text:"));
        generateTextPanel.add(textInput);
        generateTextPanel.add(btnGenerateText);
        bottomPanel.add(generateTextPanel);


        // Panel for Shortest Path
        JPanel shortestPathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shortestPathPanel.setBorder(BorderFactory.createTitledBorder("Shortest Path"));
        shortestPathPanel.add(new JLabel("Word 1:"));
        shortestPathPanel.add(spWord1Input); // Use separate input fields if needed, or reuse word1/word2
        shortestPathPanel.add(new JLabel("Word 2 (Optional):"));
        shortestPathPanel.add(spWord2Input);
        shortestPathPanel.add(btnShortestPath);
        // Reusing word1Input and word2Input for Shortest Path for simplicity
        bottomPanel.add(shortestPathPanel);


        // Panel for PageRank
        JPanel pageRankPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pageRankPanel.setBorder(BorderFactory.createTitledBorder("PageRank (d=0.85)"));
        pageRankPanel.add(btnPageRank);
        pageRankPanel.add(new JLabel(" (Result for all nodes shown in output)"));
        bottomPanel.add(pageRankPanel);


        // Panel for Random Walk
        JPanel randomWalkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        randomWalkPanel.setBorder(BorderFactory.createTitledBorder("Random Walk"));
        randomWalkPanel.add(btnRandomWalk);
        randomWalkPanel.add(btnStopWalk);
        bottomPanel.add(randomWalkPanel);


        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void setGraphOperationButtonsEnabled(boolean enabled) {
        btnShowGraph.setEnabled(enabled);
        btnQueryBridge.setEnabled(enabled);
        btnGenerateText.setEnabled(enabled);
        btnShortestPath.setEnabled(enabled);
        btnPageRank.setEnabled(enabled);
        btnRandomWalk.setEnabled(enabled);
         // btnStopWalk is handled separately
    }


     private void setupActions() {
        // --- File Loading Action ---
        btnLoadFile.addActionListener(e -> loadFileAndGenerateGraph());

        // --- Show Graph Action ---
        btnShowGraph.addActionListener(e -> {
            if (graph == null) {
                outputArea.setText("Error: No graph generated. Load a file first.");
                return;
            }
            showDirectedGraph(graph, null); // Show graph without highlighting initially
        });

        // --- Query Bridge Words Action ---
        btnQueryBridge.addActionListener(e -> {
            if (graph == null) {
                outputArea.setText("Error: No graph generated. Load a file first.");
                return;
            }
            String word1 = word1Input.getText().trim().toLowerCase();
            String word2 = word2Input.getText().trim().toLowerCase();
            if (word1.isEmpty() || word2.isEmpty()) {
                 outputArea.setText("Please enter both Word 1 and Word 2 for bridge word query.");
                 return;
             }
             // Clear previous shortest path highlighting if any
             currentGraphDotString = graph.toDotFormat(null);
             outputArea.setText(queryBridgeWords(word1, word2));
        });

        // --- Generate New Text Action ---
         btnGenerateText.addActionListener(e -> {
             if (graph == null) {
                outputArea.setText("Error: No graph generated. Load a file first.");
                return;
            }
            String inputText = textInput.getText().trim();
             if (inputText.isEmpty()) {
                 outputArea.setText("Please enter text to generate from.");
                 return;
             }
             outputArea.setText("Original Text: " + inputText + "\n");
             outputArea.append("Generated Text: " + generateNewText(inputText));
        });


         // --- Shortest Path Action ---
        btnShortestPath.addActionListener(e -> {
            if (graph == null) {
                outputArea.setText("Error: No graph generated. Load a file first.");
                return;
            }
            String word1 = spWord1Input.getText().trim().toLowerCase(); // <-- 修改：读取 spWord1Input
            String word2 = spWord2Input.getText().trim().toLowerCase(); // <-- 修改：读取 spWord2Input

            if (word1.isEmpty()) {
                 outputArea.setText("Please enter at least Word 1 for shortest path calculation.");
                 return;
             }

             // If word2 is empty, calculate path from word1 to all other nodes
            if (word2.isEmpty()) {
                outputArea.setText(calcShortestPath(word1, null)); // Pass null for word2
                currentGraphDotString = graph.toDotFormat(null); // No highlighting for all paths
            } else {
                 // Case: Path between word1 and word2
                 String resultText = calcShortestPath(word1, word2); // Calculate path ONCE and store result
                 outputArea.setText(resultText); // Display the result (path, "No path...", or "Error: Word not found...")
 
                 // --- CORRECTED CHECK: Only proceed if a valid path was ACTUALLY found ---
                 // A successful path result will NOT start with "No path found" AND will NOT start with "Error:"
                 boolean pathFoundSuccessfully = !resultText.startsWith("No path found") && !resultText.startsWith("Error:");
 
                 if (pathFoundSuccessfully) {
                     // Both words exist AND a path was found by calcShortestPath.
                     // Now, get the path list for highlighting.
                     List<String> path = findShortestPathList(word1, word2);
 
                     // Double-check if findShortestPathList succeeded (it should, but be safe)
                     if (path != null && !path.isEmpty()) {
                         System.out.println("[DEBUG] Path found successfully. Highlighting graph."); // Optional Debug
                         showDirectedGraph(graph, path); // Show graph *with* highlighting
                     } else {
                         // This indicates an unexpected inconsistency between the two path functions.
                         System.err.println("Warning: Path text indicated success, but findShortestPathList failed to return a valid path. Showing unhighlighted graph.");
                         showDirectedGraph(graph, null); // Fallback to showing unhighlighted graph
                     }
                 } else {
                     // EITHER "No path found..." OR "Error: Word ... not found..." was returned.
                     // In both these failure cases, DO NOT show the graph.
                     System.out.println("[DEBUG] Path not found OR word missing. Graph display skipped."); // Optional Debug
                     currentGraphDotString = graph.toDotFormat(null); // Ensure graph data has no old highlight
                 }
                 // --- End Corrected Check ---
            }
        });


         // --- PageRank Action ---
        btnPageRank.addActionListener(e -> {
            if (graph == null) {
                outputArea.setText("Error: No graph generated. Load a file first.");
                return;
            }
            calculateAndDisplayAllPageRanks();
        });


        // --- Random Walk Action ---
        btnRandomWalk.addActionListener(e -> {
            if (graph == null) {
                outputArea.setText("Error: No graph generated. Load a file first.");
                return;
            }
            stopRandomWalk = false; // Reset stop flag
            btnRandomWalk.setEnabled(false); // Disable start while running
            btnStopWalk.setEnabled(true);   // Enable stop
            setGraphOperationButtonsEnabled(false); // Disable others during walk

            // Run walk in a separate thread to avoid blocking GUI
            new Thread(() -> {
                String result = randomWalk();
                 // Update GUI back on the Event Dispatch Thread
                SwingUtilities.invokeLater(() -> {
                    outputArea.setText(result);
                    btnRandomWalk.setEnabled(true);  // Re-enable start
                    btnStopWalk.setEnabled(false); // Disable stop
                    setGraphOperationButtonsEnabled(true); // Re-enable others
                });
            }).start();
        });

         // --- Stop Random Walk Action ---
        btnStopWalk.addActionListener(e -> {
             stopRandomWalk = true; // Set the flag
             outputArea.append("\n\n>>> Random walk stop requested <<<");
             btnStopWalk.setEnabled(false); // Disable stop button after clicking
         });
    }


     // --- Core Function Implementations ---

     /**
      * Function 1 (Helper): Loads file and builds the graph.
      */
     private void loadFileAndGenerateGraph() {
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            lastOpenedFile = selectedFile;
            outputArea.setText("Loading file: " + selectedFile.getAbsolutePath() + "\n");
            try {
                graph = buildGraphFromFile(selectedFile.getAbsolutePath());
                outputArea.append("Graph generated successfully.\n");
                outputArea.append("Nodes: " + graph.getAllNodes().size() + "\n");
                // Calculate initial dot string without highlighting
                currentGraphDotString = graph.toDotFormat(null);
                 pageRankScores = null; // Reset pagerank on new graph
                setGraphOperationButtonsEnabled(true);
                statusLabel.setText("Status: File loaded: " + selectedFile.getName());

            } catch (IOException ex) {
                 graph = null; // Ensure graph is null on error
                setGraphOperationButtonsEnabled(false);
                outputArea.append("Error reading file: " + ex.getMessage());
                statusLabel.setText("Status: Error loading file.");
                 JOptionPane.showMessageDialog(this,
                        "Error reading file: " + ex.getMessage(),
                        "File Error", JOptionPane.ERROR_MESSAGE);
            } catch (Exception ex) {
                 graph = null;
                 setGraphOperationButtonsEnabled(false);
                 outputArea.append("An unexpected error occurred during graph generation: " + ex.getMessage());
                 statusLabel.setText("Status: Error generating graph.");
                 JOptionPane.showMessageDialog(this,
                         "Error generating graph: " + ex.getMessage(),
                         "Graph Error", JOptionPane.ERROR_MESSAGE);
            }
        }
     }


     /**
      * Helper for Function 1: Reads file content and builds the graph object.
      */
     private DirectedGraph buildGraphFromFile(String filePath) throws IOException {
        DirectedGraph newGraph = new DirectedGraph();
        this.wordCounts = new HashMap<>();
        long totalWordsInDoc = 0;
        StringBuilder textContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
             String line;
             while ((line = reader.readLine()) != null) {
                 textContent.append(line.trim()).append(" "); // Treat newline as space
             }
         }

        String processedText = textContent.toString()
                .toLowerCase() // Convert to lowercase
                .replaceAll("[^a-z\\s]", " ") // Replace non-letters/non-whitespace with space
                .replaceAll("\\s+", " "); // Collapse multiple spaces into one

        String[] words = processedText.trim().split("\\s+"); // Split by one or more spaces

        if (words.length > 0 && !words[0].isEmpty()) {
            for (String word : words) {
                if (!word.isEmpty()) {
                    wordCounts.put(word, wordCounts.getOrDefault(word, 0) + 1);
                    totalWordsInDoc++; // 累加文档中的总词数（有效的）
                }
            }
       }
        if (totalWordsInDoc < 2) { // 使用 totalWordsInDoc 判断更准确
            outputArea.append("Warning: Not enough words in the text to form edges or meaningful frequencies.\n");
            // 即使只有一个词，也要加入图和词频 (如果需要单独节点的 PR)
            if (totalWordsInDoc == 1) {
                String singleWord = words[0]; // 确保 words[0] 存在且非空
                if (!singleWord.isEmpty()){
                    newGraph.addNode(singleWord);
                    // wordCounts 已在上面循环中添加
                }
            }
            // 如果 totalWordsInDoc = 0, wordCounts 也是空的，图也是空的
            return newGraph;
        }

        for (int i = 0; i < words.length - 1; i++) {
            String word1 = words[i];
            String word2 = words[i+1];
            if (!word1.isEmpty() && !word2.isEmpty()) {
                newGraph.addEdge(word1, word2); // addEdge 内部会调用 addNode
            }
        }
        // 确保最后一个词也被添加为节点 (addEdge 内部会处理，但以防万一)
        // String lastWord = words[words.length - 1];
        // if (!lastWord.isEmpty()) {
        //    newGraph.addNode(lastWord); // addEdge 已确保节点存在
        // }

        // 打印词频信息 (可选调试)
        System.out.println("[DEBUG] Word Counts: " + wordCounts);
        System.out.println("[DEBUG] Total words processed for TF: " + totalWordsInDoc);
        System.out.println("[DEBUG] Final Word Counts: " + this.wordCounts);

         return newGraph;    
    }   


     /**
      * Function 2: Displays the graph (CLI and generates image file).
      * @param G The graph to display (type defined as DirectedGraph).
      * @param highlightedPath Optional list of nodes representing a path to highlight.
      */
      public void showDirectedGraph(DirectedGraph G, List<String> highlightedPath) {
         if (G == null) {
             outputArea.setText("Graph is not generated yet.");
             return;
         }

         // 1. Display in CLI format in the output area
         outputArea.setText("--- Graph Structure (CLI Format) ---\n");
         outputArea.append(G.toCliString());
         outputArea.append("\n--- End of CLI Format ---\n\n");

         // 2. Generate DOT file and attempt to create image
         String dotFileName = "graph_output.dot";
         String pngFileName = "graph_output.png";
         currentGraphDotString = G.toDotFormat(highlightedPath); // Update dot string with potential highlight

         try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(dotFileName), StandardCharsets.UTF_8))) {
             out.println(currentGraphDotString);
             outputArea.append("Graph DOT file saved as: " + dotFileName + "\n");
         } catch (IOException e) {
             outputArea.append("Error writing DOT file: " + e.getMessage() + "\n");
             JOptionPane.showMessageDialog(this,
                     "Could not save DOT file: " + e.getMessage(),
                     "File Save Error", JOptionPane.ERROR_MESSAGE);
             return; // Don't proceed if DOT file failed
         }

         // 3. Attempt to generate PNG using Graphviz 'dot' command
         try {
            // Command: dot -Tpng graph_output.dot -o graph_output.png
            ProcessBuilder pb = new ProcessBuilder("dot", "-Tpng", dotFileName, "-o", pngFileName);
             Process process = pb.start();

            // Capture potential errors from the 'dot' command
            StringBuilder errorOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

             int exitCode = process.waitFor(); // Wait for the command to complete

             if (exitCode == 0) {
                outputArea.append("Graph image generated successfully: " + pngFileName + "\n");
                 // Try to open the generated image
                 try {
                    Desktop.getDesktop().open(new File(pngFileName));
                 } catch (UnsupportedOperationException | IOException openError) {
                    outputArea.append("Could not automatically open the image file. Please find it at: " + new File(pngFileName).getAbsolutePath() + "\n");
                 }
             } else {
                 outputArea.append("Graphviz 'dot' command failed (Exit Code: " + exitCode + ").\n");
                 outputArea.append("Error output (if any):\n" + errorOutput.toString() + "\n");
                 outputArea.append("Please ensure Graphviz is installed and the 'dot' command is in your system's PATH.\n");
                 outputArea.append("The DOT file ("+dotFileName+") was saved, you can render it manually.\n");
                JOptionPane.showMessageDialog(this,
                        "Graphviz 'dot' command failed (Exit Code: " + exitCode + ").\n" +
                        "Ensure Graphviz is installed and 'dot' is in PATH.\n" +
                        "DOT file saved as " + dotFileName + ".\n\nError: " + errorOutput.toString(),
                        "Graphviz Error", JOptionPane.WARNING_MESSAGE);
            }

         } catch (IOException | InterruptedException e) {
             outputArea.append("Error executing Graphviz 'dot' command: " + e.getMessage() + "\n");
             outputArea.append("Please ensure Graphviz is installed and the 'dot' command is in your system's PATH.\n");
             outputArea.append("The DOT file ("+dotFileName+") was saved, you can render it manually.\n");
            JOptionPane.showMessageDialog(this,
                    "Could not run Graphviz 'dot' command: " + e.getMessage() + "\n" +
                    "Ensure Graphviz is installed and 'dot' is in PATH.\n" +
                    "DOT file saved as " + dotFileName + ".",
                    "Graphviz Error", JOptionPane.WARNING_MESSAGE);
             // Restore interrupted status
            if (e instanceof InterruptedException) {
                 Thread.currentThread().interrupt();
             }
         }
     }

      /**
       * Function 3: Finds bridge words between two given words.
       * @param word1 The first word.
       * @param word2 The second word.
       * @return A string describing the bridge words found or appropriate error messages.
       */
       public String queryBridgeWords(String word1, String word2) {
           if (graph == null) return "Error: Graph not generated.";
           word1 = word1.toLowerCase();
           word2 = word2.toLowerCase();

           boolean word1Exists = graph.containsNode(word1);
           boolean word2Exists = graph.containsNode(word2);

           if (!word1Exists && !word2Exists) {
               return "No \"" + word1 + "\" or \"" + word2 + "\" in the graph!";
           } else if (!word1Exists) {
               return "No \"" + word1 + "\" in the graph!";
           } else if (!word2Exists) {
                return "No \"" + word2 + "\" in the graph!";
           }

           List<String> bridgeWords = new ArrayList<>();
           Map<String, Integer> word1Neighbors = graph.getNeighbors(word1);

           if (word1Neighbors.isEmpty()) {
                return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
            }


           for (String bridgeCandidate : word1Neighbors.keySet()) {
               // Check if there's an edge from bridgeCandidate to word2
               if (graph.getNeighbors(bridgeCandidate).containsKey(word2)) {
                   bridgeWords.add(bridgeCandidate);
               }
           }

           if (bridgeWords.isEmpty()) {
                return "No bridge words from \"" + word1 + "\" to \"" + word2 + "\"!";
           } else {
                StringBuilder result = new StringBuilder("The bridge words from \"" + word1 + "\" to \"" + word2 + "\" are: ");
                for (int i = 0; i < bridgeWords.size(); i++) {
                     result.append(bridgeWords.get(i));
                     if (i < bridgeWords.size() - 2) {
                         result.append(", ");
                     } else if (i == bridgeWords.size() - 2) {
                         result.append(", and ");
                     }
                 }
                 result.append(".");
                return result.toString();
           }
       }


      /**
       * Function 4: Generates new text by inserting random bridge words.
       * @param inputText The original text line.
       * @return The new text with bridge words inserted, or the original text if no bridge words are found.
       */
      public String generateNewText(String inputText) {
         if (graph == null) return "Error: Graph not generated. Cannot generate new text.";
         if (inputText == null || inputText.trim().isEmpty()) return ""; // Handle empty input

         String processedInput = inputText.toLowerCase()
                                     .replaceAll("[^a-z\\s]", " ")
                                     .replaceAll("\\s+", " ");
         String[] words = processedInput.trim().split("\\s+");

         if (words.length < 2) {
             return inputText; // Not enough words to find bridges
         }

         StringBuilder newTextBuilder = new StringBuilder();
         Random random = new Random();
         newTextBuilder.append(capitalizeFirst(words[0])); // Append the first word, capitalized


         for (int i = 0; i < words.length - 1; i++) {
             String word1 = words[i];
             String word2 = words[i + 1];
             List<String> bridgeWords = findBridgeWordsList(word1, word2); // Use helper

             newTextBuilder.append(" "); // Space before next word or bridge word
             if (!bridgeWords.isEmpty()) {
                 // Select a random bridge word
                 String chosenBridge = bridgeWords.get(random.nextInt(bridgeWords.size()));
                 newTextBuilder.append(chosenBridge).append(" "); // Insert bridge word and space
             }
             newTextBuilder.append(words[i + 1]); // Append the next original word
         }

         return newTextBuilder.toString();
     }

     // Helper to capitalize the first letter of a word
    private String capitalizeFirst(String word) {
         if (word == null || word.isEmpty()) return word;
         return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }


    // Helper for generateNewText: returns a list of bridge words, or empty list
    private List<String> findBridgeWordsList(String word1, String word2) {
        List<String> bridgeWords = new ArrayList<>();
        if (!graph.containsNode(word1) || !graph.containsNode(word2)) {
            return bridgeWords; // Empty list if words not in graph
        }

        Map<String, Integer> word1Neighbors = graph.getNeighbors(word1);
        for (String bridgeCandidate : word1Neighbors.keySet()) {
             if (graph.getNeighbors(bridgeCandidate).containsKey(word2)) {
                 bridgeWords.add(bridgeCandidate);
             }
         }
        return bridgeWords;
    }

     /**
      * Function 5: Calculates the shortest path between two words using Dijkstra's algorithm.
      * If word2 is null, calculates shortest paths from word1 to all other reachable nodes.
      * @param word1 The starting word.
      * @param word2 The ending word (or null to find paths to all nodes).
      * @return A string describing the shortest path(s) and length(s), or an error message.
      */
     public String calcShortestPath(String word1, String word2) {
         if (graph == null) return "Error: Graph not generated.";
         word1 = word1.toLowerCase();
         if (word2 != null) {
             word2 = word2.toLowerCase();
         }

         if (!graph.containsNode(word1) || (word2 != null && !graph.containsNode(word2))) {
            String missing = "";
            if (!graph.containsNode(word1)) missing += "\"" + word1 + "\"";
            if (word2 != null && !graph.containsNode(word2)) {
                if (!missing.isEmpty()) missing += " or ";
                missing += "\"" + word2 + "\"";
            }
             return "Error: Word " + missing + " not found in the graph.";
         }


         // Dijkstra's Algorithm Implementation
         Map<String, Integer> distances = new HashMap<>();
         Map<String, String> previousNodes = new HashMap<>();
         PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
         Set<String> settledNodes = new HashSet<>();


         // Initialize distances
         for (String node : graph.getAllNodes()) {
             distances.put(node, Integer.MAX_VALUE);
         }
         distances.put(word1, 0);
         pq.add(new AbstractMap.SimpleEntry<>(word1, 0));


         while (!pq.isEmpty()) {
             String currentNode = pq.poll().getKey();

             if (settledNodes.contains(currentNode)) continue;
             settledNodes.add(currentNode);


             // If we are looking for a specific target and found it, we can potentially stop early
             // BUT for finding *all* paths if word2 is null, we must continue until pq is empty.
              if (word2 != null && currentNode.equals(word2)) {
                 // break; // Optimization if ONLY interested in word1 -> word2
              }


             Map<String, Integer> neighbors = graph.getNeighbors(currentNode);
             for (Map.Entry<String, Integer> neighborEntry : neighbors.entrySet()) {
                 String neighbor = neighborEntry.getKey();
                 int edgeWeight = neighborEntry.getValue();


                 if (!settledNodes.contains(neighbor)) {
                     int newDist = distances.get(currentNode) + edgeWeight;
                     if (newDist < distances.get(neighbor)) {
                         distances.put(neighbor, newDist);
                         previousNodes.put(neighbor, currentNode);
                         // Add/update neighbor in priority queue
                         pq.add(new AbstractMap.SimpleEntry<>(neighbor, newDist));
                     }
                 }
             }
         }

         // --- Reconstruct and Format Path(s) ---
         StringBuilder result = new StringBuilder();


         if (word2 != null) {
             // Case 1: Path between word1 and word2
             if (distances.get(word2) == Integer.MAX_VALUE) {
                 return "No path found between \"" + word1 + "\" and \"" + word2 + "\".";
             } else {
                 List<String> path = reconstructPath(previousNodes, word1, word2);
                 result.append("Shortest path from \"").append(word1).append("\" to \"").append(word2).append("\":\n");
                 result.append(String.join(" -> ", path)).append("\n");
                 result.append("Total weight: ").append(distances.get(word2));
                 // Note: Highlighting is handled by calling showDirectedGraph separately
                 return result.toString();
             }
         } else {
            // Case 2: Paths from word1 to all other nodes
            result.append("Shortest paths from \"").append(word1).append("\" to all other reachable nodes:\n");
             boolean foundPath = false;
             List<String> sortedNodes = new ArrayList<>(graph.getAllNodes());
             Collections.sort(sortedNodes); // For consistent output


             for (String targetNode : sortedNodes) {
                 if (!targetNode.equals(word1) && distances.get(targetNode) != Integer.MAX_VALUE) {
                     foundPath = true;
                     List<String> path = reconstructPath(previousNodes, word1, targetNode);
                     result.append("  To \"").append(targetNode).append("\": ");
                     result.append(String.join(" -> ", path));
                     result.append(" (Weight: ").append(distances.get(targetNode)).append(")\n");
                 }
             }
             if (!foundPath) {
                 result.append("  No other nodes are reachable from \"").append(word1).append("\".");
             }
            return result.toString();
         }
     }

     // Helper to reconstruct the path from Dijkstra's results
     private List<String> reconstructPath(Map<String, String> previousNodes, String start, String end) {
         LinkedList<String> path = new LinkedList<>();
         for (String at = end; at != null; at = previousNodes.get(at)) {
             path.addFirst(at);
             if (at.equals(start)) break; // Should happen if path exists
         }
          // If path doesn't start with 'start', something went wrong or no path
         if (path.isEmpty() || !path.getFirst().equals(start)) {
             return Collections.emptyList(); // Or handle error appropriately
         }
         return path;
     }

    // Helper used by the action listener to get the path list for highlighting
    private List<String> findShortestPathList(String word1, String word2) {
        if (graph == null || word1 == null || word2 == null || word1.isEmpty() || word2.isEmpty()) return null;
        word1 = word1.toLowerCase();
        word2 = word2.toLowerCase();
         if (!graph.containsNode(word1) || !graph.containsNode(word2)) return null;

         // Re-run Dijkstra (or reuse results if stored) - simpler to re-run here
         Map<String, Integer> distances = new HashMap<>();
         Map<String, String> previousNodes = new HashMap<>();
         PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
         Set<String> settledNodes = new HashSet<>();

         for (String node : graph.getAllNodes()) distances.put(node, Integer.MAX_VALUE);
         distances.put(word1, 0);
         pq.add(new AbstractMap.SimpleEntry<>(word1, 0));

         while (!pq.isEmpty()) {
             String currentNode = pq.poll().getKey();
             if (settledNodes.contains(currentNode)) continue;
             settledNodes.add(currentNode);
             if (currentNode.equals(word2)) break; // Found target

             Map<String, Integer> neighbors = graph.getNeighbors(currentNode);
             for (Map.Entry<String, Integer> neighborEntry : neighbors.entrySet()) {
                 String neighbor = neighborEntry.getKey();
                 int edgeWeight = neighborEntry.getValue();
                 if (!settledNodes.contains(neighbor)) {
                     int newDist = distances.get(currentNode) + edgeWeight;
                     if (newDist < distances.get(neighbor)) {
                         distances.put(neighbor, newDist);
                         previousNodes.put(neighbor, currentNode);
                         pq.add(new AbstractMap.SimpleEntry<>(neighbor, newDist));
                     }
                 }
             }
         }

         if (distances.get(word2) != Integer.MAX_VALUE) {
             return reconstructPath(previousNodes, word1, word2);
         } else {
             return null; // No path found
         }
     }

    /**
     * Function 6 (Helper): Calculates PageRank for all nodes and stores it.
     * The public `calPageRank` method (not explicitly defined per spec change)
     * would look up the value for a specific word from the pre-calculated map.
     * This method calculates and displays all ranks.
     */
    private void calculateAndDisplayAllPageRanks() {
        if (graph == null || graph.getAllNodes().isEmpty()) {
            outputArea.setText("Graph is empty or not loaded. Cannot calculate PageRank.");
            pageRankScores = null; // Ensure scores are null
            return;
        }

        Set<String> nodes = graph.getAllNodes();
        int numNodes = nodes.size();
        if (numNodes == 0) { // Extra check
            outputArea.setText("Graph has no nodes. Cannot calculate PageRank.");
            pageRankScores = null;
            return;
        }
        
        if (this.wordCounts == null) { // Check if wordCounts was initialized
             outputArea.setText("Error: Word counts map is null. Cannot initialize PageRank based on TF. Load file again?");
             pageRankScores = null;
             return;
         }
        if (this.wordCounts.isEmpty() && numNodes > 0) {
            // Graph has nodes, but wordCounts is empty (e.g., file had structure but no words?)
            // Fallback to uniform initialization OR report error
            System.err.println("Warning: Graph has nodes but word counts are empty. Falling back to uniform PageRank initialization.");
            pageRankScores = new HashMap<>();
            double initialRank = 1.0 / numNodes;
            for (String node : nodes) {
                pageRankScores.put(node, initialRank);
            }
            // Proceed with calculation using uniform start
        } else {
            // Initialize PageRank scores based on Term Frequency (TF)
            pageRankScores = new HashMap<>();
            long totalWordOccurrences = 0; // Sum of frequencies of all words in the doc
            for (int count : this.wordCounts.values()) {
                totalWordOccurrences += count;
            }

            if (totalWordOccurrences == 0 && numNodes > 0) {
                // This case might happen if words existed but counts somehow became zero.
                System.err.println("Warning: Total word occurrences is zero, but graph has nodes. Falling back to uniform PageRank initialization.");
                pageRankScores = new HashMap<>();
                double initialRank = 1.0 / numNodes;
                for (String node : nodes) {
                    pageRankScores.put(node, initialRank);
                }
            } else if (totalWordOccurrences > 0) {
                 System.out.println("[DEBUG] Initializing PageRank based on TF. Total Occurrences: " + totalWordOccurrences);
                 double sumCheck = 0.0;
                 for (String node : nodes) {
                    int count = this.wordCounts.getOrDefault(node, 0);
                    // Initial PR = TF / TotalTF
                    // Nodes in the graph might not have appeared in the text if added differently, handle 0 count.
                    double initialRank = (count > 0) ? (double)count / totalWordOccurrences : 0.0;
                    pageRankScores.put(node, initialRank);
                    sumCheck += initialRank;
                 }
                 System.out.printf("[DEBUG] Initial PageRank sum (should be close to 1.0): %.6f\n", sumCheck);
                 // Optional: Normalize if sum isn't exactly 1 due to floating point issues or graph structure mismatches
                 if (Math.abs(sumCheck - 1.0) > 1e-5) {
                     System.err.println("Warning: Initial PageRank sum deviates significantly from 1. Normalizing.");
                     if (sumCheck > 0) { // Avoid division by zero if all counts were somehow 0
                        for (String node : nodes) {
                            pageRankScores.put(node, pageRankScores.get(node) / sumCheck);
                        }
                     } else {
                         // Fallback again if normalization failed
                         System.err.println("Error: Cannot normalize zero sum. Falling back to uniform distribution.");
                         double initialRank = 1.0 / numNodes;
                         for (String node : nodes) {
                             pageRankScores.put(node, initialRank);
                         }
                     }
                 }

                 System.out.println("[DEBUG] Initial PageRank Values (TF-based, potentially normalized):");
                 for (Map.Entry<String, Double> entry : pageRankScores.entrySet()) {
                     System.out.printf("  %-15s : %.6f\n", entry.getKey(), entry.getValue());
                 }
            } else {
                 // totalWordOccurrences is 0 and numNodes is 0 - graph is empty, handled at start.
                 // This 'else' should ideally not be reached if checks above are correct.
            }
        }


        // --- PageRank Iteration ---
        final double d = 0.85; // Damping factor
        final int maxIterations = 100;
        final double tolerance = 1e-6;

        Map<String, Double> newRankScores = new HashMap<>();
        Map<String, Integer> outDegrees = new HashMap<>(); // Precompute out-degrees
        for (String node : nodes) {
            outDegrees.put(node, graph.getNeighbors(node).size());
        }

        int iteration = 0;
        double delta = 1.0;

        while (iteration < maxIterations && delta > tolerance) {
            double danglingSum = 0.0; // Sum of PR from dangling nodes in the *previous* iteration

            // --- Step 1: Calculate the total PageRank of dangling nodes ---
            for (String node : nodes) {
                if (outDegrees.get(node) == 0) {
                    // Accumulate PR from dangling nodes using scores from the previous iteration
                    danglingSum += pageRankScores.get(node);
                }
            }

            // The portion of dangling PR to redistribute to *each* node
            double danglingContributionPerNode = d * (danglingSum / numNodes);

            double totalChange = 0.0; // Reset total change for convergence check this iteration

            // --- Step 2: Calculate new ranks for all nodes ---
            for (String nodeP : nodes) {
                // Base rank: random jump probability + share of redistributed dangling PR
                double newRank = (1.0 - d) / numNodes + danglingContributionPerNode;

                // Add rank from incoming links (only non-dangling nodes contribute via links)
                double incomingRankSum = 0;
                Set<String> inNeighbors = graph.getInNeighbors(nodeP); // Get nodes Q such that Q -> P
                for (String nodeQ : inNeighbors) {
                    int outDegreeQ = outDegrees.get(nodeQ);
                    // IMPORTANT: Only consider contribution if the source node Q is NOT dangling.
                    // Its contribution (if dangling) is already accounted for in danglingContributionPerNode.
                    if (outDegreeQ > 0) {
                        incomingRankSum += pageRankScores.get(nodeQ) / outDegreeQ;
                    }
                }
                newRank += d * incomingRankSum; // Scale the contribution from actual links

                newRankScores.put(nodeP, newRank); // Store the calculated new rank temporarily
                totalChange += Math.abs(newRank - pageRankScores.get(nodeP)); // Accumulate the change
            }

            // --- Step 3: Update PageRank scores and check for convergence ---
            pageRankScores.putAll(newRankScores); // Update all scores for the next iteration
            delta = totalChange; // Use the total absolute change as the convergence metric

            iteration++;
            // System.out.printf("[DEBUG] Iteration %d, Delta: %.8f\n", iteration, delta); // Optional debug
        }

        // --- Display Results ---
        StringBuilder result = new StringBuilder("PageRank Calculation Results (d=0.85, ");
        if (delta <= tolerance) {
            result.append("Converged in ").append(iteration).append(" iterations):\n");
        } else {
            result.append("Stopped after max iterations: ").append(iteration).append("):\n");
        }

        // Verify sum is close to 1 (optional check)
        double finalSum = 0;
        for(double score : pageRankScores.values()) finalSum += score;
        result.append(String.format("  (Final sum: %.6f)\n", finalSum));


        List<Map.Entry<String, Double>> sortedRanks = new ArrayList<>(pageRankScores.entrySet());
        sortedRanks.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue())); // Sort descending by PR

        for (Map.Entry<String, Double> entry : sortedRanks) {
            result.append(String.format("  %-15s : %.8f\n", entry.getKey(), entry.getValue())); // Increased precision
        }
        outputArea.setText(result.toString());
    }

     /**
      * Function 6 Interface (as required, though calculation is done for all):
      * Returns the pre-calculated PageRank for a specific word.
      * @param word The word to get the PageRank for.
      * @return The PageRank value, or null/error indicator if not calculated or word not found.
      * NOTE: The GUI button triggers calculation for ALL nodes now.
      * This function complies with the spec but relies on the map being populated.
      */
     public Double calPageRank(String word) {
         if (pageRankScores == null) {
             // Optionally trigger the full calculation here if needed on demand
              System.err.println("PageRank not calculated yet. Please use the 'Calc PageRank (All)' button first.");
             // calculateAndDisplayAllPageRanks(); // Or trigger calculation
              return null; // Indicate not ready or not found
         }
         return pageRankScores.getOrDefault(word.toLowerCase(), null); // Return null if word not in map
     }


    /**
     * Function 7: Performs a random walk on the graph.
     * Stops on first repeated edge or dead end. Can be interrupted by the user.
     * @return A string containing the path traversed and status (stopped/completed).
     */
    public String randomWalk() {
        if (graph == null || graph.getAllNodes().isEmpty()) {
            return "Error: Graph is empty or not loaded.";
        }

        Random random = new Random();
        List<String> nodesList = new ArrayList<>(graph.getAllNodes());
        if (nodesList.isEmpty()) {
            return "Error: No nodes in the graph to start walk.";
        }

        String currentNode = nodesList.get(random.nextInt(nodesList.size())); // Random start node
        List<String> visitedPath = new ArrayList<>();
        Set<String> visitedEdges = new HashSet<>(); // Store edges as "source->target"

        StringBuilder pathString = new StringBuilder();
        String stopReason = "Completed (Dead End)"; // Default stop reason

        while (!stopRandomWalk) {
            visitedPath.add(currentNode);
            pathString.append(currentNode);

            Map<String, Integer> neighbors = graph.getNeighbors(currentNode);
            if (neighbors.isEmpty()) {
                stopReason = "Stopped (Dead End at '" + currentNode + "')";
                break; // No outgoing edges
            }

            // Select random next node from neighbors
            List<String> neighborKeys = new ArrayList<>(neighbors.keySet());
            String nextNode = neighborKeys.get(random.nextInt(neighborKeys.size()));

            String edge = currentNode + "->" + nextNode;
            if (visitedEdges.contains(edge)) {
                stopReason = "Stopped (Repeated edge: " + edge + ")";
                visitedPath.add(nextNode); // Add the final node leading to the repeat
                pathString.append(" -> ").append(nextNode);
                break; // Edge repeated
            }

            visitedEdges.add(edge);
            pathString.append(" -> ");
            currentNode = nextNode; // Move to the next node

            // Optional: Add a small delay to make the walk observable if needed
            // try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }

         if (stopRandomWalk) {
             stopReason = "Stopped (User Interruption)";
         }

        // Save the walk to a file
        String walkFileName = "random_walk_output.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(walkFileName))) {
            writer.println("Random Walk Path:");
             writer.println(String.join(" -> ", visitedPath));
             writer.println("\nStatus: " + stopReason);
        } catch (IOException e) {
            // Also report error to GUI
            final String errorMsg = "Error writing random walk to file '" + walkFileName + "': " + e.getMessage();
            SwingUtilities.invokeLater(() -> outputArea.append("\n" + errorMsg));
             System.err.println(errorMsg);
        }

        return "Random Walk Path:\n" + String.join(" -> ", visitedPath) +
               "\n\nStatus: " + stopReason +
                "\nPath saved to: " + walkFileName;
    }


    // --- Main Method ---
    public static void main(String[] args) {
         // Set Look and Feel (optional, for better appearance)
         try {
              UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         } catch (Exception e) {
              System.err.println("Couldn't set system look and feel.");
         }

        // Run the GUI on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            TextGraphApp app = new TextGraphApp();
            // Handle command line argument for file path (optional)
             if (args.length > 0) {
                 String filePath = args[0];
                 File file = new File(filePath);
                 if (file.exists() && !file.isDirectory()) {
                     app.fileChooser.setSelectedFile(file); // Pre-select file
                     app.loadFileAndGenerateGraph();      // Load it immediately
                 } else {
                     System.err.println("Provided file path is invalid: " + filePath);
                     app.statusLabel.setText("Status: Invalid file path in argument.");
                 }
             }
            app.setVisible(true);
        });
    }
}

// --- Helper class needed for Dijkstra with PriorityQueue ---
// (Already included AbstractMap.SimpleEntry use, so this isn't strictly needed anymore,
// but demonstrates how you might create a custom pair class if needed)
/*
class NodeDistance implements Comparable<NodeDistance> {
    String node;
    int distance;

    NodeDistance(String node, int distance) {
        this.node = node;
        this.distance = distance;
    }

    @Override
    public int compareTo(NodeDistance other) {
        return Integer.compare(this.distance, other.distance);
    }

     @Override
    public String toString() { return node + "(" + distance + ")"; }
}
*/
