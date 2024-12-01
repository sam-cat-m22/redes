package practica_2;
import java.net.*;
import java.io.*;
import java.util.*;

/**
 *
 * @author cesar
 * @author Sam
 */
public class ServidorDrive {
    private static final int PUERTO = 1234;
    private static final String DIRECTORIO_RAIZ = "C:\\Users\\cesar\\OneDrive\\Documentos\\NetBeansProjects\\Practica_2\\DRIVE";
    private static final int TAMANIO_VENTANA = 4;
    private static Map<Integer, byte[]> bufferRecepcion = new HashMap<>(); // Almacenar los paquetes recibidos

public static void main(String[] args) {
    try {
        DatagramSocket socketServidor = new DatagramSocket(PUERTO);
        socketServidor.setSoTimeout(5000); // Configurar tiempo de espera de 5 segundos
        System.out.println("Servidor iniciado en el puerto " + PUERTO);

        while (true) {
            try {
                byte[] bufferRecibir = new byte[1024];
                DatagramPacket paqueteRecibido = new DatagramPacket(bufferRecibir, bufferRecibir.length);
                socketServidor.receive(paqueteRecibido);

                String comando = new String(paqueteRecibido.getData(), 0, paqueteRecibido.getLength());
                System.out.println("Comando recibido: " + comando);

                if (comando.startsWith("LIST_DIRECTORY")) {
                    listarContenido(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("CREATE_FOLDER")) {
                    crearCarpeta(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("UPLOAD_FILE")) {
                    manejarSubidaArchivo(socketServidor, paqueteRecibido, comando);
                } else if (comando.startsWith("NAVIGATE")) {
                    navegarCarpeta(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("DELETE_FILE")) {
                    eliminarArchivo(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("RENAME_FILE")) {
                    renombrarArchivo(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("DOWNLOAD_FILE")) {
                    descargarArchivo(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                } else if (comando.startsWith("DELETE_FOLDER")) {
                    borrarCarpeta(comando, paqueteRecibido, socketServidor, DIRECTORIO_RAIZ);
                }
            } catch (SocketTimeoutException e) {
                // Tiempo de espera agotado, continuar esperando nuevos paquetes
                System.out.println("Tiempo de espera agotado, esperando nuevos paquetes...");
            } catch (Exception e) {
                // Manejar otras excepciones y continuar el bucle principal
                System.err.println("Error manejando comando: " + e.getMessage());
                e.printStackTrace();
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    private static void listarContenido(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaSolicitada = partes.length > 1 ? partes[1] : "/";
            String rutaAbsoluta = directorioRaiz + rutaSolicitada.replace("/", "\\");

            File carpeta = new File(rutaAbsoluta);
            StringBuilder contenido = new StringBuilder();

            if (carpeta.exists() && carpeta.isDirectory()) {
                File[] archivos = carpeta.listFiles();
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        contenido.append("[C] ").append(archivo.getName()).append("\n");
                    } else if (archivo.isFile()) {
                        contenido.append("[A] ").append(archivo.getName()).append("\n");
                    }
                }
                contenido.append("[..] Regresar al nivel anterior\n"); // Agregar opción para regresar
            } else {
                contenido.append("ERROR: Ruta no válida o no encontrada.\n");
            }

            byte[] bufferRespuesta = contenido.toString().getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void crearCarpeta(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaNuevaCarpeta = partes.length > 1 ? partes[1] : "";
            String rutaAbsoluta = directorioRaiz + rutaNuevaCarpeta.replace("/", "\\");

            File nuevaCarpeta = new File(rutaAbsoluta);
            String respuesta;

            if (nuevaCarpeta.mkdirs()) {
                respuesta = "Carpeta creada con éxito: " + rutaNuevaCarpeta;
            } else {
                respuesta = "ERROR: No se pudo crear la carpeta en la ruta especificada.";
            }

            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
private static void borrarCarpeta(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
    try {
        // Parsear el comando para obtener la ruta de la carpeta
        String[] partes = comando.split(":");
        String rutaNuevaCarpeta = partes.length > 1 ? partes[1] : "";
        String rutaAbsoluta = directorioRaiz + rutaNuevaCarpeta.replace("/", "\\");

        File nuevaCarpeta = new File(rutaAbsoluta);
        String respuesta;

        // Verificar si la carpeta existe
        if (nuevaCarpeta.exists() && nuevaCarpeta.isDirectory()) {
            // Eliminar la carpeta y su contenido
            if (eliminarCarpetaRecursiva(nuevaCarpeta)) {
                respuesta = "Carpeta borrada con éxito: " + rutaNuevaCarpeta;
            } else {
                respuesta = "ERROR: No se pudo borrar la carpeta completamente.";
            }
        } else {
            respuesta = "ERROR: La carpeta especificada no existe.";
        }

        // Enviar respuesta al cliente
        byte[] bufferRespuesta = respuesta.getBytes();
        InetAddress direccionCliente = paqueteRecibido.getAddress();
        int puertoCliente = paqueteRecibido.getPort();

        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
        socketServidor.send(paqueteRespuesta);
        System.out.println("Respuesta enviada al cliente: " + respuesta);

    } catch (Exception e) {
        e.printStackTrace();
    }
}

// Función auxiliar recursiva para eliminar el contenido de una carpeta
private static boolean eliminarCarpetaRecursiva(File carpeta) {
    if (carpeta.isDirectory()) {
        // Obtener todos los archivos y subdirectorios
        File[] archivos = carpeta.listFiles();
        if (archivos != null) {
            for (File archivo : archivos) {
                // Eliminar archivos y subdirectorios recursivamente
                if (!eliminarCarpetaRecursiva(archivo)) {
                    return false; // Si falla al eliminar algo, retorna false
                }
            }
        }
    }
    // Eliminar la carpeta o archivo (cuando ya está vacía)
    return carpeta.delete();
}


    private static void manejarSubidaArchivo(DatagramSocket socketServidor, DatagramPacket paqueteRecibido, String comando) throws IOException {
        String[] partes = comando.split(":");
        String rutaArchivo = partes[1];
        long tamanioArchivo = Long.parseLong(partes[2]);

        System.out.println("Preparado para recibir archivo: " + rutaArchivo + " con tamaño: " + tamanioArchivo);
        File archivoDestino = new File(DIRECTORIO_RAIZ + rutaArchivo);
        FileOutputStream fos = new FileOutputStream(archivoDestino);

        int numeroPaqueteEsperado = 0;
        int totalPaquetes = (int) Math.ceil((double) tamanioArchivo / 1024);
        boolean archivoCompleto = false;

        while (!archivoCompleto) {
            try {
                byte[] bufferPaquete = new byte[1028]; // 1024 bytes de datos + 4 bytes para el número de secuencia
                DatagramPacket paqueteDatos = new DatagramPacket(bufferPaquete, bufferPaquete.length);
                socketServidor.receive(paqueteDatos);

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
                socketServidor.send(paqueteACK);
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


    private static void navegarCarpeta(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaNavegar = partes.length > 1 ? partes[1] : "";
            String rutaAbsoluta;

            // Verificar si es para regresar al nivel anterior
            if (rutaNavegar.equals("..")) {
                int ultimaBarra = directorioRaiz.lastIndexOf("\\");
                rutaAbsoluta = (ultimaBarra > 0) ? directorioRaiz.substring(0, ultimaBarra) : directorioRaiz;
            } else {
                rutaAbsoluta = directorioRaiz + "\\" + rutaNavegar;
            }

            File carpeta = new File(rutaAbsoluta);
            StringBuilder contenido = new StringBuilder();

            if (carpeta.exists() && carpeta.isDirectory()) {
                File[] archivos = carpeta.listFiles();
                for (File archivo : archivos) {
                    if (archivo.isDirectory()) {
                        contenido.append("[C] ").append(archivo.getName()).append("\n");
                    } else if (archivo.isFile()) {
                        contenido.append("[A] ").append(archivo.getName()).append("\n");
                    }
                }
                contenido.append("[..] Regresar al nivel anterior\n"); // Agregar opción para regresar
            } else {
                contenido.append("ERROR: Ruta no válida o no encontrada.\n");
            }

            byte[] bufferRespuesta = contenido.toString().getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente...");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void eliminarArchivo(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            String rutaArchivo = partes.length > 1 ? partes[1] : "";
            String rutaAbsoluta = directorioRaiz + rutaArchivo.replace("/", "\\");

            File archivo = new File(rutaAbsoluta);
            String respuesta;

            if (archivo.exists() && archivo.isFile() && archivo.delete()) {
                respuesta = "Archivo eliminado con éxito: " + rutaArchivo;
            } else {
                respuesta = "ERROR: No se pudo eliminar el archivo especificado.";
            }

            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente: " + respuesta);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void descargarArchivo(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
    try {
        InetAddress direccionCliente = paqueteRecibido.getAddress();
        int puertoCliente = paqueteRecibido.getPort();

        // Parsear el comando y obtener la ruta del archivo
        String[] partes = comando.split(":");
        String rutaArchivo = partes.length > 1 ? partes[1] : "";
        String rutaAbsoluta = directorioRaiz + rutaArchivo.replace("/", "\\");

        File archivo = new File(rutaAbsoluta);
        if (!archivo.exists()) {
            System.out.println("Error: El archivo no existe.");
            String respuestaError = "ERROR:Archivo no encontrado";
            socketServidor.send(new DatagramPacket(respuestaError.getBytes(), respuestaError.getBytes().length, direccionCliente, puertoCliente));
            return;
        }

        long tamanioArchivo = archivo.length();
        String tamanioArchivoStr = String.valueOf(tamanioArchivo);

        // Enviar tamaño del archivo al cliente
        byte[] bufferRespuesta = tamanioArchivoStr.getBytes();
        DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
        socketServidor.send(paqueteRespuesta);
        System.out.println("Tamaño del archivo enviado al cliente: " + tamanioArchivo);

        // Esperar confirmación del cliente
        byte[] bufferConfirmacion = new byte[1024];
        DatagramPacket paqueteConfirmacion = new DatagramPacket(bufferConfirmacion, bufferConfirmacion.length);
        socketServidor.receive(paqueteConfirmacion);
        String confirmacion = new String(paqueteConfirmacion.getData(), 0, paqueteConfirmacion.getLength()).trim();
        System.out.println("Confirmación recibida del cliente: " + confirmacion);


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

                DatagramPacket paquete = new DatagramPacket(datosPaquete, datosPaquete.length, direccionCliente, puertoCliente);
                ventana.add(paquete);

                // Enviar los paquetes de la ventana
                if (ventana.size() == tamanioVentana) {
                    enviarVentana(socketServidor, ventana);
                    ventana.clear();
                }
                numeroPaquete++;
            }

            // Enviar cualquier paquete restante en la ventana
            if (!ventana.isEmpty()) {
                enviarVentana(socketServidor, ventana);
            }

            fis.close();
            System.out.println("Archivo enviado correctamente.");
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    

private static void enviarVentana(DatagramSocket socketServidor, List<DatagramPacket> ventana) throws IOException {
    for (DatagramPacket paquete : ventana) {
        socketServidor.send(paquete);
        System.out.println("Enviando paquete con número de secuencia: " + new DataInputStream(new ByteArrayInputStream(paquete.getData())).readInt());
        esperarACK(socketServidor, paquete);
    }
}

private static void esperarACK(DatagramSocket socketServidor, DatagramPacket paquete) throws IOException {
    byte[] bufferACK = new byte[1024];
    DatagramPacket paqueteACK = new DatagramPacket(bufferACK, bufferACK.length);
    try {
        socketServidor.setSoTimeout(2000); // Tiempo de espera de 2 segundos
        socketServidor.receive(paqueteACK);
        String respuesta = new String(paqueteACK.getData(), 0, paqueteACK.getLength());
        if (!respuesta.equals("ACK:" + new DataInputStream(new ByteArrayInputStream(paquete.getData())).readInt())) {
            System.out.println("No se recibió el ACK esperado, reenviando paquete...");
            socketServidor.send(paquete); // Reenviar paquete si no se recibe el ACK correcto
        } else {
            System.out.println("ACK recibido para el paquete " + new DataInputStream(new ByteArrayInputStream(paquete.getData())).readInt());
        }
    } catch (SocketTimeoutException e) {
        System.out.println("Tiempo de espera agotado para el ACK, reenviando paquete...");
        socketServidor.send(paquete); // Reenviar paquete si se agota el tiempo de espera
    }
}


    private static void renombrarArchivo(String comando, DatagramPacket paqueteRecibido, DatagramSocket socketServidor, String directorioRaiz) {
        try {
            String[] partes = comando.split(":");
            if (partes.length < 3) {
                String errorRespuesta = "ERROR: Comando incorrecto para renombrar archivo.";
                byte[] bufferError = errorRespuesta.getBytes();
                InetAddress direccionCliente = paqueteRecibido.getAddress();
                int puertoCliente = paqueteRecibido.getPort();
                DatagramPacket paqueteError = new DatagramPacket(bufferError, bufferError.length, direccionCliente, puertoCliente);
                socketServidor.send(paqueteError);
                return;
            }

            String rutaArchivo = partes[1];
            String nuevoNombre = partes[2];
            String rutaAbsoluta = directorioRaiz + rutaArchivo.replace("/", "\\");
            File archivo = new File(rutaAbsoluta);
            String respuesta;

            if (archivo.exists() && archivo.isFile()) {
                String rutaPadre = archivo.getParent();
                File archivoRenombrado = new File(rutaPadre, nuevoNombre);
                if (archivo.renameTo(archivoRenombrado)) {
                    respuesta = "Archivo renombrado con éxito a: " + nuevoNombre;
                } else {
                    respuesta = "ERROR: No se pudo renombrar el archivo.";
                }
            } else {
                respuesta = "ERROR: Archivo no encontrado o ruta inválida.";
            }

            byte[] bufferRespuesta = respuesta.getBytes();
            InetAddress direccionCliente = paqueteRecibido.getAddress();
            int puertoCliente = paqueteRecibido.getPort();

            DatagramPacket paqueteRespuesta = new DatagramPacket(bufferRespuesta, bufferRespuesta.length, direccionCliente, puertoCliente);
            socketServidor.send(paqueteRespuesta);
            System.out.println("Respuesta enviada al cliente: " + respuesta);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}