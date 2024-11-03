/**
 * @author cesar
 * @author Sami
 */

package buscaminas;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;


public class Cliente {
    public static int ganar=0;
    public static void main(String[] args) {
        try {
            Socket cliente = new Socket("127.0.0.1", 1234);

            // Flujo para enviar datos al servidor
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream()), true); // Auto-flush
            // Leer datos desde la consola del cliente
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            // Solicitar el nombre del jugador
            System.out.println("Ingresa tu nombre: ");
            String nombreJugador = br.readLine(); // Nombre del jugador
            pw.println(nombreJugador);  // Enviar el nombre al servidor

            System.out.println("----------------BUSCAMINAS--------------------");
            System.out.println("NIVELES       \n\t1. Facil    \n\t2. Intermedio   \n\t3. Dificil");
            // Leer la respuesta del servidor desde el socket
            BufferedReader br2 = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
            String line= br2.readLine();

            //---------------------------------------------------- Leer el  MENU, ESCOGER EL NIVEL DE DIFICULTAD----------------------------------------------------
            System.out.println(line);
            
            // Leer el nivel desde la consola
            String nivel = br.readLine(); // El cliente debe ingresar su elección
            pw.println(nivel); // Enviar el nivel al servidor
            
            

            // Esperar confirmación del servidor
            String confirmacion = br2.readLine(); // Leer la confirmación del servidor
            System.out.println(confirmacion); // Mostrar la confirmación

            ObjectOutputStream oos = new ObjectOutputStream(cliente.getOutputStream());
            ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());
            
            //---------------------------------------------------- TABLERO CON PUROS "-"----------------------------------------------------

            Instant tiempoInicio = Instant.now();
            Tablero ob2 = (Tablero) ois.readObject();
            System.out.println("Objeto recibido desde " + cliente.getInetAddress() + ":" + cliente.getPort() + " con los datos:");
            //System.out.println("x:" + ob2.getX() + " y:" + ob2.getY());
            String[][]MatrizJuego = ob2.getM();
            MatrizJuego[0][0]="-";

            System.out.println("Tablero del juego: \n");
            for(int i=0;i<MatrizJuego.length;i++){
                for(int j=0;j<MatrizJuego[1].length;j++){
                    System.out.print(MatrizJuego[i][j]+"    ");
                }//for
                System.out.println("");
            }
            
                String accion;
            //---------------------------------------------------- ESCOGER QUE ACCION HARÁ----------------------------------------------------
            do {
                    System.out.println("\n\nOpciones:\n     1. Descubrir casilla\n     2. Insertar bandera\n    3. Quitar bandera\nEscoge una:");

                accion = br.readLine(); // El cliente debe ingresar su elección
                pw.println(accion); // Enviar la opción al servidor

                //---------------------------------------------------- LEER COORDENADAS QUE SE PASAN COMO UN OBJETO PERO SOLO X,Y ----------------------------------------------------
                if (accion.equals("1") || accion.equals("2") || accion.equals("3")) {
                    // Pedir las coordenadas al usuario
                    String coordenada="";
                    boolean formatoCorrecto = false;

                    while (!formatoCorrecto) {
                        System.out.println("Inserta las coordenadas (ejemplo: A 3):");
                        coordenada = br.readLine();
                        // Verifica que la coordenada siga el formato correcto usando una expresión regular
                        if (coordenada.matches("^[A-Za-z]{1,2}\\s\\d{1,2}$")) {
                            formatoCorrecto = true;
                            System.out.println("Coordenada valida: " + coordenada);
                        } else {
                            System.out.println("Formato incorrecto. Inténtalo de nuevo.");
                        }
                    }
                    String[] coordenadas2 = coordenada.split(" ");

                    // Enviar el objeto Tablero al servidor con las coordenadas
                    Tablero juego = new Tablero(coordenadas2[0], coordenadas2[1], null,0);
                    oos.writeObject(juego);
                    oos.flush();

                    //---------------------------------------------------- NOS MANDA EL TABLERO DE LAS QUE DESTAPAMOS O PERDIMOS ----------------------------------------------------
                    // Recibir la respuesta del servidor (tablero actualizado)
                    Tablero ob3 = (Tablero) ois.readObject();
                    String[][] MatrizJuego2 = ob3.getM();

                    //MatrizJuego[0][0]="-";
                    // Imprimir el tablero del juego
                    System.out.println("Tablero del juego: \n");
                    for (int i = 0; i < MatrizJuego2.length; i++) {
                        for (int j = 0; j < MatrizJuego2[2].length; j++) {
                            System.out.print(MatrizJuego2[i][j] + "    ");
                        }
                        System.out.println("");
                    }
                    ganar = ob3.getGanar();

                    


                }else {
                    System.out.println("Opción inválida");
                }

                // Leer el valor de mina desde el servidor
                int mina = Integer.parseInt(br2.readLine());
                // Si mina es 1, significa que se ha encontrado una mina, terminar el bucle
                if (mina == 1) {
                    System.out.println("¡Fin del juego! Has encontrado una mina.");
                    break;
                }
                if (ganar ==1) {
                    System.out.println("¡Fin del juego! Has ganado.");
                    break;
                    }
            } while (true); // Continuar el bucle hasta que se encuentre una mina
            // Registrar tiempo de fin
            Instant tiempoFin = Instant.now();
            Duration duracion = Duration.between(tiempoInicio, tiempoFin);
            long tiempoTotal = duracion.getSeconds(); // En segundos

            pw.println(tiempoTotal);
            
            System.out.println("\n--- Records ---");
            String lineFromRecords;
            while ((lineFromRecords = br2.readLine()) != null) {
                System.out.println(lineFromRecords);  // Imprimir cada línea de records recibida del servidor
            }

        
            // Cerrar flujos y socket&
            ois.close();
            br2.close();
            br.close();
            pw.close();
            cliente.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
