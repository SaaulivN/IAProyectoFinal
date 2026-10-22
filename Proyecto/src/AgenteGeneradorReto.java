import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class AgenteGeneradorReto extends Agent {

    @Override
    protected void setup() {
        System.out.println("[GeneradorReto] Agente iniciado: " + getAID().getLocalName());

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage msg = myAgent.receive();
                if (msg != null) {
                    if (msg.getPerformative() == ACLMessage.REQUEST) {
                        String content = msg.getContent();
                        
                        if (content != null && content.contains("SOLICITAR_RETO")) {
                            // Genera el estado inicial asegurando que tenga solución
                            int[] tableroValido = PuzzleState.generateRandomValidBoard();
                            String tableroStr = PuzzleState.serialize(tableroValido);

                            ACLMessage reply = msg.createReply();
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent(tableroStr);
                            myAgent.send(reply);
                            
                            System.out.println("[GeneradorReto] Reto válido generado y enviado: " + tableroStr);
                        }
                    }
                } else {
                    block();
                }
            }
        });
    }
}