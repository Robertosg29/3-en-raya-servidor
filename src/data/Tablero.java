package data;

/**
 *
 * @author Rober
 */
public class Tablero {

    final int NUM_FILAS = 3;
    char[][] tablero = new char[NUM_FILAS][NUM_FILAS];
    final int NUM_JUGADORES = 2;

    public Tablero() {
        inicializarTablero();
    }

    public boolean colocarFicha(int fila, int columna, char ficha) {

        if (fila <= 2 & columna <= 2 & fila >= 0 & columna >= 0) {

            if (tablero[fila][columna] == ' ') {

                tablero[fila][columna] = ficha;
                return true;//comprobamos que los numeros esten dentro del array y comprobamos que no haya ficha pintada
            }
        }

        return false;
    }

    private void inicializarTablero() {
        for (int i = 0; i < NUM_FILAS; i++) {
            for (int j = 0; j < NUM_FILAS; j++) {
                tablero[i][j] = ' ';

            }

        }
    }

     public char comprobarGanador() {
        char ficha;
        for (int i = 0; i < 2; i++) {
            if (i == 0) {
                ficha = 'X';
            } else {
                ficha = 'O';
            }
            if (tablero[0][0] != ' ' && tablero[0][0] == ficha) {

                if (tablero[0][1] == ficha && tablero[0][2] == ficha) {
                    return ficha;
                } else if (tablero[1][1] == ficha && tablero[2][2] == ficha) {
                    return ficha;
                } else if (tablero[1][0] == ficha && tablero[2][0] == ficha) {
                    return ficha;
                }

            }

            if (tablero[0][1] != ' ' && tablero[0][1] == ficha) {
                if (tablero[1][1] == ficha && tablero[2][1] == ficha) {
                    return ficha;
                }
            }

            if (tablero[0][2] != ' ' && tablero[0][2] == ficha) {
                if (tablero[1][1] == ficha && tablero[2][0] == ficha) {
                    return ficha;
                } else if (tablero[1][2] == ficha && tablero[2][2] == ficha) {
                    return ficha;
                }
            }

            if (tablero[1][0] != ' ' && tablero[1][0] == ficha) {
                if (tablero[1][1] == ficha && tablero[1][2] == ficha) {
                    return ficha;
                }
            }

            if (tablero[2][0] != ' ' && tablero[2][0] == ficha) {
                if (tablero[2][1] == ficha && tablero[2][2] == ficha) {
                    return ficha;
                }
            }

        }

        return 'F';
    }

    public void pintarTablero() {

        System.out.print("  ");
        for (int i = 0; i < NUM_FILAS; i++) {
            System.out.print(i + "  ");
        }
        System.out.println("");
        for (int i = 0; i < NUM_FILAS; i++) {
            System.out.print(i + "|");
            for (int j = 0; j < NUM_FILAS; j++) {
                System.out.print(tablero[i][j] + " |");
            }
            System.out.println("");
        }
    }

}
