import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.StaleProxyException;

import java.io.IOException;
import java.net.ServerSocket;

public class Main {
    public static void main(String[] args) {
        int puertoLibre = obtenerPuertoLibre();
        System.out.println("Iniciando JADE en puerto " + puertoLibre);

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.MAIN_PORT, String.valueOf(puertoLibre));
        profile.setParameter(Profile.GUI, "true");

        AgentContainer container = Runtime.instance().createMainContainer(profile);

        if (container == null) {
            System.out.println("No se pudo crear el contenedor JADE. Revisa el puerto o si ya hay otro JADE corriendo.");
            return;
        }

        try {
            container.createNewAgent("AgenteGUI", "AgenteGUI", new Object[0]).start();
            container.createNewAgent("Jugador1", "AgenteJugador", new Object[0]).start();
            container.createNewAgent("Jugador2", "AgenteJugador", new Object[0]).start();
            container.createNewAgent("Jugador3", "AgenteJugador", new Object[0]).start();
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    private static int obtenerPuertoLibre() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo obtener un puerto libre para JADE", e);
        }
    }
}