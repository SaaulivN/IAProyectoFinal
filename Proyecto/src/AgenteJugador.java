import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.List;

public class AgenteJugador extends Agent {

    private static final String START_PREFIX = "INICIAR:";

    @Override
    protected void setup() {
        System.out.println("Agente Jugador iniciado: " + getLocalName());

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                ACLMessage msg = receive(mt);

                if (msg != null) {
                    String contenido = msg.getContent();
                    System.out.println("[" + getLocalName() + "] Mensaje recibido: " + contenido);

                    if (contenido != null && contenido.startsWith(START_PREFIX)) {
                        responderSolucion(msg, contenido.substring(START_PREFIX.length()));
                    }
                } else {
                    block(); // Bloquear hasta recibir un nuevo mensaje
                }
            }
        });
    }

    private void responderSolucion(ACLMessage msg, String boardInput) {
        try {
            String[] values = boardInput.trim().split("[,\\s]+");
            int[] board = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                board[i] = Integer.parseInt(values[i]);
            }

            EightPuzzleSolver.Result result = EightPuzzleSolver.solve(board);
            if (!result.found()) {
                responder(msg, "El tablero no es soluble respecto a la meta 1,2,3,4,5,6,7,8,0.");
                return;
            }

            List<int[]> path = result.getPath();
            StringBuilder response = new StringBuilder();
            response.append("Greedy Best-First con heurística euclidiana\n")
                    .append("Movimientos: ").append(path.size() - 1).append('\n')
                    .append("Nodos expandidos: ").append(result.getExpandedNodes()).append('\n')
                    .append("Camino:\n");
            for (int i = 0; i < path.size(); i++) {
                int[] currentBoard = path.get(i);
                response.append(i)
                        .append(". h(n)=").append(EightPuzzleSolver.euclideanDistance(currentBoard))
                        .append("  ").append(formatBoard(currentBoard)).append('\n');
            }
            response.append("ANIMACION:");
            for (int i = 0; i < path.size(); i++) {
                if (i > 0) {
                    response.append(';');
                }
                response.append(PuzzleState.serialize(path.get(i)));
            }
            responder(msg, response.toString());
        } catch (IllegalArgumentException e) {
            responder(msg, "Tablero inválido: " + e.getMessage());
        }
    }

    private String formatBoard(int[] board) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            if (i > 0) {
                formatted.append(i % 3 == 0 ? " / " : " ");
            }
            formatted.append(board[i] == 0 ? "_" : Integer.toString(board[i]));
        }
        return formatted.toString();
    }

    private void responder(ACLMessage request, String content) {
        ACLMessage reply = request.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(content);
        send(reply);
        System.out.println("[" + getLocalName() + "] Respondió:\n" + content);
    }
}