import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AgenteGUI extends Agent {
    private static final String AGENTE_SOLUCIONADOR = "Jugador3";
    private static final int[] TABLERO_META = {1, 2, 3, 4, 5, 6, 7, 8, 0};

    private VentanaCompetencia gui;
    private int[] currentBoard = {1, 2, 3, 4, 0, 5, 7, 8, 6};
    private int[] pendingBoard = currentBoard.clone();

    @Override
    protected void setup() {
        System.out.println("[AgenteGUI] Iniciado correctamente.");

        SwingUtilities.invokeLater(() -> {
            gui = new VentanaCompetencia(this);
            gui.setVisible(true);
        });

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg == null) {
                    block();
                    return;
                }

                if (msg.getPerformative() != ACLMessage.INFORM || msg.getContent() == null) {
                    return;
                }

                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                if ("AgenteGeneradorReto".equals(sender)) {
                    currentBoard = PuzzleState.deserialize(content);
                    pendingBoard = currentBoard.clone();
                    SwingUtilities.invokeLater(() -> {
                        if (gui != null) {
                            gui.actualizarTablero(currentBoard);
                            gui.mostrarEstado("Reto listo para resolver");
                            gui.appendLog("Nuevo reto recibido. Pulsa «Solucionar» para iniciar.");
                        }
                    });
                } else if (AGENTE_SOLUCIONADOR.equals(sender)) {
                    String animationMarker = "\nANIMACION:";
                    int markerPosition = content.indexOf(animationMarker);
                    String report = markerPosition >= 0 ? content.substring(0, markerPosition) : content;
                    List<int[]> solutionPath = markerPosition >= 0
                            ? parseSolutionPath(content.substring(markerPosition + animationMarker.length()))
                            : null;
                    boolean solved = report.contains("Camino:") && solutionPath != null
                            && !solutionPath.isEmpty()
                            && Arrays.equals(solutionPath.get(0), pendingBoard)
                            && Arrays.equals(solutionPath.get(solutionPath.size() - 1), TABLERO_META);
                    SwingUtilities.invokeLater(() -> {
                        if (gui != null) {
                            if (solved) {
                                gui.mostrarEstado("Reproduciendo solución...");
                                gui.mostrarResultado(report);
                                gui.animarSolucion(solutionPath, () -> {
                                    currentBoard = TABLERO_META.clone();
                                    gui.mostrarEstado("Solución encontrada");
                                });
                            } else {
                                gui.mostrarEstado("No se encontró solución");
                                gui.mostrarResultado(report);
                            }
                        }
                    });
                }
            }
        });
    }

    public void solicitarReto() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("AgenteGeneradorReto", AID.ISLOCALNAME));
        msg.setContent("SOLICITAR_RETO");
        send(msg);
        actualizarEstado("Generando un nuevo reto...");
    }

    public void establecerTablero(int[] board) {
        currentBoard = board.clone();
        pendingBoard = currentBoard.clone();
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.actualizarTablero(currentBoard);
                gui.mostrarEstado("Tablero listo para resolver");
                gui.appendLog("Tablero ingresado manualmente. Pulsa «Solucionar» para iniciar.");
            }
        });
    }

    public void iniciarCompetencia() {
        pendingBoard = currentBoard.clone();
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID(AGENTE_SOLUCIONADOR, AID.ISLOCALNAME));
        msg.setContent("INICIAR:" + PuzzleState.serialize(pendingBoard));
        send(msg);
        actualizarEstado("Jugador3 está buscando la solución...");
    }

    private List<int[]> parseSolutionPath(String encodedPath) {
        try {
            String[] encodedBoards = encodedPath.split(";", -1);
            List<int[]> path = new ArrayList<>(encodedBoards.length);
            for (String encodedBoard : encodedBoards) {
                if (encodedBoard.split(",", -1).length != 9) {
                    return null;
                }
                int[] board = PuzzleState.deserialize(encodedBoard);
                if (!isValidBoard(board)) {
                    return null;
                }
                if (!path.isEmpty() && !areAdjacentMoves(path.get(path.size() - 1), board)) {
                    return null;
                }
                path.add(board);
            }
            return path;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean isValidBoard(int[] board) {
        if (board.length != 9) {
            return false;
        }
        boolean[] seen = new boolean[9];
        for (int value : board) {
            if (value < 0 || value > 8 || seen[value]) {
                return false;
            }
            seen[value] = true;
        }
        return true;
    }

    private boolean areAdjacentMoves(int[] before, int[] after) {
        int blankBefore = indexOfBlank(before);
        int blankAfter = indexOfBlank(after);
        int rowDifference = Math.abs(blankBefore / 3 - blankAfter / 3);
        int columnDifference = Math.abs(blankBefore % 3 - blankAfter % 3);
        if (rowDifference + columnDifference != 1) {
            return false;
        }
        for (int i = 0; i < before.length; i++) {
            if (i != blankBefore && i != blankAfter && before[i] != after[i]) {
                return false;
            }
        }
        return before[blankAfter] == after[blankBefore];
    }

    private int indexOfBlank(int[] board) {
        for (int i = 0; i < board.length; i++) {
            if (board[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private void actualizarEstado(String estado) {
        SwingUtilities.invokeLater(() -> {
            if (gui != null) {
                gui.mostrarEstado(estado);
            }
        });
    }
}

class VentanaCompetencia extends JFrame {
    private static final Color FONDO = new Color(242, 231, 211);
    private static final Color PAPEL = new Color(251, 246, 235);
    private static final Color MADERA = new Color(62, 39, 28);
    private static final Color TERRACOTA = new Color(165, 79, 51);
    private static final Color OLIVA = new Color(105, 111, 69);
    private static final Color TEXTO = new Color(67, 49, 37);
    private static final Color LINEA = new Color(218, 199, 171);

    private final PuzzleBoardPanel panelTablero = new PuzzleBoardPanel();
    private JTextArea areaResultado;
    private JLabel etiquetaEstado;

    VentanaCompetencia(AgenteGUI agente) {
        setTitle("Ocho piezas | Laboratorio de búsqueda");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(850, 570));
        setSize(1040, 680);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout(0, 22));
        raiz.setBackground(FONDO);
        raiz.setBorder(new EmptyBorder(26, 32, 30, 32));
        setContentPane(raiz);

        raiz.add(crearEncabezado(agente), BorderLayout.NORTH);

        JPanel contenido = new JPanel(new GridBagLayout());
        contenido.setOpaque(false);
        GridBagConstraints restricciones = new GridBagConstraints();
        restricciones.gridy = 0;
        restricciones.fill = GridBagConstraints.BOTH;
        restricciones.weighty = 1;

        restricciones.gridx = 0;
        restricciones.weightx = 1.05;
        restricciones.insets = new Insets(0, 0, 0, 20);
        contenido.add(crearPanelTablero(), restricciones);

        restricciones.gridx = 1;
        restricciones.weightx = 0.95;
        restricciones.insets = new Insets(0, 0, 0, 0);
        contenido.add(crearPanelAgente(), restricciones);

        raiz.add(contenido, BorderLayout.CENTER);
        panelTablero.mostrarTablero(new int[]{1, 2, 3, 4, 0, 5, 7, 8, 6});
    }

    private JPanel crearEncabezado(AgenteGUI agente) {
        JPanel encabezado = new JPanel(new BorderLayout(16, 0));
        encabezado.setOpaque(false);

        JPanel superior = new JPanel(new BorderLayout(16, 0));
        superior.setOpaque(false);
        JPanel titulos = new JPanel();
        titulos.setOpaque(false);
        titulos.setLayout(new BoxLayout(titulos, BoxLayout.Y_AXIS));

        JLabel sobrelinea = new JLabel("LABORATORIO DE INTELIGENCIA ARTIFICIAL");
        sobrelinea.setFont(new Font("SansSerif", Font.BOLD, 11));
        sobrelinea.setForeground(OLIVA);

        JLabel titulo = new JLabel("El reto de las ocho piezas");
        titulo.setFont(new Font("Serif", Font.BOLD, 30));
        titulo.setForeground(MADERA);

        JLabel subtitulo = new JLabel("Búsqueda Greedy Best-First con heurística euclidiana");
        subtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitulo.setForeground(TEXTO);

        titulos.add(sobrelinea);
        titulos.add(Box.createVerticalStrut(4));
        titulos.add(titulo);
        titulos.add(Box.createVerticalStrut(3));
        titulos.add(subtitulo);
        superior.add(titulos, BorderLayout.CENTER);

        JPanel acciones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        acciones.setOpaque(false);
        JButton btnGenerar = crearBoton("Nuevo reto", PAPEL, MADERA);
        JButton btnIngresar = crearBoton("Ingresar tablero", PAPEL, MADERA);
        JButton btnResolver = crearBoton("Solucionar", TERRACOTA, Color.WHITE);
        btnGenerar.addActionListener(event -> agente.solicitarReto());
        btnIngresar.addActionListener(event -> ingresarTablero(agente));
        btnResolver.addActionListener(event -> agente.iniciarCompetencia());
        acciones.add(btnGenerar);
        acciones.add(btnIngresar);
        acciones.add(btnResolver);
        superior.add(acciones, BorderLayout.EAST);
        encabezado.add(superior, BorderLayout.CENTER);

        JSeparator separador = new JSeparator();
        separador.setForeground(LINEA);
        encabezado.add(separador, BorderLayout.SOUTH);
        return encabezado;
    }

    private void ingresarTablero(AgenteGUI agente) {
        String entrada = JOptionPane.showInputDialog(
                this,
                "Escribe los valores del tablero en orden, usando comas o espacios.\n"
                        + "Debe incluir una vez cada número del 0 al 8 (0 representa el espacio vacío).",
                "Ingresar tablero",
                JOptionPane.PLAIN_MESSAGE);
        if (entrada == null) {
            return;
        }

        String[] valores = entrada.trim().split("[,\\s]+");
        if (valores.length != 9) {
            mostrarErrorTablero("Debes ingresar exactamente 9 números.");
            return;
        }

        int[] board = new int[9];
        boolean[] vistos = new boolean[9];
        try {
            for (int i = 0; i < valores.length; i++) {
                int valor = Integer.parseInt(valores[i]);
                if (valor < 0 || valor > 8 || vistos[valor]) {
                    mostrarErrorTablero("El tablero debe contener una vez cada número del 0 al 8.");
                    return;
                }
                board[i] = valor;
                vistos[valor] = true;
            }
        } catch (NumberFormatException exception) {
            mostrarErrorTablero("Usa únicamente números enteros del 0 al 8.");
            return;
        }

        if (!PuzzleState.isSolvable(board)) {
            mostrarErrorTablero("Ese tablero no tiene solución para la meta 1,2,3,4,5,6,7,8,0.");
            return;
        }
        agente.establecerTablero(board);
    }

    private void mostrarErrorTablero(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Tablero inválido", JOptionPane.WARNING_MESSAGE);
    }

    private JPanel crearPanelTablero() {
        JPanel panel = crearTarjeta();
        panel.setLayout(new BorderLayout(0, 18));

        JPanel encabezado = new JPanel(new BorderLayout());
        encabezado.setOpaque(false);
        JLabel titulo = new JLabel("TABLERO");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 12));
        titulo.setForeground(OLIVA);
        JLabel leyenda = new JLabel("Meta: 1  2  3  /  4  5  6  /  7  8  ·");
        leyenda.setFont(new Font("Monospaced", Font.PLAIN, 12));
        leyenda.setForeground(TEXTO);
        encabezado.add(titulo, BorderLayout.NORTH);
        encabezado.add(leyenda, BorderLayout.SOUTH);
        panel.add(encabezado, BorderLayout.NORTH);

        panelTablero.setPreferredSize(new Dimension(430, 430));
        panel.add(panelTablero, BorderLayout.CENTER);

        JLabel pie = new JLabel("Las fichas muestran el estado actual del reto.");
        pie.setFont(new Font("SansSerif", Font.ITALIC, 12));
        pie.setForeground(TEXTO);
        panel.add(pie, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel crearPanelAgente() {
        JPanel panel = crearTarjeta();
        panel.setLayout(new BorderLayout(0, 15));

        JPanel cabecera = new JPanel();
        cabecera.setOpaque(false);
        cabecera.setLayout(new BoxLayout(cabecera, BoxLayout.Y_AXIS));

        JPanel identificador = new JPanel(new BorderLayout());
        identificador.setOpaque(false);
        JLabel titulo = new JLabel("AGENTE 03");
        titulo.setFont(new Font("SansSerif", Font.BOLD, 12));
        titulo.setForeground(OLIVA);
        JLabel distintivo = new JLabel("ACTIVO");
        distintivo.setFont(new Font("SansSerif", Font.BOLD, 10));
        distintivo.setForeground(OLIVA);
        identificador.add(titulo, BorderLayout.WEST);
        identificador.add(distintivo, BorderLayout.EAST);

        JLabel nombre = new JLabel("Heurística euclidiana");
        nombre.setFont(new Font("Serif", Font.BOLD, 23));
        nombre.setForeground(MADERA);
        JLabel algoritmo = new JLabel("Greedy Best-First Search");
        algoritmo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        algoritmo.setForeground(TEXTO);

        etiquetaEstado = new JLabel("Esperando un reto");
        etiquetaEstado.setFont(new Font("SansSerif", Font.BOLD, 12));
        etiquetaEstado.setForeground(TERRACOTA);
        etiquetaEstado.setBorder(new EmptyBorder(10, 0, 0, 0));

        cabecera.add(identificador);
        cabecera.add(Box.createVerticalStrut(12));
        cabecera.add(nombre);
        cabecera.add(Box.createVerticalStrut(3));
        cabecera.add(algoritmo);
        cabecera.add(etiquetaEstado);
        panel.add(cabecera, BorderLayout.NORTH);

        areaResultado = new JTextArea("Solicita un reto y pulsa «Solucionar» para ver el camino, los movimientos y los nodos expandidos.");
        areaResultado.setEditable(false);
        areaResultado.setLineWrap(true);
        areaResultado.setWrapStyleWord(true);
        areaResultado.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaResultado.setForeground(TEXTO);
        areaResultado.setBackground(PAPEL);
        areaResultado.setBorder(new EmptyBorder(13, 13, 13, 13));

        JScrollPane scroll = new JScrollPane(areaResultado);
        scroll.setBorder(BorderFactory.createLineBorder(LINEA));
        scroll.getViewport().setBackground(PAPEL);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel crearTarjeta() {
        JPanel tarjeta = new JPanel();
        tarjeta.setBackground(PAPEL);
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LINEA),
                new EmptyBorder(22, 22, 22, 22)));
        return tarjeta;
    }

    private JButton crearBoton(String texto, Color fondo, Color primerPlano) {
        JButton boton = new JButton(texto);
        boton.setFont(new Font("SansSerif", Font.BOLD, 13));
        boton.setForeground(primerPlano);
        boton.setBackground(fondo);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(fondo.darker()),
                new EmptyBorder(10, 15, 10, 15)));
        boton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return boton;
    }

    public void actualizarTablero(int[] board) {
        panelTablero.mostrarTablero(board);
    }

    public void animarSolucion(List<int[]> path, Runnable onFinished) {
        panelTablero.animar(path, 420, onFinished);
    }

    public void mostrarEstado(String estado) {
        etiquetaEstado.setText(estado);
    }

    public void mostrarResultado(String resultado) {
        areaResultado.setText(resultado);
        areaResultado.setCaretPosition(0);
    }

    public void appendLog(String texto) {
        areaResultado.setText(texto);
        areaResultado.setCaretPosition(0);
    }
}

class PuzzleBoardPanel extends JPanel {
    private static final int TIMER_INTERVAL_MS = 15;
    private static final Color MADERA = new Color(62, 39, 28);
    private static final Color FICHA = new Color(236, 220, 191);
    private static final Color HUECO = new Color(224, 211, 190);

    private int[] board = {1, 2, 3, 4, 0, 5, 7, 8, 6};
    private List<int[]> solutionPath = new ArrayList<>();
    private int pathIndex;
    private int from = -1;
    private int to = -1;
    private int movingTile;
    private long transitionStartedAt;
    private int transitionDurationMs;
    private double progress;
    private javax.swing.Timer timer;
    private Runnable onFinished;

    PuzzleBoardPanel() {
        setOpaque(false);
    }

    void mostrarTablero(int[] newBoard) {
        stopAnimation();
        board = newBoard.clone();
        repaint();
    }

    void animar(List<int[]> path, int durationMs, Runnable callback) {
        stopAnimation();
        solutionPath = new ArrayList<>(path.size());
        for (int[] state : path) {
            solutionPath.add(state.clone());
        }
        if (solutionPath.isEmpty()) {
            if (callback != null) {
                callback.run();
            }
            return;
        }

        board = solutionPath.get(0).clone();
        pathIndex = 1;
        transitionDurationMs = Math.max(100, durationMs);
        onFinished = callback;
        repaint();
        startNextMove();
    }

    private void startNextMove() {
        if (pathIndex >= solutionPath.size()) {
            Runnable callback = onFinished;
            onFinished = null;
            solutionPath.clear();
            if (callback != null) {
                callback.run();
            }
            return;
        }

        int[] nextBoard = solutionPath.get(pathIndex);
        int blankBefore = findBlank(board);
        int blankAfter = findBlank(nextBoard);
        from = blankAfter;
        to = blankBefore;
        movingTile = board[from];
        progress = 0;
        transitionStartedAt = System.nanoTime();
        timer = new javax.swing.Timer(TIMER_INTERVAL_MS, event -> {
            double elapsedMs = (System.nanoTime() - transitionStartedAt) / 1_000_000.0;
            double linearProgress = Math.min(1.0, elapsedMs / transitionDurationMs);
            progress = linearProgress * linearProgress * (3.0 - 2.0 * linearProgress);
            repaint();
            if (linearProgress >= 1.0) {
                timer.stop();
                timer = null;
                board = nextBoard.clone();
                pathIndex++;
                from = -1;
                to = -1;
                repaint();
                startNextMove();
            }
        });
        timer.start();
    }

    private int findBlank(int[] state) {
        for (int i = 0; i < state.length; i++) {
            if (state[i] == 0) {
                return i;
            }
        }
        throw new IllegalArgumentException("El estado del tablero no contiene espacio vacío.");
    }

    private void stopAnimation() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        solutionPath.clear();
        pathIndex = 0;
        from = -1;
        to = -1;
        onFinished = null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padding = 10;
        int gap = 9;
        int side = Math.min(getWidth(), getHeight()) - padding * 2;
        int cell = (side - gap * 2) / 3;
        int originX = (getWidth() - (cell * 3 + gap * 2)) / 2;
        int originY = (getHeight() - (cell * 3 + gap * 2)) / 2;

        g.setColor(MADERA);
        g.fillRoundRect(originX - 8, originY - 8, cell * 3 + gap * 2 + 16,
                cell * 3 + gap * 2 + 16, 20, 20);

        for (int i = 0; i < board.length; i++) {
            if (i == from || i == to) {
                continue;
            }
            int x = originX + (i % 3) * (cell + gap);
            int y = originY + (i / 3) * (cell + gap);
            drawTile(g, board[i], x, y, cell);
        }

        if (from >= 0 && to >= 0) {
            int fromX = originX + (from % 3) * (cell + gap);
            int fromY = originY + (from / 3) * (cell + gap);
            int toX = originX + (to % 3) * (cell + gap);
            int toY = originY + (to / 3) * (cell + gap);
            drawTile(g, 0, fromX, fromY, cell);
            int tileX = (int) Math.round(fromX + (toX - fromX) * progress);
            int tileY = (int) Math.round(fromY + (toY - fromY) * progress);
            drawTile(g, movingTile, tileX, tileY, cell);
        }
        g.dispose();
    }

    private void drawTile(Graphics2D g, int value, int x, int y, int cell) {
        if (value == 0) {
            g.setColor(HUECO);
            g.fillRoundRect(x, y, cell, cell, 18, 18);
            return;
        }

        g.setColor(new Color(28, 18, 12, 45));
        g.fillRoundRect(x + 2, y + 4, cell, cell, 18, 18);
        g.setColor(FICHA);
        g.fillRoundRect(x, y, cell, cell, 18, 18);
        g.setColor(new Color(202, 169, 124));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(x + 1, y + 1, cell - 2, cell - 2, 18, 18);
        g.setColor(MADERA);
        g.setFont(new Font("Serif", Font.BOLD, Math.max(24, cell / 3)));
        String label = Integer.toString(value);
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (cell - metrics.stringWidth(label)) / 2;
        int textY = y + (cell - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(label, textX, textY);
    }
}
