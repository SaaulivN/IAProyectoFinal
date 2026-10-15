import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AgenteGUI extends Agent {

    private JFrame ventana;
    private JTextArea areaTexto;
    private JButton btnIniciar;
    private JTextField tableroInicial;

    private String[] jugadores = {"Jugador1", "Jugador2", "Jugador3"};
    private static final String TABLERO_INICIAL = "1,2,3,4,0,6,7,5,8";

    @Override
    protected void setup() {
        System.out.println("Agente GUI iniciado: " + getLocalName());

        // Crear Interfaz Gráfica
        crearYMostrarGUI();

        // Comportamiento para recibir las respuestas de los jugadores
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String remitente = msg.getSender().getLocalName();
                    String respuesta = msg.getContent();

                    SwingUtilities.invokeLater(() -> {
                        areaTexto.append("[" + remitente + "]: " + respuesta + "\n");
                    });
                } else {
                    block();
                }
            }
        });
    }

    private void crearYMostrarGUI() {
        ventana = new JFrame("Panel del Agente GUI - 8-Puzzle");
        ventana.setSize(600, 420);
        ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ventana.setLayout(new BorderLayout());

        JPanel panelEntrada = new JPanel(new BorderLayout(8, 0));
        panelEntrada.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelEntrada.add(new JLabel("Tablero inicial (0 representa el espacio):"), BorderLayout.NORTH);
        tableroInicial = new JTextField(TABLERO_INICIAL);
        panelEntrada.add(tableroInicial, BorderLayout.CENTER);
        ventana.add(panelEntrada, BorderLayout.NORTH);

        areaTexto = new JTextArea();
        areaTexto.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(areaTexto);
        ventana.add(scrollPane, BorderLayout.CENTER);

        btnIniciar = new JButton("Iniciar");
        btnIniciar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                areaTexto.append("--- Iniciando jugadores y resolviendo el tablero ---\n");
                enviarNotificacionInicio();
            }
        });

        ventana.add(btnIniciar, BorderLayout.SOUTH);
        ventana.setLocationRelativeTo(null);
        ventana.setAlwaysOnTop(true); // Fuerza a la ventana a colocarse al frente
        ventana.setVisible(true);
    }

    private void enviarNotificacionInicio() {
        for (String jugador : jugadores) {
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID(jugador, AID.ISLOCALNAME));
            if ("Jugador3".equals(jugador)) {
                msg.setContent("resolver|" + tableroInicial.getText());
            } else {
                msg.setContent("competencia iniciada");
            }
            send(msg);
        }
    }

    @Override
    protected void takeDown() {
        if (ventana != null) {
            ventana.dispose();
        }
        System.out.println("Agente GUI finalizado.");
    }
}