import java.util.Random;

public class PuzzleState {
    
    // Verifica si un arreglo de 9 enteros (con 0 como vacío) es soluble
    public static boolean isSolvable(int[] flat) {
        int inversions = 0;
        for (int i = 0; i < 9; i++) {
            for (int j = i + 1; j < 9; j++) {
                if (flat[i] != 0 && flat[j] != 0 && flat[i] > flat[j]) {
                    inversions++;
                }
            }
        }
        return (inversions % 2 == 0);
    }

    // Genera un tablero aleatorio de 9 elementos validando que tenga solución
    public static int[] generateRandomValidBoard() {
        Random random = new Random();
        int[] numbers = {0, 1, 2, 3, 4, 5, 6, 7, 8};
        
        do {
            // Barajar el arreglo (Fisher-Yates)
            for (int i = numbers.length - 1; i > 0; i--) {
                int index = random.nextInt(i + 1);
                int temp = numbers[index];
                numbers[index] = numbers[i];
                numbers[i] = temp;
            }
        } while (!isSolvable(numbers)); // Descarta automáticamente los que no tengan solución

        return numbers;
    }

    // Serializa el arreglo a String para enviarlo por FIPA-ACL ("1,2,3,4,0,5,6,7,8")
    public static String serialize(int[] board) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < board.length; i++) {
            sb.append(board[i]);
            if (i < board.length - 1) sb.append(",");
        }
        return sb.toString();
    }

    // Deserializa el String recibido a un arreglo int[9]
    public static int[] deserialize(String data) {
        String[] parts = data.split(",");
        int[] board = new int[9];
        for (int i = 0; i < 9; i++) {
            board[i] = Integer.parseInt(parts[i].trim());
        }
        return board;
    }
}