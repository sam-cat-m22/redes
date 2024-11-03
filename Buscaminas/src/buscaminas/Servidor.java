/**
 * @author cesar
 * @author Sami
 */
package buscaminas;

import java.io.*;
import java.net.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;

public class Servidor {
    
    public static int mina = 0;  // 0 = no mina, 1 = mina encontrada
    public static int ganar = 0; // 0 = no ganado, 1 = ganado
    
    public static String[][] banderas(String x, String y, String[][] matriz, int opcion){
        int columna = x.toUpperCase().charAt(0) - 'A' + 1;  // Convertir letra a índice, por ejemplo 'A' -> 1
        // Convertir el número (y) en el índice correcto
        int fila = Integer.parseInt(y);  // Convertir el número string a entero para la fila
 
        if(opcion==2 && matriz[fila][columna].equals("-")){
            matriz[fila][columna]="B";
        }else if(opcion==3 && matriz[fila][columna].equals("B")){
            matriz[fila][columna]="-";            
        }
        
        return matriz;        
    }
    
    public static int verificarVictoria(String[][] matrizNumerica, String[][] matrizCliente) {
        // Recorremos la matriz desde la fila 1 y columna 1 para evitar la primera fila y columna
        for (int i = 1; i < matrizCliente.length; i++) {
            for (int j = 1; j < matrizCliente[2].length; j++) {
                // Si la casilla en matrizNumerica es diferente de "-1" (es decir, no es una mina)
                // y la casilla en matrizCliente es igual a "-", significa que no ha sido descubierta
                if (!matrizNumerica[i][j].equals("-1") && matrizCliente[i][j].equals("-")) {
                    return 0; // Aún no ha ganado
                }
            }
        }
        return 1; // Ha ganado
    }

    public static void sincronizarTableros(String[][] matrizPrincipal, String[][] matrizSecundaria) {
        int filas = matrizPrincipal.length;
        int columnas = matrizPrincipal[0].length;

        // Iterar sobre ambas matrices y sincronizar los valores
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                // Si en la matriz principal hay un espacio vacío, lo sincronizamos en la secundaria
                if (matrizPrincipal[i][j].equals(" ")) {
                    matrizSecundaria[i][j] = "0";  // Actualizar la matriz secundaria

                    // Revisar las casillas circundantes
                    for (int x = i - 1; x <= i + 1; x++) {
                        for (int y = j - 1; y <= j + 1; y++) {
                            // Verificar que la casilla esté dentro de los límites de la matriz
                            if (x >= 0 && x < filas && y >= 0 && y < columnas) {
                                // Si la casilla es un número, copiarlo a la secundaria y poner "X" en la principal
                                if (esNumero(matrizPrincipal[x][y])) {
                                    matrizSecundaria[x][y] = matrizPrincipal[x][y];  // Copiar el número a la secundaria
                                    matrizPrincipal[x][y] = "X";  // Cambiar el número a "X" en la principal
                                }
                            }
                        }
                    }
                }
            }
        }
    }
// Función auxiliar para verificar si una cadena representa un número
public static boolean esNumero(String valor) {
    try {
        Integer.parseInt(valor);  // Intenta convertir el valor a número
        return true;  // Si la conversión es exitosa, es un número
    } catch (NumberFormatException e) {
        return false;  // Si ocurre una excepción, no es un número
   }
}


//ESTA FUNCION DESTAPA RECURSIVAMENTE TODOS LOS CONTORNOS DE LA CASILLA SI CONTIENE 0
//ES EL METODO DEL PROFE
//IGUAL MODIFICA LA MATRIZ NUMERICA Y DEJA LIBRES LOS ESPACIOS QUE TENIAN CERO PARA COPIARLAS EN LA DEL CLIENTE

public static void destaparCeldas(String[][] tablero, boolean[][] descubiertas, int fila, int columna) {
    // Comprobar que las coordenadas están dentro de los límites del tablero
    tablero[0][0]= "-";
    if (fila < 0 || fila >= tablero.length || columna < 0 || columna >= tablero[0].length) {
        return; // Fuera de límites, salir
    }

    // Si la celda ya está descubierta, salir
    if (descubiertas[fila][columna]) {
        return;
    }

    // Marcar la celda como descubierta
    descubiertas[fila][columna] = true;

    // Si la celda contiene un número mayor a 0, no hacer más cambios
    if (!tablero[fila][columna].equals("0")) {
        return;
    }
    // Si la celda es "0", cambiarla a " " para indicar que está descubierta
    tablero[fila][columna] = " ";

    // Continuar destapando las casillas adyacentes (8 direcciones)
    for (int i = -1; i <= 1; i++) {
        for (int j = -1; j <= 1; j++) {
            if (i != 0 || j != 0) { // Evitar la celda actual
                destaparCeldas(tablero, descubiertas, fila + i, columna + j);
            }
        }
    }
}

public static int EjecucionTablero(String x, String y, String[][] matriz, String[][] matriz2, int mina) {
    // Convertir la letra (x) en el índice correcto para columnas (soporta hasta "ZZ")
    int columna = 0;
    x = x.toUpperCase();  // Asegurarse de que la letra esté en mayúscula

    // Convertir cada letra en base 26 (A=1, B=2, ..., Z=26, AA=27, AB=28, ...)
    for (int i = 0; i < x.length(); i++) {
        columna = columna * 26 + (x.charAt(i) - 'A' + 1);  // Base 26 para las columnas
    }

    // Convertir el número (y) en el índice correcto para la fila
    int fila = Integer.parseInt(y);  // Convertir el número string a entero para la fila

    // Acceder a la casilla de la matriz
    String valorCasilla = matriz[fila][columna];  // Matriz que pasas como parámetro

    // Decidir según el valor de la casilla
    if (valorCasilla.equals("0") && matriz2[fila][columna].equals("-")) {
        System.out.println("Es un 0, sigue jugando...");

        //imprimirMatriz(matriz);
        boolean[][] descubiertas = new boolean[matriz.length][matriz[0].length];
        destaparCeldas(matriz, descubiertas, fila, columna);

        // Aquí podrías hacer algo, como descubrir más casillas
        sincronizarTableros(matriz, matriz2);
    } else if (Integer.parseInt(valorCasilla) > 0 && matriz2[fila][columna].equals("-")) {
        System.out.println("Es un número mayor a 0, mostrar al jugador...");
        matriz2[fila][columna] = matriz[fila][columna];
        matriz[fila][columna] = "X";
    } else if (valorCasilla.equals("-1")) {
        System.out.println("Es una mina, juego terminado.");
        mina = 1;
    } else {
    }

    matriz2[0][0] = "-";

    return mina;
}



    public static String[][] generarMatrizOculta(int filas, int columnas) {
    String[][] matriz = new String[filas][columnas];  // Crear matriz de cualquier tamaño

    // Rellenar la primera fila con letras o combinaciones de letras (A, B, C, ..., Z, AA, AB, ...)
    for (int j = 1; j < columnas; j++) {
        matriz[0][j] = convertirColumnaAString(j);  // Usamos la función convertirColumnaAString para obtener el formato adecuado
    }

    // Rellenar la primera columna con números (1, 2, 3, ..., según las filas)
    for (int i = 1; i < filas; i++) {
        matriz[i][0] = String.valueOf(i);  // Números de 1 a filas-1
    }

    // Rellenar el resto de la matriz con guiones inicialmente
    for (int i = 1; i < filas; i++) {
        for (int j = 1; j < columnas; j++) {
            matriz[i][j] = "-";
        }
    }

    return matriz;
}


    // Función para imprimir la matriz
    public static void imprimirMatriz(String[][] matriz) {
        for (int i = 0; i < matriz.length; i++) {
            System.out.print(" \n\n");
            for (int j = 0; j < matriz[i].length; j++) {
                if (matriz[i][j] == null) {
                    System.out.print(" \t");
                } else {
                    System.out.print(matriz[i][j] + "\t");
                }
            }
            System.out.println();
        }
    }
    public static void rellenarPistas(String[][] matriz) {
        int filas = matriz.length;
        int columnas = matriz[0].length;

        // Recorrer toda la matriz, comenzando en la fila 1 y columna 1 (evitando las coordenadas)
        for (int i = 1; i < filas; i++) {
            for (int j = 1; j < columnas; j++) {
                // Solo contar minas en casillas que no tienen mina
                if (!matriz[i][j].equals("-1")) {
                    
                    int minasCircundantes = contarMinasCircundantes(matriz, i, j);
                    matriz[i][j] = String.valueOf(minasCircundantes);
                }
            }
        }
    }

    // Función auxiliar para contar las minas alrededor de una casilla
    public static int contarMinasCircundantes(String[][] matriz, int fila, int columna) {
        int minas = 0;
        int filas = matriz.length;
        int columnas = matriz[0].length;
        // Recorrer las 8 casillas circundantes
        for (int i = fila - 1; i <= fila + 1; i++) {
            for (int j = columna - 1; j <= columna + 1; j++) {
                // Verificar que las casillas estén dentro de los límites
                if (i >= 1 && i < filas && j >= 1 && j < columnas) { // Cambia 0 a 1
                    // Incrementar el contador si hay una mina
                    if (matriz[i][j].equals("-1")) {
                        minas++;
                    }
                }
            }
        }
        return minas;
    }
    // Función que convierte el índice de la columna a una letra o combinación de letras
    public static String convertirColumnaAString(int columna) {
        StringBuilder sb = new StringBuilder();
        while (columna > 0) {
            columna--;  // Ajuste para que la columna comience en 0 (A=1, B=2, ... Z=26, AA=27)
            sb.insert(0, (char) ('A' + columna % 26));  // Agregar la letra correspondiente al principio
            columna /= 26;  // Dividir el índice para la siguiente letra (base 26)
        }
        return sb.toString();  // Devolver la cadena resultante
    }

    
    public static String[][] crearMatriz(int filas, int columnas, int numeroMinas) {
    String[][] matriz = new String[filas + 1][columnas + 1];  // Crear matriz con espacio para coordenadas

    Random random = new Random();
    int minasColocadas = 0;

    // Rellenar la primera fila con letras o combinaciones de letras (A, B, C, ..., Z, AA, AB, ...)
    for (int j = 1; j <= columnas; j++) {
        matriz[0][j] = convertirColumnaAString(j);  // Llamar a la función para convertir el índice a columna
    }

    // Rellenar la primera columna con números (1, 2, 3, ...)
    for (int i = 1; i <= filas; i++) {
        matriz[i][0] = String.valueOf(i);
    }

    // Rellenar el resto de la matriz con ceros inicialmente
    for (int i = 1; i <= filas; i++) {
        for (int j = 1; j <= columnas; j++) {
            matriz[i][j] = "0";
        }
    }

    // Colocar minas (-1) en posiciones aleatorias
    while (minasColocadas < numeroMinas) {
        int fila = random.nextInt(filas) + 1;  // Índice aleatorio para la fila (1 a filas)
        int columna = random.nextInt(columnas) + 1;  // Índice aleatorio para la columna (1 a columnas)

        // Solo coloca la mina si la posición está vacía
        if (!matriz[fila][columna].equals("-1")) {
            matriz[fila][columna] = "-1";
            minasColocadas++;
        }
    }

    System.out.println("con pistas:");
    rellenarPistas(matriz);

    imprimirMatriz(matriz);  // Llamada al método para imprimir

    return matriz;
}
    
   /*public class records {

    public static void guardarRecord(String jugador, Duration duracion) {
        try (FileWriter writer = new FileWriter("records.txt", true)) {
            writer.write("Jugador: " + jugador + ", Tiempo: " + duracion.getSeconds() + " segundos\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void mostrarRecords() {
        try (BufferedReader reader = new BufferedReader(new FileReader("records.txt"))) {
            String linea;
            System.out.println("Historial de records:");
            while ((linea = reader.readLine()) != null) {
                System.out.println(linea);
            }
        } catch (IOException e) {
            System.out.println("No se pudo leer el archivo de records.");
            e.printStackTrace();
        }
    }
}*/
   

    private static void guardarRecord(String nombreJugador, long tiempoTotal) {
    try {
        BufferedReader br = new BufferedReader(new FileReader("records.txt"));
        String line;
        boolean jugadorExistente = false;
        
        // Revisamos si el jugador ya está en el archivo
        while ((line = br.readLine()) != null) {
            String[] parts = line.split(":");
            if (parts[0].equals(nombreJugador)) {
                jugadorExistente = true;
                break; // Si encontramos el jugador, no lo agregamos de nuevo
            }
        }
        br.close(); // Cerramos el BufferedReader

        // Si el jugador no existe, lo agregamos al archivo
        if (!jugadorExistente) {
            BufferedWriter bw = new BufferedWriter(new FileWriter("records.txt", true));
            bw.write(nombreJugador + ":" + tiempoTotal + " segundos\n");
            bw.close(); // Cerramos el BufferedWriter
        }

    } catch (IOException e) {
        e.printStackTrace();
    }
}

    private static void enviarRecords(PrintWriter pw) {
        try (BufferedReader reader = new BufferedReader(new FileReader("records.txt"))) {
            String line;
            pw.println("\n--- Records ---");
            while ((line = reader.readLine()) != null) {
                pw.println(line); // Enviar cada línea del archivo de records
            }
        } catch (IOException e) {
            pw.println("No se pudo leer el archivo de records.");
        }
    }
    
    public static void vaciarArchivo() {
        try {
            // Crear un FileWriter en modo "sobre escritura" con un archivo vacío
            FileWriter writer = new FileWriter("records.txt");
            writer.close(); // Cierra el archivo vacío, borrando su contenido
            System.out.println("El archivo de records ha sido vaciado.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        try {
            ServerSocket s = new ServerSocket(1234);
            System.out.println("Servidor iniciado en el puerto " + s.getLocalPort());
            
            for (;;) {
                Socket cliente = s.accept();
                System.out.println("Cliente conectado desde: " + cliente.getInetAddress() + ": " + cliente.getPort());

                BufferedReader br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                // Enviar el menú al cliente
                PrintWriter pw = new PrintWriter(new OutputStreamWriter(cliente.getOutputStream()), true); // Auto-flush
                // Recibir el nombre del jugador
                String nombreJugador = br.readLine();
                System.out.println("Jugador conectado: " + nombreJugador);

                
               //---------------------------------------------------- ENVIAR EL MENU----------------------------------------------------
                String menu = "Escoge el nivel de dificultad: ";
                pw.println(menu); // Enviar el menú
                //----------------------------------------------------Leer el nivel enviado por el cliente desde el socket----------------------------------------------------
                //BufferedReader br = new BufferedReader(new InputStreamReader(cliente.getInputStream()));
                String nivel = br.readLine(); // Leer el nivel desde el cliente
                System.out.println("Recibiendo datos del cliente, nivel escogido: " + nivel);

                // Confirmación
                String confirmacion = "Hola, has escogido el nivel: " + nivel;
                pw.println(confirmacion); // Enviar la confirmación al cliente
                
                
                //ENTRADA Y SALIDA DE OBJETOS
                ObjectOutputStream oos = new ObjectOutputStream(cliente.getOutputStream());
                ObjectInputStream ois = new ObjectInputStream(cliente.getInputStream());
                
                //----------------------------------------------------ENTRA A UN SWITCH DONDE ENTRARÁ SEGUN EL NIVEL ----------------------------------------------------
                /*
                1. FACIIL
                2. INTERMEDIO
                3. DIFICIL
                */
                
                switch (nivel) {
                    case "1":
                        //HACE LA MATRIZ NUMERICA DE LA FACIL -> LA RELLENA DE 0 , -1 Y NUMEROS MAYORES A 0
                        String[][] matrizNumerica =  crearMatriz(9, 9, 9);
                        //HACE LA MATRIZ QUE SE MANDA A CLIENTE -> ESTA LLENA DE PURAS "-"
                        String[][] matrizCliente = generarMatrizOculta(10,10);
                        
                        //HACE UN OBJETO Y LE MANDA EL TABLERO DE matrizCliente
                        Tablero ob2 = new Tablero(null,null,matrizCliente.clone(),0);
                        oos.writeObject(ob2);
                        oos.flush();
                      
                       // PRIMERA MARCA DE TIEMPO
                        //AQUI SE LEERA LO QUE EL CLIENTE QUIERE HACER, SI USAR UNA BANDERA O DESTAPAR CASILLA
                        String accion;
                        Tablero ob2I;

                        do {
                            // Leer la acción del cliente
                            accion = br.readLine();
                            System.out.println("Recibiendo datos del cliente, acción escogida: " + accion);



                            // Si la opción es "1", procesar las coordenadas
                            if (accion.equals("1")) {
                                                                // Leer el objeto Tablero del cliente con las coordenadas
                                ob2I = (Tablero) ois.readObject();
                                System.out.println("x:" + ob2I.getX() + " y:" + ob2I.getY());

                                // Ejecutar la lógica del tablero y obtener si se ha encontrado una mina
                                mina = EjecucionTablero(ob2I.getX(), ob2I.getY(), matrizNumerica, matrizCliente, mina);
                                imprimirMatriz(matrizNumerica);
                                    
                                
                                // Actualizar el tablero del cliente
                                if(mina==1){
                                    for(int i=1; i<matrizCliente.length;i++){
                                        for(int j=1; j<matrizCliente[2].length;j++){
                                            if (matrizNumerica[i][j].equals("-1")){
                                            matrizCliente[i][j]=matrizNumerica[i][j];
                                        }
                                    }
                                }
                                }
                                  
                                
                                String[][] nuevaMatriz = new String[matrizCliente.length][];
                                
                                    for (int i = 0; i < matrizCliente.length; i++) {
                                        nuevaMatriz[i] = matrizCliente[i].clone();
                                    }
                                ganar = verificarVictoria(matrizNumerica, nuevaMatriz);
         

                                // Enviar el tablero actualizado al cliente
                                Tablero ob5 = new Tablero(null, null, nuevaMatriz,ganar);
                                oos.writeObject(ob5);
                                oos.flush();
                            } else if(accion.equals("2")) {
                                // Leer el objeto Tablero del cliente con las coordenadas
                                ob2I = (Tablero) ois.readObject();
                                System.out.println("x:" + ob2I.getX() + " y:" + ob2I.getY());
                                banderas(ob2I.getX(), ob2I.getY(),matrizCliente,2);
                                System.out.println("El cliente ha escogido insertar bandera.");
                                String[][] nuevaMatriz = new String[matrizCliente.length][];
                                
                                for (int i = 0; i < matrizCliente.length; i++) {
                                        nuevaMatriz[i] = matrizCliente[i].clone();
                                }
                                
                                // Enviar el tablero actualizado al cliente
                                Tablero ob5 = new Tablero(null, null, nuevaMatriz,ganar);
                                oos.writeObject(ob5);
                                oos.flush();
                                
                                // Lógica adicional para insertar bandera si es necesario
                            } else if (accion.equals("3")) {
                                                                // Leer el objeto Tablero del cliente con las coordenadas
                                ob2I = (Tablero) ois.readObject();
                                System.out.println("x:" + ob2I.getX() + " y:" + ob2I.getY());
                                banderas(ob2I.getX(), ob2I.getY(),matrizCliente,3);
                                System.out.println("El cliente ha escogido quitar bandera.");
                                String[][] nuevaMatriz = new String[matrizCliente.length][];
                                
                                for (int i = 0; i < matrizCliente.length; i++) {
                                        nuevaMatriz[i] = matrizCliente[i].clone();
                                }
                                // Enviar el tablero actualizado al cliente
                                Tablero ob5 = new Tablero(null, null, nuevaMatriz,ganar);
                                oos.writeObject(ob5);
                                oos.flush();
                                
                                // Lógica adicional para insertar bandera si es necesario
                            } else{
                                System.out.println("Opción inválida recibida del cliente.");
                            }

                            // Enviar el valor de mina al cliente
                            pw.println(mina);  // Enviamos el valor de mina como un string
                            pw.flush();
                            
    

                        } while (mina == 0 && ganar ==0); // El bucle continúa mientras no haya encontrado una mina
                            long tiempoTotal = Long.parseLong(br.readLine());
                    System.out.println("Tiempo de partida: " + tiempoTotal + " segundos");
                    if (ganar == 1){
                    // Guardar el récord en el archivo
                    guardarRecord(nombreJugador, tiempoTotal);
                    // Enviar confirmación al cliente
                    pw.println("¡Tu record fue guardado!");}
                    
                    enviarRecords(pw);  // Envía los records al cliente
                    

                    // Cerrar flujos y socket
                    br.close();
                    pw.close();
                              
                    break;  // Termina el caso, evita que siga ejecutando otros casos
                    case "2":
                         // CREAR LA MATRIZ NUMÉRICA DEL NIVEL INTERMEDIO -> LA RELLENA DE 0, -1 Y NÚMEROS MAYORES A 0
                            String[][] matrizNumerica2 = crearMatriz(16, 16, 40);  // Tamaño 16x16 con 40 minas
                            // CREAR LA MATRIZ QUE SE MANDA AL CLIENTE -> ESTA LLENA DE PURAS "-"
                            String[][] matrizCliente2 = generarMatrizOculta(17, 17);  // Tamaño 16x16

                            // HACER UN OBJETO Y MANDAR EL TABLERO matrizCliente AL CLIENTE
                            Tablero intermedio = new Tablero(null, null, matrizCliente2.clone(), 0);
                            oos.writeObject(intermedio);
                            oos.flush();

                            // LEER LO QUE EL CLIENTE QUIERE HACER: SI USAR UNA BANDERA O DESTAPAR CASILLA
                            String accion2;
                            Tablero obIntermedioInput;
                            do {
                                // LEER LA ACCIÓN DEL CLIENTE
                                accion2 = br.readLine();
                                System.out.println("Recibiendo datos del cliente, acción escogida: " + accion2);

                                // SI LA OPCIÓN ES "1", PROCESAR COORDENADAS
                                if (accion2.equals("1")) {
                                    // LEER EL OBJETO TABLERO DEL CLIENTE CON LAS COORDENADAS
                                    obIntermedioInput = (Tablero) ois.readObject();
                                    System.out.println("x:" + obIntermedioInput.getX() + " y:" + obIntermedioInput.getY());

                                    // EJECUTAR LA LÓGICA DEL TABLERO Y VERIFICAR SI SE ENCONTRÓ UNA MINA
                                    mina = EjecucionTablero(obIntermedioInput.getX(), obIntermedioInput.getY(), matrizNumerica2, matrizCliente2, mina);
                                    imprimirMatriz(matrizNumerica2);

                                    // ACTUALIZAR EL TABLERO DEL CLIENTE
                                    if (mina == 1) {
                                        for (int i = 0; i < matrizCliente2.length; i++) {
                                            for (int j = 0; j < matrizCliente2[i].length; j++) {
                                                if (matrizNumerica2[i][j].equals("-1")) {
                                                    matrizCliente2[i][j] = matrizNumerica2[i][j];
                                                }
                                            }
                                        }
                                    }

                                    // CLONAR MATRIZ PARA ENVIAR AL CLIENTE
                                    String[][] nuevaMatriz = new String[matrizCliente2.length][];
                                    for (int i = 0; i < matrizCliente2.length; i++) {
                                        nuevaMatriz[i] = matrizCliente2[i].clone();
                                    }

                                    // VERIFICAR SI EL JUGADOR GANÓ
                                    ganar = verificarVictoria(matrizNumerica2, nuevaMatriz);

                                    // ENVIAR EL TABLERO ACTUALIZADO AL CLIENTE
                                    Tablero ob5 = new Tablero(null, null, nuevaMatriz, ganar);
                                    oos.writeObject(ob5);
                                    oos.flush();

                                } else if (accion2.equals("2")) {
                                    // LEER EL OBJETO TABLERO DEL CLIENTE CON LAS COORDENADAS
                                    obIntermedioInput = (Tablero) ois.readObject();
                                    System.out.println("x:" + obIntermedioInput.getX() + " y:" + obIntermedioInput.getY());
                                    banderas(obIntermedioInput.getX(), obIntermedioInput.getY(), matrizCliente2, 2);
                                    System.out.println("El cliente ha escogido insertar bandera.");

                                    // CLONAR MATRIZ Y ENVIARLA AL CLIENTE
                                    String[][] nuevaMatriz = new String[matrizCliente2.length][];
                                    for (int i = 0; i < matrizCliente2.length; i++) {
                                        nuevaMatriz[i] = matrizCliente2[i].clone();
                                    }
                                    Tablero ob5 = new Tablero(null, null, nuevaMatriz, ganar);
                                    oos.writeObject(ob5);
                                    oos.flush();

                                } else if (accion2.equals("3")) {
                                    // LEER EL OBJETO TABLERO DEL CLIENTE CON LAS COORDENADAS
                                    ob2I = (Tablero) ois.readObject();
                                    System.out.println("x:" + ob2I.getX() + " y:" + ob2I.getY());
                                    banderas(ob2I.getX(), ob2I.getY(), matrizCliente2, 3);
                                    System.out.println("El cliente ha escogido quitar bandera.");

                                    // CLONAR MATRIZ Y ENVIARLA AL CLIENTE
                                    String[][] nuevaMatriz = new String[matrizCliente2.length][];
                                    for (int i = 0; i < matrizCliente2.length; i++) {
                                        nuevaMatriz[i] = matrizCliente2[i].clone();
                                    }
                                    Tablero ob5 = new Tablero(null, null, nuevaMatriz, ganar);
                                    oos.writeObject(ob5);
                                    oos.flush();

                                } else {
                                    System.out.println("Opción inválida recibida del cliente.");
                                }

                                // ENVIAR EL VALOR DE MINA AL CLIENTE
                                pw.println(mina);  // Enviar el valor de mina como un string
                                pw.flush();

                            } while (mina == 0 && ganar == 0);  // EL BUCLE CONTINÚA MIENTRAS NO HAYA MINA NI VICTORIA
                        break;  
                    // Puedes agregar más casos si es necesario
                    case "3":
                        // CREAR LA MATRIZ NUMÉRICA DEL NIVEL INTERMEDIO -> LA RELLENA DE 0, -1 Y NÚMEROS MAYORES A 0
                            String[][] matrizNumerica3 = crearMatriz(16, 30, 99);  // Tamaño 16x16 con 40 minas
                            // CREAR LA MATRIZ QUE SE MANDA AL CLIENTE -> ESTA LLENA DE PURAS "-"
                            String[][] matrizCliente3 = generarMatrizOculta(17, 31);  // Tamaño 16x30

                            // HACER UN OBJETO Y MANDAR EL TABLERO matrizCliente AL CLIENTE
                            Tablero dificil = new Tablero(null, null, matrizCliente3.clone(), 0);
                            oos.writeObject(dificil);
                            oos.flush();

                            // LEER LO QUE EL CLIENTE QUIERE HACER: SI USAR UNA BANDERA O DESTAPAR CASILLA
                            String accion3;
                            Tablero obdificilInput;
                            do {
                                // LEER LA ACCIÓN DEL CLIENTE
                                accion3 = br.readLine();
                                System.out.println("Recibiendo datos del cliente, acción escogida: " + accion3);

                                // SI LA OPCIÓN ES "1", PROCESAR COORDENADAS
                                if (accion3.equals("1")) {
                                    // LEER EL OBJETO TABLERO DEL CLIENTE CON LAS COORDENADAS
                                    obdificilInput = (Tablero) ois.readObject();
                                    System.out.println("x:" + obdificilInput.getX() + " y:" + obdificilInput.getY());

                                    // EJECUTAR LA LÓGICA DEL TABLERO Y VERIFICAR SI SE ENCONTRÓ UNA MINA
                                    mina = EjecucionTablero(obdificilInput.getX(), obdificilInput.getY(), matrizNumerica3, matrizCliente3, mina);
                                    imprimirMatriz(matrizNumerica3);

                                    // ACTUALIZAR EL TABLERO DEL CLIENTE
                                    if (mina == 1) {
                                        for (int i = 0; i < matrizCliente3.length; i++) {
                                            for (int j = 0; j < matrizCliente3[i].length; j++) {
                                                if (matrizNumerica3[i][j].equals("-1")) {
                                                    matrizCliente3[i][j] = matrizNumerica3[i][j];
                                                }
                                            }
                                        }
                                    }

                                    // CLONAR MATRIZ PARA ENVIAR AL CLIENTE
                                    String[][] nuevaMatriz = new String[matrizCliente3.length][];
                                    for (int i = 0; i < matrizCliente3.length; i++) {
                                        nuevaMatriz[i] = matrizCliente3[i].clone();
                                    }

                                    // VERIFICAR SI EL JUGADOR GANÓ
                                    ganar = verificarVictoria(matrizNumerica3, nuevaMatriz);

                                    // ENVIAR EL TABLERO ACTUALIZADO AL CLIENTE
                                    Tablero ob5 = new Tablero(null, null, nuevaMatriz, ganar);
                                    oos.writeObject(ob5);
                                    oos.flush();

                                } else if (accion3.equals("2")) {
                                    // LEER EL OBJETO TABLERO DEL CLIENTE CON LAS COORDENADAS
                                    obdificilInput = (Tablero) ois.readObject();
                                    System.out.println("x:" + obdificilInput.getX() + " y:" + obdificilInput.getY());
                                    banderas(obdificilInput.getX(), obdificilInput.getY(), matrizCliente3, 2);
                                    System.out.println("El cliente ha escogido insertar bandera.");

                                    // CLONAR MATRIZ Y ENVIARLA AL CLIENTE
                                    String[][] nuevaMatriz = new String[matrizCliente3.length][];
                                    for (int i = 0; i < matrizCliente3.length; i++) {
                                        nuevaMatriz[i] = matrizCliente3[i].clone();
                                    }
                                    Tablero ob5 = new Tablero(null, null, nuevaMatriz, ganar);
                                    oos.writeObject(ob5);
                                    oos.flush();

                                } else if (accion3.equals("3")) {
                                    // LEER EL OBJETO TABLERO DEL CLIENTE CON LAS COORDENADAS
                                    ob2I = (Tablero) ois.readObject();
                                    System.out.println("x:" + ob2I.getX() + " y:" + ob2I.getY());
                                    banderas(ob2I.getX(), ob2I.getY(), matrizCliente3, 3);
                                    System.out.println("El cliente ha escogido quitar bandera.");

                                    // CLONAR MATRIZ Y ENVIARLA AL CLIENTE
                                    String[][] nuevaMatriz = new String[matrizCliente3.length][];
                                    for (int i = 0; i < matrizCliente3.length; i++) {
                                        nuevaMatriz[i] = matrizCliente3[i].clone();
                                    }
                                    Tablero ob5 = new Tablero(null, null, nuevaMatriz, ganar);
                                    oos.writeObject(ob5);
                                    oos.flush();

                                } else {
                                    System.out.println("Opción inválida recibida del cliente.");
                                }

                                // ENVIAR EL VALOR DE MINA AL CLIENTE
                                pw.println(mina);  // Enviar el valor de mina como un string
                                pw.flush();

                            } while (mina == 0 && ganar == 0);  // EL BUCLE CONTINÚA MIENTRAS NO HAYA MINA NI VICTORIA
                        break;
                    default:
                        break;
                }

                // Cerrar el socket después de procesar el cliente
                cliente.close();
                pw.close();
                br.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
