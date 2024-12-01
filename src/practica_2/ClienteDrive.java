package practica_2;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;

/**
 *
 * @author cesar
 */
public class ClienteDrive {

    private static String rutaActual = "/"; // Comenzamos en la carpeta raíz
    private static Map<Integer, byte[]> bufferRecepcion = new HashMap<>(); // Almacenar los paquetes recibidos

    public static void main(String[] args) {
        try {
            // Paso 1: Crear el socket para el cliente
            int puertoServidor = 1234; // Puerto del servidor
            String direccionServidor = "127.0.0.1"; // Dirección del servidor (puede cambiarse)
            DatagramSocket socketCliente = new DatagramSocket(); // Creamos el socket del cliente
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            while (true) {
                listarDirectorio(socketCliente, direccionServidor, puertoServidor);

                System.out.println("Elija un recurso ingresando el número correspondiente:");
                int opcionSeleccionada = Integer.parseInt(reader.readLine().trim());
                manejarSeleccion(socketCliente, direccionServidor, puertoServidor, opcionSeleccionada, reader);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void listarDirectorio(DatagramSocket socketCliente, String direccionServidor, int puertoServidor) throws IOException {
        String comando = "LIST_DIRECTORY:" + rutaActual; // Comando para solicitar la lista de la carpeta actual
        byte[] bufferComando = comando.getBytes(); // Convertimos la cadena a bytes

        InetAddress direccionIP = InetAddress.getByName(direccionServidor);
        DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, puertoServidor);
        socketCliente.send(paquete); // Enviar el comando al servidor
        System.out.println("Comando 'LIST_DIRECTORY' enviado al servidor...");

        byte[] bufferRespuesta = new byte[1024]; // Buffer para recibir la respuesta del servidor
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
        socketCliente.receive(paqueteRespuesta); // Esperar la respuesta del servidor

        String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        System.out.println("\nContenido del Drive (" + rutaActual + "):");
        System.out.println(respuesta);

        String[] recursos = respuesta.split("\n");
        for (int i = 0; i < recursos.length; i++) {
            System.out.println((i + 1) + ") " + recursos[i]);
        }

        int indiceAdicional = recursos.length + 1;
        System.out.println(indiceAdicional + ") Crear una nueva carpeta");
        System.out.println((indiceAdicional + 1) + ") Subir un archivo");
    }

private static void manejarSeleccion(DatagramSocket socketCliente, String direccionServidor, int puertoServidor, int opcionSeleccionada, BufferedReader reader) throws IOException {
    InetAddress direccionIP = InetAddress.getByName(direccionServidor);
    String[] recursos = listarRecursos(socketCliente, direccionServidor, puertoServidor);
    int indiceAdicional = recursos.length + 1;

    if (opcionSeleccionada >= 1 && opcionSeleccionada <= recursos.length) {
        String recursoSeleccionado = recursos[opcionSeleccionada - 1];
        System.out.println("Seleccionó: " + recursoSeleccionado);

        if (recursoSeleccionado.startsWith("[C]")) {
            manejarCarpeta(socketCliente, direccionIP, puertoServidor, reader, recursoSeleccionado);
        } else if (recursoSeleccionado.startsWith("[A]")) {
            String nombreArchivo = recursoSeleccionado.substring(3).trim(); // Elimina "[A]" del inicio
            manejarArchivo(reader, nombreArchivo, socketCliente, direccionIP);
        } else if (recursoSeleccionado.startsWith("[..]")) {
            rutaActual = navegarAtras();
        }
    } else if (opcionSeleccionada == indiceAdicional) {
        crearCarpeta(socketCliente, direccionIP, puertoServidor, reader);
    } else if (opcionSeleccionada == (indiceAdicional + 1)) {
        subirArchivo(socketCliente, direccionIP, puertoServidor);
    } else {
        System.out.println("Opción no válida, intente nuevamente.");
    }
}


    private static String[] listarRecursos(DatagramSocket socketCliente, String direccionServidor, int puertoServidor) throws IOException {
        String comando = "LIST_DIRECTORY:" + rutaActual;
        byte[] bufferComando = comando.getBytes();

        InetAddress direccionIP = InetAddress.getByName(direccionServidor);
        DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, puertoServidor);
        socketCliente.send(paquete);

        byte[] bufferRespuesta = new byte[1024];
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
        socketCliente.receive(paqueteRespuesta);

        String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
        return respuesta.split("\n");
    }

    private static void manejarCarpeta(DatagramSocket socketCliente, InetAddress direccionIP, int puertoServidor, BufferedReader reader, String recursoSeleccionado) throws IOException {
        System.out.println("¿Qué desea hacer con la carpeta?");
        System.out.println("1) Abrir carpeta");
        System.out.println("2) borrar carpeta");

        System.out.println("3) Cancelar");

        int accionSeleccionada = Integer.parseInt(reader.readLine().trim());
        if (accionSeleccionada == 1) {
            String nombreCarpeta = recursoSeleccionado.substring(3).trim();
            rutaActual += nombreCarpeta + "/";
        } else if(accionSeleccionada ==2){
           String nombreCarpeta = recursoSeleccionado.substring(3).trim();
            borrarCarpeta(nombreCarpeta,socketCliente, direccionIP,puertoServidor, reader);
        }
        else {
            System.out.println("Acción cancelada.");
        }
    }

    private static void manejarArchivo(BufferedReader reader, String archivoSeleccionado, DatagramSocket socketCliente, InetAddress direccionIP) throws IOException {
    System.out.println("¿Qué desea hacer con el archivo seleccionado: " + archivoSeleccionado + "?");
    System.out.println("1) Cambiar nombre");
    System.out.println("2) Descargar");
    System.out.println("3) Eliminar");
    System.out.println("4) Cancelar");

    int accionSeleccionada = Integer.parseInt(reader.readLine().trim());
    System.out.println("Acción seleccionada: " + accionSeleccionada);

    if (accionSeleccionada == 1) {
        // Renombrar archivo
        System.out.println("Ingrese el nuevo nombre para el archivo:");
        String nuevoNombre = reader.readLine().trim();

        renombrarArchivo(archivoSeleccionado, nuevoNombre);
    } else if (accionSeleccionada == 2) {
        descargarArchivo(archivoSeleccionado,socketCliente, direccionIP);
    } else if (accionSeleccionada == 3) {
        eliminarArchivo(archivoSeleccionado);
    } else if (accionSeleccionada == 4) {
        System.out.println("Acción cancelada.");
    } else {
        System.out.println("Opción no válida.");
    }
}

    private static void crearCarpeta(DatagramSocket socketCliente, InetAddress direccionIP, int puertoServidor, BufferedReader reader) throws IOException {
        System.out.println("Ingrese el nombre de la nueva carpeta:");
        String nombreCarpeta = reader.readLine().trim();

        String comandoCrearCarpeta = "CREATE_FOLDER:" + rutaActual + nombreCarpeta;
        byte[] bufferComandoCrearCarpeta = comandoCrearCarpeta.getBytes();
        DatagramPacket paqueteCrearCarpeta = new DatagramPacket(bufferComandoCrearCarpeta, bufferComandoCrearCarpeta.length, direccionIP, puertoServidor);
        socketCliente.send(paqueteCrearCarpeta);
        System.out.println("Comando 'CREATE_FOLDER' enviado al servidor...");

        byte[] bufferRespuestaCrearCarpeta = new byte[1024];
        DatagramPacket paqueteRespuestaCrearCarpeta = new DatagramPacket(bufferRespuestaCrearCarpeta, bufferRespuestaCrearCarpeta.length);
        socketCliente.receive(paqueteRespuestaCrearCarpeta);

        String respuestaCrearCarpeta = new String(paqueteRespuestaCrearCarpeta.getData(), 0, paqueteRespuestaCrearCarpeta.getLength());
        System.out.println("Respuesta del servidor: " + respuestaCrearCarpeta);
    }
    
 
    private static void borrarCarpeta(String nombreCarpeta, DatagramSocket socketCliente, InetAddress direccionIP, int puertoServidor, BufferedReader reader) throws IOException {
        String comandoCrearCarpeta = "DELETE_FOLDER:" + rutaActual + nombreCarpeta;
        byte[] bufferComandoCrearCarpeta = comandoCrearCarpeta.getBytes();
        DatagramPacket paqueteCrearCarpeta = new DatagramPacket(bufferComandoCrearCarpeta, bufferComandoCrearCarpeta.length, direccionIP, puertoServidor);
        socketCliente.send(paqueteCrearCarpeta);
        System.out.println("Comando 'DELETE_FOLDER' enviado al servidor...");

        byte[] bufferRespuestaCrearCarpeta = new byte[1024];
        DatagramPacket paqueteRespuestaCrearCarpeta = new DatagramPacket(bufferRespuestaCrearCarpeta, bufferRespuestaCrearCarpeta.length);
        socketCliente.receive(paqueteRespuestaCrearCarpeta);

        String respuestaCrearCarpeta = new String(paqueteRespuestaCrearCarpeta.getData(), 0, paqueteRespuestaCrearCarpeta.getLength());
        System.out.println("Respuesta del servidor: " + respuestaCrearCarpeta);
    }
    
    private static void renombrarArchivo(String nombreActual, String nuevoNombre) throws IOException {
    String comandoRenombrar = "RENAME_FILE:" + rutaActual + nombreActual + ":" + nuevoNombre;
    byte[] bufferComando = comandoRenombrar.getBytes();

    InetAddress direccionIP = InetAddress.getByName("127.0.0.1"); // Cambiar por la IP del servidor
    DatagramSocket socketCliente = new DatagramSocket(); // Reutilizar el socket existente si es posible
    DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, 1234);

    socketCliente.send(paquete);
    System.out.println("Comando 'RENAME_FILE' enviado al servidor...");

    byte[] bufferRespuesta = new byte[1024];
    DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
    socketCliente.receive(paqueteRespuesta);

    String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
    System.out.println("Respuesta del servidor: " + respuesta);
}

    private static void eliminarArchivo(String archivoSeleccionado) throws IOException {
    // Construir el comando DELETE_FILE
    String comandoEliminar = "DELETE_FILE:" + rutaActual + archivoSeleccionado;
    byte[] bufferComando = comandoEliminar.getBytes();

    InetAddress direccionIP = InetAddress.getByName("127.0.0.1"); // Cambiar por la IP del servidor
    DatagramSocket socketCliente = new DatagramSocket(); // Usar el socket adecuado
    DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, 1234);

    // Enviar el comando al servidor
    socketCliente.send(paquete);
    System.out.println("Comando 'DELETE_FILE' enviado al servidor...");

    // Recibir la respuesta del servidor
    byte[] bufferRespuesta = new byte[1024];
    DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
    socketCliente.receive(paqueteRespuesta);

    // Mostrar la respuesta del servidor
    String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
    System.out.println("Respuesta del servidor: " + respuesta);
}

    private static void descargarArchivo(String archivoSeleccionado, DatagramSocket socketCliente, InetAddress direccionIP) throws IOException {
    String comandoDescargar = "DOWNLOAD_FILE:" + rutaActual + archivoSeleccionado;
    byte[] bufferComando = comandoDescargar.getBytes();

    DatagramPacket paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, 1234);
    socketCliente.send(paquete);
    System.out.println("Comando 'DOWNLOAD_FILE' enviado al servidor...");

    // Recibir la respuesta del servidor
    byte[] bufferRespuesta = new byte[1024];
    DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length);
    socketCliente.receive(paqueteRespuesta);

    String respuesta = new String(paqueteRespuesta.getData(), 0, paqueteRespuesta.getLength());
    System.out.println("Respuesta del servidor (tamaño del archivo): " + respuesta);

    long tamanioArchivo = Long.parseLong(respuesta);
    String confirmacion = "Tamaño del archivo recibido";
    bufferComando = confirmacion.getBytes();
    paquete = new DatagramPacket(bufferComando, bufferComando.length, direccionIP, 1234);
    socketCliente.send(paquete);
    System.out.println("Confirmación enviada al servidor.");

    String rutaArchivo = "C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Practica_2\\DESCARGAS\\" + archivoSeleccionado;
    File archivoDestino = new File(rutaArchivo);
    FileOutputStream fos = new FileOutputStream(archivoDestino);

 int numeroPaqueteEsperado = 0;
        int totalPaquetes = (int) Math.ceil((double) tamanioArchivo / 1024);
        boolean archivoCompleto = false;

        while (!archivoCompleto) {
            try {
                byte[] bufferPaquete = new byte[1028]; // 1024 bytes de datos + 4 bytes para el número de secuencia
                DatagramPacket paqueteDatos = new DatagramPacket(bufferPaquete, bufferPaquete.length);
                socketCliente.receive(paqueteDatos);

                ByteArrayInputStream bais = new ByteArrayInputStream(paqueteDatos.getData());
                DataInputStream dis = new DataInputStream(bais);
                int numeroPaquete = dis.readInt();
                byte[] datos = new byte[paqueteDatos.getLength() - 4];
                dis.read(datos);

                if (numeroPaquete == numeroPaqueteEsperado) {
                    fos.write(datos);
                    fos.flush();
                    bufferRecepcion.put(numeroPaquete, datos);
                    numeroPaqueteEsperado++;
                } else {
                    System.out.println("Paquete fuera de secuencia. Esperado: " + numeroPaqueteEsperado + ", recibido: " + numeroPaquete);
                }

                // Enviar ACK para el paquete recibido
                String respuestaACK = "ACK:" + numeroPaquete;
                byte[] bufferACK = respuestaACK.getBytes();
                InetAddress direccionCliente = paqueteDatos.getAddress();
                int puertoCliente = paqueteDatos.getPort();

                DatagramPacket paqueteACK = new DatagramPacket(bufferACK, bufferACK.length, direccionCliente, puertoCliente);
                socketCliente.send(paqueteACK);
                System.out.println("ACK enviado para el paquete: " + numeroPaquete);

                // Verificar si se han recibido todos los paquetes
                if (numeroPaqueteEsperado >= totalPaquetes) {
                    archivoCompleto = true;
                    System.out.println("Archivo recibido completamente: " + rutaArchivo);
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Tiempo de espera agotado, esperando retransmisión del paquete: " + numeroPaqueteEsperado);
            }
        }
        fos.close();
}

    
    private static void subirArchivo(DatagramSocket socketCliente, InetAddress direccionIP, int puertoServidor) throws IOException {
        JFileChooser fileChooser = new JFileChooser();
        int seleccion = fileChooser.showOpenDialog(null);

        if (seleccion == JFileChooser.APPROVE_OPTION) {
            File archivo = fileChooser.getSelectedFile();
            String nombreArchivo = archivo.getName();
            long tamanioArchivo = archivo.length();

            // Enviar metadatos del archivo
            String comandoSubirArchivo = "UPLOAD_FILE:" + rutaActual + nombreArchivo + ":" + tamanioArchivo;
            byte[] bufferComandoSubirArchivo = comandoSubirArchivo.getBytes();
            DatagramPacket paqueteSubirArchivo = new DatagramPacket(bufferComandoSubirArchivo, bufferComandoSubirArchivo.length, direccionIP, puertoServidor);
            socketCliente.send(paqueteSubirArchivo);
            System.out.println("Comando 'UPLOAD_FILE' enviado al servidor...");

            // Fragmentar y enviar el archivo en partes utilizando ventana deslizante
            byte[] bufferArchivo = new byte[1024];
            FileInputStream fis = new FileInputStream(archivo);
            int bytesLeidos;
            int numeroPaquete = 0;
            int tamanioVentana = 4; // Tamaño de la ventana deslizante
            List<DatagramPacket> ventana = new ArrayList<>();

            while ((bytesLeidos = fis.read(bufferArchivo)) != -1) {
                // Crear el paquete con el número de secuencia y los datos del fragmento
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DataOutputStream dos = new DataOutputStream(baos);
                dos.writeInt(numeroPaquete);
                dos.write(bufferArchivo, 0, bytesLeidos);
                byte[] datosPaquete = baos.toByteArray();

                DatagramPacket paquete = new DatagramPacket(datosPaquete, datosPaquete.length, direccionIP, puertoServidor);
                ventana.add(paquete);

                // Enviar los paquetes de la ventana
                if (ventana.size() == tamanioVentana) {
                    enviarVentana(socketCliente, ventana);
                    ventana.clear();
                }
                numeroPaquete++;
            }

            // Enviar cualquier paquete restante en la ventana
            if (!ventana.isEmpty()) {
                enviarVentana(socketCliente, ventana);
            }

            fis.close();
            System.out.println("Archivo enviado correctamente.");
        } else {
            System.out.println("Operación de subida cancelada.");
        }
    }

    private static void enviarVentana(DatagramSocket socketCliente, List<DatagramPacket> ventana) throws IOException {
        for (DatagramPacket paquete : ventana) {
            socketCliente.send(paquete);
            System.out.println("Enviando paquete con número de secuencia: " + new DataInputStream(new ByteArrayInputStream(paquete.getData())).readInt());
            // Esperar el ACK del servidor para cada paquete
            esperarACK(socketCliente, paquete);
        }
    }

    private static void esperarACK(DatagramSocket socketCliente, DatagramPacket paquete) throws IOException {
        byte[] bufferACK = new byte[1024];
        DatagramPacket paqueteACK = new DatagramPacket(bufferACK, bufferACK.length);
        try {
            socketCliente.setSoTimeout(2000); // Establecer un tiempo de espera de 2 segundos
            socketCliente.receive(paqueteACK);
            String respuesta = new String(paqueteACK.getData(), 0, paqueteACK.getLength());
            if (!respuesta.equals("ACK:" + new DataInputStream(new ByteArrayInputStream(paquete.getData())).readInt())) {
                System.out.println("No se recibió el ACK esperado, reenviando paquete...");
                socketCliente.send(paquete); // Reenviar paquete si no se recibe el ACK correcto
            } else {
                System.out.println("ACK recibido para el paquete " + new DataInputStream(new ByteArrayInputStream(paquete.getData())).readInt());
            }
        } catch (SocketTimeoutException e) {
            System.out.println("Tiempo de espera agotado para el ACK, reenviando paquete...");
            socketCliente.send(paquete); // Reenviar paquete si se agota el tiempo de espera
        }
    }
    


    private static String navegarAtras() {
        if (!rutaActual.equals("/")) {
            int ultimaBarra = rutaActual.lastIndexOf("/", rutaActual.length() - 2);
            return rutaActual.substring(0, ultimaBarra + 1);
        } else {
            System.out.println("Ya estás en la carpeta raíz, no puedes retroceder más.");
            return rutaActual;
        }
    }
}