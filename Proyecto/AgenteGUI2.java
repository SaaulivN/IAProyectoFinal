import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

public class AgenteGUI2 extends Agent {
    private VentanaCompetencia gui;
    private int[] currentBoard = {1, 2, 3, 4, 0, 5, 7, 8, 6};
    private int[] pendingBoard;
    private String currentRunId;
    private long runSequence;

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

                if (msg.getPerformative() != ACLMessage.INFORM) {
                    return;
                }

                String content = msg.getContent();
                String sender = msg.getSender().getLocalName();
                if ("AgenteGeneradorReto".equals(sender)) {
                    recibirReto(content);
                } else if ("Jugador1".equals(sender)) {
                    recibirResultado(content, msg.getConversationId());
                }
            }
        });
    }

    private void recibirReto(String contenido) {
        try {
            int[] nuevoTablero = PuzzleState.deserialize(contenido);
            if (contenido.split(",", -1).length != 9 || !esTableroValido(nuevoTablero)) {
                throw new IllegalArgumentException("el generador envió un tablero inválido.");
            }
            currentBoard = nuevoTablero.clone();
            currentRunId = null;
            SwingUtilities.invokeLater(() -> {
                if (gui != null) {
                    gui.actualizarTablero(nuevoTablero);
                    gui.appendLog("Nuevo reto recibido y mostrado en el tablero.");
                }
            });
        } catch (RuntimeException ex) {
            SwingUtilities.invokeLater(() -> {
                if (gui != null) gui.appendLog("No se pudo cargar el reto: " + ex.getMessage());
            });
        }
    }

    private void recibirResultado(String contenido, String conversationId) {
        if (currentRunId == null || !currentRunId.equals(conversationId)) {
            return;
        }
        if (contenido == null || !contenido.startsWith("RESULTADO|")) {
            SwingUtilities.invokeLater(() -> {
                if (gui != null) gui.appendLog(contenido);
            });
            return;
        }

        String[] partes = contenido.split("\\|", -1);
        if (partes.length < 5) {
            SwingUtilities.invokeLater(() -> {
                if (gui != null) gui.appendLog("El jugador devolvió un resultado incompleto.");
            });
            return;
        }

        try {
            String estado = partes[1];
            long tiempoMs = Long.parseLong(partes[2]);
            int nodos = Integer.parseInt(partes[3]);
            List<String> movimientos = partes[4].isEmpty()
                    ? java.util.Collections.<String>emptyList()
                    : Arrays.asList(partes[4].split(";"));

            SwingUtilities.invokeLater(() -> {
                if (gui == null) return;
                if ("OK".equals(estado)) {
                    String camino = movimientos.isEmpty()
                            ? "(el tablero ya estaba resuelto)"
                            : String.join(" -> ", movimientos);
                    String reporte = "Solución encontrada en " + movimientos.size() + " pasos. Tiempo: "
                            + tiempoMs + " ms. Nodos explorados: " + nodos + ". Camino: " + camino;
                    gui.animarSolucion(pendingBoard.clone(), movimientos, reporte);
                } else {
                    gui.appendLog("No se encontró solución. Tiempo: " + tiempoMs
                            + " ms. Nodos explorados: " + nodos + ".");
                }
            });
        } catch (RuntimeException ex) {
            SwingUtilities.invokeLater(() -> {
                if (gui != null) gui.appendLog("No se pudo interpretar el resultado: " + ex.getMessage());
            });
        }
    }

    private boolean esTableroValido(int[] tablero) {
        if (tablero.length != 9) return false;
        boolean[] vistos = new boolean[9];
        for (int valor : tablero) {
            if (valor < 0 || valor > 8 || vistos[valor]) return false;
            vistos[valor] = true;
        }
        return PuzzleState.isSolvable(tablero);
    }

    public void solicitarReto() {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("AgenteGeneradorReto", AID.ISLOCALNAME));
        msg.setContent("SOLICITAR_RETO");
        send(msg);
    }

    public void iniciarCompetencia() {
        pendingBoard = currentBoard.clone();
        currentRunId = "solve-" + (++runSequence);
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("Jugador1", AID.ISLOCALNAME));
        msg.setConversationId(currentRunId);
        msg.setContent("INICIAR:" + PuzzleState.serialize(pendingBoard));
        send(msg);
        if (gui != null) gui.appendLog("Búsqueda iniciada por Jugador1.");
    }
}

class VentanaCompetencia extends JFrame {
    private static final int DURACION_MOVIMIENTO_MS = 700;

    private final AgenteGUI agente;
    private final PuzzleBoardPanel panelTablero = new PuzzleBoardPanel();
    private final JTextArea areaLog = new JTextArea();

    VentanaCompetencia(AgenteGUI agente) {
        this.agente = agente;
        setTitle("8-Puzzle - Agente Greedy Best-First");
        setSize(850, 520);
        setMinimumSize(new Dimension(760, 460));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel contenido = new JPanel(new BorderLayout(12, 12));
        contenido.setBorder(new EmptyBorder(12, 12, 12, 12));
        contenido.setBackground(new Color(242, 245, 249));
        setContentPane(contenido);

        JPanel panelTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panelTop.setOpaque(false);
        JButton btnGenerar = new JButton("Solicitar nuevo reto");
        JButton btnIniciar = new JButton("Resolver puzzle");
        panelTop.add(btnGenerar);
        panelTop.add(btnIniciar);
        contenido.add(panelTop, BorderLayout.NORTH);

        panelTablero.setPreferredSize(new Dimension(390, 390));
        contenido.add(panelTablero, BorderLayout.WEST);

        areaLog.setEditable(false);
        areaLog.setLineWrap(true);
        areaLog.setWrapStyleWord(true);
        areaLog.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setBorder(BorderFactory.createTitledBorder("Resultado y movimientos"));
        contenido.add(scroll, BorderLayout.CENTER);

        btnGenerar.addActionListener((ActionEvent e) -> agente.solicitarReto());
        btnIniciar.addActionListener((ActionEvent e) -> agente.iniciarCompetencia());
        actualizarTablero(new int[]{1, 2, 3, 4, 0, 5, 7, 8, 6});
        appendLog("Solicita un reto o resuelve el tablero visible.");
    }

    void actualizarTablero(int[] board) {
        panelTablero.mostrarTablero(board);
    }

    void animarSolucion(int[] board, List<String> movimientos, String reporteFinal) {
        panelTablero.animar(board, movimientos, DURACION_MOVIMIENTO_MS, () -> appendLog(reporteFinal));
    }

    void appendLog(String texto) {
        areaLog.append(texto + "\n\n");
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }
}

class PuzzleBoardPanel extends JPanel {
    private static final int TIMER_INTERVAL_MS = 15;
    private static final Color EMPTY_COLOR = new Color(220, 226, 234);
    private static final Color TILE_COLOR = new Color(54, 103, 166);

    private int[] board = {1, 2, 3, 4, 0, 5, 7, 8, 6};
    private Timer timer;
    private List<String> movimientos = java.util.Collections.emptyList();
    private int indiceMovimiento;
    private int desde = -1;
    private int hasta = -1;
    private int baldosaEnMovimiento;
    private long inicioTransicion;
    private int duracionTransicion;
    private double progreso;
    private Runnable alTerminar;

    PuzzleBoardPanel() {
        setOpaque(false);
    }

    void mostrarTablero(int[] nuevoTablero) {
        detenerAnimacion();
        board = nuevoTablero.clone();
        repaint();
    }

    void animar(int[] tableroInicial, List<String> listaMovimientos, int duracionMs, Runnable callback) {
        detenerAnimacion();
        board = tableroInicial.clone();
        movimientos = new java.util.ArrayList<>(listaMovimientos);
        indiceMovimiento = 0;
        duracionTransicion = Math.max(100, duracionMs);
        alTerminar = callback;
        repaint();
        iniciarSiguienteMovimiento();
    }

    private void iniciarSiguienteMovimiento() {
        if (indiceMovimiento >= movimientos.size()) {
            Runnable callback = alTerminar;
            alTerminar = null;
            movimientos = java.util.Collections.emptyList();
            if (callback != null) callback.run();
            return;
        }

        int indiceVacio = indiceDeVacio();
        int delta = deltaPara(movimientos.get(indiceMovimiento));
        int vecino = indiceVacio + delta;
        if (delta == 0 || vecino < 0 || vecino >= 9
                || (delta == -1 && indiceVacio % 3 == 0)
                || (delta == 1 && indiceVacio % 3 == 2)
                || board[vecino] == 0) {
            // Un movimiento inválido cancela la animación sin mostrar un estado incoherente.
            detenerAnimacion();
            repaint();
            return;
        }

        desde = vecino;
        hasta = indiceVacio;
        baldosaEnMovimiento = board[desde];
        progreso = 0;
        inicioTransicion = System.nanoTime();
        timer = new Timer(TIMER_INTERVAL_MS, e -> {
            double transcurrido = (System.nanoTime() - inicioTransicion) / 1_000_000.0;
            double t = Math.min(1.0, transcurrido / duracionTransicion);
            progreso = t * t * (3.0 - 2.0 * t); // suavizado ease-in/ease-out
            repaint();
            if (t >= 1.0) {
                timer.stop();
                timer = null;
                board[hasta] = baldosaEnMovimiento;
                board[desde] = 0;
                indiceMovimiento++;
                desde = -1;
                hasta = -1;
                repaint();
                iniciarSiguienteMovimiento();
            }
        });
        timer.start();
    }

    private int deltaPara(String movimiento) {
        if ("Arriba".equals(movimiento)) return -3;
        if ("Abajo".equals(movimiento)) return 3;
        if ("Izquierda".equals(movimiento)) return -1;
        if ("Derecha".equals(movimiento)) return 1;
        return 0;
    }

    private int indiceDeVacio() {
        for (int i = 0; i < board.length; i++) if (board[i] == 0) return i;
        return -1;
    }

    private void detenerAnimacion() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        movimientos = java.util.Collections.emptyList();
        indiceMovimiento = 0;
        desde = -1;
        hasta = -1;
        alTerminar = null;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int padding = 10;
        int gap = 8;
        int side = Math.min(getWidth(), getHeight()) - padding * 2;
        int cell = (side - gap * 2) / 3;
        int originX = (getWidth() - (cell * 3 + gap * 2)) / 2;
        int originY = (getHeight() - (cell * 3 + gap * 2)) / 2;

        for (int i = 0; i < board.length; i++) {
            if (i == desde || i == hasta) continue;
            int x = originX + (i % 3) * (cell + gap);
            int y = originY + (i / 3) * (cell + gap);
            dibujarCelda(g, board[i], x, y, cell);
        }

        if (desde >= 0 && hasta >= 0) {
            double fromX = originX + (desde % 3) * (cell + gap);
            double fromY = originY + (desde / 3) * (cell + gap);
            double toX = originX + (hasta % 3) * (cell + gap);
            double toY = originY + (hasta / 3) * (cell + gap);
            dibujarCelda(g, 0, (int) fromX, (int) fromY, cell);
            int x = (int) Math.round(fromX + (toX - fromX) * progreso);
            int y = (int) Math.round(fromY + (toY - fromY) * progreso);
            dibujarCelda(g, baldosaEnMovimiento, x, y, cell);
        }
        g.dispose();
    }

    private void dibujarCelda(Graphics2D g, int valor, int x, int y, int cell) {
        if (valor == 0) {
            g.setColor(EMPTY_COLOR);
            g.fillRoundRect(x, y, cell, cell, 20, 20);
            return;
        }

        g.setColor(new Color(18, 40, 70, 35));
        g.fillRoundRect(x + 2, y + 4, cell, cell, 20, 20);
        g.setColor(TILE_COLOR);
        g.fillRoundRect(x, y, cell, cell, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(24, cell / 3)));
        String label = String.valueOf(valor);
        FontMetrics metrics = g.getFontMetrics();
        int textX = x + (cell - metrics.stringWidth(label)) / 2;
        int textY = y + (cell - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString(label, textX, textY);
    }
}